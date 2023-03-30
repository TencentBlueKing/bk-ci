package com.tencent.devops.dispatch.codecc.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import com.tencent.devops.dispatch.codecc.pojo.devcloud.DevCloudContainer
import com.tencent.devops.dispatch.codecc.pojo.devcloud.DevCloudImage
import com.tencent.devops.dispatch.codecc.pojo.devcloud.DevCloudImageVersion
import com.tencent.devops.dispatch.codecc.pojo.devcloud.Params
import com.tencent.devops.dispatch.codecc.pojo.devcloud.TaskStatus
import com.tencent.devops.dispatch.codecc.pojo.devcloud.Action
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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
class CodeccDevCloudClient {
    private val logger = LoggerFactory.getLogger(javaClass)!!

    @Value("\${devCloud.appId}")
    val devCloudAppId: String = ""

    @Value("\${devCloud.token}")
    val devCloudToken: String = ""

    @Value("\${devCloud.url}")
    val devCloudUrl: String = ""

    @Value("\${devCloud.smartProxyToken}")
    val smartProxyToken: String = ""

    @Value("\${devCloud.closeSourceAppId}")
    val closeSourceAppId: String = ""

    @Value("\${devCloud.closeSourceToken}")
    val closeSourceToken: String = ""

    @Value("\${devCloud.closeSourceUrl}")
    val closeSourceUrl: String = ""

    @Value("\${devopsGateway.idcProxy:}")
    val devopsIdcProxyGateway: String = ""

    fun createContainer(codeccTaskId: Long, staffName: String, devCloudContainer: DevCloudContainer): String {
        val uri = "/api/v2.1/containers"
        val body = ObjectMapper().writeValueAsString(devCloudContainer)

        logger.info("request url: $uri")
        logger.info("request body: $body")

        OkhttpUtils.doHttp(buildPostRequest(
            userId = staffName,
            url = uri,
            body = body,
            codeccTaskId = codeccTaskId
        )).use { response ->
            val responseContent = response.body!!.string()
            logger.info("http code is ${response.code}, $responseContent")
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to request to devCloud, " +
                                           "http response code: ${response.code}, msg: $responseContent")
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

    fun operateContainer(
        codeccTaskId: Long,
        staffName: String,
        name: String,
        action: Action,
        param: Params? = null
    ): String {
        val uri = "/api/v2.1/containers/$name"
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
        logger.info("request url: $uri")
        logger.info("request body: $body")
        OkhttpUtils.doHttp(buildPostRequest(
            userId = staffName,
            url = uri,
            body = body,
            codeccTaskId = codeccTaskId
        )).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to start docker, http code: ${response.code}")
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

    fun getContainers(codeccTaskId: Long, staffName: String, keyword: String?, page: Int, size: Int): JSONObject {
        val uri = "/api/v2.1/containers?page=$page&size=$size" +
            if (null != keyword && keyword.isNotBlank()) "&keyword=" + URLEncoder.encode(keyword, "UTF-8") else ""
        logger.info("request url: $uri")
        OkhttpUtils.doHttp(buildGetRequest(
            userId = staffName,
            url = uri,
            codeccTaskId = codeccTaskId
        )).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                throw RuntimeException("Fail to get containers")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun getContainerStatus(codeccTaskId: Long, staffName: String, name: String): JSONObject {
        val uri = "/api/v2.1/containers/$name/status"
        logger.info("request url: $uri")
        OkhttpUtils.doHttp(buildGetRequest(
            userId = staffName,
            url = uri,
            codeccTaskId = codeccTaskId
        )).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.error(response.toString())
                throw RuntimeException("Fail to get container status")
            }
            logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun createImage(codeccTaskId: Long, staffName: String, devCloudImage: DevCloudImage): String {
        val uri = "/api/v2.1/images"
        val body = ObjectMapper().writeValueAsString(devCloudImage)
        logger.info("request url: $uri")
        logger.info("request body: $body")
        OkhttpUtils.doHttp(buildPostRequest(
            userId = staffName,
            url = uri,
            body = body,
            codeccTaskId = codeccTaskId
        )).use { response ->
            val responseContent = response.body!!.string()
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

    fun createImageVersions(codeccTaskId: Long, staffName: String, id: String, devCloudImageVersion: DevCloudImageVersion): String {
        val uri = "/api/v2.1/images/$id/versions/${devCloudImageVersion.version}"
        val body = ObjectMapper().writeValueAsString(devCloudImageVersion)
        logger.info("request url: $uri")
        logger.info("request body: $body")

        OkhttpUtils.doHttp(buildPostRequest(
            userId = staffName,
            url = uri,
            body = body,
            codeccTaskId = codeccTaskId
        )).use { response ->
            val responseContent = response.body!!.string()
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

    fun getTasks(codeccTaskId: Long, staffName: String, taskId: String): JSONObject {
        val uri = "/api/v2.1/tasks/$taskId"
        // logger.info("request url: $url")
        logger.info("the getTask request url: $uri")
        val request = buildGetRequest(
            userId = staffName,
            url = uri,
            codeccTaskId = codeccTaskId
        )
        logger.info("getTask request is success!")
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.info("Get task status failed, responseCode: ${response.code}")

                // sleep 5s，再查一次，给一次机会
                Thread.sleep(5 * 1000)
                OkhttpUtils.doHttp(request).use {
                    val retryResponseContent = it.body!!.string()
                    if (!it.isSuccessful) {
                        // 没机会了，只能失败
                        logger.info("retry get task status failed, retry responseCode: ${it.code}")
                        throw OperationException("Fail to get container status, retry response code: ${it.code}")
                    }

                    logger.info("retry response: $retryResponseContent")
                    return JSONObject(retryResponseContent)
                }
            }

            // logger.info("response: $responseContent")
            return JSONObject(responseContent)
        }
    }

    fun executeContainerCommand(codeccTaskId: Long, staffName: String, name: String, command: List<String>): Pair<Boolean, String> {
        val uri = "/api/v2.1/containers/$name/exec"
        val body = mapOf("command" to command)
        logger.info("request url: $uri")
        logger.info("request body: $body")
        OkhttpUtils.doHttp(buildPostRequest(
            userId = staffName,
            url = uri,
            body = ObjectMapper().writeValueAsString(body),
            codeccTaskId = codeccTaskId
        )).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
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
                    Pair(true, retMsg)
                } else {
                    logger.info("execute command failed, retCode:$retCode, msg:$retMsg")
                    Pair(false, retMsg)
                }
            } else {
                val msg = responseData["actionMessage"] as String
                throw OperationException(msg)
            }
        }
    }

    /**
     * first： 成功or失败
     * second：成功时为containerName，失败时为错误信息
     */
    fun waitTaskFinish(codeccTaskId: Long, userId: String, taskId: String): Pair<TaskStatus, String> {
        logger.info("waiting for dev cloud task finish")
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 30 * 60 * 1000) {
                logger.error("dev cloud task timeout")
                return Pair(TaskStatus.TIMEOUT, "")
            }
            Thread.sleep(1 * 1000)
            val (isFinish, success, msg) = getTaskResult(codeccTaskId, userId, taskId)
            return when {
                !isFinish -> continue@loop
                !success -> {
                    logger.error("execute job failed, msg: $msg")
                    Pair(TaskStatus.FAILED, msg)
                }
                else -> Pair(TaskStatus.SUCCEEDED, msg)
            }
        }
    }

    private fun getTaskResult(codeccTaskId: Long, userId: String, taskId: String): TaskResult {
        try {
            val taskStatus = getTasks(codeccTaskId, userId, taskId)
            val actionCode = taskStatus.optString("actionCode")
            return if ("200" != actionCode) {
                // 创建失败
                val msg = taskStatus.optString("actionMessage")
                logger.error("Execute  task failed, actionCode is $actionCode, msg: $msg")
                TaskResult(
                    true,
                    false,
                    msg
                )
            } else {
                val status = taskStatus.optJSONObject("data").optString("status")
                when (status) {
                    "succeeded" -> {
                        val containerName = taskStatus.optJSONObject("data").optString("name")
                        logger.info("Task: $taskId success, containerName: $containerName, taskResponse: $taskStatus")
                        TaskResult(
                            true,
                            true,
                            containerName
                        )
                    }
                    "failed" -> {
                        val resultDisplay = taskStatus.optJSONObject("data").optString("result")
                        logger.error("Task: $taskId failed, taskResponse: $taskStatus")
                        TaskResult(
                            true,
                            false,
                            resultDisplay
                        )
                    }
                    else -> TaskResult(
                        false,
                        false,
                        ""
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Get dev cloud task error, taskId: $taskId", e)
            return TaskResult(
                true,
                false,
                "创建失败，异常信息:${e.message}"
            )
        }
    }

    private fun buildPostRequest(
        userId: String,
        url: String,
        body: String,
        codeccTaskId: Long
    ): Request {
        return Request.Builder()
            .url(buildUrl(url, codeccTaskId))
            .headers(buildHeader(userId, codeccTaskId).toHeaders())
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body))
            .build()
    }

    private fun buildGetRequest(
        userId: String,
        url: String,
        codeccTaskId: Long
    ): Request {
        return Request.Builder()
            .url(buildUrl(url, codeccTaskId))
            .headers(buildHeader(userId, codeccTaskId).toHeaders())
            .get()
            .build()
    }

    private fun buildUrl(uri: String, codeccTaskId: Long): String {
        return if (codeccTaskId == -3L) {
            "$devopsIdcProxyGateway/proxy-devnet?" +
                "url=${URLEncoder.encode(closeSourceUrl + uri, "UTF-8")}"
        } else {
            devCloudUrl + uri
        }
    }

    private fun buildHeader(userId: String, codeccTaskId: Long): Map<String, String> {
        return if (codeccTaskId == -3L) {
            makeIdcProxyHeaders(closeSourceAppId, closeSourceToken, userId)
        } else {
            SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, userId, smartProxyToken)
        }
    }

    fun makeIdcProxyHeaders(
        appId: String,
        token: String,
        userId: String
    ): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["APPID"] = appId
        val random = RandomStringUtils.randomAlphabetic(8)
        headerBuilder["RANDOM"] = random
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        headerBuilder["TIMESTP"] = timestamp
        val encKey = DigestUtils.md5Hex("$token$timestamp$random")
        headerBuilder["ENCKEY"] = encKey
        headerBuilder["TIMESTAMP"] = timestamp
        headerBuilder["X-STAFFNAME"] = userId

        return headerBuilder
    }
}

data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)
