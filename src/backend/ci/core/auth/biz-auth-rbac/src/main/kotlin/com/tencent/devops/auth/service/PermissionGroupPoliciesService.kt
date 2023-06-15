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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_AUTH_GROUP_NOT_EXIST
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.vo.IamGroupPoliciesVo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

/**
 * 权限组策略
 */
@Suppress("LongParameterList", "LongMethod")
class PermissionGroupPoliciesService(
    private val iamConfiguration: IamConfiguration,
    private val iamV2ManagerService: V2ManagerService,
    private val authActionDao: AuthActionDao,
    private val dslContext: DSLContext,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val authResourceGroupDao: AuthResourceGroupDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionGroupPoliciesService::class.java)
        private const val SYSTEM_PLACEHOLDER = "#system#"
        private const val PROJECT_ID_PLACEHOLDER = "#projectId#"
        private const val PROJECT_NAME_PLACEHOLDER = "#projectName#"
        private const val RESOURCE_CODE_PLACEHOLDER = "#resourceCode#"
        private const val RESOURCE_NAME_PLACEHOLDER = "#resourceName#"
    }

    /**
     * 构建分级管理员或分级管理员用户组授权范围
     */
    fun buildAuthorizationScopes(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        iamResourceCode: String,
        resourceName: String
    ): List<AuthorizationScopes> {
        val replaceAuthorizationScopesStr =
            authorizationScopesStr.replace(SYSTEM_PLACEHOLDER, iamConfiguration.systemId)
                .replace(PROJECT_ID_PLACEHOLDER, projectCode)
                .replace(PROJECT_NAME_PLACEHOLDER, projectName)
                .replace(RESOURCE_CODE_PLACEHOLDER, iamResourceCode)
                // 如果资源名中有\,需要转义,不然json序列化时会报错
                .replace(RESOURCE_NAME_PLACEHOLDER, resourceName.replace("\\", "\\\\"))
        logger.info("$projectCode authorization scopes after replace $replaceAuthorizationScopesStr ")
        return JsonUtil.to(replaceAuthorizationScopesStr, object : TypeReference<List<AuthorizationScopes>>() {})
    }

    fun grantGroupPermission(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        iamResourceCode: String,
        resourceName: String,
        iamGroupId: Int
    ) {
        val authorizationScopes = buildAuthorizationScopes(
            authorizationScopesStr = authorizationScopesStr,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = iamResourceCode,
            resourceName = resourceName
        )
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
        val groupConfigInfo = authResourceGroupConfigDao.get(
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
