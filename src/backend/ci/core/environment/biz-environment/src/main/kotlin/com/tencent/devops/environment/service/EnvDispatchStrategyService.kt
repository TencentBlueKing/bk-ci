package com.tencent.devops.environment.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.EnvDispatchStrategyDao
import com.tencent.devops.environment.pojo.DispatchStrategyConfig
import com.tencent.devops.environment.pojo.LabelSelector
import com.tencent.devops.environment.pojo.enums.NodeRule
import com.tencent.devops.environment.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.enums.StrategyType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EnvDispatchStrategyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDispatchStrategyDao: EnvDispatchStrategyDao,
    private val redisOperation: RedisOperation
) {
    fun getOrInitStrategies(
        projectId: String,
        envId: Long,
        userId: String
    ): List<DispatchStrategyConfig> {
        val existing = envDispatchStrategyDao.listByEnv(dslContext, projectId, envId)
        if (existing.isNotEmpty()) {
            return existing
        }
        val defaults = DispatchStrategyConfig.buildDefaults(projectId, envId, userId)
        envDispatchStrategyDao.batchCreate(dslContext, defaults)
        return envDispatchStrategyDao.listByEnv(dslContext, projectId, envId)
    }

    fun getEnabledStrategies(projectId: String, envId: Long?): List<DispatchStrategyConfig> {
        if (envId == null) {
            return DispatchStrategyConfig.buildDefaults(projectId, 0, "system")
        }
        // 这里全拿出来是兼容没有开启过策略的情况
        val all = envDispatchStrategyDao.listByEnv(dslContext, projectId, envId)
        if (all.isEmpty()) {
            return DispatchStrategyConfig.buildDefaults(projectId, envId, "system")
        }
        val enabled = all.filter { it.enabled }
        if (enabled.isEmpty()) {
            return all.filter { it.strategyType == StrategyType.DEFAULT }.map { it.copy(enabled = true) }
        }
        return enabled
    }

    fun createCustomStrategy(
        projectId: String, envId: Long, userId: String,
        strategyName: String, scope: StrategyScope, nodeRule: NodeRule,
        labelSelector: List<LabelSelector>?
    ): Long {
        val lock = strategiesUpdateLock(projectId, envId)
        if (!lock.tryLock()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_STRATEGY_NOW_USING)
        }
        try {
            val existing = envDispatchStrategyDao.listByEnv(dslContext, projectId, envId)
            val maxPriority = existing.maxOfOrNull { it.priority } ?: -1
            val config = DispatchStrategyConfig(
                id = null, projectId = projectId, envId = envId,
                strategyType = StrategyType.CUSTOM, defaultStrategyCode = null,
                strategyName = strategyName, scope = scope, nodeRule = nodeRule,
                labelSelector = labelSelector, enabled = true, priority = maxPriority + 1,
                createdUser = userId, updatedUser = userId
            )
            return envDispatchStrategyDao.create(dslContext, config)
        } finally {
            lock.unlock()
        }
    }

    fun updateStrategy(
        id: Long, userId: String,
        strategyName: String? = null, scope: StrategyScope? = null,
        nodeRule: NodeRule? = null, labelSelector: List<LabelSelector>? = null,
        enabled: Boolean? = null
    ) {
        val existing = envDispatchStrategyDao.getById(dslContext, id)
            ?: throw InvalidParamException("Strategy not found: $id")
        if (existing.strategyType == StrategyType.DEFAULT) {
            if (strategyName != null || scope != null || nodeRule != null || labelSelector != null) {
                throw InvalidParamException("Default strategy only allows toggling enabled")
            }
        }
        // 不能存在全部都关闭的策略组
        if (enabled == false) {
            enforceProtection(existing.projectId, existing.envId)
        }
        envDispatchStrategyDao.update(
            dslContext, id, strategyName, scope, nodeRule, labelSelector, enabled, userId
        )
    }

    fun deleteStrategy(projectId: String, envId: Long, id: Long) {
        val lock = strategiesUpdateLock(projectId, envId)
        if (!lock.tryLock()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_STRATEGY_NOW_USING)
        }
        try {
            val existing = envDispatchStrategyDao.getById(dslContext, id)
                ?: throw InvalidParamException("Strategy not found: $id")
            if (existing.strategyType == StrategyType.DEFAULT) {
                throw InvalidParamException("Cannot delete default strategy")
            }
            envDispatchStrategyDao.delete(dslContext, id)
        } finally {
            lock.unlock()
        }
    }

    fun batchDeleteStrategy(projectId: String, envId: Long, ids: Set<Long>) {
        if (ids.isEmpty()) return
        val lock = strategiesUpdateLock(projectId, envId)
        if (!lock.tryLock()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_STRATEGY_NOW_USING)
        }
        try {
            val existing = envDispatchStrategyDao.getByIds(dslContext, ids)
                .ifEmpty { throw InvalidParamException("Strategy not found") }
            if (existing.any { it.strategyType == StrategyType.DEFAULT }) {
                throw InvalidParamException("Cannot delete default strategy")
            }
            envDispatchStrategyDao.batchDelete(dslContext, ids)
        } finally {
            lock.unlock()
        }
    }

    fun reorderStrategies(projectId: String, envId: Long, orderedIds: List<Long>) {
        val lock = strategiesUpdateLock(projectId, envId)
        if (!lock.tryLock()) {
            throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_ENV_STRATEGY_NOW_USING)
        }
        try {
            // 这里每次过来排序的一定要是所有id
            val all = envDispatchStrategyDao.listByEnv(dslContext, projectId, envId).map { it.id }.toSet()
            if (all != orderedIds.toSet()) {
                throw InvalidParamException("strategy Prioritization requires that all strategies be ranked")
            }
            val updates = orderedIds.mapIndexed { index, id -> Pair(id, index) }
            envDispatchStrategyDao.batchUpdatePriority(dslContext, updates)
        } finally {
            lock.unlock()
        }
    }

    private fun enforceProtection(projectId: String, envId: Long) {
        val customEnabledCount = envDispatchStrategyDao.countEnabled(dslContext, projectId, envId)
        if (customEnabledCount <= 1L) {
            throw InvalidParamException("Cannot disable strategy when no strategy is enabled")
        }
    }

    private fun strategiesUpdateLock(projectId: String, envId: Long) = RedisLock(
        redisOperation,
        "$STRATEGY_UPDATE_LOCK_PREFIX:$projectId:$envId",
        60,
    )

    companion object {
        private const val STRATEGY_UPDATE_LOCK_PREFIX = "environment:strategy:update:"
        private val logger = LoggerFactory.getLogger(EnvDispatchStrategyService::class.java)
    }
}
