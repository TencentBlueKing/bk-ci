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
import com.tencent.bk.codecc.defect.service.DUPCUploadStatisticService;
import com.tencent.bk.codecc.defect.vo.CCNUploadStatisticVO;
import com.tencent.bk.codecc.defect.vo.UploadDUPCStatisticVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private CCNUploadStatisticService ccnUploadStatisticService;

    @Autowired
    private DUPCUploadStatisticService dupcUploadStatisticService;

    @Override
    public Result reportDefects(UploadDefectVO uploadDefectVO)
    {
        if (StringUtils.isNotEmpty(uploadDefectVO.getToolName()))
        {
            uploadDefectVO.setToolName(uploadDefectVO.getToolName().toUpperCase());
        }
        logger.info("upload defects of file, streamName: {}, toolName: {}, file: {}",
                uploadDefectVO.getStreamName(), uploadDefectVO.getToolName(), uploadDefectVO.getFilePath());

        IBizService uploadDefectService = bizServiceFactory.createBizService(uploadDefectVO.getToolName(),
                ComConstants.BusinessType.UPLOAD_DEFECT.value(), IBizService.class);
        return uploadDefectService.processBiz(uploadDefectVO);
    }

    @Override
    public Result uploadCCNStatistic(CCNUploadStatisticVO uploadStatisticVO)
    {
        logger.info("upload ccn statistic, stream name: {}", uploadStatisticVO.getStreamName());
        return ccnUploadStatisticService.uploadStatistic(uploadStatisticVO);
    }

    @Override
    public Result uploadDUPCStatistic(UploadDUPCStatisticVO uploadStatisticVO)
    {
        logger.info("upload dupc statistic, stream name: {}", uploadStatisticVO.getStreamName());
        return dupcUploadStatisticService.uploadStatistic(uploadStatisticVO);
    }
}
