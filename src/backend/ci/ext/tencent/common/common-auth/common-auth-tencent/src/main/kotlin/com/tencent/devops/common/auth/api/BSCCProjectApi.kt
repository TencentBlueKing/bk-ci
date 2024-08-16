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
import com.tencent.devops.common.auth.api.pojo.BkAuthProject
import com.tencent.devops.common.auth.api.pojo.BkAuthResponse
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class BSCCProjectApi @Autowired constructor(
    private val bkCCProperties: BkCCProperties,
    private val objectMapper: ObjectMapper,
    private val bsAuthTokenApi: AuthTokenApi,
    private val bsPipelineAuthServiceCode: PipelineAuthServiceCode
) {

    fun getProject(projectCode: String): BkAuthProject? {
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        val url = "${bkCCProperties.url}/projects/$projectCode?access_token=$accessToken"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get project $projectCode. $responseContent")
                throw RemoteServiceException("Fail to get project $projectCode")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<BkAuthProject>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(bsPipelineAuthServiceCode)
                }
                logger.warn("Fail to get project $projectCode. $responseContent")
//                throw RemoteServiceException("Fail to get project $projectCode")
                // #2836 只有当权限中心出现500系统，才抛出异常
                if (responseObject.code >= HTTP_500) {
                    throw RemoteServiceException(
                        httpStatus = responseObject.code, errorMessage = responseObject.message
                    )
                }
            }
            return responseObject.data
        }
    }

    fun getProjectList(projectCodes: Set<String>): List<BkAuthProject> {
        if (projectCodes.isEmpty()) {
            return emptyList()
        }
        val accessToken = bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
        val url = "${bkCCProperties.url}/project_list/?access_token=$accessToken"
        logger.info("BSCCProjectApi getProjectList url:$url")
        val requestContent = objectMapper.writeValueAsString(mapOf("project_codes" to projectCodes))
        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), requestContent))
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to list project. $responseContent")
                throw RemoteServiceException("Fail to list project.")
            }

            val responseObject = objectMapper.readValue<BkAuthResponse<List<BkAuthProject>>>(responseContent)
            if (responseObject.code != 0) {
                if (responseObject.code == HTTP_403) {
                    bsAuthTokenApi.refreshAccessToken(bsPipelineAuthServiceCode)
                }
                logger.warn("Fail to list projects. $responseContent")
//                throw RemoteServiceException("Fail to get projects")
                // #2836 只有当权限中心出现500系统，才抛出异常
                if (responseObject.code >= HTTP_500) {
                    throw RemoteServiceException(
                        httpStatus = responseObject.code, errorMessage = responseObject.message
                    )
                }
            }
            return responseObject.data ?: emptyList()
        }
    }

    /**
     * 外网使用获取project列表
     */
    fun getProjectListAsOuter(projectCodes: Set<String>): List<BkAuthProject> {
        return getProjectList(projectCodes).map {
            BkAuthProject(
                bgId = it.bgId,
                bgName = it.bgName,
                ccAppId = it.ccAppId,
                centerId = it.centerId,
                centerName = it.centerName,
                createdAt = it.createdAt,
                creator = it.creator,
                deptId = it.deptId,
                deptName = it.deptName,
                description = it.description,
                projectCode = it.projectCode,
                isOfflined = it.isOfflined,
                logoAddr = it.logoAddr,
                projectId = it.projectId,
                projectName = it.projectName,
                projectType = it.projectType,
                updatedAt = it.updatedAt,
                useBk = it.useBk,
                approvalStatus = it.approvalStatus
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BSCCProjectApi::class.java)
        private const val HTTP_403 = 403
        private const val HTTP_500 = 500
    }
}
