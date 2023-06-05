package com.tencent.devops.dispatch.macos.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants
import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import com.tencent.devops.dispatch.macos.dao.DevcloudVirtualMachineDao
import com.tencent.devops.dispatch.macos.enums.DevCloudCreateMacVMStatus
import com.tencent.devops.dispatch.macos.pojo.TaskResponse
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmCreate
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmCreateInfo
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDelete
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmInfo
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class DevCloudMacosService @Autowired constructor(
    private val dslContext: DSLContext,
    private val macVmTypeService: MacVmTypeService,
    private val devcloudVirtualMachineDao: DevcloudVirtualMachineDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudMacosService::class.java)
        private const val XCODE_VERSION = "devops_xcodeVersion"
    }

    @Value("\${macos.devCloud.appId:}")
    private lateinit var devCloudAppId: String

    @Value("\${macos.devCloud.token:}")
    private lateinit var devCloudToken: String

    @Value("\${macos.devCloud.url:}")
    private lateinit var devCloudUrl: String

    @Value("\${devopsGateway.idcProxy:}")
    private lateinit var devopsIdcProxyGateway: String

    fun creatVM(
        dispatchMessage: DispatchMessage
    ): DevCloudMacosVmCreateInfo? {
        val buildId = dispatchMessage.buildId

        var taskId = ""
        val body = ObjectMapper().writeValueAsString(buildCreateBody(dispatchMessage))
        logger.info("$buildId DevCloud creatVM request body: $body")
        val request = Request.Builder()
            .url(toIdcUrl("$devCloudUrl/api/mac/vm/create"))
            .headers(
                SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, dispatchMessage.userId)
                    .toHeaders()
            )
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("$buildId DevCloud creatVM http code is ${response.code}, $responseContent")
            if (!response.isSuccessful) {
                logger.error(
                    "$buildId Fail to request to DevCloud creatVM, http response code: ${response.code}, " +
                        "msg: $responseContent"
                )
                return null
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            val message = responseData["actionMessage"] as String
            if (code != 200) {
                logger.info("$buildId DevCloud fail to create MacOS,actionCode is $code ,actionMessage is $message")
                return null
            }
            val temp = responseData["data"] as Map<String, Any>
            taskId = temp["taskId"] as String
            logger.info("$buildId success send creating VM request,enters the query process,taskId is $taskId")
        }

        // 轮训task执行结果，10min超时
        repeat(200) { times ->
            val taskResponse = getTaskStatus(taskId, dispatchMessage.userId)
            if (taskResponse?.data != null) {
                when (taskResponse.data.status) {
                    DevCloudCreateMacVMStatus.failed.title, DevCloudCreateMacVMStatus.canceled.title -> {
                        logger.info("$taskId status: failed or canceled, Try again")
                        return null
                    }
                    DevCloudCreateMacVMStatus.succeeded.title -> {
                        logger.info("$taskId status: succeeded")
                        return taskResponse.data
                    }
                }
            }

            if (times % 50 == 0) logger.info("Query times is ${times + 1}")

            Thread.sleep(3000)
        }

        logger.info("Loop task timout 10min")
        return null
    }

    private fun buildCreateBody(dispatchMessage: DispatchMessage): DevCloudMacosVmCreate {
        val (systemVersion, xcodeVersion) = dispatchMessage.dispatchMessage.split(":").let { macOSEnv ->
            when (macOSEnv.size) {
                0 -> Pair(null, null)
                1 -> Pair(macOSEnv[0], null)
                else -> Pair(macOSEnv[0], macOSEnv[1])
            }
        }

        val isGitProject = dispatchMessage.projectId.startsWith("git_")

        return with(dispatchMessage) {
            DevCloudMacosVmCreate(
                project = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                source = if (isGitProject) "gongfeng" else "landun",
                os = if (isGitProject)
                    macVmTypeService.getSystemVersionByVersion(systemVersion)
                else
                    systemVersion,
                xcode = xcodeVersion,
                env = mapOf(
                    DockerConstants.ENV_KEY_PROJECT_ID to projectId,
                    DockerConstants.ENV_KEY_AGENT_ID to dispatchMessage.id,
                    DockerConstants.ENV_KEY_AGENT_SECRET_KEY to dispatchMessage.secretKey,
                    DockerConstants.ENV_KEY_GATEWAY to dispatchMessage.gateway,
                    XCODE_VERSION to (xcodeVersion ?: "")
                )
            )
        }
    }

    private fun getTaskStatus(taskId: String, creator: String): TaskResponse? {
        val url = "$devCloudUrl/api/mac/task/result/$taskId"
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, creator).toHeaders())
            .get()
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                if (!response.isSuccessful) {
                    logger.error("Failed to get $url, response: $responseContent")
                    return null
                }
                val taskResponse = JsonUtil.to(responseContent, TaskResponse::class.java)
                if (taskResponse.actionCode != 200) {
                    logger.error("Get $url response not 200, $responseContent")
                    return null
                }

                return taskResponse
            }
        } catch (e: Exception) {
            logger.error("Failed to get $url.", e)
            return null
        }
    }

    fun saveVM(vmCreateInfo: DevCloudMacosVmCreateInfo): Boolean {
        return devcloudVirtualMachineDao.create(dslContext, vmCreateInfo)
    }

    fun deleteVM(
        creator: String,
        devCloudMacosVmDelete: DevCloudMacosVmDelete
    ): Boolean {
        val url = "$devCloudUrl/api/mac/vm/delete"
        val body = ObjectMapper().writeValueAsString(devCloudMacosVmDelete)
        logger.info("Delete MacOS VM body:$body")
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, creator).toHeaders())
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        var result: Boolean
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.error(
                    "Failed to delete MacOS VM response: ${response.code}, msg: $responseContent"
                )
                result = false
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            result = 200 == code
        }

        return result
    }

    fun getVmList(): List<DevCloudMacosVmInfo> {
        val url = "$devCloudUrl/api/mac/pool/list?page=1&size=9999"
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, "").toHeaders())
            .get()
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("DevCloud getVmList http code is ${response.code}, $responseContent")
            if (!response.isSuccessful) {
                logger.error(
                    "Fail to request to DevCloud getVmList, http response code: ${response.code}, " +
                        "msg: $responseContent"
                )
                return emptyList()
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            if (200 != code) {
                logger.error(
                    "Fail to request to DevCloud getVmList, http response code: ${response.code}, " +
                        "msg: $responseContent"
                )
                return emptyList()
            }

            val dataMap = responseData["data"] as Map<String, Any>
            if (!dataMap.containsKey("items")) {
                logger.error(
                    "Fail to request to DevCloud getVmList, http response code: ${response.code}, " +
                        "msg: $responseContent"
                )
                return emptyList()
            }

            return getVMInfos(dataMap)
        }
    }

    private fun getVMInfos(
        dataMap: Map<String, Any>
    ): List<DevCloudMacosVmInfo> {
        val vmInfoList = mutableListOf<DevCloudMacosVmInfo>()
        val itemsList = dataMap["items"] as List<Any>
        itemsList.forEach { item ->
            val itemTmp = item as Map<String, Any>

            if (itemTmp["ip"] == null) {
                return@forEach
            }

            vmInfoList.add(
                DevCloudMacosVmInfo(
                    name = itemTmp["name"] as String ?: "",
                    memory = itemTmp["memory"] as String ?: "",
                    assetId = itemTmp["assetId"] as String ?: "",
                    ip = itemTmp["ip"] as String ?: "",
                    disk = itemTmp["disk"] as String ?: "",
                    os = itemTmp["os"] as String ?: "",
                    id = itemTmp["id"] as Int ?: 0,
                    cpu = itemTmp["cpu"] as String ?: ""
                )
            )
        }

        return vmInfoList
    }

    fun toIdcUrl(realUrl: String) = "$devopsIdcProxyGateway/proxy-devnet?" +
        "url=${URLEncoder.encode(realUrl, "UTF-8")}"
}
