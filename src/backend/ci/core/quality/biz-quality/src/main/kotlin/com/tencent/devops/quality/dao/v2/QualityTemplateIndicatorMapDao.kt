/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
