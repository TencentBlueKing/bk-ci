package com.tencent.devops.dispatch.macos.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.UserMacosDebugResource
import com.tencent.devops.dispatch.macos.dao.BuildHistoryDao
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDebugLoginRequest
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDebugLoginResponse
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * User接口 - macOS调试登录资源实现
 */
@RestResource
class UserMacosDebugResourceImpl @Autowired constructor(
    private val devCloudMacosService: DevCloudMacosService,
    private val buildHistoryDao: BuildHistoryDao,
    private val dslContext: DSLContext
) : UserMacosDebugResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserMacosDebugResourceImpl::class.java)
    }

    override fun startDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?,
        executeCount: Int
    ): Result<DevCloudMacosVmDebugLoginResponse> {
        logger.info(
            "Start macOS debug login - userId: $userId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: $buildId, executeCount: $executeCount"
        )

        return try {
            val taskId = getTaskIdFromBuildHistory(pipelineId, vmSeqId, buildId, executeCount) ?: return Result(
                status = 404,
                message = "构建历史记录不存在或任务ID不存在"
            )

            logger.info("Found taskId: $taskId for pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId")

            val debugLoginRequest = DevCloudMacosVmDebugLoginRequest(taskId = taskId)
            val debugLoginResponse = devCloudMacosService.debugLogin(userId, debugLoginRequest)
                ?: return Result(status = 500, message = "调试登录失败")

            logger.info("Debug login successful for taskId: $taskId")
            Result(data = debugLoginResponse)
        } catch (e: Exception) {
            logger.error(
                "Start macOS debug login failed - userId: $userId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                        "buildId: $buildId",
                e
            )
            Result(status = 500, message = "调试登录异常: ${e.message}")
        }
    }

    override fun stopDebug(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        buildId: String?,
        executeCount: Int
    ): Result<Boolean> {
        logger.info("Stop macOS debug login - userId: $userId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                "buildId: $buildId, executeCount: $executeCount")

        return try {
            val taskId = getTaskIdFromBuildHistory(pipelineId, vmSeqId, buildId, executeCount) ?: return Result(
                status = 404,
                message = "构建历史记录不存在或任务ID不存在"
            )

            logger.info("Found taskId: $taskId for pipelineId: $pipelineId, vmSeqId: $vmSeqId, buildId: $buildId")

            val debugCloseRequest = DevCloudMacosVmDebugLoginRequest(taskId = taskId)
            devCloudMacosService.debugClose(userId, debugCloseRequest)
                ?: return Result(status = 500, message = "关闭调试登录失败")

            logger.info("Debug close successful for taskId: $taskId")
            Result(data = true)
        } catch (e: Exception) {
            logger.error(
                "Stop macOS debug login failed - userId: $userId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                        "buildId: $buildId",
                e
            )
            Result(status = 500, message = "停止调试异常: ${e.message}")
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
            logger.warn("Build history record not found for pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: $buildId, executeCount: $executeCount")
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