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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.process.tables.TPipelineAtomReplaceBase
import com.tencent.devops.model.process.tables.records.TPipelineAtomReplaceBaseRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineAtomReplaceBaseDao {

    fun createAtomReplaceBase(
        dslContext: DSLContext,
        baseId: String,
        projectId: String?,
        pipelineIdList: List<String>?,
        fromAtomCode: String,
        toAtomCode: String,
        userId: String
    ) {
        with(TPipelineAtomReplaceBase.T_PIPELINE_ATOM_REPLACE_BASE) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                PIPELINE_ID_INFO,
                FROM_ATOM_CODE,
                TO_ATOM_CODE,
                CREATOR,
                MODIFIER
            )
                .values(
                    baseId,
                    projectId,
                    pipelineIdList?.let { self -> JsonUtil.toJson(self, formatted = false) },
                    fromAtomCode,
                    toAtomCode,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun getAtomReplaceBaseList(
        dslContext: DSLContext,
        statusList: List<String>? = null,
        descFlag: Boolean,
        page: Int,
        pageSize: Int
    ): Result<TPipelineAtomReplaceBaseRecord>? {
        with(TPipelineAtomReplaceBase.T_PIPELINE_ATOM_REPLACE_BASE) {
            val baseStep = dslContext.selectFrom(this)
            if (statusList != null) {
                baseStep.where(STATUS.`in`(statusList))
            }
            if (descFlag) {
                baseStep.orderBy(CREATE_TIME.desc())
            } else {
                baseStep.orderBy(CREATE_TIME.asc())
            }
            return baseStep.limit((page - 1) * pageSize, pageSize).fetch()
        }
    }

    fun updateAtomReplaceBase(
        dslContext: DSLContext,
        baseId: String,
        status: String? = null,
        userId: String
    ) {
        with(TPipelineAtomReplaceBase.T_PIPELINE_ATOM_REPLACE_BASE) {
            val baseStep = dslContext.update(this)
            if (status != null) {
                baseStep.set(STATUS, status)
            }
            baseStep.set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(baseId))
                .execute()
        }
    }
}
