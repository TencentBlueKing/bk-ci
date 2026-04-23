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
import com.tencent.devops.dispatch.macos.dao.DebugHistoryDao
import com.tencent.devops.dispatch.macos.dao.DevcloudVirtualMachineDao
import com.tencent.devops.dispatch.macos.enums.DevCloudCreateMacVMStatus
import com.tencent.devops.dispatch.macos.enums.MacJobStatus
import com.tencent.devops.dispatch.macos.pojo.TaskResponse
import com.tencent.devops.dispatch.macos.pojo.devcloud.DMAllVmModelRsp
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmCreate
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmCreateInfo
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDebugCloseResponse
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
    private val buildHistoryDao: BuildHistoryDao,
    private val debugHistoryDao: DebugHistoryDao
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
        val userId = dispatchMessage.event.userId
        val createBody = buildCreateBody(dispatchMessage)

        val taskId = sendCreateVmRequest(createBody, userId, buildId)
        if (taskId.isNullOrBlank()) return Pair(null, "")

        val vmCreateInfo = waitForVmReady(taskId, userId, buildId)
        return Pair(vmCreateInfo, taskId)
    }

    /**
     * 发送创建VM的HTTP请求，解析响应并返回taskId
     * @param createBody 创建VM的请求体
     * @param userId 用户ID
     * @param logTag 日志标识，用于区分调用来源
     * @return taskId，请求失败返回null
     */
    private fun sendCreateVmRequest(
        createBody: DevCloudMacosVmCreate,
        userId: String,
        logTag: String
    ): String? {
        val realUrl = toIdcUrl("$devCloudUrl/api/mac/vm/create")
        val body = ObjectMapper().writeValueAsString(createBody)
        logger.info("$logTag DevCloud creatVM request - realUrl: $realUrl, body: $body")

        val request = Request.Builder()
            .url(realUrl)
            .headers(
                SmartProxyUtil.makeIdcProxyHeaders(devCloudAppId, devCloudToken, userId).toHeaders()
            )
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            logger.info("$logTag DevCloud creatVM http code: ${response.code}, response: $responseContent")
            if (!response.isSuccessful) {
                logger.error(
                    "$logTag Failed to creatVM, http response code: ${response.code}, msg: $responseContent"
                )
                return null
            }
            val responseData: Map<String, Any> = jacksonObjectMapper().readValue(responseContent)
            val code = responseData["actionCode"] as Int
            val message = responseData["actionMessage"] as String
            if (code != 200) {
                logger.error("$logTag DevCloud fail to creatVM, actionCode: $code, actionMessage: $message")
                return null
            }
            val temp = responseData["data"] as Map<String, Any>
            val taskId = temp["taskId"] as String
            logger.info("$logTag Success send creating VM request, taskId: $taskId")
            return taskId
        }
    }

    /**
     * 轮询等待VM创建任务完成，10min超时
     * @param taskId 任务ID
     * @param userId 用户ID
     * @param logTag 日志标识，用于区分调用来源
     * @return VM创建信息，失败/取消/超时返回null
     */
    private fun waitForVmReady(
        taskId: String,
        userId: String,
        logTag: String
    ): DevCloudMacosVmCreateInfo? {
        repeat(200) { times ->
            val taskResponse = getTaskStatus(taskId, userId)
            if (taskResponse?.data != null) {
                when (taskResponse.data.status) {
                    DevCloudCreateMacVMStatus.failed.title, DevCloudCreateMacVMStatus.canceled.title -> {
                        logger.error("$logTag taskId: $taskId status: failed or canceled")
                        return null
                    }
                    DevCloudCreateMacVMStatus.succeeded.title -> {
                        logger.info("$logTag taskId: $taskId status: succeeded")
                        return taskResponse.data
                    }
                }
            }

            if (times % 50 == 0) logger.info("$logTag Query times: ${times + 1}, taskId: $taskId")

            Thread.sleep(3000)
        }

        logger.error("$logTag Loop task timeout 10min, taskId: $taskId")
        return null
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
    ): DevCloudMacosVmDebugCloseResponse? {
        return executeDebugCloseRequest(
            creator = creator,
            debugLoginRequest = debugLoginRequest,
            apiPath = "/api/mac/vm/close/debuglogin",
            operationType = "close"
        )
    }

    /**
     * 执行开启调试登录请求
     */
    private fun executeDebugLoginRequest(
        creator: String,
        debugLoginRequest: DevCloudMacosVmDebugLoginRequest,
        apiPath: String,
        operationType: String
    ): DevCloudMacosVmDebugLoginResponse? {
        val responseContent = doDebugRequest(creator, debugLoginRequest, apiPath, operationType) ?: return null
        return JsonUtil.to(responseContent, DevCloudMacosVmDebugLoginResponse::class.java)
    }

    /**
     * 执行关闭调试登录请求
     */
    private fun executeDebugCloseRequest(
        creator: String,
        debugLoginRequest: DevCloudMacosVmDebugLoginRequest,
        apiPath: String,
        operationType: String
    ): DevCloudMacosVmDebugCloseResponse? {
        val responseContent = doDebugRequest(creator, debugLoginRequest, apiPath, operationType) ?: return null
        return JsonUtil.to(responseContent, DevCloudMacosVmDebugCloseResponse::class.java)
    }

    /**
     * 执行调试请求的通用方法，返回响应内容字符串
     */
    private fun doDebugRequest(
        creator: String,
        debugLoginRequest: DevCloudMacosVmDebugLoginRequest,
        apiPath: String,
        operationType: String
    ): String? {
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
                
                return responseContent
            }
        } catch (e: Exception) {
            logger.error("Exception occurred when ${operationType}ing MacOS VM debug login - " +
                "taskId: ${debugLoginRequest.taskId}, url: $url", e)
            return null
        }
    }

    fun toIdcUrl(realUrl: String) = "$devopsIdcProxyGateway/proxy-devnet?" +
        "url=${URLEncoder.encode(realUrl, "UTF-8")}"

    /**
     * 开启MacOS虚拟机调试
     * 根据构建历史记录的status判断：
     * - Running：直接使用已有的taskId发起调试登录
     * - Done：重新创建虚拟机，等待开机成功后使用新的taskId发起调试登录
     * 调试成功后将taskId和是否新建VM等信息记录到debug表
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

        val debugTaskInfo = getTaskIdFromBuildHistory(pipelineId, vmSeqId, buildId, executeCount, userId)
        if (debugTaskInfo == null) {
            logger.warn(
                "TaskId not found for startDebug - pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: $buildId, executeCount: $executeCount"
            )
            return null
        }

        val (taskId, newCreatedVm, actualBuildId, projectId) = debugTaskInfo
        logger.info(
            "Found taskId: $taskId, newCreatedVm: $newCreatedVm for pipelineId: $pipelineId, " +
                "vmSeqId: $vmSeqId, buildId: $actualBuildId"
        )

        val debugLoginRequest = DevCloudMacosVmDebugLoginRequest(taskId = taskId)
        // 调用debug接口，actionCode非0时重试最多3次，每次间隔1秒
        val maxRetryCount = 3
        var debugLoginResponse: DevCloudMacosVmDebugLoginResponse? = null
        for (retryCount in 0..maxRetryCount) {
            debugLoginResponse = debugLogin(userId, debugLoginRequest)
            if (debugLoginResponse == null) {
                logger.error("Debug login request failed for taskId: $taskId, retryCount: $retryCount")
                break
            }
            if (debugLoginResponse.actionCode == 200) {
                logger.info(
                    "Debug login successful for taskId: $taskId, actionCode: ${debugLoginResponse.actionCode}"
                )
                break
            }
            logger.warn(
                "Debug login returned non-zero actionCode: ${debugLoginResponse.actionCode}, " +
                    "actionMessage: ${debugLoginResponse.actionMessage}, taskId: $taskId, " +
                    "retryCount: $retryCount/$maxRetryCount"
            )
            if (retryCount < maxRetryCount) {
                Thread.sleep(3000)
            }
        }

        if (debugLoginResponse != null && debugLoginResponse.actionCode == 0 && !debugTaskInfo.isExistingDebug) {
            // Running状态复用已有VM时，debugLogin成功后记录debug信息到debug表
            logger.info("Debug login successful for taskId: $taskId, saving debug history record")
            debugHistoryDao.saveDebugHistory(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = actualBuildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount,
                taskId = taskId,
                newCreatedVm = newCreatedVm,
                userId = userId
            )
        } else if (debugLoginResponse == null || debugLoginResponse.actionCode != 0) {
            logger.error(
                "Debug login failed for taskId: $taskId, " +
                    "actionCode: ${debugLoginResponse?.actionCode}, " +
                    "actionMessage: ${debugLoginResponse?.actionMessage}"
            )
        }

        return debugLoginResponse
    }

    /**
     * 停止MacOS虚拟机调试
     * 首先查询debug表获取taskId和是否是新创建的VM信息
     * 发起关闭调试登录请求，如果是新创建的VM则额外发起关机（deleteVM）
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

        // 优先从debug表查询调试记录
        val debugRecord = if (buildId != null) {
            debugHistoryDao.getDebuggingRecord(dslContext, buildId, vmSeqId, executeCount)
        } else {
            debugHistoryDao.getLatestDebuggingRecord(dslContext, pipelineId, vmSeqId)
        }

        if (debugRecord == null) {
            logger.warn(
                "Debug history record not found for stopDebug - pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: $buildId, executeCount: $executeCount"
            )
            return false
        }

        val taskId = debugRecord.taskId
        val newCreatedVm = debugRecord.newCreatedVm
        logger.info(
            "Found debug record - taskId: $taskId, newCreatedVm: $newCreatedVm, " +
                "pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: ${debugRecord.buildId}"
        )

        // 发起关闭调试登录请求
        val debugCloseRequest = DevCloudMacosVmDebugLoginRequest(taskId = taskId)
        val closeResult = debugClose(userId, debugCloseRequest)

        if (closeResult == null) {
            logger.error("Debug close failed for taskId: $taskId")
            return false
        }

        logger.info("Debug close successful for taskId: $taskId")

        // 更新debug记录状态为已停止
        debugHistoryDao.updateStatusToStopped(dslContext, debugRecord.id)

        // 如果是新创建的VM，需要发起关机
        if (newCreatedVm) {
            logger.info("VM was newly created for debug, deleting VM - taskId: $taskId")
            val deleteResult = deleteVM(
                creator = userId,
                devCloudMacosVmDelete = DevCloudMacosVmDelete(
                    project = debugRecord.projectId,
                    pipelineId = debugRecord.pipelineId,
                    buildId = debugRecord.buildId,
                    vmSeqId = debugRecord.vmSeqId,
                    id = taskId
                )
            )
            if (deleteResult) {
                logger.info("Successfully deleted debug VM for taskId: $taskId")
            } else {
                logger.error("Failed to delete debug VM for taskId: $taskId")
            }
        }

        return true
    }

    /**
     * 调试任务信息，包含taskId、是否新创建VM、构建ID和项目ID
     */
    data class DebugTaskInfo(
        val taskId: String,
        val newCreatedVm: Boolean,
        val buildId: String,
        val projectId: String,
        val os: String?,
        val xcode: String?,
        val macOSHwSpec: String?,
        val source: String?,
        val isExistingDebug: Boolean = false
    )

    /**
     * 获取调试任务信息
     * 优先查询debug表中是否有正在运行的调试任务（DEBUGGING状态），如果有则直接复用调试信息
     * 如果debug表中没有，则从buildHistory表中查询，根据记录的status判断：
     * - Running：直接返回已有的taskId，标记为非新建VM
     * - Done：重新发起createVm，等待开机成功并获取新的taskId，标记为新建VM
     * @param pipelineId 流水线ID
     * @param vmSeqId 虚拟机序列ID
     * @param buildId 构建ID，可选
     * @param executeCount 执行次数
     * @param userId 用户ID，用于Done状态下重新创建VM
     * @return 调试任务信息，如果查询不到或创建失败则返回null
     */
    private fun getTaskIdFromBuildHistory(
        pipelineId: String,
        vmSeqId: String,
        buildId: String?,
        executeCount: Int,
        userId: String
    ): DebugTaskInfo? {
        // 优先查询debug表中是否有正在运行的调试任务，有则直接复用
        val existingDebugRecord = if (buildId != null) {
            debugHistoryDao.getDebuggingRecord(dslContext, buildId, vmSeqId, executeCount)
        } else {
            debugHistoryDao.getLatestDebuggingRecord(dslContext, pipelineId, vmSeqId)
        }

        if (existingDebugRecord != null) {
            logger.info(
                "Found existing debugging record, reuse debug info - id: ${existingDebugRecord.id}, " +
                    "taskId: ${existingDebugRecord.taskId}, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: ${existingDebugRecord.buildId}, newCreatedVm: ${existingDebugRecord.newCreatedVm}"
            )
            return DebugTaskInfo(
                taskId = existingDebugRecord.taskId,
                newCreatedVm = existingDebugRecord.newCreatedVm,
                buildId = existingDebugRecord.buildId,
                projectId = existingDebugRecord.projectId,
                os = null,
                xcode = null,
                macOSHwSpec = null,
                source = null,
                isExistingDebug = true
            )
        }

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

        val status = buildHistoryRecord.status
        logger.info(
            "Build history record status: $status for pipelineId: $pipelineId, " +
                "vmSeqId: $vmSeqId, buildId: $buildId"
        )

        return when (status) {
            MacJobStatus.Running.name -> {
                // 状态为Running，直接返回已有的taskId，非新建VM
                val taskId = buildHistoryRecord.taskId
                if (taskId.isNullOrBlank()) {
                    logger.warn(
                        "TaskId is null or empty for running record - pipelineId: $pipelineId, " +
                            "vmSeqId: $vmSeqId, buildId: $buildId"
                    )
                    return null
                }
                logger.info("VM is running, reuse existing taskId: $taskId")
                DebugTaskInfo(
                    taskId = taskId,
                    newCreatedVm = false,
                    buildId = buildHistoryRecord.buildId,
                    projectId = buildHistoryRecord.projectId,
                    os = buildHistoryRecord.os,
                    xcode = buildHistoryRecord.xcode,
                    macOSHwSpec = buildHistoryRecord.macosHwSpec,
                    source = buildHistoryRecord.source
                )
            }
            MacJobStatus.Done.name -> {
                // 状态为Done，重新发起createVm等待开机成功并获取新的taskId，标记为新建VM
                logger.info(
                    "VM is done, re-creating VM for debug - pipelineId: $pipelineId, " +
                        "vmSeqId: $vmSeqId, buildId: ${buildHistoryRecord.buildId}"
                )
                val newTaskId = creatVmForDebug(
                    userId = userId,
                    projectId = buildHistoryRecord.projectId,
                    pipelineId = pipelineId,
                    buildId = buildHistoryRecord.buildId,
                    vmSeqId = vmSeqId,
                    os = buildHistoryRecord.os,
                    xcode = buildHistoryRecord.xcode,
                    macOSHwSpec = buildHistoryRecord.macosHwSpec,
                    source = buildHistoryRecord.source
                ) ?: return null

                // VM创建成功后立即记录debug记录，确保定时任务能清理新建的VM
                logger.info(
                    "VM created successfully for debug, saving debug history - taskId: $newTaskId, " +
                        "pipelineId: $pipelineId, buildId: ${buildHistoryRecord.buildId}"
                )
                debugHistoryDao.saveDebugHistory(
                    dslContext = dslContext,
                    projectId = buildHistoryRecord.projectId,
                    pipelineId = pipelineId,
                    buildId = buildHistoryRecord.buildId,
                    vmSeqId = vmSeqId,
                    executeCount = executeCount,
                    taskId = newTaskId,
                    newCreatedVm = true,
                    userId = userId
                )

                DebugTaskInfo(
                    taskId = newTaskId,
                    newCreatedVm = true,
                    buildId = buildHistoryRecord.buildId,
                    projectId = buildHistoryRecord.projectId,
                    os = buildHistoryRecord.os,
                    xcode = buildHistoryRecord.xcode,
                    macOSHwSpec = buildHistoryRecord.macosHwSpec,
                    source = buildHistoryRecord.source,
                    isExistingDebug = true
                )
            }
            else -> {
                logger.warn(
                    "Unexpected build history status: $status for pipelineId: $pipelineId, " +
                        "vmSeqId: $vmSeqId, buildId: $buildId"
                )
                null
            }
        }
    }

    /**
     * 为调试场景重新创建虚拟机，等待开机成功并返回新的taskId
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param buildId 构建ID
     * @param vmSeqId 虚拟机序列ID
     * @param os 操作系统版本
     * @param xcode Xcode版本
     * @param macOSHwSpec MacOS硬件规格
     * @param source 来源（landun/gongfeng）
     * @return 新的taskId，创建失败返回null
     */
    private fun creatVmForDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        os: String?,
        xcode: String?,
        macOSHwSpec: String?,
        source: String?
    ): String? {
        val logTag = "[debug]$buildId"
        val createBody = DevCloudMacosVmCreate(
            project = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            source = source ?: "",
            os = os,
            xcode = xcode,
            env = mapOf(
                "IS_DEBUG_LOGIN" to "true",
                DockerConstants.ENV_KEY_DEVCLOUD_MODEL to (macOSHwSpec ?: DEFAULT_HW_TYPE)
            )
        )

        val taskId = sendCreateVmRequest(createBody, userId, logTag)
        if (taskId.isNullOrBlank()) return null

        val vmCreateInfo = waitForVmReady(taskId, userId, logTag)
        return if (vmCreateInfo != null) taskId else null
    }
}