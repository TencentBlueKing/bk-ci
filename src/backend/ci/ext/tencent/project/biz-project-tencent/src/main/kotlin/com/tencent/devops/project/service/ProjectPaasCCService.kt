package com.tencent.devops.project.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.project.pojo.PaasCCProjectForCreate
import com.tencent.devops.project.pojo.PaasCCProjectForUpdate
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

    fun createPaasCCProject(
        userId: String,
        accessToken: String,
        projectCreateInfo: ProjectCreateInfo,
        projectId: String
    ) {
        logger.info("Create the paas cc project $projectCreateInfo by user $userId with token $accessToken")
        val paasCCProject = PaasCCProjectForCreate(
            project_name = projectCreateInfo.projectName,
            english_name = projectCreateInfo.englishName,
            project_type = projectCreateInfo.projectType,
            description = projectCreateInfo.description,
            bg_id = projectCreateInfo.bgId,
            bg_name = projectCreateInfo.bgName,
            dept_id = projectCreateInfo.deptId,
            dept_name = projectCreateInfo.deptName,
            center_id = projectCreateInfo.centerId,
            center_name = projectCreateInfo.centerName,
            is_secrecy = projectCreateInfo.secrecy,
            kind = projectCreateInfo.kind,
            project_id = projectId,
            creator = userId
        )

        val url = "$ccUrl/?access_token=$accessToken"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val param = objectMapper.writeValueAsString(paasCCProject)
        val requestBody = RequestBody.create(mediaType, param)
        logger.info("createPaasCCProject url:$url, body:$requestBody")
        val request = Request.Builder().url(url).post(requestBody).build()
        val responseContent = request(request, "调用PaasCC接口创建项目失败")
        val result = objectMapper.readValue<Result<Map<String, Any>>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to create the projects in paas cc with response $responseContent")
            throw OperationException("同步项目到PaasCC失败")
        }
    }

    fun updatePaasCCProject(
        userId: String,
        accessToken: String,
        projectUpdateInfo: ProjectUpdateInfo,
        projectId: String
    ) {
        logger.info("Update the paas cc project $projectUpdateInfo by user $userId with token $accessToken")
        val paasCCProjectForUpdate = PaasCCProjectForUpdate(
            project_name = projectUpdateInfo.projectName,
            project_code = projectUpdateInfo.englishName,
            project_type = projectUpdateInfo.projectType,
            bg_id = projectUpdateInfo.bgId,
            bg_name = projectUpdateInfo.bgName,
            center_id = projectUpdateInfo.centerId,
            center_name = projectUpdateInfo.centerName,
            dept_id = projectUpdateInfo.deptId,
            dept_name = projectUpdateInfo.deptName,
            description = projectUpdateInfo.description,
            english_name = projectUpdateInfo.englishName,
            updator = userId,
            cc_app_id = projectUpdateInfo.ccAppId,
            cc_app_name = projectUpdateInfo.ccAppName,
            kind = projectUpdateInfo.kind,
            secrecy = projectUpdateInfo.secrecy
        )

        val url = "$ccUrl/$projectId?access_token=$accessToken"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val param = objectMapper.writeValueAsString(paasCCProjectForUpdate)
        val requestBody = RequestBody.create(mediaType, param)
        logger.info("updatePaasCCProject url:$url, body:$requestBody")
        val request = Request.Builder().url(url).put(requestBody).build()
        val responseContent = request(request, "更新PaaSCC的项目信息失败")
        logger.info("Success to update the project with response $responseContent")
        val result: Response<Any> = objectMapper.readValue(responseContent)

        if (result.code.toInt() != 0) {
            logger.warn("Fail to update the project in paas cc with response $responseContent")
            throw OperationException("更新PaaSCC的项目信息失败")
        }
    }

    fun updatePaasCCProjectLogo(
        userId: String,
        accessToken: String,
        projectUpdateLogoInfo: ProjectUpdateLogoInfo,
        projectId: String
    ) {
        logger.info("Update the paas cc projectLogo $projectUpdateLogoInfo by user $userId with token $accessToken")

        val url = "$ccUrl/$projectId?access_token=$accessToken"
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val param = objectMapper.writeValueAsString(projectUpdateLogoInfo)
        val requestBody = RequestBody.create(mediaType, param)
        val request = Request.Builder().url(url).put(requestBody).build()
        val responseContent = request(request, "更新PaaSCC的项目LOGO信息失败")
        logger.info("Success to update the projectLogo with response $responseContent")
        val result: Response<Any> = objectMapper.readValue(responseContent)

        if (result.code.toInt() != 0) {
            logger.warn("Fail to update the projectLogo in paas cc with response $responseContent")
            throw OperationException("更新PaaSCC的项目LOGO信息失败")
        }
    }

    private fun request(request: Request, errorMessage: String): String {
//        val httpClient = okHttpClient.newBuilder().build()
        OkhttpUtils.doHttp(request).use { response ->
            //        httpClient.newCall(request).execute().use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()} , message ${response.message()} and response $responseContent")
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}