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

import com.tencent.bk.codecc.defect.api.ServiceDefectRestResource;
import com.tencent.bk.codecc.defect.service.MetricsService;
import com.tencent.bk.codecc.defect.service.impl.StatQueryWarningServiceImpl;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.MetricsVO;
import com.tencent.devops.common.api.constant.CommonMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.web.RestResource;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 告警模块树服务实现
 *
 * @version V1.0
 * @date 2019/10/20
 */
@RestResource
public class ServiceDefectRestResourceImpl implements ServiceDefectRestResource {

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private StatQueryWarningServiceImpl statQueryWarningService;

    @Override
    public Result<Boolean> batchDefectProcess(long taskId,
            String userName,
            BatchDefectProcessReqVO batchDefectProcessReqVO) {
        batchDefectProcessReqVO.setTaskId(taskId);
        batchDefectProcessReqVO.setIgnoreAuthor(userName);
        IBizService<BatchDefectProcessReqVO> bizService =
                bizServiceFactory.createBizService(batchDefectProcessReqVO.getToolName(),
                        batchDefectProcessReqVO.getDimension(),
                        ComConstants.BATCH_PROCESSOR_INFIX + batchDefectProcessReqVO.getBizType(),
                        IBizService.class);
        return bizService.processBiz(batchDefectProcessReqVO);
    }

    @Override
    public Result<MetricsVO> getMetrics(String repoId, String buildId) {
        if (StringUtils.isBlank(repoId)) {
            throw new CodeCCException(CommonMessageCode.ERROR_INVALID_PARAM_);
        }

        try {
            MetricsVO mericsInfo = metricsService.getMetrics(repoId, buildId);
            return new Result<>(mericsInfo);
        } catch (CodeCCException e) {
            MetricsVO failData = new MetricsVO();
            failData.setStatus(2);
            return new Result<>(failData);
        }
    }

    @Override public Result<Long> lastestStatDefect(long taskId, String toolName) {
        return new Result<>(statQueryWarningService.getLastestMsgTime(taskId));
    }

}
