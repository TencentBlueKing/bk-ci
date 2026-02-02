package com.tencent.devops.dispatch.macos.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants
import com.tencent.devops.common.environment.agent.utils.SmartProxyUtil
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.dispatch.macos.dao.BuildHistoryDao
import com.tencent.devops.dispatch.macos.dao.DevcloudVirtualMachineDao
import com.tencent.devops.dispatch.macos.enums.DevCloudCreateMacVMStatus
import com.tencent.devops.dispatch.macos.pojo.TaskResponse
import com.tencent.devops.dispatch.macos.pojo.devcloud.DMAllVmModelRsp
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmCreate
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmCreateInfo
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDebugLoginRequest
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDebugLoginResponse
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDelete
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmInfo
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmModelRequest
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmModelResponse
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
    private val devcloudVirtualMachineDao: DevcloudVirtualMachineDao,
    private val buildHistoryDao: BuildHistoryDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudMacosService::class.java)
        private const val XCODE_VERSION = "devops_xcodeVersion"
        private const val DEFAULT_HW_TYPE = "VMware"
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
    ): Pair<DevCloudMacosVmCreateInfo?, String> {
        val buildId = dispatchMessage.event.buildId

        var taskId = ""
        val realUrl = toIdcUrl("$devCloudUrl/api/mac/vm/create")
        val body = ObjectMapper().writeValueAsString(buildCreateBody(dispatchMessage))
        logger.info("$buildId DevCloud creatVM request realUrl: $realUrl body: $body")
        val request = Request.Builder()
            .url(realUrl)
            .headers(
                SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, dispatchMessage.event.userId)
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
                return Pair(null, "")
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            val message = responseData["actionMessage"] as String
            if (code != 200) {
                logger.info("$buildId DevCloud fail to create MacOS,actionCode is $code ,actionMessage is $message")
                return Pair(null, "")
            }
            val temp = responseData["data"] as Map<String, Any>
            taskId = temp["taskId"] as String
            logger.info("$buildId success send creating VM request,enters the query process,taskId is $taskId")
        }

        // 轮训task执行结果，10min超时
        repeat(200) { times ->
            val taskResponse = getTaskStatus(taskId, dispatchMessage.event.userId)
            if (taskResponse?.data != null) {
                when (taskResponse.data.status) {
                    DevCloudCreateMacVMStatus.failed.title, DevCloudCreateMacVMStatus.canceled.title -> {
                        logger.info("$taskId status: failed or canceled, Try again")
                        return Pair(null, taskId)
                    }
                    DevCloudCreateMacVMStatus.succeeded.title -> {
                        logger.info("$taskId status: succeeded")
                        return Pair(taskResponse.data, taskId)
                    }
                }
            }

            if (times % 50 == 0) logger.info("Query times is ${times + 1}")

            Thread.sleep(3000)
        }

        logger.info("Loop task timout 10min")
        return Pair(null, "")
    }

    private fun buildCreateBody(dispatchMessage: DispatchMessage): DevCloudMacosVmCreate {

        val dispatchType = dispatchMessage.event.dispatchType as MacOSDispatchType
        logger.info("dispatchType: ${dispatchType.macOSEvn}")
/*        var (macOSHwSpec, systemVersion, xcodeVersion) = dispatchType.macOSEvn.split(":")
            .let { macOSEnv ->
                when (macOSEnv.size) {
                    0 -> Triple(null, null, null)
                    1 -> Triple(null, macOSEnv[0], null)
                    2 -> Triple(null, macOSEnv[0], macOSEnv[1])
                    else -> Triple(macOSEnv[0], macOSEnv[1], macOSEnv[2])
                }
            }*/
        var macOSHwSpec = dispatchType.macOSHwSpec
        if (macOSHwSpec.isNullOrBlank()) {
            macOSHwSpec = DEFAULT_HW_TYPE
        }
        var systemVersion = dispatchType.systemVersion
        var xcodeVersion = dispatchType.xcodeVersion
        logger.info("macOSHwSpec: $macOSHwSpec, systemVersion: $systemVersion, xcodeVersion: $xcodeVersion")

        val isStreamProject = dispatchMessage.event.projectId.startsWith("git_")

        if (isStreamProject) {
            val (streamSystemVersion, streamXcodeVersion) = macVmTypeService.getStreamSystemVersionByVersion(
                systemVersion,
                xcodeVersion
            )
            xcodeVersion = streamXcodeVersion
            systemVersion = streamSystemVersion
        }

        return with(dispatchMessage.event) {
            DevCloudMacosVmCreate(
                project = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                source = if (isStreamProject) "gongfeng" else "landun",
                os = systemVersion,
                xcode = xcodeVersion,
                env = mapOf(
                    DockerConstants.ENV_KEY_PROJECT_ID to projectId,
                    DockerConstants.ENV_KEY_AGENT_ID to dispatchMessage.id,
                    DockerConstants.ENV_KEY_AGENT_SECRET_KEY to dispatchMessage.secretKey,
                    DockerConstants.ENV_KEY_GATEWAY to dispatchMessage.gateway,
                    XCODE_VERSION to (xcodeVersion ?: ""),
                    DockerConstants.ENV_KEY_DEVCLOUD_MODEL to macOSHwSpec
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
                    name = itemTmp["name"] as String,
                    memory = itemTmp["memory"] as String,
                    assetId = itemTmp["assetId"] as String,
                    ip = itemTmp["ip"] as String,
                    disk = itemTmp["disk"] as String,
                    os = itemTmp["os"] as String,
                    id = itemTmp["id"] as Int,
                    cpu = itemTmp["cpu"] as String
                )
            )
        }

        return vmInfoList
    }

    fun getVmModel(
        request: DevCloudMacosVmModelRequest,
        creator: String
    ): DevCloudMacosVmModelResponse? {
        val url = "$devCloudUrl/api/mac/vm/model"
        val body = ObjectMapper().writeValueAsString(request)
        logger.info("DevCloud getVmModel request body: $body")
        
        val httpRequest = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, creator).toHeaders())
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(httpRequest).use { response ->
                val responseContent = response.body!!.string()
                logger.info("DevCloud getVmModel http code is ${response.code}, response: $responseContent")
                
                if (!response.isSuccessful) {
                    logger.error(
                        "Failed to request DevCloud getVmModel, http response code: ${response.code}, " +
                            "msg: $responseContent"
                    )
                    return null
                }
                
                return JsonUtil.to(responseContent, DevCloudMacosVmModelResponse::class.java)
            }
        } catch (e: Exception) {
            logger.error("Failed to get VM model from DevCloud, url: $url", e)
            return null
        }
    }

    fun getAllVmModels(
        creator: String
    ): DMAllVmModelRsp? {
        val url = "$devCloudUrl/api/mac/vm/model/all"

        val httpRequest = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, creator).toHeaders())
            .get()
            .build()

        try {
            OkhttpUtils.doHttp(httpRequest).use { response ->
                val responseContent = response.body!!.string()
                logger.info("DevCloud getAllVmModel http code is ${response.code}, response: $responseContent")

                if (!response.isSuccessful) {
                    logger.error(
                        "Failed to request DevCloud getAllVmModel, http response code: ${response.code}, " +
                                "msg: $responseContent"
                    )
                    return null
                }

                return JsonUtil.to(responseContent, DMAllVmModelRsp::class.java)
            }
        } catch (e: Exception) {
            logger.error("Failed to getAllVmModes from DevCloud, url: $url", e)
            return null
        }
    }

    /**
     * 开启MacOS虚拟机调试登录
     */
    fun debugLogin(
        creator: String,
        debugLoginRequest: DevCloudMacosVmDebugLoginRequest
    ): DevCloudMacosVmDebugLoginResponse? {
        return executeDebugLoginRequest(
            creator = creator,
            debugLoginRequest = debugLoginRequest,
            apiPath = "/api/mac/vm/open/debuglogin",
            operationType = "open"
        )
    }

    /**
     * 关闭MacOS虚拟机调试登录
     */
    fun debugClose(
        creator: String,
        debugLoginRequest: DevCloudMacosVmDebugLoginRequest
    ): DevCloudMacosVmDebugLoginResponse? {
        return executeDebugLoginRequest(
            creator = creator,
            debugLoginRequest = debugLoginRequest,
            apiPath = "/api/mac/vm/close/debuglogin",
            operationType = "close"
        )
    }

    /**
     * 执行调试登录请求的通用方法
     */
    private fun executeDebugLoginRequest(
        creator: String,
        debugLoginRequest: DevCloudMacosVmDebugLoginRequest,
        apiPath: String,
        operationType: String
    ): DevCloudMacosVmDebugLoginResponse? {
        val url = "$devCloudUrl$apiPath"
        val body = ObjectMapper().writeValueAsString(debugLoginRequest)
        logger.info("MacOS VM debug login $operationType request - $url taskId: ${debugLoginRequest.taskId}, " +
                "creator: $creator, body: $body")
        
        val request = Request.Builder()
            .url(toIdcUrl(url))
            .headers(SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, creator).toHeaders())
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        try {
            OkhttpUtils.doHttp(request).use { response ->
                val responseContent = response.body!!.string()
                logger.info(
                    "MacOS VM debug login $operationType response - taskId: ${debugLoginRequest.taskId}, " +
                        "httpCode: ${response.code}, response: $responseContent"
                )
                
                if (!response.isSuccessful) {
                    logger.error(
                        "Failed to $operationType MacOS VM debug login - taskId: ${debugLoginRequest.taskId}, " +
                            "httpCode: ${response.code}, msg: $responseContent"
                    )
                    return null
                }
                
                return JsonUtil.to(responseContent, DevCloudMacosVmDebugLoginResponse::class.java)
            }
        } catch (e: Exception) {
            logger.error("Exception occurred when ${operationType}ing MacOS VM debug login - taskId: ${debugLoginRequest.taskId}, url: $url", e)
            return null
        }
    }

    fun toIdcUrl(realUrl: String) = "$devopsIdcProxyGateway/proxy-devnet?" +
        "url=${URLEncoder.encode(realUrl, "UTF-8")}"

    /**
     * 开启MacOS虚拟机调试
     * @param userId 用户ID
     * @param pipelineId 流水线ID
     * @param vmSeqId 虚拟机序列ID
     * @param buildId 构建ID，可选
     * @param executeCount 执行次数
     * @return 调试登录响应信息，失败返回null
     */
    fun startDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?,
        executeCount: Int
    ): DevCloudMacosVmDebugLoginResponse? {
        logger.info(
            "Start macOS debug login - userId: $userId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                "buildId: $buildId, executeCount: $executeCount"
        )

        val taskId = getTaskIdFromBuildHistory(pipelineId, vmSeqId, buildId, executeCount)
        if (taskId == null) {
            logger.warn(
                "TaskId not found for startDebug - pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: $buildId, executeCount: $executeCount"
            )
            return null
        }

        logger.info("Found taskId: $taskId for pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId")

        val debugLoginRequest = DevCloudMacosVmDebugLoginRequest(taskId = taskId)
        val debugLoginResponse = debugLogin(userId, debugLoginRequest)
        
        if (debugLoginResponse != null) {
            logger.info("Debug login successful for taskId: $taskId")
        } else {
            logger.error("Debug login failed for taskId: $taskId")
        }
        
        return debugLoginResponse
    }

    /**
     * 停止MacOS虚拟机调试
     * @param userId 用户ID
     * @param pipelineId 流水线ID
     * @param vmSeqId 虚拟机序列ID
     * @param buildId 构建ID，可选
     * @param executeCount 执行次数
     * @return 是否成功停止调试
     */
    fun stopDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?,
        executeCount: Int
    ): Boolean {
        logger.info(
            "Stop macOS debug login - userId: $userId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                "buildId: $buildId, executeCount: $executeCount"
        )

        val taskId = getTaskIdFromBuildHistory(pipelineId, vmSeqId, buildId, executeCount)
        if (taskId == null) {
            logger.warn(
                "TaskId not found for stopDebug - pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: $buildId, executeCount: $executeCount"
            )
            return false
        }

        logger.info("Found taskId: $taskId for pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId")

        val debugCloseRequest = DevCloudMacosVmDebugLoginRequest(taskId = taskId)
        val result = debugClose(userId, debugCloseRequest)
        
        if (result != null) {
            logger.info("Debug close successful for taskId: $taskId")
            return true
        } else {
            logger.error("Debug close failed for taskId: $taskId")
            return false
        }
    }

    /**
     * 从buildHistory表中查询taskId
     * @param pipelineId 流水线ID
     * @param vmSeqId 虚拟机序列ID
     * @param buildId 构建ID，可选
     * @param executeCount 执行次数
     * @return taskId，如果查询不到或taskId为空则返回null
     */
    private fun getTaskIdFromBuildHistory(
        pipelineId: String,
        vmSeqId: String,
        buildId: String?,
        executeCount: Int
    ): String? {
        val buildHistoryRecord = if (buildId != null) {
            buildHistoryDao.getBuildHistory(dslContext, buildId, vmSeqId, executeCount)?.firstOrNull()
        } else {
            buildHistoryDao.getLatestByPipelineIdAndVmSeqId(dslContext, pipelineId, vmSeqId, executeCount)
        }

        if (buildHistoryRecord == null) {
            logger.warn(
                "Build history record not found for pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: $buildId, executeCount: $executeCount"
            )
            return null
        }

        val taskId = buildHistoryRecord.taskId
        if (taskId.isNullOrBlank()) {
            logger.warn("TaskId is null or empty for pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId")
            return null
        }

        return taskId
    }
}