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

package com.tencent.devops.auth.provider.rbac.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_AUTH_GROUP_NOT_EXIST
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.auth.service.AuthAuthorizationScopesService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import org.jooq.DSLContext

/**
 * 权限组策略
 */
@Suppress("LongParameterList", "LongMethod")
class PermissionGroupPoliciesService(
    private val iamV2ManagerService: V2ManagerService,
    private val authActionDao: AuthActionDao,
    private val dslContext: DSLContext,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authAuthorizationScopesService: AuthAuthorizationScopesService
) {
    fun grantGroupPermission(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        groupCode: String,
        iamResourceCode: String,
        resourceName: String,
        iamGroupId: Int,
        registerMonitorPermission: Boolean = true
    ) {
        var authorizationScopes = authAuthorizationScopesService.generateBkciAuthorizationScopes(
            authorizationScopesStr = authorizationScopesStr,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = iamResourceCode,
            resourceName = resourceName
        )
        if (resourceType == AuthResourceType.PROJECT.value && registerMonitorPermission) {
            // 若为项目下的组授权，默认要加上监控平台用户组的权限资源
            val monitorAuthorizationScopes = authAuthorizationScopesService.generateMonitorAuthorizationScopes(
                projectName = projectName,
                projectCode = projectCode,
                groupCode = groupCode
            )
            authorizationScopes = authorizationScopes.plus(monitorAuthorizationScopes)
        }
        authorizationScopes.forEach { authorizationScope ->
            iamV2ManagerService.grantRoleGroupV2(iamGroupId, authorizationScope)
        }
    }

    fun getGroupPolices(
        userId: String,
        projectId: String,
        resourceType: String,
        groupId: Int
    ): List<IamGroupPoliciesVo> {
        val groupInfo = authResourceGroupDao.getByRelationId(
            dslContext = dslContext,
            projectCode = projectId,
            iamGroupId = groupId.toString()
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_AUTH_GROUP_NOT_EXIST,
            params = arrayOf(groupId.toString()),
            defaultMessage = "group $groupId not exist"
        )
        val groupConfigInfo = authResourceGroupConfigDao.getByGroupCode(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = groupInfo.groupCode
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_RESOURCE_GROUP_CONFIG_NOT_EXIST,
            params = arrayOf("${resourceType}_${groupInfo.groupCode}"),
            defaultMessage = "${resourceType}_${groupInfo.groupCode} group config  not exist"
        )
        val groupActions = JsonUtil.to(groupConfigInfo.actions, object : TypeReference<List<String>>() {})
        return authActionDao.list(
            dslContext = dslContext,
            resourceType = resourceType
        ).map {
            IamGroupPoliciesVo(
                action = it.action,
                actionName = it.actionName,
                permission = groupActions.contains(it.action)
            )
        }
    }
}
