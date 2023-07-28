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
import com.tencent.devops.model.quality.tables.TQualityIndicator
import com.tencent.devops.model.quality.tables.records.TQualityIndicatorRecord
import com.tencent.devops.quality.api.v2.pojo.enums.IndicatorType
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
import com.tencent.devops.quality.pojo.po.QualityIndicatorPO
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository@Suppress("ALL")
class QualityIndicatorDao {
    // todo performance
    fun listByType(dslContext: DSLContext, type: IndicatorType = IndicatorType.SYSTEM): Result<TQualityIndicatorRecord>? {
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            return dslContext.selectFrom(this)
                .where(TYPE.eq(type.name))
                .fetch()
        }
    }

    fun listAll(dslContext: DSLContext): Result<TQualityIndicatorRecord>? {
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun listSystemDescByPage(dslContext: DSLContext, page: Int? = null, pageSize: Int? = null): Result<TQualityIndicatorRecord>? {
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            val sql = dslContext.selectFrom(this)
                .where(TYPE.eq(IndicatorType.SYSTEM.name))
                .orderBy(CREATE_TIME.desc())
            if (page != null && pageSize != null) {
                val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
                sql.limit(sqlLimit.offset, sqlLimit.limit)
            }
            return sql.fetch()
        }
    }

    fun listByIds(dslContext: DSLContext, ids: Collection<Long>): Result<TQualityIndicatorRecord>? {
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(ids))
                .fetch()
        }
    }

    fun listByElementType(
        dslContext: DSLContext,
        elementType: String,
        type: IndicatorType? = IndicatorType.MARKET,
        enNameSet: Collection<String>? = null,
        projectId: String? = null
    ): Result<TQualityIndicatorRecord>? {
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            val conditions = mutableListOf<Condition>()
            conditions.add(ELEMENT_TYPE.eq(elementType))
            if (type != null) {
                conditions.add(TYPE.eq(type.name))
            }
            if (!enNameSet.isNullOrEmpty()) {
                conditions.add(EN_NAME.`in`(enNameSet))
            }
            if (projectId != null) {
                conditions.add(INDICATOR_RANGE.eq(projectId))
            }
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun countSystem(dslContext: DSLContext): Long {
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            return dslContext.selectCount().from(this)
                .where(TYPE.eq(IndicatorType.SYSTEM.name))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun count(dslContext: DSLContext, projectId: String? = null, enable: Boolean? = null): Long {
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            val sql = dslContext.selectCount().from(this)
            if (!projectId.isNullOrBlank()) {
                sql.where(INDICATOR_RANGE.eq("ANY").or(INDICATOR_RANGE.like("%$projectId%")))
            }
            if (enable != null) {
                sql.where(ENABLE.eq(enable))
            }
            return sql.fetchOne(0, Long::class.java)!!
        }
    }

    fun create(userId: String, indicatorUpdate: IndicatorUpdate, dslContext: DSLContext): Long {
        val now = LocalDateTime.now()
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            val record = dslContext.insertInto(
                this,
                ELEMENT_TYPE,
                ELEMENT_NAME,
                ELEMENT_DETAIL,
                EN_NAME,
                CN_NAME,
                METADATA_IDS,
                DEFAULT_OPERATION,
                OPERATION_AVAILABLE,
                THRESHOLD,
                THRESHOLD_TYPE,
                DESC,
                INDICATOR_READ_ONLY,
                STAGE,
                ENABLE,
                TYPE,
                TAG,
                CREATE_USER,
                UPDATE_USER,
                CREATE_TIME,
                UPDATE_TIME,
                ATOM_VERSION,
                LOG_PROMPT,
                INDICATOR_RANGE
            ).values(
                indicatorUpdate.elementType,
                indicatorUpdate.elementName,
                indicatorUpdate.elementDetail,
                indicatorUpdate.enName,
                indicatorUpdate.cnName,
                indicatorUpdate.metadataIds,
                indicatorUpdate.defaultOperation,
                indicatorUpdate.operationAvailable,
                indicatorUpdate.threshold,
                indicatorUpdate.thresholdType?.toUpperCase(),
                indicatorUpdate.desc,
                indicatorUpdate.readOnly,
                indicatorUpdate.stage,
                indicatorUpdate.enable,
                indicatorUpdate.type.toString(),
                indicatorUpdate.tag,
                userId,
                userId,
                now,
                now,
                indicatorUpdate.elementVersion ?: "",
                indicatorUpdate.logPrompt ?: "",
                indicatorUpdate.range ?: ""
            ).returning(ID).fetchOne()!!
            return record.id
        }
    }

    fun delete(id: Long, dslContext: DSLContext): Int {
        return delete(setOf(id), dslContext)
    }

    fun delete(ids: Collection<Long>, dslContext: DSLContext): Int {
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            return dslContext.deleteFrom(this)
                .where(ID.`in`(ids))
                .execute()
        }
    }

    fun update(userId: String, id: Long, indicatorUpdate: IndicatorUpdate, dslContext: DSLContext): Int {
        return with(TQualityIndicator.T_QUALITY_INDICATOR) {
            val update = dslContext.update(this)

            with(indicatorUpdate) {
                if (!elementType.isNullOrBlank()) update.set(ELEMENT_TYPE, elementType)
                if (!elementName.isNullOrBlank()) update.set(ELEMENT_NAME, elementName)
                if (!elementDetail.isNullOrBlank()) update.set(ELEMENT_DETAIL, elementDetail)
                if (!enName.isNullOrBlank()) update.set(EN_NAME, enName)
                if (!cnName.isNullOrBlank()) update.set(CN_NAME, cnName)
                if (!metadataIds.isNullOrBlank()) update.set(METADATA_IDS, metadataIds)
                if (!defaultOperation.isNullOrBlank()) update.set(DEFAULT_OPERATION, defaultOperation)
                if (!operationAvailable.isNullOrBlank()) update.set(OPERATION_AVAILABLE, operationAvailable)
                if (!threshold.isNullOrBlank()) update.set(THRESHOLD, threshold)
                if (!thresholdType.isNullOrBlank()) update.set(THRESHOLD_TYPE, thresholdType)
                if (!desc.isNullOrBlank()) update.set(DESC, desc)
                if (readOnly != null) update.set(INDICATOR_READ_ONLY, readOnly)
                if (!stage.isNullOrBlank()) update.set(STAGE, stage)
                if (enable != null) update.set(ENABLE, enable)
                if (type != null) update.set(TYPE, type.toString())
                if (!tag.isNullOrBlank()) update.set(TAG, tag)
                if (!logPrompt.isNullOrBlank()) update.set(LOG_PROMPT, logPrompt) else update.set(LOG_PROMPT, "")
            }
            update.set(UPDATE_TIME, LocalDateTime.now())
                .set(UPDATE_USER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, indicatorId: Long): TQualityIndicatorRecord {
        return with(TQualityIndicator.T_QUALITY_INDICATOR) {
            dslContext.selectFrom(this)
                .where(ID.eq(indicatorId))
                .fetchOne()!!
        }
    }

    fun batchCrateQualityIndicator(dslContext: DSLContext, qualityIndicatorPOs: List<QualityIndicatorPO>) {
        with(TQualityIndicator.T_QUALITY_INDICATOR) {
            dslContext.batch(
                qualityIndicatorPOs.map { qualityIndicatorPO ->
                    dslContext.insertInto(this)
                        .set(dslContext.newRecord(this, qualityIndicatorPO))
                        .onDuplicateKeyUpdate()
                        .set(ELEMENT_NAME, qualityIndicatorPO.elementName)
                        .set(DESC, qualityIndicatorPO.desc)
                        .set(STAGE, qualityIndicatorPO.stage)
                        .set(UPDATE_TIME, LocalDateTime.now())
                }
            ).execute()
        }
    }
}
