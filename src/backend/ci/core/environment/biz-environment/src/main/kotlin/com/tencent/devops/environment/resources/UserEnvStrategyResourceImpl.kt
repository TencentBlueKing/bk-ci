package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserEnvStrategyResource
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.DispatchEnvStrategyCreateReq
import com.tencent.devops.environment.pojo.DispatchEnvStrategyReorderReq
import com.tencent.devops.environment.pojo.DispatchEnvStrategyUpdateReq
import com.tencent.devops.environment.pojo.DispatchEnvStrategyVO
import com.tencent.devops.environment.pojo.DispatchStrategyConfig
import com.tencent.devops.environment.pojo.LabelSelector
import com.tencent.devops.environment.pojo.LabelSelectorVO
import com.tencent.devops.environment.service.EnvDispatchStrategyService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserEnvStrategyResourceImpl @Autowired constructor(
    private val envDispatchStrategyService: EnvDispatchStrategyService,
    private val environmentPermissionService: EnvironmentPermissionService
) : UserEnvStrategyResource {

    override fun listStrategies(
        userId: String, projectId: String, envId: Long
    ): Result<List<DispatchEnvStrategyVO>> {
        val strategies = envDispatchStrategyService.getOrInitStrategies(projectId, envId, userId)
        return Result(strategies.map { it.toVO() })
    }

    override fun createStrategy(
        userId: String, projectId: String, envId: Long,
        request: DispatchEnvStrategyCreateReq
    ): Result<Long> {
        checkEnvEditPermission(userId, projectId, envId)
        val id = envDispatchStrategyService.createCustomStrategy(
            projectId = projectId, envId = envId, userId = userId,
            strategyName = request.strategyName, scope = request.scope,
            nodeRule = request.nodeRule,
            labelSelector = request.labelSelector?.map { it.toInternal() }
        )
        return Result(id)
    }

    override fun updateStrategy(
        userId: String, projectId: String, envId: Long,
        strategyId: Long, request: DispatchEnvStrategyUpdateReq
    ): Result<Boolean> {
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.updateStrategy(
            id = strategyId, userId = userId,
            strategyName = request.strategyName, scope = request.scope,
            nodeRule = request.nodeRule,
            labelSelector = request.labelSelector?.map { it.toInternal() },
            enabled = request.enabled
        )
        return Result(true)
    }

    override fun deleteStrategy(
        userId: String, projectId: String, envId: Long, strategyId: Long
    ): Result<Boolean> {
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.deleteStrategy(strategyId)
        return Result(true)
    }

    override fun batchDeleteStrategy(
        userId: String, projectId: String, envId: Long, strategyIds: Set<Long>
    ): Result<Boolean> {
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.batchDeleteStrategy(strategyIds)
        return Result(true)
    }

    override fun reorderStrategies(
        userId: String, projectId: String, envId: Long,
        request: DispatchEnvStrategyReorderReq
    ): Result<Boolean> {
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.reorderStrategies(projectId, envId, request.orderedIds)
        return Result(true)
    }

    private fun checkEnvEditPermission(userId: String, projectId: String, envId: Long) {
        if (!environmentPermissionService.checkEnvPermission(userId, projectId, envId, AuthPermission.EDIT)) {
            throw PermissionForbiddenException(
                message = "User($userId) has no edit permission on env($envId) in project($projectId)"
            )
        }
    }

    companion object {
        private fun DispatchStrategyConfig.toVO() = DispatchEnvStrategyVO(
            id = id ?: 0L, projectId = projectId, envId = envId,
            strategyType = strategyType, defaultStrategyCode = defaultStrategyCode,
            strategyName = strategyName, scope = scope, nodeRule = nodeRule,
            labelSelector = labelSelector?.map { LabelSelectorVO(it.tagKeyId, it.tagKeyName, it.op, it.values) },
            enabled = enabled, priority = priority,
            createdUser = createdUser, updatedUser = updatedUser
        )

        private fun LabelSelectorVO.toInternal() = LabelSelector(
            tagKeyId = tagKeyId, tagKeyName = tagKeyName, op = op, values = values
        )
    }
}
