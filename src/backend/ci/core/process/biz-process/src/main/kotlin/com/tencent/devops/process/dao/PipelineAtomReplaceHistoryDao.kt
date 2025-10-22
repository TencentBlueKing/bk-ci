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

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.process.tables.TPipelineAtomReplaceHistory
import com.tencent.devops.model.process.tables.records.TPipelineAtomReplaceHistoryRecord
import com.tencent.devops.process.pojo.PipelineAtomReplaceHistory
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineAtomReplaceHistoryDao {

    fun createAtomReplaceHistory(
        dslContext: DSLContext,
        pipelineAtomReplaceHistory: PipelineAtomReplaceHistory
    ) {
        with(TPipelineAtomReplaceHistory.T_PIPELINE_ATOM_REPLACE_HISTORY) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                BUS_ID,
                BUS_TYPE,
                SOURCE_VERSION,
                TARGET_VERSION,
                STATUS,
                BASE_ID,
                ITEM_ID,
                CREATOR,
                MODIFIER,
                LOG
            )
                .values(
                    UUIDUtil.generate(),
                    pipelineAtomReplaceHistory.projectId,
                    pipelineAtomReplaceHistory.busId,
                    pipelineAtomReplaceHistory.busType,
                    pipelineAtomReplaceHistory.sourceVersion,
                    pipelineAtomReplaceHistory.targetVersion,
                    pipelineAtomReplaceHistory.status,
                    pipelineAtomReplaceHistory.baseId,
                    pipelineAtomReplaceHistory.itemId,
                    pipelineAtomReplaceHistory.userId,
                    pipelineAtomReplaceHistory.userId,
                    pipelineAtomReplaceHistory.log
                )
                .execute()
        }
    }

    fun getAtomReplaceHistoryList(
        dslContext: DSLContext,
        baseId: String,
        itemId: String? = null,
        projectId: String? = null,
        busType: String? = null,
        statusList: List<String>? = null,
        descFlag: Boolean,
        page: Int,
        pageSize: Int
    ): Result<TPipelineAtomReplaceHistoryRecord>? {
        with(TPipelineAtomReplaceHistory.T_PIPELINE_ATOM_REPLACE_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(BASE_ID.eq(baseId))
            if (itemId != null) {
                conditions.add(ITEM_ID.eq(itemId))
            }
            if (projectId != null) {
                conditions.add(PROJECT_ID.eq(projectId))
            }
            if (busType != null) {
                conditions.add(BUS_TYPE.eq(busType))
            }
            if (statusList != null) {
                conditions.add(STATUS.`in`(statusList))
            }
            val baseStep = dslContext.selectFrom(this).where(conditions)
            if (descFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        }
    }

    fun updateAtomReplaceHistory(
        dslContext: DSLContext,
        id: String,
        status: String? = null,
        log: String? = null,
        userId: String
    ) {
        with(TPipelineAtomReplaceHistory.T_PIPELINE_ATOM_REPLACE_HISTORY) {
            val baseStep = dslContext.update(this)
            if (status != null) {
                baseStep.set(STATUS, status)
            }
            if (log != null) {
                baseStep.set(LOG, log)
            }
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }
}
