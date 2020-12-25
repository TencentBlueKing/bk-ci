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
import com.tencent.bk.codecc.defect.service.GetTaskLogService;
import com.tencent.bk.codecc.defect.service.IQueryWarningBizService;
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO;
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectRspVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.service.BizServiceFactory;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

/**
 * op接口实现
 * 
 * @date 2020/3/11
 * @version V1.0
 */
@RestResource
public class OpDefectRestResourceImpl implements OpDefectRestResource
{
    @Autowired
    private BizServiceFactory<IQueryWarningBizService> fileAndDefectQueryFactory;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private GetTaskLogService getTaskLogService;


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
    public CodeCCResult<ToolDefectRspVO> queryDeptDefectList(String userName, DeptTaskDefectReqVO deptTaskDefectReqVO,
            Integer pageNum, Integer pageSize, String sortField, Sort.Direction sortType)
    {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName))
        {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER);
        }
        IQueryWarningBizService queryWarningBizService = fileAndDefectQueryFactory
                .createBizService(deptTaskDefectReqVO.getToolName(), ComConstants.BusinessType.QUERY_WARNING.value(),
                        IQueryWarningBizService.class);
        return new CodeCCResult<>(queryWarningBizService
                .processDeptDefectList(deptTaskDefectReqVO, pageNum, pageSize, sortField, sortType));
    }

    @Override
    public CodeCCResult<DeptTaskDefectRspVO> queryActiveTaskListByLog(String userName, DeptTaskDefectReqVO deptTaskDefectReqVO)
    {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName))
        {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER);
        }
        
        return new CodeCCResult<>(getTaskLogService.getActiveTaskList(deptTaskDefectReqVO));
    }


}
