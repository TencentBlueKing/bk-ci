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

import com.google.common.collect.Sets;
import com.tencent.bk.codecc.defect.api.UserDefectRestResource;
import com.tencent.bk.codecc.defect.service.FileDefectGatherService;
import com.tencent.bk.codecc.defect.service.IDefectOperateBizService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.IStatQueryWarningService;
import com.tencent.bk.codecc.defect.service.TaskLogService;
import com.tencent.bk.codecc.defect.service.impl.CLOCQueryWarningBizServiceImpl;
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO;
import com.tencent.bk.codecc.defect.vo.FileDefectGatherVO;
import com.tencent.bk.codecc.defect.vo.GetFileContentSegmentReqVO;
import com.tencent.bk.codecc.defect.vo.SingleCommentVO;
import com.tencent.bk.codecc.defect.vo.StatDefectQueryRespVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.bk.codecc.defect.vo.common.BuildVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectDetailQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.CommonDefectQueryRspVO;
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO;
import com.tencent.bk.codecc.defect.vo.common.QueryWarningPageInitRspVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.service.IBizService;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.web.RestResource;
import com.tencent.devops.common.web.security.AuthMethod;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 * 告警查询服务实现
 */
@RestResource
@AuthMethod(permission = {CodeCCAuthAction.DEFECT_VIEW})
public class UserDefectRestResourceImpl implements UserDefectRestResource {

    /**
     * 查询构建信息最大数量
     */
    private static final int MAX_BUILD_LIST_SIZE = 100;
    @Autowired
    CLOCQueryWarningBizServiceImpl clocQueryWarningBizService;
    @Autowired
    IStatQueryWarningService iStatQueryWarningService;
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
    public Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(Long taskId,
                                                                     String toolName,
                                                                     String status) {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(
            toolName,
            ComConstants.BusinessType.QUERY_WARNING.value(),
            IQueryWarningBizService.class);

        Set<String> statusSet = null;
        if (StringUtils.isNotEmpty(status)) {
            statusSet = Sets.newHashSet(List2StrUtil.fromString(status, ComConstants.STRING_SPLIT));
        }
        return new Result<>(queryWarningBizService.processQueryWarningPageInitRequest(taskId, toolName, null, statusSet, null, null));
    }

    @Override
    public Result<QueryWarningPageInitRspVO> queryCheckersAndAuthors(Long taskId,
                                                                     String toolName,
                                                                     String dimension,
                                                                     String status,
                                                                     String checkerSet) {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(
            toolName,
            dimension,
            ComConstants.BusinessType.QUERY_WARNING.value(),
            IQueryWarningBizService.class);

        Set<String> statusSet = null;
        if (StringUtils.isNotEmpty(status)) {
            statusSet = Sets.newHashSet(List2StrUtil.fromString(status, ComConstants.STRING_SPLIT));
        }
        return new Result<>(queryWarningBizService.processQueryWarningPageInitRequest(taskId, toolName, dimension, statusSet, checkerSet, null));
    }

    @Override
    public Result<CommonDefectQueryRspVO> queryDefectList(long taskId,
                                                          DefectQueryReqVO defectQueryReqVO,
                                                          int pageNum,
                                                          int pageSize,
                                                          String sortField,
                                                          Sort.Direction sortType)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(
            defectQueryReqVO.getToolName(),
            defectQueryReqVO.getDimension(),
            ComConstants.BusinessType.QUERY_WARNING.value(),
            IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.processQueryWarningRequest(taskId, defectQueryReqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<CommonDefectDetailQueryRspVO> queryDefectDetail(long taskId,
            String userId,
            CommonDefectDetailQueryReqVO commonDefectDetailQueryReqVO,
            String sortField,
            Sort.Direction sortType) {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory
                .createBizService(commonDefectDetailQueryReqVO.getToolName(),
                        commonDefectDetailQueryReqVO.getDimension(),
                        ComConstants.BusinessType.QUERY_WARNING.value(),
                        IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService
                .processQueryWarningDetailRequest(taskId, userId, commonDefectDetailQueryReqVO, sortField, sortType));
    }

    @Override
    public Result<CommonDefectDetailQueryRspVO> getFileContentSegment(long taskId, String userId,
            GetFileContentSegmentReqVO getFileContentSegmentReqVO) {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory
                .createBizService(getFileContentSegmentReqVO.getToolName(),
                        getFileContentSegmentReqVO.getDimension(),
                        ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new Result<>(
                queryWarningBizService.processGetFileContentSegmentRequest(taskId, userId, getFileContentSegmentReqVO));
    }

    @Override
    public Result<Boolean> batchDefectProcess(long taskId, String userName, BatchDefectProcessReqVO batchDefectProcessReqVO)
    {
        batchDefectProcessReqVO.setTaskId(taskId);
        batchDefectProcessReqVO.setIgnoreAuthor(userName);
        IBizService<BatchDefectProcessReqVO> bizService = bizServiceFactory.createBizService(
                batchDefectProcessReqVO.getToolName(),
                batchDefectProcessReqVO.getDimension(),
                ComConstants.BATCH_PROCESSOR_INFIX + batchDefectProcessReqVO.getBizType(),
                IBizService.class);
        return bizService.processBiz(batchDefectProcessReqVO);
    }

    @Override
    public Result<List<BuildVO>> queryBuildInfos(Long taskId)
    {
        return new Result<>(taskLogService.getTaskBuildInfos(taskId, MAX_BUILD_LIST_SIZE));
    }

    @Override
    public Result<DeptTaskDefectRspVO> queryDeptTaskDefect(String userName, DeptTaskDefectReqVO deptTaskDefectReqVO)
    {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName))
        {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER);
        }

        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(deptTaskDefectReqVO.getToolName(),
                ComConstants.BusinessType.QUERY_WARNING.value(), IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.processDeptTaskDefectReq(deptTaskDefectReqVO));
    }

    @Override
    public Result<Boolean> addCodeComment(String defectId, String toolName, String commentId,
                                          String userName, SingleCommentVO singleCommentVO)
    {
        IDefectOperateBizService defectOperateBizService = defectOperateBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.DEFECT_OPERATE.value(), IDefectOperateBizService.class);
        defectOperateBizService.addCodeComment(defectId, commentId, userName, singleCommentVO);
        return new Result<>(true);
    }


    @Override
    public Result<Boolean> updateCodeComment(String commentId, String userName, String toolName, SingleCommentVO singleCommentVO)
    {
        IDefectOperateBizService defectOperateBizService = defectOperateBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.DEFECT_OPERATE.value(), IDefectOperateBizService.class);
        defectOperateBizService.updateCodeComment(commentId, userName, singleCommentVO);
        return new Result<>(true);
    }



    @Override
    public Result<Boolean> deleteCodeComment(String commentId, String singleCommentId, String toolName, String userName)
    {
        IDefectOperateBizService defectOperateBizService = defectOperateBizServiceFactory.createBizService(toolName,
                ComConstants.BusinessType.DEFECT_OPERATE.value(), IDefectOperateBizService.class);
        defectOperateBizService.deleteCodeComment(commentId, singleCommentId, userName);
        return new Result<>(true);
    }

    @Override
    public Result<FileDefectGatherVO> queryFileDefectGather(long taskId, String toolName)
    {
        return new Result<>(fileDefectGatherService.getFileDefectGather(taskId, toolName, null));
    }

    @Override
    public Result<FileDefectGatherVO> queryFileDefectGather(long taskId, String toolName, String dimension) {
        return new Result<>(fileDefectGatherService.getFileDefectGather(taskId, toolName, dimension));
    }

    @Override
    public Result<CommonDefectQueryRspVO> queryCLOCList(long taskId, String toolName, ComConstants.CLOCOrder orderBy) {
        DefectQueryReqVO defectQueryReqVO = new DefectQueryReqVO();
        defectQueryReqVO.setToolName(toolName);
        defectQueryReqVO.setOrder(orderBy);
        if (!checkParam(defectQueryReqVO)) {
            return new Result<>(CommonMessageCode.PARAMETER_IS_INVALID, null);
        }
        return new Result<>(clocQueryWarningBizService.processQueryWarningRequest(taskId, defectQueryReqVO,
                0, 0, null, null));
    }

    private boolean checkParam(DefectQueryReqVO defectQueryReqVO) {
        if (StringUtils.isBlank(defectQueryReqVO.getToolName()) || defectQueryReqVO.getOrder() == null) {
            return false;
        }

        return defectQueryReqVO.getToolName().equalsIgnoreCase(ComConstants.Tool.CLOC.name());
    }

    @Override
    public Result<QueryWarningPageInitRspVO> pageInit(long taskId, DefectQueryReqVO defectQueryReqVO)
    {
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory.createBizService(
            defectQueryReqVO.getToolName(),
            defectQueryReqVO.getDimension(),
            ComConstants.BusinessType.QUERY_WARNING.value(),
            IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.pageInit(taskId, defectQueryReqVO));
    }

    @Override
    public Result<List<StatDefectQueryRespVO>> queryStatList(long taskId, String toolName, long startTime, long endTime) {
        if (StringUtils.isBlank(toolName)) {
            throw new CodeCCException(CommonMessageCode.INVALID_TOOL_NAME);
        }
        return new Result<>(iStatQueryWarningService.processQueryWarningRequest(taskId, toolName, startTime, endTime));
    }


}
