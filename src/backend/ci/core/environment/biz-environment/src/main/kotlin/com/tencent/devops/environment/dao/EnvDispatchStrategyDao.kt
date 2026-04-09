package com.tencent.devops.environment.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.environment.pojo.DispatchStrategyConfig
import com.tencent.devops.environment.pojo.LabelSelector
import com.tencent.devops.environment.pojo.enums.DefaultStrategyCode
import com.tencent.devops.environment.pojo.enums.NodeRule
import com.tencent.devops.environment.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.enums.StrategyType
import com.tencent.devops.model.environment.tables.TEnvDispatchStrategy
import com.tencent.devops.model.environment.tables.records.TEnvDispatchStrategyRecord
import org.jooq.DSLContext
import org.jooq.RecordMapper
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EnvDispatchStrategyDao {

    fun create(dslContext: DSLContext, config: DispatchStrategyConfig): Long {
        val now = LocalDateTime.now()
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            return dslContext.insertInto(
                this, PROJECT_ID, ENV_ID, STRATEGY_TYPE, DEFAULT_STRATEGY_CODE,
                STRATEGY_NAME, SCOPE, NODE_RULE, LABEL_SELECTOR, ENABLED, PRIORITY,
                CREATED_USER, UPDATED_USER, CREATED_TIME, UPDATED_TIME
            ).values(
                config.projectId, config.envId, config.strategyType.name,
                config.defaultStrategyCode?.name, config.strategyName,
                config.scope.name, config.nodeRule.name,
                config.labelSelector?.let { JsonUtil.toJson(it) },
                config.enabled, config.priority, config.createdUser, config.updatedUser, now, now
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun batchCreate(dslContext: DSLContext, configs: List<DispatchStrategyConfig>) {
        val now = LocalDateTime.now()
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            val insert = dslContext.insertInto(
                this, PROJECT_ID, ENV_ID, STRATEGY_TYPE, DEFAULT_STRATEGY_CODE,
                STRATEGY_NAME, SCOPE, NODE_RULE, LABEL_SELECTOR, ENABLED, PRIORITY,
                CREATED_USER, UPDATED_USER, CREATED_TIME, UPDATED_TIME
            )
            configs.forEach { config ->
                insert.values(
                    config.projectId, config.envId, config.strategyType.name,
                    config.defaultStrategyCode?.name, config.strategyName,
                    config.scope.name, config.nodeRule.name,
                    config.labelSelector?.let { JsonUtil.toJson(it) },
                    config.enabled, config.priority, config.createdUser, config.updatedUser, now, now
                )
            }
            insert.execute()
        }
    }

    fun update(
        dslContext: DSLContext, id: Long,
        strategyName: String?, scope: StrategyScope?, nodeRule: NodeRule?,
        labelSelector: List<LabelSelector>?, enabled: Boolean?, updatedUser: String
    ) {
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            val step = dslContext.update(this)
                .set(UPDATED_TIME, LocalDateTime.now())
                .set(UPDATED_USER, updatedUser)
            strategyName?.let { step.set(STRATEGY_NAME, it) }
            scope?.let { step.set(SCOPE, it.name) }
            nodeRule?.let { step.set(NODE_RULE, it.name) }
            if (labelSelector != null) {
                step.set(LABEL_SELECTOR, JsonUtil.toJson(labelSelector))
            }
            enabled?.let { step.set(ENABLED, it) }
            step.where(ID.eq(id)).execute()
        }
    }

    fun delete(dslContext: DSLContext, id: Long) {
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            dslContext.deleteFrom(this).where(ID.eq(id)).execute()
        }
    }

    fun batchDelete(dslContext: DSLContext, ids: Set<Long>) {
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            dslContext.deleteFrom(this).where(ID.`in`(ids)).execute()
        }
    }

    fun getById(dslContext: DSLContext, id: Long): DispatchStrategyConfig? {
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            return dslContext.selectFrom(this).where(ID.eq(id)).fetchAny(strategyMapper)
        }
    }

    fun getByIds(dslContext: DSLContext, ids: Set<Long>): List<DispatchStrategyConfig> {
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            return dslContext.selectFrom(this).where(ID.`in`(ids)).fetch(strategyMapper)
        }
    }

    fun listByEnv(
        dslContext: DSLContext,
        projectId: String,
        envId: Long
    ): List<DispatchStrategyConfig> {
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId)).and(ENV_ID.eq(envId))
                .orderBy(PRIORITY.asc()).fetch(strategyMapper)
        }
    }

    fun batchUpdatePriority(dslContext: DSLContext, updates: List<Pair<Long, Int>>) {
        val now = LocalDateTime.now()
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            dslContext.batched { ctx ->
                updates.forEach { (id, priority) ->
                    ctx.dsl().update(this).set(PRIORITY, priority).set(UPDATED_TIME, now)
                        .where(ID.eq(id)).execute()
                }
            }
        }
    }

    fun countEnabled(dslContext: DSLContext, projectId: String, envId: Long): Long {
        with(TEnvDispatchStrategy.T_ENV_DISPATCH_STRATEGY) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ENV_ID.eq(envId))
                .and(ENABLED.eq(true))
                .fetchOne(0, Long::class.java)!!
        }
    }

    companion object {
        private val strategyMapper = EnvDispatchStrategyRecordMapper()
    }
}

class EnvDispatchStrategyRecordMapper :
    RecordMapper<TEnvDispatchStrategyRecord, DispatchStrategyConfig> {
    override fun map(record: TEnvDispatchStrategyRecord?): DispatchStrategyConfig? {
        return record?.let {
            DispatchStrategyConfig(
                id = it.id, projectId = it.projectId, envId = it.envId,
                strategyType = StrategyType.valueOf(it.strategyType),
                defaultStrategyCode = it.defaultStrategyCode?.let { code -> DefaultStrategyCode.valueOf(code) },
                strategyName = it.strategyName,
                scope = StrategyScope.valueOf(it.scope),
                nodeRule = NodeRule.valueOf(it.nodeRule),
                labelSelector = it.labelSelector?.let { json ->
                    JsonUtil.to(json, object : TypeReference<List<LabelSelector>>() {})
                },
                enabled = it.enabled, priority = it.priority,
                createdUser = it.createdUser, updatedUser = it.updatedUser
            )
        }
    }
}
