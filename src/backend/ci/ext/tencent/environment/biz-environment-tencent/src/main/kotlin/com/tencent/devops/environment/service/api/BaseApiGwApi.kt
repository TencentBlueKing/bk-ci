package com.tencent.devops.environment.service.api

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.exception.ApiGwException
import com.tencent.devops.environment.pojo.apigw.ApiGwBasicReq
import com.tencent.devops.environment.pojo.apigw.ApiGwReq
import okhttp3.Response
import org.slf4j.LoggerFactory

/**
 * 调用Api Gateway上面的Api
 */
abstract class BaseApiGwApi(
    private val apiBaseUrl: String,
    private val appCode: String,
    private val appSecret: String,
    private val username: String?
) {

    companion object {
        /**
         * 打印的Body最大长度
         */
        private const val BODY_MAX_LENGTH = 4000

        private val log = LoggerFactory.getLogger(BaseApiGwApi::class.java)
    }

    fun doGet(pathAndParams: String): Response {
        val url = buildCompleteUrl(pathAndParams)
        logUrlAndReq(url)
        try {
            val resp = OkhttpUtils.doGet(url, getHeaderMap())
            checkAndRecordResp(url, resp)
            return resp
        } catch (e: Exception) {
            log.error("Fail to doGet, url=$url", e)
            throw e
        }
    }

    fun doShortGet(pathAndParams: String): Response {
        val url = buildCompleteUrl(pathAndParams)
        logUrlAndReq(url)
        try {
            val resp = OkhttpUtils.doShortGet(url, getHeaderMap())
            checkAndRecordResp(url, resp)
            return resp
        } catch (e: Exception) {
            log.error("Fail to doShortGet, url=$url", e)
            throw e
        }
    }

    fun doPost(pathAndParams: String, req: ApiGwReq): Response {
        val bodyStr = JsonUtil.toJson(req)
        val headerMap = getHeaderMap()
        val url = buildCompleteUrl(pathAndParams)
        logUrlAndReq(url, req)
        val resp = OkhttpUtils.doPost(url, bodyStr, headerMap)
        logOrThrow(url, bodyStr, resp)
        return resp
    }

    private fun logUrlAndReq(url: String, req: ApiGwReq? = null) {
        val reqStr: String?
        if (req != null) {
            reqStr = bodyWithLengthLimit(JsonUtil.skipLogFields(req))
            log.info("request: url=$url, body=$reqStr")
        } else {
            log.info("request: url=$url")
        }
    }

    private fun getHeaderMap(): MutableMap<String, String> {
        val headerMap = getBaseHeaderMap()
        return headerMap
    }

    private fun getBaseHeaderMap(): MutableMap<String, String> {
        val apiGwBasicReq = ApiGwBasicReq(
            appCode = appCode,
            appSecret = appSecret,
            username = username
        )
        val headerMap = mutableMapOf(
            "accept" to "*/*",
            "Content-Type" to "application/json",
            "X-Bkapi-Authorization" to JsonUtil.toJson(apiGwBasicReq)
                .replace("\r\n", "")
                .replace("\n", "")
        )
        val bkLanguageKey = I18nUtil.getBKLanguageKey()
        val bkLanguage = I18nUtil.getBKLanguageFromCookie()
        if (null != bkLanguage) {
            headerMap[bkLanguageKey] = bkLanguage
        }
        return headerMap
    }

    private fun logOrThrow(url: String, bodyStr: String, resp: Response) {
        if (!resp.isSuccessful) {
            log.warn("url=$url,req=$bodyStr,resp=${getRespDesc(resp)}")
            throw ApiGwException(resp, "response is not successful, message: " + resp.message)
        }
    }

    private fun checkAndRecordResp(url: String, resp: Response) {
        if (!resp.isSuccessful) {
            log.warn("url=$url,resp=${getRespDesc(resp)}")
            throw ApiGwException(resp, "response is not successful, message: " + resp.message)
        } else {
            log.debug("url={},resp={}", url, resp)
        }
    }

    private fun buildCompleteUrl(pathAndParams: String): String {
        return "$apiBaseUrl$pathAndParams"
    }

    private fun getRespDesc(resp: Response): String {
        return "(code=" + resp.code + ",message=" + resp.message + ",body=" + bodyWithLengthLimit(resp.body?.string()) + ")"
    }

    private fun bodyWithLengthLimit(bodyStr: String?): String? {
        if (bodyStr == null) {
            return null
        }
        return if (bodyStr.length > BODY_MAX_LENGTH)
            bodyStr.substring(0, BODY_MAX_LENGTH)
        else
            bodyStr
    }
}
