package com.tencent.devops.dispatch.bcs.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.dispatch.bcs.client.BcsBuilderClient
import com.tencent.devops.dispatch.bcs.client.BcsTaskClient
import com.tencent.devops.dispatch.bcs.common.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.bcs.common.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.bcs.dao.BcsBuildDao
import com.tencent.devops.dispatch.bcs.dao.BcsBuildHisDao
import com.tencent.devops.dispatch.bcs.pojo.BcsDebugResponse
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsBuilderStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsDeleteBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsStartBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsStopBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsTaskStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.bcs.canReStart
import com.tencent.devops.dispatch.bcs.pojo.bcs.isRunning
import com.tencent.devops.dispatch.bcs.pojo.bcs.isStarting
import com.tencent.devops.dispatch.bcs.utils.RedisUtils
import com.tencent.devops.model.dispatch_bcs.tables.records.TBcsBuildHisRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BcsDebugService @Autowired constructor(
    private val dslContext: DSLContext,
    private val bcsBuilderClient: BcsBuilderClient,
    private val bcsTaskClient: BcsTaskClient,
    private val bcsBuildDao: BcsBuildDao,
    private val bcsBuildHisDao: BcsBuildHisDao,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val redisUtils: RedisUtils
) {
    @Value("\${bcs.sleepEntrypoint}")
    val entrypoint: String = "bcs_sleep.sh"

    companion object {
        private val logger = LoggerFactory.getLogger(BcsDebugService::class.java)
    }

    fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String,
        needCheckPermission: Boolean = true
    ): BcsDebugResponse {
        logger.info("$userId start debug bcs pipelineId: $pipelineId buildId: $buildId vmSeqId: $vmSeqId")
        // 根据是否传入buildId 查找containerName
        val buildHistory: TBcsBuildHisRecord? = if (buildId == null) {
            // 查找当前pipeline下的最近一次构建
            bcsBuildHisDao.getLatestBuildHistory(dslContext, pipelineId, vmSeqId)
        } else {
            // 精确查找
            bcsBuildHisDao.get(dslContext, buildId, vmSeqId)[0]
        }

        val containerName: String
        if (buildHistory != null) {
            containerName = buildHistory.builderName
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
        val statusResponse = bcsBuilderClient.getBuilderDetail(
            buildId ?: "", vmSeqId, userId, containerName)
        if (statusResponse.isOk()) {
            val status = statusResponse.data!!

            if (status.canReStart()) {
                // 出于关机状态，开机
                logger.info("Update container status stop to running, containerName: $containerName")
                startContainer(userId, buildHistory.projectId, pipelineId, buildId, vmSeqId, containerName)
                bcsBuildDao.updateDebugStatus(dslContext, pipelineId, vmSeqId, containerName, true)
            } else if (status.isRunning()) {
                bcsBuildDao.updateDebugStatus(dslContext, pipelineId, vmSeqId, containerName, true)
            } else if (status.isStarting()) {
                // 容器正在启动中，等待启动成功
                val status = bcsBuilderClient.waitContainerRunning(
                    projectId,
                    pipelineId,
                    buildId ?: "",
                    vmSeqId,
                    userId,
                    containerName
                )
                if (status != BcsBuilderStatusEnum.RUNNING) {
                    logger.error("Status exception, containerName: $containerName, status: $status")
                    throw ErrorCodeException(
                        errorCode = "2103502",
                        defaultMessage = "Status exception, please try rebuild the pipeline",
                        params = arrayOf(pipelineId)
                    )
                }
            } else {
                // 异常状态
                logger.error("Status exception, containerName: $containerName, status: $status")
                throw ErrorCodeException(
                    errorCode = "2103502",
                    defaultMessage = "Status exception, please try rebuild the pipeline",
                    params = arrayOf(pipelineId)
                )
            }
        }

        // 设置containerName缓存
        redisUtils.setDebugContainerName(userId, pipelineId, vmSeqId, containerName)

        return BcsDebugResponse(bcsBuilderClient.getWebsocketUrl(projectId, pipelineId, userId, containerName).data!!, containerName)
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

        logger.info("$userId stop debug bcs pipelineId: $pipelineId containName: $debugContainerName vmSeqId: $vmSeqId")

        // 检验权限
        if (needCheckPermission) {
            checkPermission(userId, pipelineId, debugContainerName, vmSeqId)
        }

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            val devcloudBuild =
                bcsBuildDao.getBuilderStatus(transactionContext, pipelineId, vmSeqId, debugContainerName)
            if (devcloudBuild != null) {
                // 先更新debug状态
                bcsBuildDao.updateDebugStatus(
                    transactionContext,
                    pipelineId,
                    vmSeqId,
                    devcloudBuild.builderName,
                    false
                )
                if (devcloudBuild.status == 0 && devcloudBuild.debugStatus) {
                    // 关闭容器
                    val taskId = bcsBuilderClient.operateBuilder(
                        buildId = "",
                        vmSeqId = vmSeqId,
                        userId = userId,
                        name = debugContainerName,
                        param = BcsStopBuilderParams()
                    )
                    val opResult = bcsTaskClient.waitTaskFinish(
                        userId,
                        taskId
                    )
                    if (opResult.first == BcsTaskStatusEnum.SUCCEEDED) {
                        logger.info("stopDebug stop dev cloud vm success.")
                    } else {
                        // 停不掉，尝试删除
                        logger.info("stopDebug stop dev cloud vm failed, msg: ${opResult.second}")
                        logger.info("stopDebug stop dev cloud vm failed, try to delete it, " +
                                "containerName:${devcloudBuild.builderName}")
                        bcsBuilderClient.operateBuilder(
                            buildId = "",
                            vmSeqId = vmSeqId,
                            userId = userId,
                            name = debugContainerName,
                            param = BcsDeleteBuilderParams()
                        )
                        bcsBuildDao.delete(dslContext, pipelineId, vmSeqId, devcloudBuild.poolNo)
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

    private fun checkPermission(userId: String, pipelineId: String, containerName: String, vmSeqId: String) {
        val containerInfo = bcsBuildDao.getBuilderStatus(dslContext, pipelineId, vmSeqId, containerName)
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
        val devCloudTaskId = bcsBuilderClient.operateBuilder(
            buildId = buildId ?: "",
            vmSeqId = vmSeqId,
            userId = userId,
            name = containerName,
            param = BcsStartBuilderParams(
                env = mapOf(
                    ENV_KEY_PROJECT_ID to projectId,
                    "TERM" to "xterm-256color",
                    SLAVE_ENVIRONMENT to "Bcs",
                ),
                command = listOf("/bin/sh", entrypoint)
            )
        )

        logger.info("$userId start container, taskId:($devCloudTaskId)")
        val startResult = bcsTaskClient.waitTaskFinish(
            userId,
            devCloudTaskId
        )
        if (startResult.first == BcsTaskStatusEnum.SUCCEEDED) {
            // 启动成功
            logger.info("$userId start bcs builder success")
        } else {
            logger.error("$userId start bcs builder failed, msg: ${startResult.second}")
            throw ErrorCodeException(
                errorCode = "2103503",
                defaultMessage = "构建机启动失败，错误信息:$startResult.second"
            )
        }
    }
}
