package com.tencent.devops.project.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.common.api.constant.DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.SignatureCallbackInfo
import com.tencent.devops.project.pojo.SignatureCallbackResponse
import com.tencent.devops.project.pojo.UserSignatureStatusDTO
import com.tencent.devops.project.pojo.UserSignatureStatusResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ProjectSignatureManageService(
    private val objectMapper: ObjectMapper,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val projectService: ProjectService,
    private val authProjectApi: AuthProjectApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode
) {
    @Value("\${signature.url:}")
    private lateinit var signatureUrl: String

    @Value("\${signature.secret.key:}")
    private lateinit var secretKey: String

    @Value("\${signature.clientId:}")
    private lateinit var clientId: String

    fun listSignatureProjects(): List<String> {
        return redisOperation.get(PROJECT_NEED_TO_CHECK)?.split(",") ?: emptyList()
    }

    fun getSignatureStatus(
        projectId: String,
        userId: String
    ): UserSignatureStatusResponse {
        // 检查项目是否需要校验
        val projectsNeedToCheck = redisOperation.get(PROJECT_NEED_TO_CHECK)?.split(",") ?: emptyList()
        if (!projectsNeedToCheck.contains(projectId)) {
            return UserSignatureStatusResponse(
                userId = userId,
                signed = true
            )
        }
        val isProjectUser = authProjectApi.isProjectUser(
            user = userId,
            projectCode = projectId,
            serviceCode = pipelineAuthServiceCode,
            group = null
        )
        if (!isProjectUser) {
            throw PermissionForbiddenException("The user does not have permission to visit the project.")
        }
        logger.info("get signature status :$projectId|$userId")
        val projectNames = try {
            projectService.list(projectsNeedToCheck).map { it.projectName }
        } catch (e: Exception) {
            logger.error("Failed to get project names", e)
            return UserSignatureStatusResponse(
                userId = userId,
                signed = true
            )
        }
        val isUserSigned = redisOperation.get(USER_SIGNATURE_STATUS_CHECK.plus(userId))
        if (isUserSigned?.toBoolean() == true) {
            return UserSignatureStatusResponse(
                userId = userId,
                signed = true
            )
        }
        val url = "$signatureUrl/api/v1/signature/signature_status"
        val nonce = generateRandomString(12)
        val timestamp = System.currentTimeMillis()
        val token = cryptoToken(
            nonce = nonce,
            timestamp = timestamp
        )
        val userNickName = client.get(ServiceDeptResource::class).getUserInfo(
            userId = userId,
            name = userId
        ).data?.displayName ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.FAILED_USER_INFORMATION,
            params = arrayOf(userId)
        )

        val body = SignatureStatusQueryReq(
            user = userId,
            nick = userNickName,
            tof_id = userId
        )
        val requestBody = objectMapper.writeValueAsString(body).toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Smoba-Clientid", clientId)
            .addHeader("Smoba-Nonce", nonce)
            .addHeader("Smoba-Timestamp", timestamp.toString())
            .addHeader("Smoba-Signature", token)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("get signature status failed, uri:($url)|response: ($response)")
                throw RemoteServiceException("get signature status request failed, response:($response)")
            }
            val queryResponse = JsonUtil.to(responseContent, ResponseDTO::class.java)
            logger.info("get signature status response :$queryResponse")
            if (queryResponse.result != "success") {
                logger.warn("get signature status failed, url:($url)|response:($response)")
                throw RemoteServiceException("get signature status request failed, response:(${response.message})")
            }
            val data = queryResponse.data ?: throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.QUERY_DEPARTMENT_FAIL
                )
            )
            return if (data.whitelistUser || data.status == SUCCESS_STATUS) {
                redisOperation.set(USER_SIGNATURE_STATUS_CHECK.plus(userId), "true")
                UserSignatureStatusResponse(
                    userId = data.user,
                    signed = true
                )
            } else {
                val targetLanguage = I18nUtil.getLanguage(userId)
                val projectNamesLocalized = if (targetLanguage == DEFAULT_LOCALE_LANGUAGE) {
                    projectNames
                } else {
                    projectsNeedToCheck
                }
                val projectInformation = buildProjectInfo(projectNamesLocalized)
                UserSignatureStatusResponse(
                    userId = data.user,
                    signed = false,
                    schemeQrcodeUrl = data.schemeQrcodeUrl,
                    projectInformation = projectInformation
                )
            }
        }
    }

    private fun buildProjectInfo(projectNames: List<String>): String {
        if (projectNames.isEmpty()) return ""
        return if (projectNames.size == 1) {
            I18nUtil.getCodeLanMessage(
                messageCode = ProjectMessageCode.BK_SIGNATURE_PROJECT_INFORMATION,
                params = arrayOf("《${projectNames.first()}》")
            )
        } else {
            val firstName = "《${projectNames.first()}》"
            val otherNames = "《".plus(projectNames.drop(1).joinToString("》/《 ")).plus("》")
            I18nUtil.getCodeLanMessage(
                messageCode = ProjectMessageCode.BK_SIGNATURE_PROJECTS_INFORMATION,
                params = arrayOf(firstName, otherNames)
            )
        }
    }

    fun callback(
        clientId: String,
        nonce: String,
        timestamp: String,
        signature: String,
        callbackInfo: SignatureCallbackInfo
    ): SignatureCallbackResponse {
        logger.info("signature call back:$clientId|$nonce|$timestamp|$signature|$callbackInfo")
        if (signature != cryptoToken(nonce, timestamp.toLong())) {
            throw InvalidParamException(message = "call back token invalid!")
        }
        if (callbackInfo.whitelistUser || callbackInfo.status == SUCCESS_STATUS) {
            redisOperation.set(USER_SIGNATURE_STATUS_CHECK.plus(callbackInfo.user), "true")
        } else {
            redisOperation.delete(USER_SIGNATURE_STATUS_CHECK.plus(callbackInfo.user))
        }
        return SignatureCallbackResponse.success()
    }

    fun generateRandomString(length: Int): String = buildString(length) {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')  // 使用范围操作符
        repeat(length) {
            append(charset.random())  // 直接使用随机元素
        }
    }

    fun cryptoToken(nonce: String, timestamp: Long): String {
        return ShaUtils.sha256(clientId + secretKey + nonce + timestamp)
    }

    data class SignatureStatusQueryReq(
        val user: String,
        val nick: String,
        val tof_id: String
    )

    data class ResponseDTO(
        val message: String?,
        val result: String,
        val data: UserSignatureStatusDTO?
    )

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectSignatureManageService::class.java)
        private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        private const val USER_SIGNATURE_STATUS_CHECK = "user.signature.status.check."
        private const val PROJECT_NEED_TO_CHECK = "projects.signature.check"
        private const val SUCCESS_STATUS = 2
    }
}
