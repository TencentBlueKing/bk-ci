package com.tencent.devops.prebuild.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.environment.agent.pojo.devcloud.Action
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudContainer
import com.tencent.devops.common.environment.agent.pojo.devcloud.Params
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudImage
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudImageVersion
import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
class PrebuildDevCloudClient {
    private val logger = LoggerFactory.getLogger(javaClass)!!

    @Value("\${devCloud.appId}")
    val devCloudAppId: String = ""

    @Value("\${devCloud.token}")
    val devCloudToken: String = ""

    @Value("\${devCloud.url}")
    val devCloudUrl: String = ""

    @Value("\${devCloud.smartProxyToken}")
    val smartProxyToken: String = ""

    fun createContainer(staffName: String, devCloudContainer: DevCloudContainer): String {
        val url = "$devCloudUrl/api/v2.1/containers"
        val body = ObjectMapper().writeValueAsString(devCloudContainer)
        logger.info("request url: $url")
        logger.info("request body: $body")
        val request = Request.Builder()
                .url(url)
                // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
                .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            logger.info("http code is ${response.code()}, $responseContent")
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to request to devCloud, http response code: ${response.code()}, msg: $responseContent")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    fun operateContainer(staffName: String, name: String, action: Action, param: Params? = null): String {
        val url = "$devCloudUrl/api/v2.1/containers/$name"
        val body = when (action) {
            Action.DELETE -> "{\"action\":\"delete\",\"params\":{}}"
            Action.STOP -> "{\"action\":\"stop\",\"params\":{}}"
            Action.START -> if (null != param) {
                "{\"action\":\"start\",\"params\": ${jacksonObjectMapper().writeValueAsString(param)}}"
            } else {
                "{\"action\":\"start\",\"params\":{}}"
            }
            else -> ""
        }
        logger.info("request url: $url")
        logger.info("request body: $body")
        val request = Request.Builder()
                .url(url)
                // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
                .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to start docker")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    fun getContainers(staffName: String, keyword: String?, page: Int, size: Int): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers?page=$page&size=$size" + if (null != keyword && keyword.isNotBlank()) "&keyword=" + URLEncoder.encode(keyword, "UTF-8") else ""
        logger.info("request url: $url")
        val request = Request.Builder()
                .url(url)
                // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
                .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to get containers")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun getContainerStatus(staffName: String, name: String): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers/$name/status"
        logger.info("request url: $url")
        val request = Request.Builder()
                .url(url)
                // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
                .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to get container status")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun getContainerInstance(staffName: String, id: String): JSONObject {
        val url = "$devCloudUrl/api/v2.1/containers/$id/instances"
        logger.info("request url: $url")
        val request = Request.Builder()
                .url(url)
                // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
                .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to get container status")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun createImage(staffName: String, devCloudImage: DevCloudImage): String {
        val url = "$devCloudUrl/api/v2.1/images"
        val body = ObjectMapper().writeValueAsString(devCloudImage)
        logger.info("request url: $url")
        logger.info("request body: $body")
        val request = Request.Builder()
                .url(url)
                // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
                .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to createImage")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    fun createImageVersions(staffName: String, id: String, devCloudImageVersion: DevCloudImageVersion): String {
        val url = "$devCloudUrl/api/v2.1/images/$id/versions/${devCloudImageVersion.version}"
        val body = ObjectMapper().writeValueAsString(devCloudImageVersion)
        logger.info("request url: $url")
        logger.info("request body: $body")
        val request = Request.Builder()
                .url(url)
                // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
                .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to createImageVersions")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as Map<String, Any>
                return (dataMap["taskId"] as Int).toString()
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    fun getTasks(staffName: String, taskId: String): JSONObject {
        val url = "$devCloudUrl/api/v2.1/tasks/$taskId"
        logger.info("request url: $url")
        val request = Request.Builder()
                .url(url)
                // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
                .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                throw OperationException("Fail to get container status")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun executeContainerCommand(staffName: String, name: String, command: List<String>): Pair<Int, String> {
        val url = "$devCloudUrl/api/v2.1/containers/$name/exec"
        val body = mapOf("command" to command)
        logger.info("request url: $url")
        logger.info("request body: $body")
        val request = Request.Builder()
                .url(url)
                // .headers(Headers.of(getHeaders(devCloudAppId, devCloudToken, staffName)))
                .headers(Headers.of(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, staffName, smartProxyToken)))
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), ObjectMapper().writeValueAsString(body)))
                .build()
        OkhttpUtils.doLongHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.info("response code: ${response.code()}")
                logger.info("response: $responseContent")
                throw RuntimeException("Fail to start docker")
            }
            logger.info("response: $responseContent")
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 == code) {
                val dataMap = responseData["data"] as List<Map<String, Any>>
                val retCode = dataMap.last()["code"] as Int
                val retMsg = dataMap.last()["ret"] as String
                return if (0 == retCode) {
                    logger.info("execute command success")
                    Pair(0, retMsg)
                } else {
                    logger.info("execute command failed, retCode:$retCode, msg:$retMsg")
                    Pair(retCode, retMsg)
                }
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    private fun getHeaders(appId: String, token: String, staffName: String): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["APPID"] = appId
        val random = RandomStringUtils.randomAlphabetic(8)
        headerBuilder["RANDOM"] = random
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        headerBuilder["TIMESTP"] = timestamp
        headerBuilder["STAFFNAME"] = staffName
        val encKey = DigestUtils.md5Hex("$token$timestamp$random")
        headerBuilder["ENCKEY"] = encKey

        return headerBuilder
    }
}