package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.UserDispatchEnvStrategyResource
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyCreateReq
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyReorderReq
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyUpdateReq
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyVO
import com.tencent.devops.dispatch.pojo.LabelSelectorVO
import com.tencent.devops.dispatch.pojo.LabelSelector
import com.tencent.devops.dispatch.service.EnvDispatchStrategyService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserDispatchEnvStrategyResourceImpl @Autowired constructor(
    private val envDispatchStrategyService: EnvDispatchStrategyService
) : UserDispatchEnvStrategyResource {

    override fun listStrategies(
        userId: String,
        projectId: String,
        envId: Long
    ): Result<List<DispatchEnvStrategyVO>> {
        val strategies = envDispatchStrategyService.getOrInitStrategies(projectId, envId, userId)
        return Result(strategies.map { it.toVO() })
    }

    override fun createStrategy(
        userId: String,
        projectId: String,
        envId: Long,
        request: DispatchEnvStrategyCreateReq
    ): Result<Long> {
        val id = envDispatchStrategyService.createCustomStrategy(
            projectId = projectId,
            envId = envId,
            userId = userId,
            strategyName = request.strategyName,
            scope = request.scope,
            nodeRule = request.nodeRule,
            labelSelector = request.labelSelector?.map { it.toInternal() }
        )
        return Result(id)
    }

    override fun updateStrategy(
        userId: String,
        projectId: String,
        envId: Long,
        strategyId: Long,
        request: DispatchEnvStrategyUpdateReq
    ): Result<Boolean> {
        envDispatchStrategyService.updateStrategy(
            id = strategyId,
            userId = userId,
            strategyName = request.strategyName,
            scope = request.scope,
            nodeRule = request.nodeRule,
            labelSelector = request.labelSelector?.map { it.toInternal() },
            enabled = request.enabled
        )
        return Result(true)
    }

    override fun deleteStrategy(
        userId: String,
        projectId: String,
        envId: Long,
        strategyId: Long
    ): Result<Boolean> {
        envDispatchStrategyService.deleteStrategy(strategyId)
        return Result(true)
    }

    override fun batchDeleteStrategy(
        userId: String,
        projectId: String,
        envId: Long,
        strategyIds: Set<Long>
    ): Result<Boolean> {
        envDispatchStrategyService.batchDeleteStrategy(strategyIds)
        return Result(true)
    }

    override fun reorderStrategies(
        userId: String,
        projectId: String,
        envId: Long,
        request: DispatchEnvStrategyReorderReq
    ): Result<Boolean> {
        envDispatchStrategyService.reorderStrategies(projectId, envId, request.orderedIds)
        return Result(true)
    }

    companion object {
        private fun com.tencent.devops.dispatch.pojo.DispatchStrategyConfig.toVO() = DispatchEnvStrategyVO(
            id = id ?: 0L,
            projectId = projectId,
            envId = envId,
            strategyType = strategyType,
            defaultStrategyCode = defaultStrategyCode,
            strategyName = strategyName,
            scope = scope,
            nodeRule = nodeRule,
            labelSelector = labelSelector?.map { LabelSelectorVO(it.tagKeyId, it.op, it.tagValueIds) },
            enabled = enabled,
            priority = priority,
            createdUser = createdUser,
            updatedUser = updatedUser
        )

        private fun LabelSelectorVO.toInternal() = LabelSelector(
            tagKeyId = tagKeyId,
            op = op,
            tagValueIds = tagValueIds
        )
    }
}
