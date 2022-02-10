package com.tencent.devops.quality.dao.v2

import com.tencent.devops.model.quality.tables.TQualityRuleBuildHisOperation
import com.tencent.devops.model.quality.tables.records.TQualityRuleBuildHisOperationRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
@Suppress("ALL")
class QualityRuleBuildHisOperationDao @Autowired constructor (
    // todo remove
    private val innerDslContext: DSLContext
) {
    fun create(
        dslContext: DSLContext,
        userId: String,
        ruleId: Long,
        stageId: String
    ): Long {
        return with(TQualityRuleBuildHisOperation.T_QUALITY_RULE_BUILD_HIS_OPERATION) {
            dslContext.insertInto(
                this,
                RULE_ID,
                STAGE_ID,
                GATE_OPT_USER,
                GATE_OPT_TIME
            ).values(
                ruleId,
                stageId,
                userId,
                LocalDateTime.now()
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun list(dslContext: DSLContext, ruleIds: Collection<Long>): Result<TQualityRuleBuildHisOperationRecord> {
        return with(TQualityRuleBuildHisOperation.T_QUALITY_RULE_BUILD_HIS_OPERATION) {
            dslContext.selectFrom(this)
                .where(RULE_ID.`in`(ruleIds))
                .orderBy(GATE_OPT_TIME.desc())
                .fetch()
        }
    }

    // todo remove
    fun listStageRules(dslContext: DSLContext, stageId: String): Result<TQualityRuleBuildHisOperationRecord> {
        return with(TQualityRuleBuildHisOperation.T_QUALITY_RULE_BUILD_HIS_OPERATION) {
            dslContext.selectFrom(this)
                .where(STAGE_ID.eq(stageId))
                .fetch()
        }
    }
}
