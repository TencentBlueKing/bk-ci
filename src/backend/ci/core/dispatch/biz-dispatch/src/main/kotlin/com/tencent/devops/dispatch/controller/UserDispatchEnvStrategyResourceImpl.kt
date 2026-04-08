package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.UserDispatchEnvStrategyResource
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyCreateReq
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyReorderReq
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyUpdateReq
import com.tencent.devops.dispatch.pojo.DispatchEnvStrategyVO
import com.tencent.devops.dispatch.pojo.LabelSelector
import com.tencent.devops.dispatch.pojo.LabelSelectorVO
import com.tencent.devops.dispatch.service.EnvDispatchStrategyService
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserDispatchEnvStrategyResourceImpl @Autowired constructor(
    private val envDispatchStrategyService: EnvDispatchStrategyService,
    private val client: Client
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
        checkEnvEditPermission(userId, projectId, envId)
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
        checkEnvEditPermission(userId, projectId, envId)
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
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.deleteStrategy(strategyId)
        return Result(true)
    }

    override fun batchDeleteStrategy(
        userId: String,
        projectId: String,
        envId: Long,
        strategyIds: Set<Long>
    ): Result<Boolean> {
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.batchDeleteStrategy(strategyIds)
        return Result(true)
    }

    override fun reorderStrategies(
        userId: String,
        projectId: String,
        envId: Long,
        request: DispatchEnvStrategyReorderReq
    ): Result<Boolean> {
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.reorderStrategies(projectId, envId, request.orderedIds)
        return Result(true)
    }

    private fun checkEnvEditPermission(userId: String, projectId: String, envId: Long) {
        val hasPermission = try {
            client.get(ServiceThirdPartyAgentResource::class)
                .checkEnvEditPermission(userId, projectId, envId)
                .data ?: false
        } catch (e: Exception) {
            false
        }
        if (!hasPermission) {
            throw PermissionForbiddenException(
                message = "User($userId) has no edit permission on env($envId) in project($projectId)"
            )
        }
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
            labelSelector = labelSelector?.map {
                LabelSelectorVO(it.tagKeyId, it.tagKeyName, it.op, it.values)
            },
            enabled = enabled,
            priority = priority,
            createdUser = createdUser,
            updatedUser = updatedUser
        )

        private fun LabelSelectorVO.toInternal() = LabelSelector(
            tagKeyId = tagKeyId,
            tagKeyName = tagKeyName,
            op = op,
            values = values
        )
    }
}
