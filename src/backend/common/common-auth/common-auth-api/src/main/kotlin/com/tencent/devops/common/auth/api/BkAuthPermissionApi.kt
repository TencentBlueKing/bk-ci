package com.tencent.devops.common.auth.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.auth.api.pojo.BkAuthPermissionVerifyRequest
import com.tencent.devops.common.auth.api.pojo.BkAuthPermissionsPolicyCodeAndResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthPermissionsResources
import com.tencent.devops.common.auth.api.pojo.BkAuthPermissionsResourcesRequest
import com.tencent.devops.common.auth.api.pojo.BkAuthResponse
import com.tencent.devops.common.auth.jmx.JmxAuthApi
import com.tencent.devops.common.auth.jmx.JmxAuthApi.Companion.LIST_USER_RESOURCE
import com.tencent.devops.common.auth.jmx.JmxAuthApi.Companion.LIST_USER_RESOURCES
import com.tencent.devops.common.auth.jmx.JmxAuthApi.Companion.VALIDATE_USER_RESOURCE
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Created by Aaron Sheng on 2018/1/14.
 */
@Component
class BkAuthPermissionApi @Autowired constructor(
    private val bkAuthProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper,
    private val bkAuthTokenApi: BkAuthTokenApi,
    private val jmxAuthApi: JmxAuthApi
) {
//    private val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(60L, TimeUnit.SECONDS)
//            .writeTimeout(60L, TimeUnit.SECONDS)
//            .build()

    fun validateUserResourcePermission(
        user: String,
        serviceCode: BkAuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        permission: BkAuthPermission
    ): Boolean {
        return validateUserResourcePermission(user, serviceCode, resourceType, projectCode, "*", permission)
    }

    fun validateUserResourcePermission(
        user: String,
        serviceCode: BkAuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String,
        permission: BkAuthPermission
    ): Boolean {
        val epoch = System.currentTimeMillis()
        var success = false
        try {
            val accessToken = bkAuthTokenApi.getAccessToken(serviceCode)
            val url = "${bkAuthProperties.url}/permission/project/service/policy/resource/user/verfiy?access_token=$accessToken"
            val bkAuthPermissionRequest = BkAuthPermissionVerifyRequest(
                    projectCode,
                    serviceCode.value,
                    resourceCode,
                    permission.value,
                    resourceType.value,
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
                        bkAuthTokenApi.refreshAccessToken(serviceCode)
                    }
                    logger.error("Fail to validate user permission. $responseContent")
                    throw RuntimeException("Fail to validate user permission")
                }
                return responseObject.code == 0
            }
        } finally {
            jmxAuthApi.execute(VALIDATE_USER_RESOURCE, System.currentTimeMillis() - epoch, success)
        }
    }

    fun getUserResourceByPermission(
        user: String,
        serviceCode: BkAuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        permission: BkAuthPermission
    ): List<String> {
        val epoch = System.currentTimeMillis()
        var success = false
        try {
            val accessToken = bkAuthTokenApi.getAccessToken(serviceCode)
            val url = "${bkAuthProperties.url}/permission/project/service/policy/user/query/resources?" +
                    "access_token=$accessToken&user_id=$user&project_code=$projectCode&service_code=${serviceCode.value}" +
                    "&resource_type=${resourceType.value}&policy_code=${permission.value}&is_exact_resource=1"
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
                        bkAuthTokenApi.refreshAccessToken(serviceCode)
                    }
                    logger.error("Fail to get user resource by permission. $responseContent")
                    throw RuntimeException("Fail to get user resource by permission")
                }
                return responseObject.data ?: emptyList()
            }
        } finally {
            jmxAuthApi.execute(LIST_USER_RESOURCE, System.currentTimeMillis() - epoch, success)
        }
    }

    fun getUserResourcesByPermissions(
        user: String,
        serviceCode: BkAuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        permissions: Set<BkAuthPermission>
    ): Map<BkAuthPermission, List<String>> {
        val epoch = System.currentTimeMillis()
        var success = false
        try {
            val accessToken = bkAuthTokenApi.getAccessToken(serviceCode)
            val url = "${bkAuthProperties.url}/permission/project/service/policies/user/query/resources?access_token=$accessToken"
            val policyResourceTypeList = permissions.map {
                BkAuthPermissionsPolicyCodeAndResourceType(it.value, resourceType.value)
            }
            val bkAuthPermissionsResourcesRequest = BkAuthPermissionsResourcesRequest(
                    projectCode,
                    serviceCode.value,
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
                        bkAuthTokenApi.refreshAccessToken(serviceCode)
                    }
                    logger.error("Fail to get user resources by permissions. $responseContent")
                    throw RuntimeException("Fail to get user resources by permissions")
                }

                val permissionsResourcesMap = mutableMapOf<BkAuthPermission, List<String>>()
                responseObject.data!!.forEach {
                    val bkAuthPermission = BkAuthPermission.get(it.policyCode)
                    val resourceList = it.resourceCodeList
                    permissionsResourcesMap[bkAuthPermission] = resourceList
                }
                return permissionsResourcesMap
            }
        } finally {
            jmxAuthApi.execute(LIST_USER_RESOURCES, System.currentTimeMillis() - epoch, success)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthPermissionApi::class.java)
    }
}