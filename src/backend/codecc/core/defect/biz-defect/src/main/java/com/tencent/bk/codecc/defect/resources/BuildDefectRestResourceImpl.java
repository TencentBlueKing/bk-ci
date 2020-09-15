/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.BuildDefectRestResource;
import com.tencent.bk.codecc.defect.service.CCNUploadStatisticService;
import com.tencent.bk.codecc.defect.service.CLOCUploadStatisticService;
import com.tencent.bk.codecc.defect.service.DUPCUploadStatisticService;
import com.tencent.bk.codecc.defect.service.UploadRepositoriesService;
import com.tencent.bk.codecc.defect.vo.CCNUploadStatisticVO;
import com.tencent.bk.codecc.defect.vo.UploadCLOCStatisticVO;
import com.tencent.bk.codecc.defect.vo.UploadDUPCStatisticVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.bk.codecc.defect.vo.coderepository.UploadRepositoriesVO;
import com.tencent.bk.codecc.task.api.ServiceBaseDataResource;
import com.tencent.bk.codecc.task.vo.BaseDataVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.external.AuthTaskService;
import com.tencent.devops.common.client.Client;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.constant.RedisKeyConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.service.utils.SpringContextUtil;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 工具侧上报告警的接口实现
 *
 * @version V1.0
 * @date 2019/5/13
 */
@RestResource
public class BuildDefectRestResourceImpl implements BuildDefectRestResource
{
    private static Logger logger = LoggerFactory.getLogger(BuildDefectRestResourceImpl.class);

    private static final String PREFIX_CUSTOM_TOOL = "CUSTOMTOOL";

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private CCNUploadStatisticService ccnUploadStatisticService;

    @Autowired
    private DUPCUploadStatisticService dupcUploadStatisticService;

    @Autowired
    private CLOCUploadStatisticService clocUploadStatisticService;

    @Autowired
    private UploadRepositoriesService uploadRepositoriesService;

    @Autowired
    private AuthTaskService authTaskService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private Client client;

    @Override
    @Async("asyncReportDefectExecutor")
    public CodeCCResult asyncReportDefects(UploadDefectVO uploadDefectVO)
    {
        uploadDefectVO.setToolName(uploadDefectVO.getToolName().toUpperCase());
        String reportKey = String.format("%s:%s:%s:%s", RedisKeyConstants.KEY_REPORT_DEFECT, uploadDefectVO.getStreamName(),
                uploadDefectVO.getToolName(), uploadDefectVO.getBuildId());
        //180秒有效时间
        redisTemplate.opsForValue().set(reportKey, ComConstants.DefectReportStatus.PROCESSING.name(), 1, TimeUnit.HOURS);
        try{
            reportDefects(uploadDefectVO);
//            deleteScmFiles(uploadDefectVO);
            redisTemplate.opsForValue().set(reportKey, ComConstants.DefectReportStatus.SUCCESS.name(), 1, TimeUnit.HOURS);
        } catch (Exception e)
        {
            logger.error("upload defect report fail! task id: {}, tool_name : {}", uploadDefectVO.getTaskId(), uploadDefectVO.getToolName(), e);
            redisTemplate.opsForValue().set(reportKey, ComConstants.DefectReportStatus.FAIL.name(), 1, TimeUnit.HOURS);
        }
        return new CodeCCResult(null);

    }

    @Override
    public CodeCCResult reportDefects(UploadDefectVO uploadDefectVO)
    {
        logger.info("upload defects of file, streamName: {}, taskId: {}, toolName: {}, file: {}",
                uploadDefectVO.getStreamName(), uploadDefectVO.getTaskId(), uploadDefectVO.getToolName(), uploadDefectVO.getFilePath());
        if (StringUtils.isNotEmpty(uploadDefectVO.getToolName()))
        {
            uploadDefectVO.setToolName(uploadDefectVO.getToolName().toUpperCase());
        }

        CodeCCResult<List<BaseDataVO>> baseDataCodeCCResult = client.get(ServiceBaseDataResource.class)
                .getInfoByTypeAndCode(ComConstants.KEY_DOCKERNIZED_TOOLS, ComConstants.KEY_DOCKERNIZED_TOOLS);

        if (baseDataCodeCCResult.isNotOk() || CollectionUtils.isEmpty(baseDataCodeCCResult.getData()))
        {
            logger.error("get risk coefficient fail!");
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL);
        }
        List<String> dockernizedToolList = List2StrUtil.fromString(baseDataCodeCCResult.getData().get(0).getParamValue(), ComConstants.STRING_SPLIT);
        IBizService uploadDefectService;

        // 如果是容器化的工具，用自定义工具的实现类处理
        if (dockernizedToolList.contains(uploadDefectVO.getToolName()))
        {
            String beanName = String.format("%s%s%s", PREFIX_CUSTOM_TOOL, ComConstants.BusinessType.UPLOAD_DEFECT.value(), ComConstants.BIZ_SERVICE_POSTFIX);
            uploadDefectService = SpringContextUtil.Companion.getBean(IBizService.class, beanName);
        }
        else
        {
            uploadDefectService = bizServiceFactory.createBizService(uploadDefectVO.getToolName(),
                    ComConstants.BusinessType.UPLOAD_DEFECT.value(), IBizService.class);
        }
        return uploadDefectService.processBiz(uploadDefectVO);
    }

    @Override
    public CodeCCResult uploadCCNStatistic(CCNUploadStatisticVO uploadStatisticVO)
    {
        logger.info("upload ccn statistic, stream name: {}", uploadStatisticVO.getStreamName());
        return ccnUploadStatisticService.uploadStatistic(uploadStatisticVO);
    }

    @Override
    public CodeCCResult uploadDUPCStatistic(UploadDUPCStatisticVO uploadStatisticVO)
    {
        logger.info("upload dupc statistic, stream name: {}", uploadStatisticVO.getStreamName());
        return dupcUploadStatisticService.uploadStatistic(uploadStatisticVO);
    }

    @Override
    public CodeCCResult uploadCLOCStatistic(UploadCLOCStatisticVO uploadCLOCStatisticVO)
    {
        logger.info("upload cloc statistic, stream name: {}", uploadCLOCStatisticVO.getStreamName());
        return clocUploadStatisticService.uploadStatistic(uploadCLOCStatisticVO);
    }

    @Override
    public CodeCCResult uploadRepositories(UploadRepositoriesVO uploadRepositoriesVO)
    {
        logger.info("upload code repositories, task id: {}, tool name: {} build id: {}", uploadRepositoriesVO.getTaskId(), uploadRepositoriesVO.getToolName(),
                uploadRepositoriesVO.getBuildId());
        return uploadRepositoriesService.uploadRepositories(uploadRepositoriesVO);
    }
}
