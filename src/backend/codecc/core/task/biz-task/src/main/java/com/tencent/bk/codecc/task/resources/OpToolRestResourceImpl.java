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
 
package com.tencent.bk.codecc.task.resources;

import com.tencent.bk.codecc.task.api.OpToolRestResource;
import com.tencent.bk.codecc.task.service.TaskService;
import com.tencent.bk.codecc.task.service.ToolService;
import com.tencent.bk.codecc.task.vo.TaskDetailVO;
import com.tencent.bk.codecc.task.vo.ToolConfigPlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.api.pojo.CodeCCResult;
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.web.RestResource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * op工具接口实现
 * 
 * @date 2020/3/11
 * @version V1.0
 */
@RestResource
public class OpToolRestResourceImpl implements OpToolRestResource
{

    @Autowired
    private ToolService toolService;

    @Autowired
    private AuthExPermissionApi authExPermissionApi;

    @Autowired
    private TaskService taskService;


    @Override
    public CodeCCResult<Boolean> updateToolPlatformInfo(Long taskId, String userName,
                                                        ToolConfigPlatformVO toolConfigPlatformVO)
    {
        return new CodeCCResult<>(toolService.updateToolPlatformInfo(taskId, userName, toolConfigPlatformVO));
    }


    @Override
    public CodeCCResult<Boolean> refreshTaskOrgInfo(String userName, TaskDetailVO reqVO)
    {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName))
        {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER);
        }
        return new CodeCCResult<>(taskService.refreshTaskOrgInfo(reqVO.getTaskId()));
    }

    @Override
    public CodeCCResult<Boolean> refreshToolFollowStatus(String userName, Integer pageSize)
    {
        // 判断是否为管理员
        if (!authExPermissionApi.isAdminMember(userName))
        {
            throw new CodeCCException(CommonMessageCode.IS_NOT_ADMIN_MEMBER, new String[]{"admin member"});
        }
        pageSize = pageSize == null ? 500 : pageSize;
        return new CodeCCResult<>(toolService.batchUpdateToolFollowStatus(pageSize));
    }

}
