package com.tencent.devops.quality.dao.v2

import com.tencent.devops.model.quality.tables.TQualityTemplateIndicatorMap
import com.tencent.devops.model.quality.tables.records.TQualityTemplateIndicatorMapRecord
import com.tencent.devops.quality.api.v2.pojo.op.TemplateIndicatorMapUpdate
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

/**
 * @author eltons,  Date on 2019-03-06.
 */
@Repository
class QualityTemplateIndicatorMapDao {
    fun listByTemplateId(templateId: Long, dslContext: DSLContext): Result<TQualityTemplateIndicatorMapRecord> {
        return with(TQualityTemplateIndicatorMap.T_QUALITY_TEMPLATE_INDICATOR_MAP) {
            dslContext.selectFrom(this)
                    .where(TEMPLATE_ID.eq(templateId))
                    .fetch()
        }
    }

    fun batchCreate(dslContext: DSLContext, templateIndicatorMaps: List<TemplateIndicatorMapUpdate>?): Int {
        if (templateIndicatorMaps == null || templateIndicatorMaps.isEmpty()) return 0
        val list = templateIndicatorMaps.map {
            TQualityTemplateIndicatorMapRecord(null, it.templateId, it.indicatorId, it.operation, it.threshold)
        }
        return dslContext.batchInsert(list).execute().size
    }

    fun deleteByIndicatorId(dslContext: DSLContext, indicatorId: Long): Int {
        return with(TQualityTemplateIndicatorMap.T_QUALITY_TEMPLATE_INDICATOR_MAP) {
            dslContext.deleteFrom(this)
                    .where(INDICATOR_ID.eq(indicatorId))
                    .execute()
        }
    }

    fun deleteRealByTemplateId(dslContext: DSLContext, templateId: Long): Int {
        return with(TQualityTemplateIndicatorMap.T_QUALITY_TEMPLATE_INDICATOR_MAP) {
            dslContext.deleteFrom(this)
                    .where(TEMPLATE_ID.eq(templateId))
                    .execute()
        }
    }

    fun queryTemplateMap(templateId: Long, dslContext: DSLContext): Result<TQualityTemplateIndicatorMapRecord>? {
        with(TQualityTemplateIndicatorMap.T_QUALITY_TEMPLATE_INDICATOR_MAP) {
            return dslContext.selectFrom(this)
                    .where(TEMPLATE_ID.eq(templateId))
                    .fetch()
        }
    }
}