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

package com.tencent.devops.process.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.process.tables.TPipelineStageTag
import com.tencent.devops.model.process.tables.records.TPipelineStageTagRecord
import com.tencent.devops.process.pojo.PipelineStageTag
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineStageTagDao {

    fun add(
        dslContext: DSLContext,
        id: String,
        stageTagName: String,
        weight: Int
    ) {
        with(TPipelineStageTag.T_PIPELINE_STAGE_TAG) {
            dslContext.insertInto(
                this,
                ID,
                STAGE_TAG_NAME,
                WEIGHT
            )
                .values(
                    id,
                    stageTagName,
                    weight
                )
                .onDuplicateKeyUpdate()
                .set(STAGE_TAG_NAME, stageTagName)
                .set(WEIGHT, weight)
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TPipelineStageTag.T_PIPELINE_STAGE_TAG) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: String,
        stageTagName: String,
        weight: Int
    ) {
        with(TPipelineStageTag.T_PIPELINE_STAGE_TAG) {
            dslContext.update(this)
                .set(STAGE_TAG_NAME, stageTagName)
                .set(WEIGHT, weight)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getStageTag(dslContext: DSLContext, id: String): TPipelineStageTagRecord? {
        with(TPipelineStageTag.T_PIPELINE_STAGE_TAG) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getAllStageTag(dslContext: DSLContext): Result<TPipelineStageTagRecord> {
        with(TPipelineStageTag.T_PIPELINE_STAGE_TAG) {
            return dslContext
                .selectFrom(this)
                .orderBy(WEIGHT.desc())
                .skipCheck()
                .fetch()
        }
    }

    fun getDefaultStageTag(dslContext: DSLContext): PipelineStageTag? {
        with(TPipelineStageTag.T_PIPELINE_STAGE_TAG) {
            val record = dslContext.selectFrom(this)
                .orderBy(WEIGHT.desc())
                .limit(1)
                .skipCheck()
                .fetchOne()
            return if (record == null) null else convert(record, true)
        }
    }

    fun convert(record: TPipelineStageTagRecord, defaultFlag: Boolean): PipelineStageTag {
        with(record) {
            return PipelineStageTag(
                id = id,
                stageTagName = stageTagName,
                weight = weight,
                defaultFlag = defaultFlag,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli()
            )
        }
    }

    fun countByNameOrWeight(dslContext: DSLContext, stageTagName: String, weight: Int): Record1<Int>? {
        with(TPipelineStageTag.T_PIPELINE_STAGE_TAG) {
            return dslContext.selectCount().from(this)
                .where(STAGE_TAG_NAME.eq(stageTagName))
                .or(WEIGHT.eq(weight))
                .skipCheck()
                .fetchOne()
        }
    }
}
