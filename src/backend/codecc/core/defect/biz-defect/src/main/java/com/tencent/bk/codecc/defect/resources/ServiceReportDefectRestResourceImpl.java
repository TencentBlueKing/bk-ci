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

import com.tencent.bk.codecc.defect.api.ServiceReportDefectRestResource;
import com.tencent.bk.codecc.defect.service.IUpdateDefectBizService;
import com.tencent.bk.codecc.defect.service.impl.CommonQueryWarningBizServiceImpl;
import com.tencent.bk.codecc.defect.vo.UpdateDefectVO;
import com.tencent.bk.codecc.defect.vo.UploadDefectVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Set;

/**
 * 工具侧上报告警的接口实现
 *
 * @version V1.0
 * @date 2019/5/13
 */
@RestResource
@Slf4j
public class ServiceReportDefectRestResourceImpl implements ServiceReportDefectRestResource
{
    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private IUpdateDefectBizService updateDefectBizService;

    @Qualifier("CommonQueryWarningBizService")
    @Autowired
    private CommonQueryWarningBizServiceImpl commonQueryWarningBizService;

    @Override
    public Result<Set<Long>> queryIds(long taskId, String toolName)
    {
        Set<Long> idSet = commonQueryWarningBizService.queryIds(taskId, toolName);
        return new Result<>(idSet);
    }

    @Override
    public Result updateDefectStatus(UpdateDefectVO updateDefectVO)
    {
        updateDefectBizService.updateDefectStatus(updateDefectVO);
        return new Result(CommonMessageCode.SUCCESS, "update defectStatus success.");
    }

    @Override
    public Result reportDefects(UploadDefectVO uploadDefectVO)
    {
        log.info("report defects, taskId:{}, toolName:{}, buildId:{}", uploadDefectVO.getTaskId(), uploadDefectVO.getToolName(), uploadDefectVO.getBuildId());

        IBizService uploadDefectService = bizServiceFactory.createBizService(uploadDefectVO.getToolName(),
                    ComConstants.BusinessType.UPLOAD_DEFECT.value(), IBizService.class);
        return uploadDefectService.processBiz(uploadDefectVO);
    }

    @Override
    public Result updateDefects(UpdateDefectVO updateDefectVO) {
        updateDefectBizService.updateDefects(updateDefectVO);
        return new Result(CommonMessageCode.SUCCESS, "update defectDetail success.");
    }
}
