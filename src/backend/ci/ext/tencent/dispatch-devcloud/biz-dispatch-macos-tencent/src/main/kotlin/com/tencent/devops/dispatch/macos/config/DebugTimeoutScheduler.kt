package com.tencent.devops.dispatch.macos.config

import com.tencent.devops.dispatch.macos.dao.DebugHistoryDao
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDebugLoginRequest
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDelete
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 调试超时定时任务
 * 定期扫描debug表中超过8小时仍处于DEBUGGING状态的记录，主动关机并更新状态
 */
@Component
class DebugTimeoutScheduler @Autowired constructor(
    private val dslContext: DSLContext,
    private val debugHistoryDao: DebugHistoryDao,
    private val devCloudMacosService: DevCloudMacosService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DebugTimeoutScheduler::class.java)
        private const val DEBUG_TIMEOUT_HOURS = 8L
    }

    /**
     * 每10分钟执行一次，扫描超时的调试记录并关机
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    fun cleanTimeoutDebugVm() {
        val timeoutRecords = debugHistoryDao.listTimeoutDebuggingRecords(dslContext, DEBUG_TIMEOUT_HOURS)
        if (timeoutRecords.isEmpty()) return

        logger.info("Found ${timeoutRecords.size} timeout debug records exceeding ${DEBUG_TIMEOUT_HOURS}h")

        timeoutRecords.forEach { record ->
            try {
                logger.info(
                    "Processing timeout debug record - id: ${record.id}, taskId: ${record.taskId}, " +
                        "projectId: ${record.projectId}, pipelineId: ${record.pipelineId}, " +
                        "buildId: ${record.buildId}, userId: ${record.userId}, " +
                        "createTime: ${record.createTime}"
                )

                // 关闭调试登录
                val debugCloseRequest = DevCloudMacosVmDebugLoginRequest(taskId = record.taskId)
                val closeResult = devCloudMacosService.debugClose(record.userId, debugCloseRequest)
                if (closeResult == null) {
                    logger.error(
                        "Failed to close debug login for timeout record - id: ${record.id}, " +
                            "taskId: ${record.taskId}"
                    )
                } else {
                    logger.info(
                        "Successfully closed debug login for timeout record - id: ${record.id}, " +
                            "taskId: ${record.taskId}"
                    )
                }

                // 如果是新创建的VM，需要发起关机
                if (record.newCreatedVm) {
                    logger.info(
                        "VM was newly created for debug, deleting VM - id: ${record.id}, " +
                            "taskId: ${record.taskId}"
                    )
                    val deleteResult = devCloudMacosService.deleteVM(
                        creator = record.userId,
                        devCloudMacosVmDelete = DevCloudMacosVmDelete(
                            project = record.projectId,
                            pipelineId = record.pipelineId,
                            buildId = record.buildId,
                            vmSeqId = record.vmSeqId,
                            id = record.taskId
                        )
                    )
                    if (deleteResult) {
                        logger.info(
                            "Successfully deleted timeout debug VM - id: ${record.id}, " +
                                "taskId: ${record.taskId}"
                        )
                    } else {
                        logger.error(
                            "Failed to delete timeout debug VM - id: ${record.id}, " +
                                "taskId: ${record.taskId}"
                        )
                    }
                }

                // 更新debug记录状态为已停止
                val updated = debugHistoryDao.updateStatusToStopped(dslContext, record.id)
                logger.info(
                    "Updated timeout debug record status to STOPPED - id: ${record.id}, " +
                        "taskId: ${record.taskId}, result: $updated"
                )
            } catch (e: Exception) {
                logger.error(
                    "Exception occurred when processing timeout debug record - id: ${record.id}, " +
                        "taskId: ${record.taskId}",
                    e
                )
            }
        }

        logger.info("Finished processing ${timeoutRecords.size} timeout debug records")
    }
}
