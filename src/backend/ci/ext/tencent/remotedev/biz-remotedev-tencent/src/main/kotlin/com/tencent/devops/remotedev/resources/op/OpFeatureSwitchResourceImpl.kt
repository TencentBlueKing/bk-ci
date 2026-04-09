package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpFeatureSwitchResource
import com.tencent.devops.remotedev.pojo.FeatureSwitch
import com.tencent.devops.remotedev.pojo.FeatureSwitchType
import com.tencent.devops.remotedev.service.FeatureSwitchService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpFeatureSwitchResourceImpl @Autowired constructor(
    private val featureSwitchService: FeatureSwitchService
) : OpFeatureSwitchResource {

    override fun create(
        userId: String,
        featureSwitch: FeatureSwitch
    ): Result<Long> {
        return Result(
            featureSwitchService.create(
                operator = userId,
                featureSwitch = featureSwitch
            )
        )
    }

    override fun update(
        userId: String,
        id: Long,
        enabled: Boolean
    ): Result<Boolean> {
        return Result(
            featureSwitchService.update(
                operator = userId,
                id = id,
                enabled = enabled
            )
        )
    }

    override fun delete(userId: String, id: Long): Result<Boolean> {
        return Result(
            featureSwitchService.delete(
                operator = userId,
                id = id
            )
        )
    }

    override fun get(userId: String, id: Long): Result<FeatureSwitch?> {
        return Result(featureSwitchService.getById(id = id))
    }

    override fun list(
        userId: String,
        projectId: String?,
        targetUserId: String?,
        workspaceName: String?,
        featureType: FeatureSwitchType?
    ): Result<List<FeatureSwitch>> {
        return Result(
            featureSwitchService.list(
                projectId = projectId,
                userId = targetUserId,
                workspaceName = workspaceName,
                featureType = featureType
            )
        )
    }

    override fun check(
        userId: String,
        projectId: String,
        targetUserId: String,
        workspaceName: String,
        featureType: FeatureSwitchType
    ): Result<Boolean> {
        return Result(
            featureSwitchService.isEnabled(
                projectId = projectId,
                userId = targetUserId,
                workspaceName = workspaceName,
                featureType = featureType
            )
        )
    }
}
