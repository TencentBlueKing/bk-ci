package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.SubscriptionType
import com.tencent.devops.process.pojo.pipeline.PipelineSubscription
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.service.DockerBuildService
import com.tencent.devops.process.service.PipelineSubscriptionService
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
import com.tencent.devops.process.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class TXUserPipelineResourceImpl @Autowired constructor(
    private val pipelineSubscriptionService: PipelineSubscriptionService,
    private val dockerBuildService: DockerBuildService
) : TXUserPipelineResource {

    override fun enableDockerBuild(userId: String, projectId: String): Result<Boolean> {
        checkParam(userId, projectId)
        return Result(dockerBuildService.isEnable(userId, projectId))
    }

    override fun subscription(userId: String, projectId: String, pipelineId: String, type: SubscriptionType?): Result<Boolean> {
        checkParam(userId, projectId)
        checkPipelineId(pipelineId)
        return Result(pipelineSubscriptionService.subscription(userId, pipelineId, type))
    }

    override fun getSubscription(userId: String, projectId: String, pipelineId: String): Result<PipelineSubscription?> {
        checkParam(userId, projectId)
        checkPipelineId(pipelineId)
        return Result(pipelineSubscriptionService.getSubscriptions(userId, pipelineId))
    }

    override fun cancelSubscription(userId: String, projectId: String, pipelineId: String): Result<Boolean> {
        checkParam(userId, projectId)
        checkPipelineId(pipelineId)
        return Result(pipelineSubscriptionService.deleteSubscriptions(userId, pipelineId))
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun checkPipelineId(pipelineId: String) {
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
    }

    private fun checkParam(setting: PipelineSetting) {
        if (setting.runLockType == PipelineRunLockType.SINGLE || setting.runLockType == PipelineRunLockType.SINGLE_LOCK) {
            if (setting.waitQueueTimeMinute < PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MIN ||
                    setting.waitQueueTimeMinute > PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_MAX
            ) {
                throw InvalidParamException("最大排队时长非法")
            }
            if (setting.maxQueueSize < PIPELINE_SETTING_MAX_QUEUE_SIZE_MIN ||
                    setting.maxQueueSize > PIPELINE_SETTING_MAX_QUEUE_SIZE_MAX
            ) {
                throw InvalidParamException("最大排队数量非法")
            }
        }
    }
}
