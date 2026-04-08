package com.tencent.devops.environment.service

import com.tencent.devops.common.api.exception.InvalidParamException
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
    private val envDispatchStrategyDao: EnvDispatchStrategyDao
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
            if (enabled == false) {
                enforceDefaultProtection(existing.projectId, existing.envId)
            }
        }
        envDispatchStrategyDao.update(
            dslContext, id, strategyName, scope, nodeRule, labelSelector, enabled, userId
        )
    }

    fun deleteStrategy(id: Long) {
        val existing = envDispatchStrategyDao.getById(dslContext, id)
            ?: throw InvalidParamException("Strategy not found: $id")
        if (existing.strategyType == StrategyType.DEFAULT) {
            throw InvalidParamException("Cannot delete default strategy")
        }
        envDispatchStrategyDao.delete(dslContext, id)
    }

    fun batchDeleteStrategy(ids: Set<Long>) {
        if (ids.isEmpty()) return
        ids.forEach { id ->
            val existing = envDispatchStrategyDao.getById(dslContext, id)
                ?: throw InvalidParamException("Strategy not found: $id")
            if (existing.strategyType == StrategyType.DEFAULT) {
                throw InvalidParamException("Cannot delete default strategy: $id")
            }
        }
        envDispatchStrategyDao.batchDelete(dslContext, ids)
    }

    fun reorderStrategies(projectId: String, envId: Long, orderedIds: List<Long>) {
        val updates = orderedIds.mapIndexed { index, id -> Pair(id, index) }
        envDispatchStrategyDao.batchUpdatePriority(dslContext, updates)
    }

    private fun enforceDefaultProtection(projectId: String, envId: Long) {
        val customEnabledCount = envDispatchStrategyDao.countCustomEnabled(dslContext, projectId, envId)
        if (customEnabledCount == 0L) {
            throw InvalidParamException("Cannot disable default strategy when no custom strategy is enabled")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EnvDispatchStrategyService::class.java)
    }
}
