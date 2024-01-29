package com.tencent.devops.remotedev.service.devcloud

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.sun.org.slf4j.internal.LoggerFactory
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.remotedevsup.DevcloudCVMData
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class DevcloudService {
    @Value("\${devcloud.url:}")
    private val devcloudUrl: String = ""

    @Value("\${devcloud.appId:}")
    private val devcloudAppId: String = ""

    @Value("\${devcloud.token:}")
    private val devcloudToken: String = ""

    fun fetchCVMList(
        userId: String,
        project: String
    ): List<DevcloudCVMData>? {
        val url = "$devcloudUrl/v1/project/resourceList?landunId=$project&resourceType=cvm"

        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val random = "landun"

        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(("$devcloudToken$timestamp$random").toByteArray())
        val sb = StringBuilder()
        for (b in hashBytes) {
            sb.append(String.format("%02x", b))
        }
        val md5Hash = sb.toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .headers(
                mapOf(
                    "APPID" to devcloudAppId,
                    "USERID" to userId,
                    "RANDOM" to random,
                    "TIMESTP" to (System.currentTimeMillis() / 1000).toString(),
                    "ENCKEY" to md5Hash
                ).toHeaders()
            )
            .build()
        try {
            OkhttpUtils.doHttp(request).use { resp ->
                val responseStr = resp.body!!.string()
                logger.debug("fetchCVMList|$url|$responseStr")
                if (!resp.isSuccessful) {
                    logger.warn("fetchCVMList not success ${resp.code}|$responseStr")
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
                        params = arrayOf("cvmList", "${resp.code}|$responseStr")
                    )
                }
                val data = JsonUtil.to(responseStr, object : TypeReference<CVMListResp>() {})
                if (data.code != 200) {
                    throw ErrorCodeException(
                        errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
                        params = arrayOf("cvmList", "${data.code}|${data.msg}")
                    )
                }
                return data.data
            }
        } catch (e: Exception) {
            logger.warn("fetchCVMList error", e)
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.REQ_DEVCLOUD_ERROR.errorCode,
                params = arrayOf("cvmList", e.localizedMessage)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DevcloudService::class.java)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CVMListResp(
    val code: Int?,
    val data: List<DevcloudCVMData>?,
    val msg: String?
)
