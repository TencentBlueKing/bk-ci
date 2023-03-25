package com.tencent.devops.project.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.auth.api.BSAuthTokenApi
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.auth.code.BSProjectServiceCodec
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.pojo.AuthProjectForCreateResult
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.mq.ProjectCreateBroadCastEvent
import com.tencent.devops.project.service.ProjectExtService
import com.tencent.devops.project.service.ProjectPaasCCService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class TxProjectExtServiceImpl(
    private val bkRepoClient: BkRepoClient,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val bsProjectAuthServiceCode: BSProjectServiceCodec,
    private val projectPaasCCService: ProjectPaasCCService,
    private val bsAuthTokenApi: BSAuthTokenApi,
    private val projectDispatcher: ProjectDispatcher,
    private val authProperties: BkAuthProperties,
    private val objectMapper: ObjectMapper
) : ProjectExtService {

    companion object {
        private val logger = LoggerFactory.getLogger(TxProjectExtServiceImpl::class.java)
    }

    override fun createExtProjectInfo(
        userId: String,
        authProjectId: String,
        accessToken: String?,
        projectCreateInfo: ProjectCreateInfo,
        createExtInfo: ProjectCreateExtInfo,
        logoAddress: String?
    ) {
        // 添加repo项目
        val createSuccess = bkRepoClient.createBkRepoResource(userId, projectCreateInfo.englishName)
        logger.info("create bkrepo project ${projectCreateInfo.englishName} success: $createSuccess")

        if (createExtInfo.needAuth!!) {
            val newAccessToken = if (accessToken.isNullOrBlank()) {
                bsAuthTokenApi.getAccessToken(bsPipelineAuthServiceCode)
            } else accessToken
            // 添加paas项目
            projectPaasCCService.createPaasCCProject(
                userId = userId,
                projectId = authProjectId,
                accessToken = newAccessToken,
                projectCreateInfo = projectCreateInfo
            )
        }
        // 工蜂CI项目不会添加paas项目，但也需要广播
        projectDispatcher.dispatch(
            ProjectCreateBroadCastEvent(
                userId = userId,
                projectId = authProjectId,
                projectInfo = projectCreateInfo
            )
        )
    }

    override fun createOldAuthProject(
        userId: String,
        accessToken: String?,
        projectCreateInfo: ProjectCreateInfo
    ): String? {
        val param: MutableMap<String, String> = mutableMapOf("project_code" to projectCreateInfo.englishName)
        // 创建AUTH项目
        val newAccessToken = if (accessToken.isNullOrBlank()) {
            param["creator"] = userId
            bsAuthTokenApi.getAccessToken(bsProjectAuthServiceCode)
        } else accessToken
        val authUrl = "${authProperties.url}/projects?access_token=$newAccessToken"
        logger.info("create project $authUrl $userId,use userAccessToken${newAccessToken == accessToken}")
        param["bg_id"] = projectCreateInfo.bgId.toString()
        param["dept_id"] = projectCreateInfo.deptId.toString()
        param["center_id"] = projectCreateInfo.centerId.toString()
        logger.info("createProjectResources add org info $param")

        return try {
            createV0AuthProject(authUrl = authUrl, param = param)
        } catch (ignore: Exception) {
            logger.error("Failed to create v0 auth project", ignore)
            null
        }
    }

    private fun createV0AuthProject(authUrl: String, param: Map<String, String>): String? {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val json = objectMapper.writeValueAsString(param)
        val requestBody = json.toRequestBody(mediaType)
        val request = Request.Builder().url(authUrl).post(requestBody).build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn(
                    "Fail to request($request) with code ${response.code} ," +
                        " message ${response.message} and response $responseContent"
                )
                throw OperationException("调用权限中心创建项目失败")
            }
            val result = objectMapper.readValue<Result<AuthProjectForCreateResult>>(responseContent)
            if (result.isNotOk()) {
                logger.warn("Fail to create the project of response $responseContent")
                throw OperationException("调用权限中心创建项目失败: ${result.message}")
            }
            val authProjectForCreateResult = result.data
            if (authProjectForCreateResult != null) {
                if (authProjectForCreateResult.project_id.isBlank()) {
                    throw OperationException("权限中心创建的项目ID无效")
                }
                return authProjectForCreateResult.project_id
            } else {
                logger.warn("Fail to get the project id from response $responseContent")
                throw OperationException("权限中心创建的项目ID无效")
            }
        }
    }
}
