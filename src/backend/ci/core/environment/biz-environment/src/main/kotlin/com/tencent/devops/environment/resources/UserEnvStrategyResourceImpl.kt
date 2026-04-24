package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
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
        userId: String, projectId: String, envHashId: String
    ): Result<List<DispatchEnvStrategyVO>> {
        val strategies = envDispatchStrategyService.getOrInitStrategies(
            projectId = projectId,
            envId = HashUtil.decodeIdToLong(envHashId),
            userId = userId
        )
        return Result(strategies)
    }

    override fun createStrategy(
        userId: String, projectId: String, envHashId: String,
        request: DispatchEnvStrategyCreateReq
    ): Result<Long> {
        val envId = HashUtil.decodeIdToLong(envHashId)
        checkEnvEditPermission(userId, projectId, envId)
        val id = envDispatchStrategyService.createCustomStrategy(
            projectId = projectId, envId = envId, userId = userId,
            strategyName = request.strategyName, scope = request.scope,
            nodeRule = request.nodeRule,
            labelSelector = request.labelSelector
        )
        return Result(id)
    }

    override fun updateStrategy(
        userId: String, projectId: String, envHashId: String,
        strategyId: Long, request: DispatchEnvStrategyUpdateReq
    ): Result<Boolean> {
        val envId = HashUtil.decodeIdToLong(envHashId)
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.updateStrategy(
            projectId = projectId,
            id = strategyId, userId = userId,
            strategyName = request.strategyName, scope = request.scope,
            nodeRule = request.nodeRule,
            labelSelector = request.labelSelector,
            enabled = request.enabled
        )
        return Result(true)
    }

    override fun deleteStrategy(
        userId: String, projectId: String, envHashId: String, strategyId: Long
    ): Result<Boolean> {
        val envId = HashUtil.decodeIdToLong(envHashId)
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.deleteStrategy(projectId, envId, strategyId)
        return Result(true)
    }

    override fun batchDeleteStrategy(
        userId: String, projectId: String, envHashId: String, strategyIds: Set<Long>
    ): Result<Boolean> {
        val envId = HashUtil.decodeIdToLong(envHashId)
        checkEnvEditPermission(userId, projectId, envId)
        envDispatchStrategyService.batchDeleteStrategy(projectId, envId, strategyIds)
        return Result(true)
    }

    override fun reorderStrategies(
        userId: String, projectId: String, envHashId: String,
        request: DispatchEnvStrategyReorderReq
    ): Result<Boolean> {
        val envId = HashUtil.decodeIdToLong(envHashId)
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
}
