package com.tencent.devops.quality.dao.v2

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.model.quality.tables.TQualityRuleTemplate
import com.tencent.devops.model.quality.tables.records.TQualityRuleTemplateRecord
import com.tencent.devops.quality.api.v2.pojo.enums.TemplateType
import com.tencent.devops.quality.api.v2.pojo.op.TemplateUpdate
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QualityRuleTemplateDao {
    fun listTemplateEnable(dslContext: DSLContext): Result<TQualityRuleTemplateRecord>? {
        with(TQualityRuleTemplate.T_QUALITY_RULE_TEMPLATE) {
            return dslContext.selectFrom(this)
                    .where((TYPE.eq(TemplateType.TEMPLATE.name)).and(ENABLE.eq(true)))
                    .fetch()
        }
    }

    fun listIndicatorSetEnable(dslContext: DSLContext): Result<TQualityRuleTemplateRecord>? {
        with(TQualityRuleTemplate.T_QUALITY_RULE_TEMPLATE) {
            return dslContext.selectFrom(this)
                    .where((TYPE.eq(TemplateType.INDICATOR_SET.name)).and(ENABLE.eq(true)))
                    .fetch()
        }
    }

    fun list(userId: String, page: Int, pageSize: Int, dslContext: DSLContext): Result<TQualityRuleTemplateRecord> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        with(TQualityRuleTemplate.T_QUALITY_RULE_TEMPLATE) {
            return dslContext.selectFrom(this)
                    .orderBy(CREATE_TIME.desc())
                    .limit(sqlLimit.offset, sqlLimit.limit)
                    .fetch()
        }
    }

    fun count(dslContext: DSLContext): Long {
        with(TQualityRuleTemplate.T_QUALITY_RULE_TEMPLATE) {
            return dslContext.selectCount().from(this)
                .fetchOne(0, Long::class.java)
        }
    }

    /**
     * 返回记录的id
     */
    fun create(userId: String, templateUpdate: TemplateUpdate, dslContext: DSLContext): Long {
        val now = LocalDateTime.now()
        with(TQualityRuleTemplate.T_QUALITY_RULE_TEMPLATE) {
            val record = dslContext.insertInto(
                    this, NAME, TYPE, DESC, STAGE, CONTROL_POINT,
                    CONTROL_POINT_POSITION, CREATE_USER,
                    UPDATE_USER, CREATE_TIME, UPDATE_TIME, ENABLE
            ).values(
                    templateUpdate.name,
                    templateUpdate.type,
                    templateUpdate.desc,
                    templateUpdate.stage,
                    templateUpdate.elementType,
                    templateUpdate.controlPointPostion,
                    userId,
                    userId,
                    now,
                    now,
                    templateUpdate.enable
            ).returning(ID)
                    .fetchOne()
            return record.id
        }
    }

    fun delete(userId: String, id: Long, dslContext: DSLContext): Long {
        with(TQualityRuleTemplate.T_QUALITY_RULE_TEMPLATE) {
            return dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
                    .toLong()
        }
    }

    fun update(userId: String, id: Long, templateUpdate: TemplateUpdate, dslContext: DSLContext): Int {
        return with(TQualityRuleTemplate.T_QUALITY_RULE_TEMPLATE) {
            val update = dslContext.update(this)

            with(templateUpdate) {
                if (!name.isNullOrBlank()) update.set(NAME, name)
                if (!type.isNullOrBlank()) update.set(TYPE, type)
                if (!desc.isNullOrBlank()) update.set(DESC, desc)
                if (!stage.isNullOrBlank()) update.set(STAGE, stage)
                if (!elementType.isNullOrBlank()) update.set(CONTROL_POINT, elementType)
                if (!controlPointPostion.isNullOrBlank()) update.set(CONTROL_POINT_POSITION, controlPointPostion)
                if (enable != null) update.set(ENABLE, enable)
            }
            update.set(UPDATE_TIME, LocalDateTime.now())
                    .set(UPDATE_USER, userId)
                    .where(ID.eq(id))
                    .execute()
        }
    }
}