/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.OpDefectRestResource;
import com.tencent.bk.codecc.defect.service.CodeRepoService;
import com.tencent.bk.codecc.defect.service.GetTaskLogService;
import com.tencent.bk.codecc.defect.service.ICLOCQueryCodeLineService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.service.IV3CheckerSetBizService;
import com.tencent.bk.codecc.defect.service.RefreshCheckerScriptService;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.devops.common.api.QueryTaskListReqVO;
import com.tencent.devops.common.api.checkerset.CheckerSetParamsVO;
import com.tencent.devops.common.api.checkerset.V3UpdateCheckerSetReqExtVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.web.RestResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 * op接口实现
 *
 * @version V1.0
 * @date 2020/3/11
 */
@RestResource
public class OpDefectRestResourceImpl implements OpDefectRestResource {
    @Autowired
    private BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private GetTaskLogService getTaskLogService;

    @Autowired
    private RefreshCheckerScriptService refreshCheckerScriptService;

    @Autowired
    private ICLOCQueryCodeLineService queryCodeLineService;
    @Autowired
    private IV3CheckerSetBizService checkerSetBizService;
    @Autowired
    private CodeRepoService codeRepoService;

    @Override
    public Result<DeptTaskDefectRspVO> queryDeptTaskDefect(String userName, DeptTaskDefectReqVO deptTaskDefectReqVO) {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER);
        }

        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory
                .createBizService(deptTaskDefectReqVO.getToolName(), ComConstants.BusinessType.QUERY_WARNING.value(),
                        IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService.processDeptTaskDefectReq(deptTaskDefectReqVO));
    }

    @Override
    public Result<ToolDefectRspVO> queryDeptDefectList(String userName, DeptTaskDefectReqVO deptTaskDefectReqVO,
            Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType) {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER);
        }
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory
                .createBizService(deptTaskDefectReqVO.getToolName(), ComConstants.BusinessType.QUERY_WARNING.value(),
                        IQueryWarningBizService.class);
        return new Result<>(queryWarningBizService
                .processDeptDefectList(deptTaskDefectReqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<DeptTaskDefectRspVO> queryActiveTaskListByLog(String userName,
            DeptTaskDefectReqVO deptTaskDefectReqVO) {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER);
        }

        return new Result<>(getTaskLogService.getActiveTaskList(deptTaskDefectReqVO));
    }

    @Override
    public Result<Boolean> initCheckerDetailScript(String toolName, Integer pageNum, Integer pageSize, String sortField,
            String sortType) {
        return new Result<>(
                refreshCheckerScriptService.initCheckerDetailScript(toolName, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<Long> getTaskCodeLineCount(@NotNull QueryTaskListReqVO reqVO) {
        return new Result<>(queryCodeLineService.queryCodeLineByTaskIds(reqVO.getTaskIds()));
    }

    @Override
    public Result<Boolean> updateCheckerSetBaseInfo(String userName,
            V3UpdateCheckerSetReqExtVO updateCheckerSetReqExtVO) {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName)) {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, userName + " is not an admin member");
        }
        return new Result<>(checkerSetBizService.updateCheckerSetBaseInfoByOp(userName, updateCheckerSetReqExtVO));
    }

    @Override
    public Result<CheckerSetParamsVO> getCheckerSetParams() {
        return new Result<>(checkerSetBizService.getCheckerSetParams());
    }

    @Override
    public Result<Boolean> initCodeRepoStatistic(DeptTaskDefectReqVO reqVO, Integer pageNum, Integer pageSize,
            String sortField, String sortType) {
        return new Result<>(codeRepoService.initCodeRepoStatistic(reqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public Result<Boolean> initCodeRepoStatTrend(QueryTaskListReqVO reqVO) {
        return new Result<>(codeRepoService.initCodeRepoStatTrend(reqVO));
    }
}
