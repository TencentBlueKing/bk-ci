/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

import com.tencent.bk.codecc.defect.api.UserDefectRestResource;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.common.*;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.pojo.external.BkAuthExAction;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 * lint类告警查询服务实现
 */
@RestResource
@AuthMethod(permission = {BkAuthExAction.TASK_MEMBER, BkAuthExAction.TASK_OWNER, BkAuthExAction.ADMIN_MEMBER})
public class UserDefectRestResourceImpl implements UserDefectRestResource
{

    @Autowired
    private BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory;

    @Override
    public Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(Long taskId, String toolName)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(toolName,
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.processQueryWarningPageInitRequest(taskId, toolName));
    }

    @Override
    public Result<CommonFileQueryRspVO> queryFileList(Long taskId,
                                                      CommonFileQueryReqVO commonFileQueryReqVO,
                                                      Integer pageNum,
                                                      Integer pageSize,
                                                      String sortField,
                                                      Sort.Direction sortType)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(commonFileQueryReqVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.processQueryWarningRequest(taskId, commonFileQueryReqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<CommonDefectQueryRspVO> queryDefectDetail(Long taskId,
                                                            CommonDefectQueryReqVO commonDefectQueryReqVO,
                                                            String sortField,
                                                            Sort.Direction sortType)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(commonDefectQueryReqVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.processQueryWarningDetailRequest(taskId, commonDefectQueryReqVO, sortField, sortType));
    }

    @Override
    public Result<CommonDefectQueryRspVO> getFileContentSegment(Long taskId, GetFileContentSegmentReqVO getFileContentSegmentReqVO)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(getFileContentSegmentReqVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.processGetFileContentSegmentRequest(taskId, getFileContentSegmentReqVO));
    }

    @Override
    public Result<Boolean> authorTransfer(Long taskId, CommonAuthorTransVO commonAuthorTransVO)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(commonAuthorTransVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.authorTransfer(taskId, commonAuthorTransVO));
    }


}
