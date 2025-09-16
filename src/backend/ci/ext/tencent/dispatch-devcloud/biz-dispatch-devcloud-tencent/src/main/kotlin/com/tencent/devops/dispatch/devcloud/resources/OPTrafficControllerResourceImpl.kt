package com.tencent.devops.dispatch.devcloud.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.op.OPTrafficControlResource
import com.tencent.devops.dispatch.devcloud.service.TrafficControlService

@RestResource
class OPTrafficControllerResourceImpl constructor(
    private val trafficControlService: TrafficControlService
) : OPTrafficControlResource {
    override fun getTrafficStats(userId: String): Result<Map<String, Any>> {
        return Result(trafficControlService.getTrafficStats())
    }

    override fun getGrayRatio(userId: String): Result<Int> {
        return Result(trafficControlService.getGrayRatio())
    }

    override fun setGrayRatio(userId: String, ratio: Int): Result<Boolean> {
        return Result(trafficControlService.setGrayRatio(ratio))
    }

    override fun addToWhitelist(
        operatorUserId: String,
        projectId: String,
        pipelineId: String?
    ): Result<Boolean> {
        return Result(trafficControlService.addToWhitelist(operatorUserId, projectId, pipelineId))
    }

    override fun removeFromWhitelist(
        operatorUserId: String,
        projectId: String,
        pipelineId: String?
    ): Result<Boolean> {
        return Result(trafficControlService.removeFromWhitelist(operatorUserId, projectId, pipelineId))
    }

    override fun addToBlacklist(
        operatorUserId: String,
        projectId: String,
        pipelineId: String?
    ): Result<Boolean> {
        return Result(trafficControlService.addToBlacklist(operatorUserId, projectId, pipelineId))
    }

    override fun removeFromBlacklist(
        operatorUserId: String,
        projectId: String,
        pipelineId: String?
    ): Result<Boolean> {
        return Result(trafficControlService.removeFromBlacklist(operatorUserId, projectId, pipelineId))
    }
}
