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

package com.tencent.devops.common.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.auth.api.pojo.BkUserAuthVerifyRequest
import com.tencent.devops.common.auth.api.pojo.BkUserAuthVerifyResponse
import com.tencent.devops.common.auth.api.pojo.BkUserResourcesAuthRequest
import com.tencent.devops.common.auth.api.pojo.BkUserResourcesAuthResponse
import com.tencent.devops.common.auth.api.utils.AuthUtils
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.PROJECT_SCOPE_TYPE
import org.json.JSONObject
import org.slf4j.LoggerFactory

class BkAuthPermissionApi constructor(
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val authUtils: AuthUtils
) : AuthPermissionApi {

    override fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        permission: BkAuthPermission
    ): Boolean {
        return validateUserResourcePermission(user, serviceCode, resourceType, projectCode, "*", permission)
    }

    override fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String,
        permission: BkAuthPermission
    ): Boolean {
        return validateUserResourcePermission(
            bkAuthProperties.principalType!!, user, "project", projectCode, resourceType,
            resourceCode, permission, serviceCode, bkAuthProperties.appCode!!, bkAuthProperties.appSecret!!
        )
    }

    // 批量校验权限,查询用户是否有某个资源某个权限(新版权限中心）
    private fun validateUserResourcePermission(
        principalType: String, // "user"
        principalId: String, // 用户id
        scopeType: String, // "project"
        scopeId: String, // projectCode
        resourceType: BkAuthResourceType,
        resourceId: String,
        actionId: BkAuthPermission,
        systemId: AuthServiceCode, // 旧版本的serviceCode
        appCode: String,
        appSecret: String
    ): Boolean {
//        val epoch = System.currentTimeMillis()
        val uri = "/bkiam/api/v1/perm/systems/${systemId.id()}/resources-perms/batch-verify"

//        logger.info("开始调用权限中心校验权限，uri:$uri , systemId= ${systemId.id()}")

        val requestBean = BkUserAuthVerifyRequest(
            principalId, principalType,
            listOf(
                BkUserAuthVerifyRequest.ResourcesAction(
                    actionId.value,
                    listOf(
                        BkUserAuthVerifyRequest.ResourceId(
                            resourceId,
                            resourceType.value
                        )
                    ),
                    resourceType.value
                )
            ),
            scopeId, scopeType
        )

        val requestBeanString = objectMapper.writeValueAsString(requestBean)
        // 发送请求
        val responseBody =
            authUtils.doAuthPostRequest(uri, JSONObject(requestBeanString), appCode, appSecret)
        val responseBean =
            jacksonObjectMapper().readValue<BkUserAuthVerifyResponse>(responseBody.toString())

        return try {
            responseBean.data!![0]!!.pass
        } catch (ignored: Exception) {
            logger.error("bkiam, An exception occurs in the parse response bean, msg: $ignored", ignored)
            false
        }
    }

    override fun getUserResourceByPermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        permission: BkAuthPermission,
        supplier: (() -> List<String>)?
    ): List<String> {
        return getUserResourcesByPermissions(
            user,
            serviceCode,
            resourceType,
            projectCode,
            setOf(permission),
            supplier
        )[permission] ?: emptyList()
    }

    override fun getUserResourcesByPermissions(
        user: String,
        serviceCode: AuthServiceCode, // 对应新版的systemId
        resourceType: BkAuthResourceType,
        projectCode: String,
        permissions: Set<BkAuthPermission>,
        supplier: (() -> List<String>)?
    ): Map<BkAuthPermission, List<String>> {

        return getUserResourcesByPermissions(
            userId = user, scopeType = PROJECT_SCOPE_TYPE, scopeId = projectCode,
            resourceType = resourceType, permissions = permissions, systemId = serviceCode
        )
    }

    // 批量查询有权限的资源,若返回的map的Entry中Boolean为true，则表明用户对该资源拥有所有权限
    override fun getUserResourcesByPermissions(
        userId: String,
        scopeType: String,
        scopeId: String, // 项目id
        resourceType: BkAuthResourceType,
        permissions: Set<BkAuthPermission>,
        systemId: AuthServiceCode,
        supplier: (() -> List<String>)?
    ): Map<BkAuthPermission, List<String>> {

        val uri = "/bkiam/api/v1/perm/systems/${systemId.id()}/authorized-resources/search"
        val resultMap = LinkedHashMap<BkAuthPermission, List<String>>()

        val requestBean = BkUserResourcesAuthRequest(
            principalId = userId,
            principalType = bkAuthProperties.principalType!!,
            scopeType = scopeType,
            scopeId = scopeId,
            resourceTypesActions = permissions.map {
                BkUserResourcesAuthRequest.ResourceTypesAction(
                    it.value,
                    resourceType.value
                )
            },
            resourceDataType = "array",
            exactResource = true
        )

        val requestBeanString = objectMapper.writeValueAsString(requestBean)
        // 发送请求
        val responseBody =
            authUtils.doAuthPostRequest(
                uri,
                JSONObject(requestBeanString),
                bkAuthProperties.appCode!!,
                bkAuthProperties.appSecret!!
            )
        val responseBean =
            jacksonObjectMapper().readValue<BkUserResourcesAuthResponse>(responseBody.toString())

        try {
            responseBean.data!!.forEach { reqData ->
                putData(reqData, resultMap)
            }
            return resultMap
        } catch (ignored: Exception) {
            logger.error("bkiam, An exception occurs in the parse response bean, msg: $ignored")
            throw RemoteServiceException("bkiam, An exception occurs in the parse response bean, msg: $ignored")
        }
    }

    private fun putData(
        reqData: BkUserResourcesAuthResponse.Data,
        resultMap: LinkedHashMap<BkAuthPermission, List<String>>
    ) {
        val resourceIds = reqData.resourceIds
        val bkAuthPermission = BkAuthPermission.get(reqData.actionId)

        if (resourceIds.isEmpty()) {
            resultMap[bkAuthPermission] = emptyList()
        } else {
            val resources = mutableSetOf<String>()
            resourceIds.forEach { resourceId ->
                resourceId!!.forEach { resourceIdMap ->
                    resources.add(resourceIdMap!!["resource_id"] ?: "")
                }
            }

            resultMap[bkAuthPermission] = resources.toList()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthPermissionApi::class.java)
    }
}