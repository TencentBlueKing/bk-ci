package com.tencent.devops.remotedev.dao

import com.tencent.devops.model.remotedev.tables.TRemotedevProjectStrategy
import com.tencent.devops.model.remotedev.tables.records.TRemotedevProjectStrategyRecord
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.strategy.StrategyType
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class ProjectStrategyDao {
    fun createOrUpdateStrategy(
        dslContext: DSLContext,
        projectId: String,
        zoneType: WindowsResourceZoneConfigType,
        strategyType: StrategyType,
        content: String
    ) {
        with(TRemotedevProjectStrategy.T_REMOTEDEV_PROJECT_STRATEGY) {
            val condition = PROJECT_ID.eq(projectId)
                .and(ZONE_TYPE.eq(zoneType.name))
                .and(STRATEGY_TYPE.eq(strategyType.name))

            dslContext.transaction { config ->
                val ctx = DSL.using(config)

                val updated = ctx.update(this)
                    .set(STRATEGY_CONTENT, JSON.json(content))
                    .where(condition)
                    .execute()

                if (updated == 0) {
                    ctx.insertInto(
                        this,
                        PROJECT_ID,
                        ZONE_TYPE,
                        STRATEGY_TYPE,
                        STRATEGY_CONTENT
                    )
                        .values(
                            projectId,
                            zoneType.name,
                            strategyType.name,
                            JSON.json(content)
                        )
                        .execute()
                }
            }
        }
    }

    fun fetchStrategyList(
        dslContext: DSLContext,
        projectId: String,
        zoneType: WindowsResourceZoneConfigType,
        strategyType: List<StrategyType>
    ): List<TRemotedevProjectStrategyRecord> {
        with(TRemotedevProjectStrategy.T_REMOTEDEV_PROJECT_STRATEGY) {
            return dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
                .and(ZONE_TYPE.eq(zoneType.name))
                .and(STRATEGY_TYPE.`in`(strategyType.map { it.name }))
                .fetch()
        }
    }
}
