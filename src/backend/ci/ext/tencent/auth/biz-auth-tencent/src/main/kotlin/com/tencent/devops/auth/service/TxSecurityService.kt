package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
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
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import okhttp3.MultipartBody
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TxSecurityService constructor(
    val objectMapper: ObjectMapper,
    val permissionProjectService: PermissionProjectService,
    val bkHttpRequestService: BkHttpRequestService
) {
    @Value("\${secops.url:#{null}}")
    private val secUrlPrefix = ""

    @Value("\${moa.key.verify.url:#{null}}")
    private val moaKeyVerifyUrl = ""

    @Value("\${secops.token:#{null}}")
    private val secToken = ""

    @ActionAuditRecord(
        actionId = ActionId.PROJECT_USER_VERIFY,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.SECURITY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PROJECT_USER_VERIFY_CONTENT
    )
    fun verifyProjectUserByCredentialKey(
        credentialKey: String,
        projectId: String
    ): Result<Boolean?> {
        logger.info("verify project user by credential key($credentialKey) under project($projectId)")
        val result = verifyUser(credentialKey, projectId)
        return if (result.status != 0) {
            ActionAuditContext.current().disable()
            Result(message = result.message, status = result.status)
        } else {
            val userId = result.data!!
            ActionAuditContext.current()
                .setInstanceId(credentialKey)
                .setInstanceName(userId)
            logger.info("verify project user pass:$userId|$credentialKey|$projectId")
            Result(data = true)
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.WATER_MARK_GET,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.SECURITY
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.WATER_MARK_GET_CONTENT
    )
    fun getUserWaterMarkByCredentialKey(
        credentialKey: String,
        projectId: String
    ): Result<String?> {
        logger.info("get user water mark by credentialKey:$credentialKey|$projectId")
        val result = verifyUser(credentialKey, projectId)
        return if (result.status != 0) {
            ActionAuditContext.current().disable()
            result
        } else {
            val userId = result.data!!
            val waterMark = getUserWaterMark(userId = userId).data
            logger.info("user get water mark success!|$userId|$projectId|$credentialKey|$waterMark")
            ActionAuditContext.current()
                .setInstanceId(credentialKey)
                .setInstanceName(userId)
            Result(data = waterMark)
        }
    }

    private fun verifyUser(credentialKey: String, projectCode: String): Result<String> {
        val moaCredentialKeyVerifyResponse = getUserInfoByMoaCredentialKey(credentialKey = credentialKey)
        return if (moaCredentialKeyVerifyResponse.returnFlag != 0) {
            val errorMeg = moaCredentialKeyVerifyResponse.msg
            logger.info("Invalid MOA credentials:$errorMeg|$credentialKey|$projectCode")
            Result(
                status = ERROR_MOA_CREDENTIAL_KEY_VERIFY_FAIL.toInt(),
                message = errorMeg ?: ""
            )
        } else {
            val userId = moaCredentialKeyVerifyResponse.userId
            val isUserBelongToProject = permissionProjectService.isProjectUser(
                userId = userId!!,
                projectCode = projectCode,
                group = null
            )
            if (!isUserBelongToProject) {
                logger.info("User($userId) does not have permission to visit the project($projectCode)!")
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
        val url = moaKeyVerifyUrl
        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", credentialKey)
            .build()
        val requestBuilder = Request.Builder()
            .url(url)
            .post(formBody)
        OkhttpUtils.doHttp(requestBuilder.build()).use {
            if (!it.isSuccessful) {
                logger.warn("request moa failed, uri:($url)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            logger.info("executeHttpRequest:${it.body!!}")
            val responseStr = it.body!!.string()
            logger.info("executeHttpRequest:$responseStr")
            return objectMapper.readValue(responseStr)
        }
    }

    private fun getUserWaterMark(userId: String): SecOpsWaterMarkInfoVo {
        logger.info("get user water mark:$userId")
        val responseDTO: ResponseDTO<List<Map<*, *>>> = bkHttpRequestService.executeHttpPost(
            url = secUrlPrefix + USER_WATER_MARK_GET_SUFFIX,
            body = SecOpsWaterMarkDTO(
                token = secToken,
                username = userId
            )
        )
        val waterMarkInfo = responseDTO.data?.firstOrNull { it["type"] == "image_base64" }

        return if (waterMarkInfo != null) {
            SecOpsWaterMarkInfoVo(
                type = waterMarkInfo["type"].toString(),
                data = waterMarkInfo["data"].toString()
            )
        } else {
            throw ErrorCodeException(
                errorCode = ERROR_WATER_MARK_NOT_EXIST,
                defaultMessage = "user water mark not exist!$userId"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxSecurityService::class.java)
        private const val USER_WATER_MARK_GET_SUFFIX = "/web/api/v2/watermark/"
    }
}
