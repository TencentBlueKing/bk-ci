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

package com.tencent.devops.project.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.project.pojo.BcsProjectForCreate
import com.tencent.devops.project.pojo.BcsProjectForUpdate
import com.tencent.devops.project.pojo.BcsProjectInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectUpdateLogoInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.tof.Response
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ProjectPaasCCService @Autowired constructor(
    val objectMapper: ObjectMapper
) {
    @Value("\${paas_cc.new_url}")
    private lateinit var ccUrl: String

    @Value("\${bcs.token}")
    private lateinit var bcsToken: String

    fun createPaasCCProject(
        userId: String,
        accessToken: String,
        projectCreateInfo: ProjectCreateInfo,
        projectId: String
    ) {
        logger.info("Create the bcs project $projectCreateInfo by user $userId with token $accessToken")
        val bscProject = BcsProjectForCreate(
            projectCode = projectCreateInfo.englishName,
            name = projectCreateInfo.projectName,
            projectType = projectCreateInfo.projectType,
            description = projectCreateInfo.description,
            bgId = projectCreateInfo.bgId.toString(),
            bgName = projectCreateInfo.bgName,
            deptID = projectCreateInfo.deptId.toString(),
            deptName = projectCreateInfo.deptName,
            centerID = projectCreateInfo.centerId.toString(),
            centerName = projectCreateInfo.centerName,
            isSecret = projectCreateInfo.secrecy,
            creator = userId,
            projectID = projectId
        )

        val url = ccUrl
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val param = objectMapper.writeValueAsString(bscProject)
        val requestBody = RequestBody.create(mediaType, param)
        logger.info("createBcsProject url:$url, body:$requestBody")
        val request = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $bcsToken")
            .addHeader("X-Project-Username", userId)
            .post(requestBody).build()
        val responseContent = request(request, "调用BSC接口创建项目失败")
        val result = objectMapper.readValue<Result<Map<String, Any>>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to create the projects in bcs with response $responseContent")
            throw OperationException("同步项目到BCS失败")
        }
    }

    fun updatePaasCCProject(
        userId: String,
        accessToken: String,
        projectUpdateInfo: ProjectUpdateInfo,
        projectId: String
    ) {
        logger.info("Update the bcs project $projectUpdateInfo by user $userId with token $accessToken")
        val bscProjectUpdate = BcsProjectForUpdate(
            updater = userId,
            name = projectUpdateInfo.projectName,
            projectType = projectUpdateInfo.projectType,
            bgId = projectUpdateInfo.bgId.toString(),
            bgName = projectUpdateInfo.bgName,
            deptID = projectUpdateInfo.deptId.toString(),
            deptName = projectUpdateInfo.deptName,
            centerID = projectUpdateInfo.centerId.toString(),
            centerName = projectUpdateInfo.centerName,
            isSecret = projectUpdateInfo.secrecy,
            description = projectUpdateInfo.description
        )
        val url = "$ccUrl/$projectId"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val param = objectMapper.writeValueAsString(bscProjectUpdate)
        val requestBody = RequestBody.create(mediaType, param)
        logger.info("updateBcsProject url:$url, body:$requestBody")
        val request = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $bcsToken")
            .addHeader("X-Project-Username", userId)
            .put(requestBody).build()
        val responseContent = request(request, "更新bcs的项目信息失败")
        logger.info("Success to update the project with response $responseContent")
        val result: Response<Any> = objectMapper.readValue(responseContent)

        if (result.code.toInt() != 0) {
            logger.warn("Fail to update the project in bcs with response $responseContent")
            throw OperationException("更新bcs的项目信息失败")
        }
    }

    fun updatePaasCCProjectLogo(
        userId: String,
        accessToken: String,
        projectUpdateLogoInfo: ProjectUpdateLogoInfo,
        projectId: String
    ) {
        logger.info("Update the bcs projectLogo $projectUpdateLogoInfo by user $userId with token $accessToken")

        val url = "$ccUrl/$projectId"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val param = objectMapper.writeValueAsString(projectUpdateLogoInfo)
        val requestBody = RequestBody.create(mediaType, param)
        val request = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $bcsToken")
            .addHeader("X-Project-Username", userId)
            .put(requestBody).build()
        val responseContent = request(request, "更新bcs的项目LOGO信息失败")
        logger.info("Success to update the projectLogo with response $responseContent")
        val result: Response<Any> = objectMapper.readValue(responseContent)

        if (result.code.toInt() != 0) {
            logger.warn("Fail to update the projectLogo in bcs with response $responseContent")
            throw OperationException("更新bcs的项目LOGO信息失败")
        }
    }

    fun getPaasCCProjectInfo(projectCode: String, accessToken: String): BcsProjectInfo? {
        logger.info("get the bsc projectInfo $projectCode with token $accessToken")
        val url = "$ccUrl/$projectCode"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val request = Request.Builder().url(url)
            .addHeader("Authorization", "Bearer $bcsToken")
            .get().build()
        val responseContent = request(request, "获取PAASCC项目信息失败")
        val result = objectMapper.readValue<Result<BcsProjectInfo>>(responseContent)
        if (result.code != 0) {
            if (result.code == 2001600) {
                logger.warn("Fail to get Project in bcs with: ${result.message}")
                return result.data
            }
            logger.warn("Fail to get Project in bcs with response $responseContent")
        }
        return result.data
    }

    private fun request(request: Request, errorMessage: String): String {
//        val httpClient = okHttpClient.newBuilder().build()
        OkhttpUtils.doHttp(request).use { response ->
            //        httpClient.newCall(request).execute().use { response ->
            val responseContent = response.body()!!.string()
            logger.info("bcs: $responseContent")
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()} " +
                                ", message ${response.message()} and response $responseContent")
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectPaasCCService::class.java)
    }
}
