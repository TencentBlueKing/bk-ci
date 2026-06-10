package com.tencent.devops.dispatch.macos.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.UserMacosDebugResource
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDebugLoginResponse
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * User接口 - macOS调试登录资源实现
 */
@RestResource
class UserMacosDebugResourceImpl @Autowired constructor(
    private val devCloudMacosService: DevCloudMacosService
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
            val debugLoginResponse = devCloudMacosService.startDebug(
                userId = userId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                buildId = buildId,
                executeCount = executeCount
            )

            if (debugLoginResponse == null) {
                Result(status = 404, message = "此 Job 尚未启动过构建，不支持登录调试")
            } else {
                Result(data = debugLoginResponse)
            }
        } catch (e: Exception) {
            logger.error(
                "Start macOS debug login failed - userId: $userId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: $buildId, executeCount: $executeCount",
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
        logger.info(
            "Stop macOS debug login - userId: $userId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                "buildId: $buildId, executeCount: $executeCount"
        )

        return try {
            val success = devCloudMacosService.stopDebug(
                userId = userId,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                buildId = buildId,
                executeCount = executeCount
            )

            if (success) {
                Result(data = true)
            } else {
                Result(status = 404, message = "停止登录调试异常")
            }
        } catch (e: Exception) {
            logger.error(
                "Stop macOS debug login failed - userId: $userId, pipelineId: $pipelineId, vmSeqId: $vmSeqId, " +
                    "buildId: $buildId, executeCount: $executeCount",
                e
            )
            Result(status = 500, message = "停止调试异常: ${e.message}")
        }
    }
}