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

package com.tencent.devops.common.auth.api.external

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.UnauthorizedException
import com.tencent.devops.common.auth.api.pojo.external.BkAuthExAction
import com.tencent.devops.common.auth.api.pojo.external.BkAuthExResponse
import com.tencent.devops.common.auth.api.pojo.external.HEADER_APP_CODE
import com.tencent.devops.common.auth.api.pojo.external.HEADER_APP_SECRET
import com.tencent.devops.common.auth.api.pojo.external.model.*
import com.tencent.devops.common.auth.api.pojo.external.request.BkAuthExBatchAuthorizedUserRequest
import com.tencent.devops.common.auth.api.pojo.external.request.BkAuthExBatchPermissionVerityRequest
import com.tencent.devops.common.auth.api.pojo.external.request.BkAuthExPermissionVerifyRequest
import com.tencent.devops.common.auth.api.pojo.external.request.BkAuthExResourceListRequest
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.web.utils.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired


class BkAuthExPermissionApi @Autowired constructor(
        private val bkAuthProperties: BkAuthExProperties,
        private val objectMapper: ObjectMapper
) {

    /**
     * 查询指定用户特定权限下的代码检查任务清单
     */
    fun queryResourceListForUser(
            user: String,
            projectId: String,
            actions: List<BkAuthExAction>
    ): MutableSet<Long> {
        val actionList = actions.map {
            BkAuthExTypeActionModel(
                    actionId = it.actionName,
                    resourceType = bkAuthProperties.resourceType!!
            )
        }
        val result = queryResourceList(
                systemId = bkAuthProperties.systemId!!,
                principalType = bkAuthProperties.principalType!!,
                pricipalId = user,
                scopeType = bkAuthProperties.scopeType!!,
                scopeId = projectId,
                resourceTypesActions = actionList
        )
        if (!result.isSuccess()) {
            logger.error("mongorepository resource list failed! projectId: $projectId, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("mongorepository resource list failed!")
        }
        return result.data?.let {
            it.map { bkAuthExResourceListModel ->
                bkAuthExResourceListModel.resourceIds?.map { resourceId ->
                    if(resourceId.isNotEmpty())
                        resourceId[0].resourceId?.toLong() ?: -1
                    else
                        -1L
                } ?: emptyList()
            }.reduce { acc, list -> acc.plus(list) }
        }?.toMutableSet() ?: mutableSetOf()
    }


    /**
     * 查询指定代码检查任务下特定权限的用户清单
     */
    fun queryUserListForAction(
            taskId: String,
            projectId: String,
            actions: List<BkAuthExAction>
    ): Map<String?, List<String>> {
        val actionList = actions.map {
            BkAuthExBatchResouceActionModel(
                    actionId = it.actionName,
                    resourceType = bkAuthProperties.resourceType!!,
                    resourceId = listOf(BkAuthExSingleResourceModel(
                            resourceId = taskId,
                            resourceType = bkAuthProperties.resourceType
                    ))
            )
        }
        val result = queryauthorizedUserList(
                systemId = bkAuthProperties.systemId!!,
                scopeType = bkAuthProperties.scopeType!!,
                scopeId = projectId,
                resourcesActions = actionList
        )
        if (!result.isSuccess()) {
            logger.error("mongorepository user list failed! taskId: $taskId, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("mongorepository user list failed!")
        }
        return result.data?.associate {
            it.actionId to (it.principals?.map { bkAuthExPrincipalModel -> bkAuthExPrincipalModel.principalId }
                    ?: listOf())
        }
                ?: mapOf()
    }


    /**
     * 批量校验权限
     */
    fun validateBatchPermission(
            user: String,
            taskId: String?,
            projectId: String,
            action: List<BkAuthExAction>
    ): MutableList<BkAuthExResourceActionModel> {
        val actionList = action.map {
            BkAuthExResourceActionModel(
                    actionId = it.actionName,
                    resourceType = bkAuthProperties.resourceType!!,
                    resourceId = if (it == BkAuthExAction.ADMIN_MEMBER) emptyList()
                    else
                        listOf(BkAuthExSingleResourceModel(
                                resourceType = bkAuthProperties.resourceType,
                                resourceId = taskId
                        ))
            )
        }
        val result = validateUserBatchPermission(
                systemId = bkAuthProperties.systemId!!,
                principalType = bkAuthProperties.principalType!!,
                principalId = user,
                scopeType = bkAuthProperties.scopeType!!,
                scopeId = projectId,
                resourcesActions = actionList
        )
        if (!result.isSuccess()) {
            logger.error("batch authorization failed! user: $user, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("batch authorization failed!")
        }
        return result.data?.toMutableList() ?: mutableListOf()
    }


    /**
     * 单个权限校验
     */
    fun validatePermission(
            user: String,
            taskId: String,
            projectId: String,
            action: BkAuthExAction
    ): Boolean {
        val result = validateUserSinglePermission(
                systemId = bkAuthProperties.systemId!!,
                principalType = bkAuthProperties.principalType!!,
                principalId = user,
                scopeType = bkAuthProperties.scopeType!!,
                scopeId = projectId,
                actionId = action.actionName,
                resourceType = bkAuthProperties.resourceType!!,
                resourceId = taskId
        )
        if (!result.isSuccess()) {
            logger.error("single authorization failed! user: $user, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("single authorization failed!")
        }
        return result.data?.isPass ?: false
    }


    /**
     * 调用api进行单个权限校验
     */
    private fun validateUserSinglePermission(
            systemId: String,
            principalType: String,
            principalId: String,
            scopeType: String,
            scopeId: String,
            actionId: String,
            resourceType: String,
            resourceId: String
    ): BkAuthExResponse<BkAuthExSingleVerifyModel> {
        if (systemId.isEmpty()) {
            throw UnauthorizedException("system id is null!")
        }
        val url = "${bkAuthProperties.url}/bkiam/api/v1/perm/systems/$systemId/resources-perms/verify"
        val bkAuthExPermissionVerifyRequest = BkAuthExPermissionVerifyRequest(
                principalType = principalType,
                principalId = principalId,
                scopeType = scopeType,
                scopeId = scopeId,
                actionId = actionId,
                resourceType = resourceType,
                resourceId = resourceId
        )
        val content = objectMapper.writeValueAsString(bkAuthExPermissionVerifyRequest)
        val result = OkhttpUtils.doHttpPost(url, content, mapOf(
                HEADER_APP_CODE to bkAuthProperties.codeccCode!!,
                HEADER_APP_SECRET to bkAuthProperties.codeccSecret!!))
        return objectMapper.readValue(result, object : TypeReference<BkAuthExResponse<BkAuthExSingleVerifyModel>>() {})
    }


    /**
     * 调用api进行批量权限校验
     */
    private fun validateUserBatchPermission(
            systemId: String,
            principalType: String,
            principalId: String,
            scopeType: String,
            scopeId: String,
            resourcesActions: List<BkAuthExResourceActionModel>
    ): BkAuthExResponse<List<BkAuthExResourceActionModel>> {
        if (systemId.isEmpty()) {
            throw UnauthorizedException("system id is null!")
        }
        val url = "${bkAuthProperties.url}/bkiam/api/v1/perm/systems/$systemId/resources-perms/batch-verify"
        val bkAuthExBatchPermissionVerityRequest = BkAuthExBatchPermissionVerityRequest(
                principalType = principalType,
                principalId = principalId,
                scopeType = scopeType,
                scopeId = scopeId,
                resourcesActions = resourcesActions
        )
        val content = objectMapper.writeValueAsString(bkAuthExBatchPermissionVerityRequest)
        val result = OkhttpUtils.doHttpPost(url, content, mapOf(
                HEADER_APP_CODE to bkAuthProperties.codeccCode!!,
                HEADER_APP_SECRET to bkAuthProperties.codeccSecret!!
        ))
        return objectMapper.readValue(result, object : TypeReference<BkAuthExResponse<List<BkAuthExResourceActionModel>>>() {})
    }


    /**
     * 调用api批量查询有权限用户清单
     */
    private fun queryauthorizedUserList(
            systemId: String,
            scopeType: String,
            scopeId: String,
            resourcesActions: List<BkAuthExBatchResouceActionModel>
    ): BkAuthExResponse<List<BkAuthExBatchResouceActionModel>> {
        if (systemId.isEmpty()) {
            throw UnauthorizedException("system id is null!")
        }
        val url = "${bkAuthProperties.url}/bkiam/api/v1/perm/systems/$systemId/resources-perms-principals/search"
        val bkAuthExBatchAuthorizedUser = BkAuthExBatchAuthorizedUserRequest(
                scopeType = scopeType,
                scopeId = scopeId,
                resourcesActions = resourcesActions
        )
        val content = objectMapper.writeValueAsString(bkAuthExBatchAuthorizedUser)
        val result = OkhttpUtils.doHttpPost(url, content, mapOf(
                HEADER_APP_CODE to bkAuthProperties.codeccCode!!,
                HEADER_APP_SECRET to bkAuthProperties.codeccSecret!!
        ))
        return objectMapper.readValue(result, object : TypeReference<BkAuthExResponse<List<BkAuthExBatchResouceActionModel>>>() {})
    }


    /**
     * 查询资源实例清单
     */
    private fun queryResourceList(
            systemId: String,
            principalType: String,
            pricipalId: String,
            scopeType: String,
            scopeId: String,
            resourceTypesActions: List<BkAuthExTypeActionModel>
    ): BkAuthExResponse<List<BkAuthExResourceListModel>> {
        if (systemId.isEmpty()) {
            throw UnauthorizedException("system id is null!")
        }
        val url = "${bkAuthProperties.url}/bkiam/api/v1/perm/systems/$systemId/authorized-resources/search"
        val bkAuthExResourceListRequest = BkAuthExResourceListRequest(
                principalType = principalType,
                principalId = pricipalId,
                scopeType = scopeType,
                scopeId = scopeId,
                resourceTypesActions = resourceTypesActions
        )
        val content = objectMapper.writeValueAsString(bkAuthExResourceListRequest)
        val result = OkhttpUtils.doHttpPost(url, content, mapOf(
                HEADER_APP_CODE to bkAuthProperties.codeccCode!!,
                HEADER_APP_SECRET to bkAuthProperties.codeccSecret!!
        ))
        return objectMapper.readValue(result, object : TypeReference<BkAuthExResponse<List<BkAuthExResourceListModel>>>() {})
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthExPermissionApi::class.java)
    }
}