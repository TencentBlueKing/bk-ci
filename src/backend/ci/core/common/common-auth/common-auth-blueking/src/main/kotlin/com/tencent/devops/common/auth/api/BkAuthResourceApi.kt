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
 */

package com.tencent.devops.common.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkDeleteResourceAuthRequest
import com.tencent.devops.common.auth.api.pojo.BkDeleteResourceAuthResponse
import com.tencent.devops.common.auth.api.pojo.BkRegisterResourcesRequest
import com.tencent.devops.common.auth.api.pojo.BkRegisterResourcesResponse
import com.tencent.devops.common.auth.api.pojo.BkUpdateResourceRequest
import com.tencent.devops.common.auth.api.pojo.BkUpdateResourceResponse
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.api.utils.AuthUtils
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.PROJECT_SCOPE_TYPE
import org.json.JSONObject
import org.slf4j.LoggerFactory

@Suppress("ALL")
class BkAuthResourceApi constructor(
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val authUtils: AuthUtils
) : AuthResourceApi {

    override fun createGrantResource(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String,
        authGroupList: List<BkAuthGroup>?
    ) {
        batchCreateResource(
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            user = user,
            resourceList = listOf(ResourceRegisterInfo(resourceCode = resourceCode, resourceName = resourceName))
        )
    }

    override fun createResource(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        batchCreateResource(
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            user = user,
            resourceList = listOf(ResourceRegisterInfo(resourceCode = resourceCode, resourceName = resourceName))
        )
    }

    override fun createResource(
        scopeType: String,
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        batchCreateResource(
            principalId = user,
            scopeType = scopeType,
            scopeId = projectCode,
            resourceType = resourceType,
            resourceList = listOf(ResourceRegisterInfo(resourceCode = resourceCode, resourceName = resourceName)),
            systemId = serviceCode
        )
    }

    /**
     * 新版批量注册资源
     */
    override fun batchCreateResource(
        principalId: String,
        scopeType: String, // "project"
        scopeId: String,
        resourceType: AuthResourceType,
        resourceList: List<ResourceRegisterInfo>,
        systemId: AuthServiceCode // 旧版本的serviceCode
    ): Boolean {
        val principalType = bkAuthProperties.principalType!!
        val uri = "/bkiam/api/v1/perm/systems/${systemId.id()}/resources/batch-register"
//        logger.info("开始调用权限中心批量注册资源，uri:$uri , systemId= ${systemId.id()}")

        try {
            val resourceIds = resourceList.map {
                BkRegisterResourcesRequest.Resource(
                    resourceId = setOf(
                        BkRegisterResourcesRequest.ResourceId(
                            resourceId = it.resourceCode,
                            resourceType = resourceType.value
                        )
                    ),
                    resourceName = it.resourceName,
                    resourceType = resourceType.value,
                    scopeId = scopeId,
                    scopeType = scopeType
                )
            }.toSet()

            val requestBean = BkRegisterResourcesRequest(
                creatorId = principalId,
                creatorType = principalType,
                resources = resourceIds
            )
            val requestBeanString = objectMapper.writeValueAsString(requestBean)
            // 发送请求
            val responseBody =
                authUtils.doAuthPostRequest(
                    uri = uri,
                    jsonbody = JSONObject(requestBeanString),
                    bkAppCode = bkAuthProperties.appCode!!,
                    bkAppSecret = bkAuthProperties.appSecret!!
                )
            val responseBean = objectMapper.readValue<BkRegisterResourcesResponse>(responseBody.toString())

            if (!responseBean.result) {
                logger.warn("bkiam create resources failed, msg: ${responseBean.message}")
            }
//            logger.info("结束调用权限中心批量注册资源，uri:$uri , systemId= ${systemId.id()}")
            return true
        } catch (ignored: Exception) {
            logger.warn("bkiam, create resources exception, msg: $ignored")
            throw RemoteServiceException(ignored.message!!)
        }
    }

    override fun modifyResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        modifyResource(
            scopeType = PROJECT_SCOPE_TYPE,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName,
            serviceCode = serviceCode
        )
    }

    override fun modifyResource(
        scopeType: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        modifyResource(
            scopeType = scopeType,
            scopeId = projectCode,
            resourceType = resourceType,
            resourceId = resourceCode,
            resourceName = resourceName,
            systemId = serviceCode
        )
    }

    private fun modifyResource(
        scopeType: String,
        scopeId: String,
        resourceType: AuthResourceType,
        resourceId: String,
        resourceName: String,
        systemId: AuthServiceCode
    ) {
        val uri = "/bkiam/api/v1/perm/systems/${systemId.id()}/resources"

//        logger.info("开始调用权限中心修改资源接口，uri:$uri , systemId= ${systemId.id()}")

        try {
            val requestBean = BkUpdateResourceRequest(
                resourceId = setOf(
                    BkUpdateResourceRequest.ResourceId(
                        resourceId = resourceId,
                        resourceType = resourceType.value
                    )
                ),
                resourceName = resourceName,
                resourceType = resourceType.value,
                scopeId = scopeId,
                scopeType = scopeType
            )
            val requestBeanString = objectMapper.writeValueAsString(requestBean)
            // 发送请求
            val responseBody =
                authUtils.doAuthPutRequest(
                    uri = uri,
                    jsonbody = JSONObject(requestBeanString),
                    xBkAppCode = bkAuthProperties.appCode!!,
                    xBkAppSecret = bkAuthProperties.appSecret!!
                )
            val responseBean = objectMapper.readValue<BkUpdateResourceResponse>(responseBody.toString())

            if (!responseBean.result) {
                logger.warn("bkiam update resources failed, msg: ${responseBean.message}")
            }
//            logger.info("结束调用权限中心更新资源，uri:$uri , systemId= ${systemId.id()}")
        } catch (ignored: Exception) {
            logger.warn("bkiam, update resources exception, msg: $ignored")
            throw RemoteServiceException(ignored.message!!)
        }
    }

    override fun deleteResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) {
        deleteResource(
            scopeType = PROJECT_SCOPE_TYPE,
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            resourceCode = resourceCode
        )
    }

    override fun deleteResource(
        scopeType: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) {
        batchDeleteResource(
            scopeType = scopeType,
            scopeId = projectCode,
            resources = setOf(
                BkDeleteResourceAuthRequest.ResourceId(
                    resourceId = resourceCode,
                    resourceType = resourceType.value
                )
            ),
            systemId = serviceCode
        )
    }

    override fun cancelCreateResource(
        userId: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) = Unit

    override fun cancelUpdateResource(
        userId: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) = Unit

    // 批量删除资源权限
    private fun batchDeleteResource(
        scopeType: String,
        scopeId: String,
        resources: Set<BkDeleteResourceAuthRequest.ResourceId>,
        systemId: AuthServiceCode
    ) {
        val uri = "/bkiam/api/v1/perm/systems/${systemId.id()}/resources/batch-delete"

//        logger.info("开始调用权限中心删除资源权限接口，uri:$uri , systemId= ${systemId.id()}")

        try {
            val requestBean = BkDeleteResourceAuthRequest(resources = resources.map {
                BkDeleteResourceAuthRequest.Resource(
                    resourceId = setOf(
                        BkDeleteResourceAuthRequest.ResourceId(
                            resourceId = it.resourceId,
                            resourceType = it.resourceType
                        )
                    ), resourceType = it.resourceType, scopeId = scopeId, scopeType = scopeType
                )
            })

            val requestBeanString = objectMapper.writeValueAsString(requestBean)
            // 发送请求
            val responseBody =
                authUtils.doAuthDeleteRequest(
                    uri = uri,
                    jsonbody = JSONObject(requestBeanString),
                    xBkAppCode = bkAuthProperties.appCode!!,
                    xBkAppSecret = bkAuthProperties.appSecret!!
                )
            val responseBean = objectMapper.readValue<BkDeleteResourceAuthResponse>(responseBody.toString())

            if (!responseBean.result) {
                logger.warn("bkiam delete resources failed, msg: ${responseBean.message}")
            }
//            logger.info("结束调用权限中心删除资源权限接口，uri:$uri , systemId= ${systemId.id()}")
        } catch (ignored: Exception) {
            logger.warn("bkiam, delete resources exception, msg: $ignored")
            throw RemoteServiceException(ignored.message!!)
        }
    }

    override fun batchCreateResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        user: String,
        resourceList: List<ResourceRegisterInfo>
    ) {
        batchCreateResource(
            principalId = user,
            scopeType = PROJECT_SCOPE_TYPE,
            scopeId = projectCode,
            resourceType = resourceType,
            resourceList = resourceList,
            systemId = serviceCode
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthResourceApi::class.java)
    }
}
