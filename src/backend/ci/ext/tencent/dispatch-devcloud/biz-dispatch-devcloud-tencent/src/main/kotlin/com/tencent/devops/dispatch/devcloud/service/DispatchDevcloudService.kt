package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.pojo.devcloud.JobResponse
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.devcloud.dao.DcPerformanceOptionsDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildHisDao
import com.tencent.devops.dispatch.devcloud.dao.DevcloudPerformanceConfigDao
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudBuildHisRecord
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudContainerStatus
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.dispatch.devcloud.pojo.Params
import com.tencent.devops.dispatch.devcloud.pojo.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.devcloud.pojo.TaskStatus
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.DevCloudJobReq
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceConfigVO
import com.tencent.devops.dispatch.devcloud.pojo.performance.PerformanceMap
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import com.tencent.devops.dispatch.devcloud.utils.DevCloudJobRedisUtils
import com.tencent.devops.dispatch.devcloud.utils.RedisUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DispatchDevcloudService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisUtils: RedisUtils,
    private val dispatchDevCloudClient: com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val devCloudBuildHisDao: DevCloudBuildHisDao,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val dcPerformanceOptionsDao: DcPerformanceOptionsDao,
    private val devcloudPerformanceConfigDao: DevcloudPerformanceConfigDao,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val devCloudJobRedisUtils: DevCloudJobRedisUtils
) {
    @Value("\${devCloud.cpu}")
    var cpu: Int = 32

    @Value("\${devCloud.memory}")
    var memory: String = "65535M"

    @Value("\${devCloud.disk}")
    var disk: String = "500G"

    @Value("\${devCloud.sleepEntrypoint}")
    val entrypoint: String = "sleep.sh"

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchDevcloudService::class.java)
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
            devCloudBuildHisDao.get(dslContext, buildId, vmSeqId)[0]
        }

        val containerName: String
        if (buildHistory != null) {
            containerName = buildHistory.containerName
        } else {
            throw ErrorCodeException(
                errorCode = "2103501",
                defaultMessage = "no container is ready to debug",
                params = arrayOf(pipelineId)
            )
        }

        // 检验权限
        if (needCheckPermission) {
            checkPermission(userId, pipelineId, containerName, vmSeqId)
        }

        // 查看当前容器的状态
        val statusResponse = dispatchDevCloudClient.getContainerStatus(
            projectId, pipelineId, buildId ?: "", vmSeqId, userId, containerName)
        val actionCode = statusResponse.optInt("actionCode")
        if (actionCode == 200) {
            val status = statusResponse.optString("data")
            when (status) {
                "stopped", "stop" -> {
                    // 出于关机状态，开机
                    logger.info("Update container status stop to running, containerName: $containerName")
                    startContainer(userId, buildHistory.projectId, pipelineId, buildId, vmSeqId, containerName)
                    devCloudBuildDao.updateDebugStatus(dslContext, pipelineId, vmSeqId, containerName, true)
                }
                "running" -> {
                    devCloudBuildDao.updateDebugStatus(dslContext, pipelineId, vmSeqId, containerName, true)
                }
                "starting" -> {
                    // 容器正在启动中，等待启动成功
                    val status = dispatchDevCloudClient.waitContainerRunning(
                        projectId,
                        pipelineId,
                        buildId ?: "",
                        vmSeqId,
                        userId,
                        containerName
                    )
                    if (status != DevCloudContainerStatus.RUNNING) {
                        logger.error("Status exception, containerName: $containerName, status: $status")
                        throw ErrorCodeException(
                            errorCode = "2103502",
                            defaultMessage = "Status exception, please try rebuild the pipeline",
                            params = arrayOf(pipelineId)
                        )
                    }
                }
                else -> {
                    // 异常状态
                    logger.error("Status exception, containerName: $containerName, status: $status")
                    throw ErrorCodeException(
                        errorCode = "2103502",
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
        vmSeqId: String,
        needCheckPermission: Boolean = true
    ): Boolean {
        val debugContainerName = if (containerName.isBlank()) {
            redisUtils.getDebugContainerName(userId, pipelineId, vmSeqId) ?: ""
        } else {
            containerName
        }

        logger.info("$userId stop debug devcloud pipelineId: $pipelineId " +
                        "containName: $debugContainerName vmSeqId: $vmSeqId")

        // 检验权限
        if (needCheckPermission) {
            checkPermission(userId, pipelineId, debugContainerName, vmSeqId)
        }

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val devcloudBuild =
                devCloudBuildDao.getContainerStatus(transactionContext, pipelineId, vmSeqId, debugContainerName)
            if (devcloudBuild != null) {
                // 先更新debug状态
                devCloudBuildDao.updateDebugStatus(
                    transactionContext,
                    pipelineId,
                    vmSeqId,
                    devcloudBuild.containerName,
                    false
                )
                if (devcloudBuild.status == 0 && devcloudBuild.debugStatus) {
                    // 关闭容器
                    val taskId = dispatchDevCloudClient.operateContainer(
                        projectId = devcloudBuild.projectId,
                        pipelineId = devcloudBuild.pipelineId,
                        buildId = "",
                        vmSeqId = vmSeqId,
                        userId = userId,
                        name = debugContainerName,
                        action = Action.STOP
                    )
                    val opResult = dispatchDevCloudClient.waitTaskFinish(
                        userId,
                        devcloudBuild.projectId,
                        devcloudBuild.pipelineId,
                        taskId
                    )
                    if (opResult.first == TaskStatus.SUCCEEDED) {
                        logger.info("stopDebug stop dev cloud vm success.")
                    } else {
                        // 停不掉，尝试删除
                        logger.info("stopDebug stop dev cloud vm failed, msg: ${opResult.second}")
                        logger.info("stopDebug stop dev cloud vm failed, try to delete it, " +
                                        "containerName:${devcloudBuild.containerName}")
                        dispatchDevCloudClient.operateContainer(
                            projectId = devcloudBuild.projectId,
                            pipelineId = devcloudBuild.pipelineId,
                            buildId = "",
                            vmSeqId = vmSeqId,
                            userId = userId,
                            name = debugContainerName,
                            action = Action.DELETE
                        )
                        devCloudBuildDao.delete(dslContext, pipelineId, vmSeqId, devcloudBuild.poolNo)
                    }
                } else {
                    logger.info("stopDebug pipelineId: $pipelineId, vmSeqId: $vmSeqId " +
                                    "containerName:$debugContainerName 容器没有处于debug或正在占用中")
                }
            } else {
                logger.info("stopDebug pipelineId: $pipelineId, vmSeqId: $vmSeqId " +
                                "containerName:$debugContainerName 容器已不存在")
            }
        }

        return true
    }

    fun getDcPerformanceConfigList(userId: String, projectId: String): UserPerformanceOptionsVO {
        val projectPerformance = devcloudPerformanceConfigDao.getByProjectId(dslContext, projectId)

        val memoryG = (memory.dropLast(1).toInt() / 1024).toString() + "G"
        var default = "0"
        var needShow = false
        val performanceMaps = mutableListOf<PerformanceMap>()
        if (projectPerformance != null) {
            needShow = true
            val cpuInt = projectPerformance["CPU"] as Int
            val memoryInt = projectPerformance["MEMORY"] as Int
            val diskInt = projectPerformance["DISK"] as Int

            val optionList = dcPerformanceOptionsDao.getOptionsList(dslContext, cpuInt, memoryInt, diskInt)
            if (optionList.size == 0) {
                performanceMaps.add(
                    PerformanceMap("0", PerformanceConfigVO(
                    projectId = projectId,
                    cpu = cpu,
                    memory = memoryG,
                    disk = disk,
                    description = "Basic"
                )
                    )
                )
            } else {
                optionList.forEach {
                    if (it.memory == memory.dropLast(1).toInt()) {
                        default = it.id.toString()
                    }
                    performanceMaps.add(
                        PerformanceMap(it.id.toString(), PerformanceConfigVO(
                        projectId = projectId,
                        cpu = it.cpu,
                        memory = "${it.memory / 1024}G",
                        disk = "${it.disk}G",
                        description = it.description
                    )
                        )
                    )
                }

                // 若没有application默认的配置，默认第一个
                if (default == "0") {
                    default = optionList[0].id.toString()
                }
            }
        } else {
            performanceMaps.add(
                PerformanceMap("0", PerformanceConfigVO(
                projectId = projectId,
                cpu = cpu,
                memory = memoryG,
                disk = disk,
                description = "Basic"
            )
                )
            )
        }

        return UserPerformanceOptionsVO(default, needShow, performanceMaps)
    }

    fun createJob(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        jobReq: DevCloudJobReq
    ): JobResponse {
        logger.info("【**】projectId: $projectId, buildId: $buildId create devCloud jobContainer. userId: $userId")
        // 检查job数量是否超出限制

        val containerName = jobReq.podNameSelector!!.split("-").first()

        if (devCloudJobRedisUtils.getJobCount(buildId, containerName) > 10) {
            throw ErrorCodeException(
                statusCode = 500,
                errorCode = ErrorCodeEnum.CREATE_JOB_LIMIT_ERROR.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.CREATE_JOB_LIMIT_ERROR.formatErrorMessage
            )
        }
        devCloudJobRedisUtils.setJobCount(buildId, containerName)

        return dispatchDevCloudClient.createJob(userId, projectId, pipelineId, buildId, jobReq)
    }

    fun getJobStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        jobName: String
    ): String {
        return dispatchDevCloudClient.getJobStatus(userId, projectId, pipelineId, jobName)
    }

    fun getJobLogs(
        projectId: String,
        pipelineId: String,
        userId: String,
        jobName: String
    ): String {
        return dispatchDevCloudClient.getJobLogs(userId, projectId, pipelineId, jobName)
    }

    fun getTask(
        userId: String,
        projectId: String,
        pipelineId: String,
        taskId: String
    ): String {
        return dispatchDevCloudClient.getTasks(projectId, pipelineId, userId, taskId).toString()
    }

    private fun checkPermission(userId: String, pipelineId: String, containerName: String, vmSeqId: String) {
        val containerInfo = devCloudBuildDao.getContainerStatus(dslContext, pipelineId, vmSeqId, containerName)
        val projectId = containerInfo!!.projectId
        // 检验权限
        if (!bkAuthPermissionApi.validateUserResourcePermission(
                userId, pipelineAuthServiceCode, AuthResourceType.PIPELINE_DEFAULT,
                projectId, pipelineId, AuthPermission.EDIT
            )
        ) {
            logger.info("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下编辑流水线($pipelineId)")
        }
    }

    private fun startContainer(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String,
        containerName: String
    ) {
        val devCloudTaskId = dispatchDevCloudClient.operateContainer(
            projectId,
            pipelineId,
            buildId ?: "",
            vmSeqId,
            userId,
            containerName,
            Action.START,
            Params(
                mapOf(
                    "projectId" to projectId,
                    "pipelineId" to pipelineId,
                    "TERM" to "xterm-256color",
                    SLAVE_ENVIRONMENT to "DevCloud"
                ),
                listOf("/bin/sh", entrypoint),
                mapOf(
                    "projectId" to projectId,
                    "pipelineId" to pipelineId,
                    "buildId" to (buildId ?: ""),
                    "vmSeqId" to vmSeqId
                )
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
                    errorCode = "2103503",
                    defaultMessage = "构建机启动失败，错误信息:$actionMessage"
                )
            }
            // 启动成功
            logger.info("start dev cloud vm success")
        } else {
            logger.error("create dev cloud vm failed, msg: ${startResult.second}")
            throw ErrorCodeException(
                errorCode = "2103503",
                defaultMessage = "构建机启动失败，错误信息:$startResult.second"
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
                errorCode = "2103504",
                defaultMessage = "获取websocket url失败，错误信息:$actionMessage"
            )
        }

        return result.getString("data")
    }
}
