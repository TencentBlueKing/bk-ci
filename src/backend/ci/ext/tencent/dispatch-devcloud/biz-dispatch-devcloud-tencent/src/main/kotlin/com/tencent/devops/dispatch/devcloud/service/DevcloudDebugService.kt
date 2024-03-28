package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.api.constant.CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.constant.DispatchDevcloudMessageCode.BK_BUILD_MACHINE_FAILS_START
import com.tencent.devops.dispatch.devcloud.constant.DispatchDevcloudMessageCode.BK_CONTAINER_STATUS_EXCEPTION
import com.tencent.devops.dispatch.devcloud.constant.DispatchDevcloudMessageCode.BK_GET_WEBSOCKET_URL_FAIL
import com.tencent.devops.dispatch.devcloud.constant.DispatchDevcloudMessageCode.BK_NO_CONTAINER_IS_READY_DEBUG
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceContainerDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildHisDao
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudContainerStatus
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.dispatch.devcloud.pojo.Params
import com.tencent.devops.dispatch.devcloud.pojo.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.devcloud.pojo.TaskStatus
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceContainerStatus
import com.tencent.devops.dispatch.devcloud.utils.RedisUtils
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudBuildHisRecord
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DevcloudDebugService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val redisUtils: RedisUtils,
    private val dispatchDevCloudClient: DispatchDevCloudClient,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val devCloudBuildHisDao: DevCloudBuildHisDao,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val dcPersistenceContainerDao: DcPersistenceContainerDao,
    private val dcContainerShutdownHandler: DcContainerShutdownHandler
) {
    @Value("\${devCloud.cpu}")
    var cpu: Int = 32

    @Value("\${devCloud.memory}")
    var memory: String = "65535M"

    @Value("\${devCloud.sleepEntrypoint}")
    val entrypoint: String = "sleep.sh"

    @Value("\${atom.fuse.container.label}")
    val fuseContainerLabel: String? = null

    @Value("\${atom.fuse.atom-code}")
    val fuseAtomCode: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(DevcloudDebugService::class.java)
    }

    fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String,
        needCheckPermission: Boolean = true
    ): DevCloudDebugResponse {
        logger.info("$userId start debug devcloud pipelineId: $pipelineId buildId: $buildId vmSeqId: $vmSeqId")
        // 根据是否传入buildId 查找containerName
        val buildHistory: TDevcloudBuildHisRecord? = if (buildId == null) {
            // 查找当前pipeline下的最近一次构建
            devCloudBuildHisDao.getLatestBuildHistory(dslContext, pipelineId, vmSeqId)
        } else {
            // 精确查找
            devCloudBuildHisDao.get(dslContext, buildId, vmSeqId)
        }

        val containerName: String
        if (buildHistory != null) {
            containerName = buildHistory.containerName
        } else {
            throw ErrorCodeException(
                errorCode = BK_NO_CONTAINER_IS_READY_DEBUG,
                defaultMessage = "no container is ready to debug",
                params = arrayOf(pipelineId)
            )
        }

        // 检验权限
        if (needCheckPermission) {
            checkPermission(buildHistory.projectId, userId, pipelineId)
        }

        // 查看当前容器的状态
        val statusResponse = dispatchDevCloudClient.getContainerStatus(
            projectId, pipelineId, buildId ?: "", vmSeqId, userId, containerName)
        val actionCode = statusResponse.optInt("actionCode")
        if (actionCode == 200) {
            when (val status = statusResponse.optString("data")) {
                "stopped", "stop" -> {
                    // 处于关机状态，开机
                    logger.info("Update container status stop to running, containerName: $containerName")
                    startContainer(userId, buildHistory.projectId, pipelineId, buildId, vmSeqId, containerName)
                    devCloudBuildDao.updateDebugStatus(dslContext, pipelineId, vmSeqId, containerName, true)
                }
                "running" -> {
                    devCloudBuildDao.updateDebugStatus(dslContext, pipelineId, vmSeqId, containerName, true)
                }
                "starting" -> {
                    // 容器正在启动中，等待启动成功
                    val devCloudContainerStatus = dispatchDevCloudClient.waitContainerRunning(
                        projectId,
                        pipelineId,
                        buildId ?: "",
                        vmSeqId,
                        userId,
                        containerName
                    )
                    if (devCloudContainerStatus != DevCloudContainerStatus.RUNNING) {
                        logger.error("Status exception, containerName: $containerName, " +
                                "status: $devCloudContainerStatus")
                        throw ErrorCodeException(
                            errorCode = BK_CONTAINER_STATUS_EXCEPTION,
                            defaultMessage = "Status exception, please try rebuild the pipeline",
                            params = arrayOf(pipelineId)
                        )
                    }
                }
                else -> {
                    // 异常状态
                    logger.error("Status exception, containerName: $containerName, status: $status")
                    throw ErrorCodeException(
                        errorCode = BK_CONTAINER_STATUS_EXCEPTION,
                        defaultMessage = "Status exception, please try rebuild the pipeline",
                        params = arrayOf(pipelineId)
                    )
                }
            }
        }

        // 设置containerName缓存
        redisUtils.setDebugContainerName(userId, pipelineId, vmSeqId, containerName)

        return DevCloudDebugResponse(getWebsocketUrl(projectId, pipelineId, userId, containerName), containerName)
    }

    fun stopDebug(
        userId: String,
        pipelineId: String,
        containerName: String,
        vmSeqId: String
    ): Boolean {
        val debugContainerName = containerName.ifBlank {
            redisUtils.getDebugContainerName(userId, pipelineId, vmSeqId) ?: ""
        }

        logger.info("$userId|$pipelineId|$vmSeqId stop debug container:$debugContainerName")
        val devcloudBuild = devCloudBuildDao.getContainerStatus(dslContext, pipelineId, vmSeqId, debugContainerName)
        if (devcloudBuild != null) {
            // 先更新debug状态
            devCloudBuildDao.updateDebugStatus(
                dslContext = dslContext,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                containerName = devcloudBuild.containerName,
                debugStatus = false
            )

            // 当前JOB是否为持久化构建,持久化容器登录调试结束后不关闭容器
            if (devcloudBuild.status == 0 &&
                devcloudBuild.debugStatus &&
                !persistenceContainer(pipelineId, vmSeqId)
            ) {
                dcContainerShutdownHandler.forceStopContainer(devcloudBuild)
            } else {
                logger.info(
                    "stopDebug pipelineId: $pipelineId, vmSeqId: $vmSeqId " +
                            "containerName:$debugContainerName container is idle or not in debug."
                )
            }
        } else {
            logger.info(
                "stopDebug pipelineId: $pipelineId, vmSeqId: $vmSeqId " +
                        "containerName:$debugContainerName container no longer exists."
            )
        }

        redisUtils.deleteDebugContainerName(userId, pipelineId, vmSeqId)

        return true
    }

    private fun checkPermission(
        projectId: String,
        userId: String,
        pipelineId: String
    ) {
        // 检验权限
        if (!bkAuthPermissionApi.validateUserResourcePermission(
                userId, pipelineAuthServiceCode, AuthResourceType.PIPELINE_DEFAULT,
                projectId, pipelineId, AuthPermission.EDIT
            )
        ) {
            logger.info("user($userId)You do not have permission to edit " +
                    "pipelines($pipelineId) under the project($projectId)")
            throw PermissionForbiddenException(
                I18nUtil.getCodeLanMessage(
                    messageCode = USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    params = arrayOf(
                        userId,
                        projectId,
                        AuthPermission.EDIT.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
            )
        }
    }

    private fun persistenceContainer(
        pipelineId: String,
        vmSeqId: String
    ): Boolean {
        val persistenceContainers = dcPersistenceContainerDao.getPersistenceContainer(dslContext, pipelineId, vmSeqId)

        persistenceContainers.forEach {
            if (it.containerStatus == PersistenceContainerStatus.RUNNING.status) {
                return true
            }
        }

        return false
    }

    private fun startContainer(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String,
        containerName: String
    ) {
        val mapData = client.get(ServicePipelineTaskResource::class).list(projectId, listOf(pipelineId)).data
        val atomCodeList = (mapData?.get(pipelineId) ?: emptyList()).map { it.atomCode }
        val containerLabels = mutableMapOf(
            "projectId" to projectId,
            "pipelineId" to pipelineId,
            "buildId" to (buildId ?: ""),
            "vmSeqId" to vmSeqId
        )

        // 针对fuse插件优化
        fuseAtomCode?.split(",")?.forEach {
            if (it in atomCodeList) {
                val (key, value) = fuseContainerLabel!!.split(":")
                containerLabels[key] = value
                return@forEach
            }
        }

        val devCloudTaskId = dispatchDevCloudClient.operateContainer(
            projectId,
            pipelineId,
            buildId ?: "",
            vmSeqId,
            userId,
            containerName,
            Action.START,
            Params(
                env = mapOf(
                    "projectId" to projectId,
                    "pipelineId" to pipelineId,
                    "TERM" to "xterm-256color",
                    SLAVE_ENVIRONMENT to "DevCloud"
                ),
                command = listOf("/bin/sh", entrypoint),
                labels = containerLabels
            )
        )

        logger.info("start container, taskId:($devCloudTaskId)")
        val startResult = dispatchDevCloudClient.waitTaskFinish(
            userId,
            projectId,
            pipelineId,
            devCloudTaskId
        )
        if (startResult.first == TaskStatus.SUCCEEDED) {
            // 得到本次任务的实例的信息
            val instContainerName = startResult.second
            val containerInstanceInfo = dispatchDevCloudClient.getContainerInstance(
                projectId, pipelineId, userId, instContainerName
            )
            val actionCode = containerInstanceInfo.optInt("actionCode")
            if (actionCode != 200) {
                val actionMessage = containerInstanceInfo.optString("actionMessage")
                logger.error("Get container instance failed, msg: $actionMessage")
                throw ErrorCodeException(
                    errorCode = BK_BUILD_MACHINE_FAILS_START,
                    params = arrayOf(actionMessage)
                )
            }
            // 启动成功
            logger.info("start dev cloud vm success")
        } else {
            logger.error("create dev cloud vm failed, msg: ${startResult.second}")
            throw ErrorCodeException(
                errorCode = BK_BUILD_MACHINE_FAILS_START,
                params = arrayOf(startResult.second)
            )
        }
    }

    private fun getWebsocketUrl(
        projectId: String,
        pipelineId: String,
        userId: String,
        containerName: String
    ): String {
        val result = dispatchDevCloudClient.getWebsocket(projectId, pipelineId, userId, containerName)
        val actionCode = result.optInt("actionCode")
        if (actionCode != 200) {
            val actionMessage = result.optString("actionMessage")
            logger.error("Get websocket url failed, msg: $actionMessage")
            throw ErrorCodeException(
                errorCode = BK_GET_WEBSOCKET_URL_FAIL,
                params = arrayOf(actionMessage)
            )
        }

        return result.getString("data")
    }
}
