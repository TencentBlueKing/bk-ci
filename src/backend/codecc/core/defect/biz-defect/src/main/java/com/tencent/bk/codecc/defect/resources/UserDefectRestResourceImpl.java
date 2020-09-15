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

import com.tencent.bk.codecc.defect.api.UserDefectRestResource;
import com.tencent.bk.codecc.defect.service.*;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.defect.vo.SingleCommentVO;
import com.tencent.bk.codecc.defect.vo.common.*;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * 告警查询服务实现
 */
@RestResource
@AuthMethod(permission = {CodeCCAuthAction.DEFECT_VIEW})
public class UserDefectRestResourceImpl implements UserDefectRestResource
{
    /**
     * 查询构建信息最大数量
     */
    private static final int MAX_BUILD_LIST_SIZE = 100;

    @Autowired
    private BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory;

    @Autowired
    private BizServiceFactory<IBizService> bizServiceFactory;

    @Autowired
    private BizServiceFactory<IDefectOperateBizService> defectOperateBizServiceFactory;

    @Autowired
    private TaskLogService taskLogService;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private FileDefectGatherService fileDefectGatherService;

    @Override
    public CodeCCResult<QueryWarningPageInitRspVO> queryCheckersAndAuthors(Long taskId, String toolName)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(toolName,
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new CodeCCResult<>(queryWarningBizService.processQueryWarningPageInitRequest(taskId, toolName));
    }

    @Override
    public CodeCCResult<CommonDefectQueryRspVO> queryDefectList(long taskId,
                                                                DefectQueryReqVO defectQueryReqVO,
                                                                int pageNum,
                                                                int pageSize,
                                                                String sortField,
                                                                Sort.Direction sortType)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(defectQueryReqVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new CodeCCResult<>(queryWarningBizService.processQueryWarningRequest(taskId, defectQueryReqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public CodeCCResult<CommonDefectDetailQueryRspVO> queryDefectDetail(long taskId,
                                                                        CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
                                                                        String sortField,
                                                                        Sort.Direction sortType)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(commonDefectDetailQueryReqVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new CodeCCResult<>(queryWarningBizService.processQueryWarningDetailRequest(taskId, commonDefectDetailQueryReqVO, sortField, sortType));
    }

    @Override
    public CodeCCResult<CommonDefectDetailQueryRspVO> getFileContentSegment(long taskId, GetFileContentSegmentReqVO getFileContentSegmentReqVO)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(getFileContentSegmentReqVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new CodeCCResult<>(queryWarningBizService.processGetFileContentSegmentRequest(taskId, getFileContentSegmentReqVO));
    }

    @Override
    public CodeCCResult<Boolean> batchDefectProcess(long taskId, String userName, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        batchDefectProcessReqVO.setTaskId(taskId);
        batchDefectProcessReqVO.setIgnoreAuthor(userName);
        IBizService<BatchDefectProcessReqVO> bizService = bizServiceFactory.createBizService(batchDefectProcessReqVO.getToolName(),
                ComConstants.BATCH_PROCESSOR_INFIX + batchDefectProcessReqVO.getBizType(), IBizService.class);
        return bizService.processBiz(batchDefectProcessReqVO);
    }

    @Override
    public CodeCCResult<List<BuildVO>> queryBuildInfos(Long taskId)
    {
        return new CodeCCResult<>(taskLogService.getTaskBuildInfos(taskId, MAX_BUILD_LIST_SIZE));
    }

    @Override
    public CodeCCResult<DeptTaskDefectRspVO> queryDeptTaskDefect(String userName, DeptTaskDefectReqVO deptTaskDefectReqVO)
    {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName))
        {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER);
        }

        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(deptTaskDefectReqVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new CodeCCResult<>(queryWarningBizService.processDeptTaskDefectReq(deptTaskDefectReqVO));
    }

    @Override
    public CodeCCResult<Boolean> addCodeComment(String fileId, String defectId, String toolName, String commentId,
                                                String userName, SingleCommentVO singleCommentVO)
    {
        IDefectOperateBizService defectOperateBizService = defectOperateBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.DEFECT_OPERATE.value(), IDefectOperateBizService.class);
        defectOperateBizService.addCodeComment(fileId, defectId, commentId, userName, singleCommentVO);
        return new CodeCCResult<>(true);
    }


    @Override
    public CodeCCResult<Boolean> updateCodeComment(String commentId, String userName, String toolName, SingleCommentVO singleCommentVO)
    {
        IDefectOperateBizService defectOperateBizService = defectOperateBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.DEFECT_OPERATE.value(), IDefectOperateBizService.class);
        defectOperateBizService.updateCodeComment(commentId, userName, singleCommentVO);
        return new CodeCCResult<>(true);
    }



    @Override
    public CodeCCResult<Boolean> deleteCodeComment(String commentId, String singleCommentId, String toolName, String userName)
    {
        IDefectOperateBizService defectOperateBizService = defectOperateBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.DEFECT_OPERATE.value(), IDefectOperateBizService.class);
        defectOperateBizService.deleteCodeComment(commentId, singleCommentId, userName);
        return new CodeCCResult<>(true);
    }

    @Override
    public CodeCCResult<FileDefectGatherVO> queryFileDefectGather(long taskId, String toolName)
    {
        return new CodeCCResult<>(fileDefectGatherService.getFileDefectGather(taskId, toolName));
    }


}
