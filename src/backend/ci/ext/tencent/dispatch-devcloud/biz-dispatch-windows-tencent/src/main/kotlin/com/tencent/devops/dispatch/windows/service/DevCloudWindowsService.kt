package com.tencent.devops.dispatch.windows.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dispatch.windows.dao.BuildHistoryDao
import com.tencent.devops.dispatch.windows.enums.DevCloudCreateWindowsStatus
import com.tencent.devops.dispatch.windows.pojo.DevCloudWindowsCreate
import com.tencent.devops.dispatch.windows.pojo.DevCloudWindowsCreateEnv
import com.tencent.devops.dispatch.windows.pojo.DevCloudWindowsCreateInfo
import com.tencent.devops.dispatch.windows.pojo.DevCloudWindowsDelete
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_LANDUN_ENV
import com.tencent.devops.dispatch.windows.pojo.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.windows.pojo.QueryTaskStatusResponse
import com.tencent.devops.dispatch.windows.pojo.WindowsMachineGetResponse
import com.tencent.devops.dispatch.windows.util.SmartProxyUtil
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import javax.ws.rs.core.Response

@Service
class DevCloudWindowsService @Autowired constructor(
    private val dslContext: DSLContext,
    private val buildHistoryDao: BuildHistoryDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudWindowsService::class.java)
        private const val retryCount = 200
    }

    @Value("\${devCloud.appId:}")
    private lateinit var devCloudAppId: String

    @Value("\${devCloud.token:}")
    private lateinit var devCloudToken: String

    @Value("\${devCloud.url:}")
    private lateinit var devCloudUrl: String

    @Value("\${devCloud.smartProxyToken:}")
    private lateinit var smartProxyToken: String

    @Value("\${devCloud.rsaPrivateKey:}")
    private lateinit var rsaPrivateKey: String

    @Value("\${credential.aes-key:C/R%3{?OS}IeGT21}")
    private lateinit var aesKey: String

    @Value("\${devCloud.idcProxy:}")
    private lateinit var devopsIdcProxyGateway: String

    // 获取windows构建机
    fun getWindowsMachine(
        os: String?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        creator: String,
        env: Map<String, Any>
    ): DevCloudWindowsCreateInfo? {

        val url = "$devCloudUrl/api/v2.1/windows/get"

        val windowsCreateEnv = DevCloudWindowsCreateEnv(
            project = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            devops_project_id = env[ENV_KEY_PROJECT_ID].toString(),
            devops_agent_secret_key = env[ENV_KEY_AGENT_SECRET_KEY].toString(),
            devops_agent_id = env[ENV_KEY_AGENT_ID].toString(),
            devops_gateway = env[ENV_KEY_GATEWAY].toString(),
            landun_env = env[ENV_KEY_LANDUN_ENV].toString()
        )
        val windowsCreate = DevCloudWindowsCreate(
            os = os,
            env = windowsCreateEnv
        )
        var taskId = ""
        val body = JsonUtil.toJson(windowsCreate, false)
        logger.info("getWindowsMachine|url=$url|body=$body")
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, smartProxyToken, creator).toHeaders())
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body.toString()))
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info(
                "DevCloudWindowsService|getWindowsMachine" +
                    "|creatVM|code|${response.code}|response|$responseContent"
            )
            if (!response.isSuccessful) {
                logger.error(
                    "DevCloudWindowsService|getWindowsMachine|Fail to request to DevCloud creatVM" +
                        "|code|${response.code}|msg|$responseContent"
                )
                return null
            }
            val responseData = JsonUtil.to(responseContent, object : TypeReference<WindowsMachineGetResponse>() {})
            if (responseData.code != 0) {
                logger.info(
                    "DevCloudWindowsService|getWindowsMachine|fail to get windows|" +
                        "code|${responseData.code}|message|${responseData.message}"
                )
                return null
            }
            taskId = responseData.data.taskGuid
            logger.info("DevCloudWindowsService|getWindowsMachine|success send request|taskId|$taskId")
        }

        var times = 0
        logger.info("DevCloudWindowsService|getWindowsMachine|start query")
        while (times < retryCount) {
            val temp = queryTaskStatus(taskId = taskId, creator = creator)
            when (temp.first) {
                DevCloudCreateWindowsStatus.Failed.title -> {
                    logger.info(
                        "DevCloudWindowsService|getWindowsMachine" +
                            "|fail to query task status|actionMessage|${temp.third}"
                    )
                    return null
                }
                DevCloudCreateWindowsStatus.Canceled.title -> {
                    logger.info("DevCloudWindowsService|getWindowsMachine|user cancel task")
                    return null
                }
                DevCloudCreateWindowsStatus.Succeeded.title -> {
                    logger.info("DevCloudWindowsService|getWindowsMachine|success create Windows")
                    return temp.second
                }
            }

            if (times % 50 == 0) {
                logger.info("DevCloudWindowsService|getWindowsMachine|query times|${times + 1}")
            }
            times++
            Thread.sleep(3000)
        }
        logger.info("DevCloudWindowsService|getWindowsMachine|create failed.time Over limit")
        return null
    }

    // 返回值为三元组,分别代表devcloud构建状态,构建成功时返回的数据,构建失败时的错误信息
    fun queryTaskStatus(taskId: String, creator: String): Triple<String, DevCloudWindowsCreateInfo?, String> {
        val url = "$devCloudUrl/api/v2.1/windows/task?id=$taskId"
        logger.debug("queryTaskStatus|url=$url")
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, smartProxyToken, creator).toHeaders())
            .get()
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("DevCloudWindowsService|queryTaskStatus|request|$request|response|$responseContent")
            // 如果网络波动导致的失败,就不需要返回failed状态,而是返回running状态,过几秒再来轮询
            if (!response.isSuccessful) {
                logger.info("DevCloudWindowsService|queryTaskStatus|request fail,retry later")
                return Triple(DevCloudCreateWindowsStatus.Running.title, null, "")
            }
            val responseData: QueryTaskStatusResponse = jacksonObjectMapper().readValue(responseContent)
            logger.info("DevCloudWindowsService|queryTaskStatus|responseData|$responseData")
            // 如果actionCode不是200,就是devcloud出问题了,返回错误状态
            if (responseData.code != 0 ) {
                return Triple(DevCloudCreateWindowsStatus.Failed.title, null, responseData.message)
            }

            when (responseData.data.status) {
                DevCloudCreateWindowsStatus.Failed.title -> {
                    return Triple(DevCloudCreateWindowsStatus.Failed.title, null, responseData.message)
                }
                DevCloudCreateWindowsStatus.Canceled.title -> {
                    return Triple(DevCloudCreateWindowsStatus.Canceled.title, null, responseData.message)
                }
                DevCloudCreateWindowsStatus.Waiting.title ->{
                    return Triple(DevCloudCreateWindowsStatus.Waiting.title, null, responseData.message)
                }
                DevCloudCreateWindowsStatus.Running.title ->{
                    return Triple(DevCloudCreateWindowsStatus.Running.title, null, responseData.message)
                }
                else -> {
                    // continue
                }
            }

            if (responseData.data.result == null) {
                return Triple(DevCloudCreateWindowsStatus.Waiting.title, null, responseData.message)
            }

            // 如果返回状态为成功,就从response里取出数据并返回
            if (responseData.data.status == DevCloudCreateWindowsStatus.Succeeded.title) {
                return Triple(
                    responseData.data.status,
                    DevCloudWindowsCreateInfo(
                        taskStatus = responseData.data.status,
                        ip = responseData.data.result!!.ip,
                        ipStatus = responseData.data.result!!.status,
                        processId = responseData.data.result!!.processId,
                        buildTime = responseData.data.buildTime,
                        createdAt = responseData.data.createdAt,
                        updatedAt = responseData.data.updatedAt,
                        taskGuid = taskId
                    ), ""
                )
            }
            // 否则返回运行中状态
            return Triple(DevCloudCreateWindowsStatus.Running.title, null, "")
        }
    }

    // 释放windows构建机资源
    fun deleteWindowsMachine(
        creator: String,
        taskGuid: String
    ): Boolean {
        val url = "$devCloudUrl/api/v2.1/windows/delete"
        val macosVmDelete = DevCloudWindowsDelete(
            taskGuid = taskGuid
        )
        val body = ObjectMapper().writeValueAsString(macosVmDelete)
        logger.info("DevCloudWindowsService|deleteWindowsMachine|deleteVM|$url|$body")
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeHeaders(devCloudAppId, devCloudToken, smartProxyToken, creator).toHeaders())
            .post(RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), body.toString()))
            .build()
        var result: Boolean = true
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info(
                "DevCloudWindowsService|deleteWindowsMachine|deleteVM" +
                    "|http code|${response.code}|content|$responseContent"
            )
            if (!response.isSuccessful) {
                logger.error(
                    "DevCloudWindowsService|deleteWindowsMachine" +
                        "|Fail|code|${response.code}|msg|$responseContent"
                )
                result = false
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["code"] as Int
            result = (0 == code)
        }
        return result
    }

    fun toIdcUrl(realUrl: String) = "$devopsIdcProxyGateway/proxy-devnet?" +
        "url=${URLEncoder.encode(realUrl, "UTF-8")}"
}
