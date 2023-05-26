/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.quality.dao.v2

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.model.quality.tables.TQualityRuleTemplate
import com.tencent.devops.model.quality.tables.records.TQualityRuleTemplateRecord
import com.tencent.devops.quality.api.v2.pojo.enums.TemplateType
import com.tencent.devops.quality.api.v2.pojo.op.TemplateUpdate
import com.tencent.devops.quality.pojo.po.QualityRuleTemplatePO
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository@Suppress("ALL")
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
                .fetchOne(0, Long::class.java)!!
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
                .fetchOne()!!
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

    fun batchCrateQualityRuleTemplate(dslContext: DSLContext, qualityRuleTemplatePOs: List<QualityRuleTemplatePO>) {
        with(TQualityRuleTemplate.T_QUALITY_RULE_TEMPLATE) {
            dslContext.batch(
                qualityRuleTemplatePOs.map { qualityRuleTemplatePO ->
                    dslContext.insertInto(this)
                        .set(dslContext.newRecord(this, qualityRuleTemplatePO))
                        .onDuplicateKeyUpdate()
                        .set(NAME, qualityRuleTemplatePO.name)
                        .set(DESC, qualityRuleTemplatePO.desc)
                        .set(STAGE, qualityRuleTemplatePO.stage)
                        .set(UPDATE_TIME, LocalDateTime.now())
                }
            ).execute()
        }
    }
}
