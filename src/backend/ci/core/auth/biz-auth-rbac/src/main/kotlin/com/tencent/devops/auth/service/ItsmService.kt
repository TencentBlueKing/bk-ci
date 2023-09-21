package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.sdk.iam.dto.itsm.ItsmAttrs
import com.tencent.bk.sdk.iam.dto.itsm.ItsmColumn
import com.tencent.bk.sdk.iam.dto.itsm.ItsmContentDTO
import com.tencent.bk.sdk.iam.dto.itsm.ItsmScheme
import com.tencent.bk.sdk.iam.dto.itsm.ItsmStyle
import com.tencent.bk.sdk.iam.dto.itsm.ItsmValue
import com.tencent.devops.auth.constant.AuthI18nConstants
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.ApplyJoinGroupFormDataInfo
import com.tencent.devops.auth.pojo.ItsmCancelApplicationInfo
import com.tencent.devops.auth.pojo.ResponseDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.pojo.enums.ProjectAuthSecrecyStatus
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class ItsmService @Autowired constructor(
    val objectMapper: ObjectMapper
) {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""

    @Value("\${itsm.url:#{null}}")
    private val itsmUrlPrefix: String = ""

    fun cancelItsmApplication(itsmCancelApplicationInfo: ItsmCancelApplicationInfo): Boolean {
        val itsmResponseDTO = executeHttpPost(
            urlSuffix = ITSM_APPLICATION_CANCEL_URL_SUFFIX,
            body = itsmCancelApplicationInfo
        )
        if (itsmResponseDTO.message != "success") {
            logger.warn("cancel itsm application failed!$itsmCancelApplicationInfo")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_ITSM_APPLICATION_CANCEL_FAIL,
                params = arrayOf(itsmCancelApplicationInfo.sn),
                defaultMessage = "cancel itsm application failed!sn(${itsmCancelApplicationInfo.sn})"
            )
        }
        return true
    }

    fun verifyItsmToken(token: String) {
        val param = mapOf("token" to token)
        val itsmResponseDTO = executeHttpPost(ITSM_TOKEN_VERITY_URL_SUFFIX, param)
        val itsmApiResData = itsmResponseDTO.data as Map<*, *>
        logger.info("itsmApiResData:$itsmApiResData")

        if (!itsmApiResData["is_passed"].toString().toBoolean()) {
            logger.warn("verify itsm token failed!$token")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_ITSM_VERIFY_TOKEN_FAIL,
                defaultMessage = "verify itsm token failed!"
            )
        }
    }

    fun getItsmTicketStatus(sn: String): String {
        val itsmResponseDTO = executeHttpGet(String.format(ITSM_TICKET_STATUS_URL_SUFFIX, sn))
        val itsmApiResData = itsmResponseDTO.data as Map<*, *>
        return itsmApiResData["current_status"].toString()
    }

    private fun executeHttpPost(urlSuffix: String, body: Any): ResponseDTO {
        val headerStr = objectMapper.writeValueAsString(mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret))
            .replace("\\s".toRegex(), "")
        val requestBody = objectMapper.writeValueAsString(body)
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val url = itsmUrlPrefix + urlSuffix

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("x-bkapi-authorization", headerStr)
            .build()
        return executeHttpRequest(url, request)
    }

    private fun executeHttpGet(urlSuffix: String): ResponseDTO {
        val headerStr = objectMapper.writeValueAsString(mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret))
            .replace("\\s".toRegex(), "")
        val url = itsmUrlPrefix + urlSuffix
        val request = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr)
            .get()
            .build()
        return executeHttpRequest(url, request)
    }

    private fun executeHttpRequest(url: String, request: Request): ResponseDTO {
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("request failed, uri:($url)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            val responseStr = it.body!!.string()
            val responseDTO = objectMapper.readValue<ResponseDTO>(responseStr)
            if (responseDTO.code != 0L || !responseDTO.result) {
                // 请求错误
                logger.warn("request failed, url:($url)|response:($it)")
                throw RemoteServiceException("request failed, response:(${responseDTO.message})")
            }
            logger.info("request response：${objectMapper.writeValueAsString(responseDTO.data)}")
            return responseDTO
        }
    }

    @Suppress("LongParameterList")
    fun buildGradeManagerItsmContentDTO(
        projectName: String,
        projectId: String,
        desc: String,
        organization: String,
        authSecrecy: Int,
        subjectScopes: List<SubjectScopeInfo>
    ): ItsmContentDTO {
        val itsmColumns = listOf(
            ItsmColumn.builder().key("projectName")
                .name(I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_PROJECT_NAME)).type(TEXT_TYPE).build(),
            ItsmColumn.builder().key("projectId").name(
                I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_PROJECT_ID)
            ).type(TEXT_TYPE).build(),
            ItsmColumn.builder().key("desc").name(
                I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_PROJECT_DESC)
            ).type(TEXT_TYPE).build(),
            ItsmColumn.builder().key("organization")
                .name(I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_ORGANIZATION)).type(TEXT_TYPE).build(),
            ItsmColumn.builder().key("authSecrecy")
                .name(I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_AUTH_SECRECY)).type(TEXT_TYPE).build(),
            ItsmColumn.builder().key("subjectScopes")
                .name(I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_SUBJECT_SCOPES)).type(TEXT_TYPE).build()
        )
        val itsmAttrs = ItsmAttrs.builder().column(itsmColumns).build()
        val itsmScheme = ItsmScheme.builder().attrs(itsmAttrs).type("table").build()
        val scheme = HashMap<String, ItsmScheme>()
        scheme["content_table"] = itsmScheme
        val value = HashMap<String, ItsmStyle>()
        value["projectName"] = ItsmStyle.builder().value(projectName).build()
        value["projectId"] = ItsmStyle.builder().value(projectId).build()
        value["desc"] = ItsmStyle.builder().value(desc).build()
        value["organization"] = ItsmStyle.builder().value(organization).build()
        value["authSecrecy"] =
            ItsmStyle.builder().value(ProjectAuthSecrecyStatus.getStatus(authSecrecy)?.desc ?: "").build()
        value["subjectScopes"] = ItsmStyle.builder().value(subjectScopes.joinToString(",") { it.name }).build()
        val itsmValue = ItsmValue.builder()
            .scheme("content_table")
            .label(
                I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_CREATE_PROJECT_APPROVAL)
            )
            .value(listOf(value))
            .build()
        return ItsmContentDTO.builder().formData(listOf(itsmValue)).schemes(scheme).build()
    }

    fun buildGroupApplyItsmContentDTO(): ItsmContentDTO {
        val itsmColumns = listOf(
            ItsmColumn.builder().key("projectName").name(
                I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_BELONG_PROJECT_NAME)
            ).type(TEXT_TYPE).build(),
            ItsmColumn.builder().key("resourceName").name(
                I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_RESOURCE_NAME)
            ).type(URL_TYPE).build(),
            ItsmColumn.builder().key("groupName").name(
                I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_GROUP_NAME)
            ).type(URL_TYPE).iframe(true).build(),
            ItsmColumn.builder().key("validityPeriod").name(
                I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_VALIDITY_PERIOD)
            ).type(TEXT_TYPE).build()
        )
        val itsmAttrs = ItsmAttrs.builder().column(itsmColumns).build()
        val itsmScheme = ItsmScheme.builder().attrs(itsmAttrs).type("table").build()
        val scheme = HashMap<String, ItsmScheme>()
        scheme["content_table"] = itsmScheme
        val itsmValue = ItsmValue.builder()
            .scheme("content_table")
            .value(emptyList())
            .label(I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_APPLY_TO_JOIN_GROUP))
            .build()
        return ItsmContentDTO.builder().formData(listOf(itsmValue)).schemes(scheme).build()
    }

    fun buildGroupApplyItsmValue(formData: ApplyJoinGroupFormDataInfo): Map<String, ItsmStyle> {
        val value = HashMap<String, ItsmStyle>()
        value["projectName"] = ItsmStyle.builder().value(formData.projectName).build()
        value["resourceName"] = ItsmStyle.builder()
            .label(formData.resourceTypeName.plus("-").plus(formData.resourceName))
            .value(formData.resourceRedirectUri).build()
        value["groupName"] = ItsmStyle.builder().label(formData.groupName)
            .value(formData.groupPermissionDetailRedirectUri).build()
        value["validityPeriod"] = ItsmStyle.builder().value(formData.validityPeriod).build()
        return value
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItsmService::class.java)
        private const val ITSM_APPLICATION_CANCEL_URL_SUFFIX = "/operate_ticket/"
        private const val ITSM_TOKEN_VERITY_URL_SUFFIX = "/token/verify/"
        private const val ITSM_TICKET_STATUS_URL_SUFFIX = "/get_ticket_status?sn=%s"
        private const val TEXT_TYPE = "text"
        private const val URL_TYPE = "url"
    }
}
