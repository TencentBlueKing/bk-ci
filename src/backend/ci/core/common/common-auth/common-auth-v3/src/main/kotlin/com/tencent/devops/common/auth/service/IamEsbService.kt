package com.tencent.devops.common.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.pojo.IamApiReq
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

class IamEsbService() {

    @Value("\${esb.code:#{null}}")
    val appCode: String? = null
    @Value("\${esb.secret:#{null}}")
    val appSecret: String? = null
    @Value("\${esb.iam.url:#{null}}")
    val iamHost: String? = null

    fun createRelationResource(iamApiReq: IamApiReq): Boolean {
        var url = "api/c/compapi/v2/iam/authorization/resource_creator_action/?bk_app_code=$appCode&bk_app_secret=$appSecret"
        url = getAuthRequestUrl(url)
        iamApiReq.bkAppCode = appCode!!
        iamApiReq.bkAppSecret = appSecret!!
        val content = objectMapper.writeValueAsString(iamApiReq)
        logger.info("v3 createRelationResource url[$url]")
        logger.info("v3 createRelationResource body[$content]")
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder().url(url)
                .post(requestBody)
                .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                // 请求错误
                throw RemoteServiceException("bkiam v3 request failed, response: ($it)")
            }
            val responseStr = it.body()!!.string()
            logger.info("v3 createRelationResource responseStr[$responseStr]")
            val iamApiRes = objectMapper.readValue<Map<String, Any>>(responseStr)
            if (iamApiRes["code"] != 0 || iamApiRes["result"] == false) {
                // 请求错误
                throw RemoteServiceException("bkiam v3 request failed, response: (${iamApiRes["message"]}, request_id[${iamApiRes["request_id"]}])")
            }

            return true
        }

        return false
    }

    /**
	 * 生成请求url
	 */
    private fun getAuthRequestUrl(uri: String): String {
        return if (iamHost?.endsWith("/")!!) {
            iamHost + uri
        } else {
            "$iamHost/$uri"
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
        val objectMapper = ObjectMapper()
    }
}