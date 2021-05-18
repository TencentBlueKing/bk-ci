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
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.web.RestResource;
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

    @Override
    public CodeCCResult<Boolean> batchDefectProcess(long taskId,
                                              String userName,
                                              BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        batchDefectProcessReqVO.setTaskId(taskId);
        batchDefectProcessReqVO.setIgnoreAuthor(userName);
        IBizService<BatchDefectProcessReqVO> bizService =
            bizServiceFactory.createBizService(batchDefectProcessReqVO.getToolName(),
                ComConstants.BATCH_PROCESSOR_INFIX + batchDefectProcessReqVO.getBizType(),
                IBizService.class);
        return bizService.processBiz(batchDefectProcessReqVO);
    }
}
