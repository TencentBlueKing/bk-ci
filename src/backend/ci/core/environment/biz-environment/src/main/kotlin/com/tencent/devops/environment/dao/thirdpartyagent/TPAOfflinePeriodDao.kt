package com.tencent.devops.environment.dao.thirdpartyagent

import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyAgentOfflinePeriod
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentOfflinePeriodRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * agent离线时间段统计
 */
@Repository
class TPAOfflinePeriodDao {
    /**
     * 插入新的离线时段
     */
    fun insertOfflinePeriod(
        dslContext: DSLContext,
        agentId: Long,
        projectId: String,
        offlineTime: LocalDateTime
    ): Long? {
        with(TEnvironmentThirdpartyAgentOfflinePeriod.T_ENVIRONMENT_THIRDPARTY_AGENT_OFFLINE_PERIOD) {
            return dslContext.insertInto(
                this,
                AGENT_ID,
                PROJECT_ID,
                OFFLINE_TIME
            ).values(
                agentId,
                projectId,
                offlineTime
            ).returning(ID)
                .fetchAny()?.id
        }
    }

    /**
     * 更新离线时段的上线时间和时长
     */
    fun updateOnlineTime(
        dslContext: DSLContext,
        agentId: Long,
        onlineTime: LocalDateTime
    ): Int {
        with(TEnvironmentThirdpartyAgentOfflinePeriod.T_ENVIRONMENT_THIRDPARTY_AGENT_OFFLINE_PERIOD) {
            return dslContext.update(this)
                .set(ONLINE_TIME, onlineTime)
                .set(
                    DURATION_SECONDS,
                    DSL.field(
                        "TIMESTAMPDIFF(SECOND, {0}, {1})",
                        Long::class.java,
                        OFFLINE_TIME,
                        onlineTime
                    )
                )
                .where(AGENT_ID.eq(agentId))
                .and(ONLINE_TIME.isNull)
                .orderBy(OFFLINE_TIME.desc())
                .limit(1)
                .execute()
        }
    }

    /**
     * 查询 Agent 的离线时段列表（带分页）
     */
    fun listOfflinePeriods(
        dslContext: DSLContext,
        agentId: Long,
        offset: Int = 0,
        limit: Int = 100
    ): List<TEnvironmentThirdpartyAgentOfflinePeriodRecord> {
        with(TEnvironmentThirdpartyAgentOfflinePeriod.T_ENVIRONMENT_THIRDPARTY_AGENT_OFFLINE_PERIOD) {
            val conditions = mutableListOf(AGENT_ID.eq(agentId))
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(OFFLINE_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun countOfflinePeriods(
        dslContext: DSLContext,
        agentId: Long
    ): Long {
        with(TEnvironmentThirdpartyAgentOfflinePeriod.T_ENVIRONMENT_THIRDPARTY_AGENT_OFFLINE_PERIOD) {
            return dslContext.selectCount().from(this).where(AGENT_ID.eq(agentId)).fetchOne(0, Long::class.java)!!
        }
    }

//    /**
//     * 统计 Agent 的离线情况
//     */
//    fun getOfflineStatistics(
//        dslContext: DSLContext,
//        agentId: Long,
//        startTime: LocalDateTime? = null,
//        endTime: LocalDateTime? = null
//    ): OfflineStatistics {
//        with(TEnvironmentThirdpartyAgentOfflinePeriod.T_ENVIRONMENT_THIRDPARTY_AGENT_OFFLINE_PERIOD) {
//            val conditions = mutableListOf(AGENT_ID.eq(agentId))
//            startTime?.let { conditions.add(OFFLINE_TIME.ge(it)) }
//            endTime?.let { conditions.add(OFFLINE_TIME.le(it)) }
//
//            val result = dslContext.select(
//                DSL.count().`as`("total_count"),
//                DSL.countDistinct(
//                    DSL.case_()
//                        .`when`(ONLINE_TIME.isNull, 1)
//                        .otherwise(DSL.inline(null as Int?))
//                ).`as`("ongoing_count"),
//                DSL.avg(DURATION_SECONDS).`as`("avg_duration"),
//                DSL.max(DURATION_SECONDS).`as`("max_duration"),
//                DSL.min(DURATION_SECONDS).`as`("min_duration"),
//                DSL.sum(DURATION_SECONDS).`as`("total_duration")
//            )
//                .from(this)
//                .where(conditions)
//                .fetchOne()
//
//            return OfflineStatistics(
//                totalCount = result?.value1() as? Int ?: 0,
//                ongoingCount = result?.value2() as? Int ?: 0,
//                avgDurationSeconds = (result?.value3() as? Number)?.toLong(),
//                maxDurationSeconds = result?.value4() as? Long,
//                minDurationSeconds = result?.value5() as? Long,
//                totalDurationSeconds = result?.value6() as? Long
//            )
//        }
//    }

    /**
     * 查询最近一次未结束的离线记录
     */
    fun getLatestOngoingOffline(
        dslContext: DSLContext,
        agentId: Long
    ): TEnvironmentThirdpartyAgentOfflinePeriodRecord? {
        with(TEnvironmentThirdpartyAgentOfflinePeriod.T_ENVIRONMENT_THIRDPARTY_AGENT_OFFLINE_PERIOD) {
            return dslContext.selectFrom(this)
                .where(AGENT_ID.eq(agentId))
                .and(ONLINE_TIME.isNull)
                .orderBy(OFFLINE_TIME.desc())
                .limit(1)
                .fetchAny()
        }
    }

//    /**
//     * 批量查询多个 Agent 的离线统计
//     */
//    fun batchGetOfflineStatistics(
//        dslContext: DSLContext,
//        agentIds: List<Long>,
//        startTime: LocalDateTime? = null,
//        endTime: LocalDateTime? = null
//    ): Map<Long, TEnvironmentThirdpartyAgentOfflinePeriodRecord> {
//        with(TEnvironmentThirdpartyAgentOfflinePeriod.T_ENVIRONMENT_THIRDPARTY_AGENT_OFFLINE_PERIOD) {
//            val conditions = mutableListOf(AGENT_ID.`in`(agentIds))
//            startTime?.let { conditions.add(OFFLINE_TIME.ge(it)) }
//            endTime?.let { conditions.add(OFFLINE_TIME.le(it)) }
//
//            return dslContext.select(
//                AGENT_ID,
//                DSL.count().`as`("total_count"),
//                DSL.sum(
//                    DSL.case_()
//                        .`when`(ONLINE_TIME.isNull, 1)
//                        .otherwise(0)
//                ).`as`("ongoing_count"),
//                DSL.avg(DURATION_SECONDS).`as`("avg_duration"),
//                DSL.max(DURATION_SECONDS).`as`("max_duration"),
//                DSL.min(DURATION_SECONDS).`as`("min_duration"),
//                DSL.sum(DURATION_SECONDS).`as`("total_duration")
//            )
//                .from(this)
//                .where(conditions)
//                .groupBy(AGENT_ID)
//                .fetch()
//                .associate { record ->
//                    record.value1() to OfflineStatistics(
//                        totalCount = record.value2() as Int,
//                        ongoingCount = (record.value3() as? Number)?.toInt() ?: 0,
//                        avgDurationSeconds = (record.value4() as? Number)?.toLong(),
//                        maxDurationSeconds = record.value5() as? Long,
//                        minDurationSeconds = record.value6() as? Long,
//                        totalDurationSeconds = record.value7() as? Long
//                    )
//                }
//        }
//    }
}