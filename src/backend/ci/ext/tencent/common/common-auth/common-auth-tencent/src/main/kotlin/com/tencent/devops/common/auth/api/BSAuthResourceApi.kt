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
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthResourceCreateRequest
import com.tencent.devops.common.auth.api.pojo.BkAuthResourceDeleteRequest
import com.tencent.devops.common.auth.api.pojo.BkAuthResourceModifyRequest
import com.tencent.devops.common.auth.api.pojo.BkAuthResponse
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.AuthServiceCode
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BSAuthResourceApi @Autowired constructor(
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val bsAuthTokenApi: BSAuthTokenApi
) : AuthResourceApi {

    override fun batchCreateResource(
        principalId: String,
        scopeType: String,
        scopeId: String,
        resourceType: AuthResourceType,
        resourceList: List<ResourceRegisterInfo>,
        systemId: AuthServiceCode
    ): Boolean {

        resourceList.forEach { resource ->
            createResource(
                user = principalId,
                serviceCode = systemId,
                resourceType = resourceType,
                projectCode = scopeId,
                resourceCode = resource.resourceCode,
                resourceName = resource.resourceName
            )
        }
        return true
    }

    override fun deleteResource(
        scopeType: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) {
        deleteResource(
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            resourceCode = resourceCode
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
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceName = resourceName
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
        createResource(
            user = user,
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceName = resourceName
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
        createGrantResource(
            user = user,
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceName = resourceName,
            authGroupList = null
        )
    }

    override fun createGrantResource(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String,
        authGroupList: List<BkAuthGroup>?
    ) {
        val authorizedGroups = if (authGroupList != null) StringUtils.join(authGroupList, ";").toLowerCase() else null
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/resource?access_token=$accessToken"
        val bkAuthResourceCreateRequest = BkAuthResourceCreateRequest(
            projectCode,
            serviceCode.id(),
            resourceCode,
            resourceName,
            resourceType.value,
            user,
            authorizedGroups
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
            logger.info("Auth create resource response: $responseContent")
            if (!response.isSuccessful) {
                logger.error("Fail to create auth resource. $responseContent")
                throw RemoteServiceException("Fail to create auth resource")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<String>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.error("Fail to create auth resource. $responseContent")
                throw RemoteServiceException("Fail to create auth resource")
            }
        }
    }

    override fun modifyResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/resource/modify?access_token=$accessToken"
        val bkAuthResourceCreateRequest = BkAuthResourceModifyRequest(
            projectCode,
            serviceCode.id(),
            resourceCode,
            resourceName,
            resourceType.value
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
                logger.warn("Fail to modify auth resource. $responseContent")
                // throw RemoteServiceException("Fail to modify auth resource")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<String>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.warn("Fail to modify auth resource. $responseContent")
                // throw RemoteServiceException("Fail to modify auth resource")
            }
        }
    }

    override fun deleteResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) {
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/resource?access_token=$accessToken"
        val bkAuthResourceDeleteRequest = BkAuthResourceDeleteRequest(
            projectCode,
            serviceCode.id(),
            resourceCode,
            resourceType.value
        )
        val content = objectMapper.writeValueAsString(bkAuthResourceDeleteRequest)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder()
            .url(url)
            .delete(requestBody)
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("Auth delete resource response: $responseContent")
            if (!response.isSuccessful) {
                logger.error("Fail to delete auth resource. $responseContent")
                throw RemoteServiceException("Fail to delete auth resource")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<String>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.error("Fail to delete auth resource. $responseContent")
                throw RemoteServiceException("Fail to delete auth resource")
            }
        }
    }

    override fun cancelCreateResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) = Unit

    override fun cancelUpdateResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String
    ) = Unit

    override fun batchCreateResource(
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        user: String,
        resourceList: List<ResourceRegisterInfo>
    ) {
        val accessToken = bsAuthTokenApi.getAccessToken(serviceCode)
        val url = "${bkAuthProperties.url}/resource/batch_register?access_token=$accessToken"

        val requestData = mapOf(
            "project_code" to projectCode,
            "service_code" to serviceCode.id(),
            "resource_type" to resourceType.value,
            "creator" to user,
            "resource_register_info_list" to resourceList.map {
                mapOf(
                    "resource_code" to it.resourceCode,
                    "resource_name" to it.resourceName
                )
            }
        )

        val content = objectMapper.writeValueAsString(requestData)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("Auth batch create resource response: $responseContent")

            if (!response.isSuccessful) {
                logger.error("Fail to create auth resource. $responseContent")
                throw RemoteServiceException("Fail to create auth resource")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<String>>(responseContent)
            if (responseObject.code != 0) {

                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(serviceCode)
                }
                logger.error("Fail to create auth resource. $responseContent")
                throw RemoteServiceException("Fail to create auth resource")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BSAuthResourceApi::class.java)
        private const val HTTP_403 = 403
    }
}
