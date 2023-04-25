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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.auth.dao.AuthGroupDao
import com.tencent.devops.auth.pojo.DefaultGroup
import com.tencent.devops.auth.pojo.dto.ProjectRoleDTO
import com.tencent.devops.auth.pojo.vo.GroupInfoVo
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.impl.IamPermissionRoleExtService
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.client.Client
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired

@Service
class TxPermissionRoleServiceImpl @Autowired constructor(
    iamManagerService: ManagerService,
    private val permissionGradeService: PermissionGradeService,
    private val iamConfiguration: IamConfiguration,
    private val groupService: AuthGroupService,
    private val authGroupDao: AuthGroupDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val stagService: StrategyService
) : IamPermissionRoleExtService(
    iamManagerService = iamManagerService,
    permissionGradeService = permissionGradeService,
    iamConfiguration = iamConfiguration,
    groupService = groupService,
    groupDao = authGroupDao,
    dslContext = dslContext,
    client = client,
    strategyService = stagService
) {
    override fun createPermissionRole(
        userId: String,
        projectId: Int,
        projectCode: String,
        groupInfo: ProjectRoleDTO
    ): Int {
        return super.createPermissionRole(userId, projectId, projectCode, groupInfo)
    }

    override fun renamePermissionRole(
        userId: String,
        projectId: Int,
        roleId: Int,
        groupInfo: ProjectRoleDTO
    ) {
        super.renamePermissionRole(userId, projectId, roleId, groupInfo)
    }

    override fun getPermissionRole(projectId: Int): List<GroupInfoVo> {
        return super.getPermissionRole(projectId)
    }

    override fun getDefaultRole(): List<DefaultGroup> {
        val defaultGroups = mutableListOf<DefaultGroup>()
        val allDefaultGroup = DefaultGroupType.getAll()
        allDefaultGroup.forEach {
            defaultGroups.add(
                DefaultGroup(
                    code = it.value,
                    name = it.displayName,
                    displayName = it.displayName
                )
            )
        }
        return defaultGroups
    }
}
