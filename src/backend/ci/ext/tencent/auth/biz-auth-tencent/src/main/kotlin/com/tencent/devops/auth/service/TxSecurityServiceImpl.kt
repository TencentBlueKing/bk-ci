package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.constant.AuthMessageCode.ERROR_WATER_MARK_NOT_EXIST
import com.tencent.devops.auth.pojo.ResponseDTO
import com.tencent.devops.auth.pojo.dto.SecOpsWaterMarkDTO
import com.tencent.devops.auth.pojo.vo.SecOpsWaterMarkInfoVo
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.security.SecurityService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TxSecurityServiceImpl constructor(
    val objectMapper: ObjectMapper,
    deptService: DeptService,
    permissionProjectService: PermissionProjectService
) : SecurityService(
    deptService = deptService,
    permissionProjectService = permissionProjectService
) {

    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""

    @Value("\${secops.url:#{null}}")
    private val secUrlPrefix = ""

    @Value("\${secops.token:#{null}}")
    private val secToken = ""

    override fun getUserWaterMark(userId: String): SecOpsWaterMarkInfoVo {
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
        private val logger = LoggerFactory.getLogger(TxSecurityServiceImpl::class.java)
        private const val USER_WATER_MARK_GET_SUFFIX = "/web/api/v2/watermark/"
    }
}
