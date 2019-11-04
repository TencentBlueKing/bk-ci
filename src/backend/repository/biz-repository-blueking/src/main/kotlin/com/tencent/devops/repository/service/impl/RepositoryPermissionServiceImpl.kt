/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.repository.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.*
import com.tencent.devops.common.auth.api.pojo.*
import com.tencent.devops.common.auth.code.BkCodeAuthServiceCode
import com.tencent.devops.repository.service.RepositoryPermissionService
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryPermissionServiceImpl @Autowired constructor(
        private val authResourceApi: AuthResourceApi,
        private val authPermissionApi: AuthPermissionApi,
        private val codeAuthServiceCode: BkCodeAuthServiceCode,
        private val bkAuthTokenApi: BkAuthTokenApi,
        private val bkAuthProperties: BkAuthProperties,
        private val objectMapper: ObjectMapper
//        private val jmxAuthApi: JmxAuthApi
) : RepositoryPermissionService {

    override fun validatePermission(
            userId: String,
            projectId: String,
            authPermission: AuthPermission,
            repositoryId: Long?,
            message: String
    ) {
        if (!hasPermission(userId, projectId, authPermission, repositoryId)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun filterRepository(userId: String, projectId: String, authPermission: AuthPermission): List<Long> {
        val resourceCodeList = authPermissionApi.getUserResourceByPermission(
            user = userId,
            serviceCode = codeAuthServiceCode,
            resourceType = AuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            permission = authPermission,
            supplier = null
        )
        return resourceCodeList.map { it.toLong() }
    }

    override fun filterRepositories(
            userId: String,
            projectId: String,
            authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourcesMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = codeAuthServiceCode,
            resourceType = AuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = null
        )
        return permissionResourcesMap.mapValues {
            it.value.map { id -> id.toLong() }
        }
    }

    override fun hasPermission(
            userId: String,
            projectId: String,
            authPermission: AuthPermission,
            repositoryId: Long?
    ): Boolean {
        if (repositoryId == null)
            return authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = codeAuthServiceCode,
                resourceType = AuthResourceType.CODE_REPERTORY,
                projectCode = projectId,
                permission = authPermission
            )
        else
            return authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = codeAuthServiceCode,
                resourceType = AuthResourceType.CODE_REPERTORY,
                projectCode = projectId,
                resourceCode = repositoryId.toString(),
                permission = authPermission
            )
    }

    override fun createResource(userId: String, projectId: String, repositoryId: Long, repositoryName: String) {
        authResourceApi.createResource(
            user = userId,
            serviceCode = codeAuthServiceCode,
            resourceType = AuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            resourceCode = repositoryId.toString(),
            resourceName = repositoryName
        )
    }

    override fun editResource(projectId: String, repositoryId: Long, repositoryName: String) {
        authResourceApi.modifyResource(
            serviceCode = codeAuthServiceCode,
            resourceType = AuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            resourceCode = repositoryId.toString(),
            resourceName = repositoryName
        )
    }

    override fun deleteResource(projectId: String, repositoryId: Long) {
        authResourceApi.deleteResource(
            serviceCode = codeAuthServiceCode,
            resourceType = AuthResourceType.CODE_REPERTORY,
            projectCode = projectId,
            resourceCode = repositoryId.toString()
        )
    }

    override fun getUserResourcesByPermissions(user: String, projectCode: String, permissions: Set<AuthPermission>): Map<AuthPermission, List<String>> {
        val epoch = System.currentTimeMillis()
        var success = false
        try {
            val accessToken = bkAuthTokenApi.getAccessToken(codeAuthServiceCode)
            val url = "${bkAuthProperties.url}/permission/project/service/policies/user/query/resources?access_token=$accessToken"
            val policyResourceTypeList = permissions.map {
                BkAuthPermissionsPolicyCodeAndResourceType(it.value, AuthResourceType.CODE_REPERTORY.value)
            }
            val bkAuthPermissionsResourcesRequest = BkAuthPermissionsResourcesRequest(
                    projectCode,
                    codeAuthServiceCode.id(),
                    policyResourceTypeList,
                    user
            )
            val content = objectMapper.writeValueAsString(bkAuthPermissionsResourcesRequest)
            val mediaType = MediaType.parse("application/json; charset=utf-8")
            val requestBody = RequestBody.create(mediaType, content)

            val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

//            val httpClient = okHttpClient.newBuilder().build()
//            httpClient.newCall(request).execute().use { response ->
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Fail to get user resources by permissions. $responseContent")
                    throw RuntimeException("Fail to get user resources by permissions")
                }

                success = true
                val responseObject = objectMapper.readValue<BkAuthResponse<List<BkAuthPermissionsResources>>>(responseContent)
                if (responseObject.code != 0) {
                    if (responseObject.code == 403) {
                        bkAuthTokenApi.refreshAccessToken(codeAuthServiceCode)
                    }
                    logger.error("Fail to get user resources by permissions. $responseContent")
                    throw RuntimeException("Fail to get user resources by permissions")
                }

                val permissionsResourcesMap = mutableMapOf<AuthPermission, List<String>>()
                responseObject.data!!.forEach {
                    val bkAuthPermission = AuthPermission.get(it.policyCode)
                    val resourceList = it.resourceCodeList
                    permissionsResourcesMap[bkAuthPermission] = resourceList
                }
                return permissionsResourcesMap
            }
        } finally {
//            jmxAuthApi.execute(LIST_USER_RESOURCES, System.currentTimeMillis() - epoch, success)
        }
    }

    override fun getUserResourceByPermission(user: String, projectCode: String, permission: AuthPermission): List<String> {
        val epoch = System.currentTimeMillis()
        var success = false
        try {
            val accessToken = bkAuthTokenApi.getAccessToken(codeAuthServiceCode)
            val url = "${bkAuthProperties.url}/permission/project/service/policy/user/query/resources?" +
                    "access_token=$accessToken&user_id=$user&project_code=$projectCode&service_code=${codeAuthServiceCode.id()}" +
                    "&resource_type=${AuthResourceType.CODE_REPERTORY.value}&policy_code=${permission.value}&is_exact_resource=1"
            val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

//            val httpClient = okHttpClient.newBuilder().build()
//            httpClient.newCall(request).execute().use { response ->
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Fail to get user resource by permission. $responseContent")
                    throw RuntimeException("Fail to get user resource by permission")
                }

                success = true
                val responseObject = objectMapper.readValue<BkAuthResponse<List<String>>>(responseContent)
                if (responseObject.code != 0) {
                    if (responseObject.code == 403) {
                        bkAuthTokenApi.refreshAccessToken(codeAuthServiceCode)
                    }
                    logger.error("Fail to get user resource by permission. $responseContent")
                    throw RuntimeException("Fail to get user resource by permission")
                }
                return responseObject.data ?: emptyList()
            }
        } finally {
//            jmxAuthApi.execute(LIST_USER_RESOURCE, System.currentTimeMillis() - epoch, success)
        }
    }

    override fun validateUserResourcePermission(user: String, projectCode: String, permission: AuthPermission): Boolean {
        return validateUserResourcePermission(user, projectCode, "*" , permission)
    }

    override fun validateUserResourcePermission(user: String, projectCode: String, resourceCode: String, permission: AuthPermission): Boolean {
        val epoch = System.currentTimeMillis()
        var success = false
        try {
            val accessToken = bkAuthTokenApi.getAccessToken(codeAuthServiceCode)
            val url = "${bkAuthProperties.url}/permission/project/service/policy/resource/user/verfiy?access_token=$accessToken"
            val bkAuthPermissionRequest = AuthPermissionVerifyRequest(
                    projectCode,
                    codeAuthServiceCode.id(),
                    resourceCode,
                    permission.value,
                    AuthResourceType.CODE_REPERTORY.value,
                    user
            )
            val content = objectMapper.writeValueAsString(bkAuthPermissionRequest)
            val mediaType = MediaType.parse("application/json; charset=utf-8")
            val requestBody = RequestBody.create(mediaType, content)

            val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

//            val httpClient = okHttpClient.newBuilder().build()
//            httpClient.newCall(request).execute().use { response ->
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body()!!.string()
                if (!response.isSuccessful) {
                    logger.error("Fail to validate user permission. $responseContent")
                    throw RuntimeException("Fail to validate user permission")
                }

                success = true
                val responseObject = objectMapper.readValue<BkAuthResponse<String>>(responseContent)
                if (responseObject.code != 0 && responseObject.code != 400) {
                    if (responseObject.code == 403) {
                        bkAuthTokenApi.refreshAccessToken(codeAuthServiceCode)
                    }
                    logger.error("Fail to validate user permission. $responseContent")
                    throw RuntimeException("Fail to validate user permission")
                }
                return responseObject.code == 0
            }
        } finally {
//            jmxAuthApi.execute(VALIDATE_USER_RESOURCE, System.currentTimeMillis() - epoch, success)
        }
    }

    override fun modifyResource(projectCode: String, resourceCode: String, resourceName: String) {
        val accessToken = bkAuthTokenApi.getAccessToken(codeAuthServiceCode)
        val url = "${bkAuthProperties.url}/resource/modify?access_token=$accessToken"
        val bkAuthResourceCreateRequest = BkAuthResourceModifyRequest(
                projectCode,
                codeAuthServiceCode.id(),
                resourceCode,
                resourceName,
                AuthResourceType.CODE_REPERTORY.value
        )
        val content = objectMapper.writeValueAsString(bkAuthResourceCreateRequest)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

//        val httpClient = okHttpClient.newBuilder().build()
//        httpClient.newCall(request).execute().use { response ->
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("Auth modify resource response: $responseContent")
            if (!response.isSuccessful) {
                logger.error("Fail to modify auth resource. $responseContent")
                throw RuntimeException("Fail to modify auth resource")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<String>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == 403) {
                    bkAuthTokenApi.refreshAccessToken(codeAuthServiceCode)
                }
                logger.error("Fail to modify auth resource. $responseContent")
                throw RuntimeException("Fail to modify auth resource")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthPermissionApi::class.java)
    }
}
