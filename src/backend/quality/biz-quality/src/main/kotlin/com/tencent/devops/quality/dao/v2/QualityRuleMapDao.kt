package com.tencent.devops.quality.dao.v2

import com.tencent.devops.model.quality.tables.TQualityRuleMap
import com.tencent.devops.model.quality.tables.records.TQualityRuleMapRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class QualityRuleMapDao {
    fun get(dslContext: DSLContext, ruleId: Long): TQualityRuleMapRecord {
        return with(TQualityRuleMap.T_QUALITY_RULE_MAP) {
            dslContext.selectFrom(this)
                    .where(RULE_ID.eq(ruleId))
                    .fetchOne()
        }
    }

    fun batchGet(dslContext: DSLContext, ruleIds: Collection<Long>): Result<TQualityRuleMapRecord>? {
        return with(TQualityRuleMap.T_QUALITY_RULE_MAP) {
            dslContext.selectFrom(this)
                    .where(RULE_ID.`in`(ruleIds))
                    .fetch()
        }
    }
}