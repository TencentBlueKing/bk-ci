package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_MOA_CREDENTIAL_KEY_VERIFY_FAIL
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_USER_NOT_BELONG_TO_THE_PROJECT
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_WATER_MARK_NOT_EXIST
import com.tencent.devops.auth.pojo.MoaCredentialKeyVerifyResponse
import com.tencent.devops.auth.pojo.ResponseDTO
import com.tencent.devops.auth.pojo.dto.SecOpsWaterMarkDTO
import com.tencent.devops.auth.pojo.vo.SecOpsWaterMarkInfoVo
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TxSecurityService constructor(
    val objectMapper: ObjectMapper,
    val permissionProjectService: PermissionProjectService
) {

    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""

    @Value("\${secops.url:#{null}}")
    private val secUrlPrefix = ""

    @Value("\${secops.token:#{null}}")
    private val secToken = ""

    fun verifyProjectUserByCredentialKey(
        credentialKey: String,
        projectId: String
    ): Result<Boolean?> {
        val result = verifyUser(credentialKey, projectId)
        return if (result.status != 0) {
            Result(message = result.message, status = result.status)
        } else {
            Result(data = true)
        }
    }

    fun getUserWaterMarkByCredentialKey(
        credentialKey: String,
        projectId: String
    ): Result<String?> {
        val result = verifyUser(credentialKey, projectId)
        return if (result.status != 0) {
            result
        } else {
            val userId = result.data!!
            Result(data = getUserWaterMark(userId = userId).data)
        }
    }

    private fun verifyUser(credentialKey: String, projectCode: String): Result<String> {
        val moaCredentialKeyVerifyResponse = getUserInfoByMoaCredentialKey(credentialKey = credentialKey)
        return if (moaCredentialKeyVerifyResponse.returnFlag != 0) {
            Result(
                status = ERROR_MOA_CREDENTIAL_KEY_VERIFY_FAIL.toInt(),
                message = moaCredentialKeyVerifyResponse.msg ?: ""
            )
        } else {
            val userId = moaCredentialKeyVerifyResponse.userId
            val isUserBelongToProject = permissionProjectService.isProjectUser(
                userId = userId!!,
                projectCode = projectCode,
                group = null
            )
            if (!isUserBelongToProject) {
                Result(
                    status = ERROR_USER_NOT_BELONG_TO_THE_PROJECT.toInt(),
                    message = "User does not have permission to visit the project!"
                )
            } else {
                Result(data = userId)
            }
        }
    }

    private fun getUserInfoByMoaCredentialKey(credentialKey: String): MoaCredentialKeyVerifyResponse {
        return MoaCredentialKeyVerifyResponse(
            returnFlag = 0,
            msg = "",
            userId = ""
        )
    }


    private fun getUserWaterMark(userId: String): SecOpsWaterMarkInfoVo {
        logger.info("get user water mark:$userId")
        return executePostHttpRequest(
            urlSuffix = USER_WATER_MARK_GET_SUFFIX,
            body = objectMapper.writeValueAsString(
                SecOpsWaterMarkDTO(
                    token = secToken,
                    username = userId
                )
            )
        ).data?.firstOrNull { it.type == "image_base64" } ?: throw ErrorCodeException(
            errorCode = ERROR_WATER_MARK_NOT_EXIST,
            defaultMessage = "user water mark not exist!$userId"
        )
    }

    private fun executePostHttpRequest(
        urlSuffix: String,
        body: String
    ): ResponseDTO<List<SecOpsWaterMarkInfoVo>> {
        val headerMap = mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret)
        val headerStr = objectMapper.writeValueAsString(headerMap).replace("\\s".toRegex(), "")
        val url = secUrlPrefix + urlSuffix

        val requestBody = body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr)
        requestBuilder.post(requestBody)

        OkhttpUtils.doHttp(requestBuilder.build()).use {
            if (!it.isSuccessful) {
                logger.warn("request failed, uri:($url)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            logger.info("executeHttpRequest:${it.body!!}")
            val responseStr = it.body!!.string()
            logger.info("executeHttpRequest:$responseStr")
            val responseDTO = objectMapper.readValue<ResponseDTO<List<SecOpsWaterMarkInfoVo>>>(responseStr)
            if (responseDTO.code != 0L) {
                // 请求错误
                logger.warn("request failed, url:($url)|response :($it)")
                throw RemoteServiceException("request failed, response:(${responseDTO.message})")
            }
            logger.info("request response：${objectMapper.writeValueAsString(responseDTO.data)}")
            return responseDTO
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxSecurityService::class.java)
        private const val USER_WATER_MARK_GET_SUFFIX = "/web/api/v2/watermark/"
    }
}
