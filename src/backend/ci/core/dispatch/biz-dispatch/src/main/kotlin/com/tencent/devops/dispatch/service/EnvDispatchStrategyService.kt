package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.dispatch.dao.EnvDispatchStrategyDao
import com.tencent.devops.dispatch.pojo.DispatchStrategyConfig
import com.tencent.devops.dispatch.pojo.LabelSelector
import com.tencent.devops.dispatch.pojo.enums.NodeRule
import com.tencent.devops.dispatch.pojo.enums.StrategyScope
import com.tencent.devops.dispatch.pojo.enums.StrategyType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EnvDispatchStrategyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDispatchStrategyDao: EnvDispatchStrategyDao
) {
    /**
     * 获取环境的策略列表，若环境还没有策略记录则懒初始化 4 条默认策略。
     */
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

    /**
     * 获取调度时使用的已启用策略列表（按 priority 升序）。
     * 老环境没有策略记录时，返回内存中构造的默认配置（不写 DB），保持向后兼容。
     */
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
        projectId: String,
        envId: Long,
        userId: String,
        strategyName: String,
        scope: StrategyScope,
        nodeRule: NodeRule,
        labelSelector: List<LabelSelector>?
    ): Long {
        val existing = envDispatchStrategyDao.listByEnv(dslContext, projectId, envId)
        val maxPriority = existing.maxOfOrNull { it.priority } ?: -1

        val config = DispatchStrategyConfig(
            id = null,
            projectId = projectId,
            envId = envId,
            strategyType = StrategyType.CUSTOM,
            defaultStrategyCode = null,
            strategyName = strategyName,
            scope = scope,
            nodeRule = nodeRule,
            labelSelector = labelSelector,
            enabled = true,
            priority = maxPriority + 1,
            createdUser = userId,
            updatedUser = userId
        )
        return envDispatchStrategyDao.create(dslContext, config)
    }

    fun updateStrategy(
        id: Long,
        userId: String,
        strategyName: String? = null,
        scope: StrategyScope? = null,
        nodeRule: NodeRule? = null,
        labelSelector: List<LabelSelector>? = null,
        enabled: Boolean? = null
    ) {
        val existing = envDispatchStrategyDao.getById(dslContext, id)
            ?: throw InvalidParamException("Strategy not found: $id")

        if (existing.strategyType == StrategyType.DEFAULT) {
            if (scope != null || nodeRule != null || labelSelector != null) {
                throw InvalidParamException("Default strategy only allows changing enabled and priority")
            }
            if (enabled == false) {
                enforceDefaultProtection(existing.projectId, existing.envId)
            }
        }

        envDispatchStrategyDao.update(
            dslContext = dslContext,
            id = id,
            strategyName = strategyName,
            scope = scope,
            nodeRule = nodeRule,
            labelSelector = labelSelector,
            enabled = enabled,
            updatedUser = userId
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

    fun reorderStrategies(
        projectId: String,
        envId: Long,
        orderedIds: List<Long>
    ) {
        val updates = orderedIds.mapIndexed { index, id -> Pair(id, index) }
        envDispatchStrategyDao.batchUpdatePriority(dslContext, updates)
    }

    /**
     * 当没有任何启用的自定义策略时，默认策略不可关闭。
     */
    private fun enforceDefaultProtection(projectId: String, envId: Long) {
        val customEnabledCount = envDispatchStrategyDao.countCustomEnabled(dslContext, projectId, envId)
        if (customEnabledCount == 0L) {
            throw InvalidParamException(
                "Cannot disable default strategy when no custom strategy is enabled"
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EnvDispatchStrategyService::class.java)
    }
}
