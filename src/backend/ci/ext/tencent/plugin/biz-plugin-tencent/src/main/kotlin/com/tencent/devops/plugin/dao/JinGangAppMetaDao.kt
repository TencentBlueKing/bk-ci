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

package com.tencent.devops.plugin.dao

import com.tencent.devops.model.plugin.tables.TJingangMeta
import com.tencent.devops.model.plugin.tables.records.TJingangMetaRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class JinGangAppMetaDao {
    fun getMeta(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        name: String
    ): TJingangMetaRecord? {

        with(TJingangMeta.T_JINGANG_META) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId)
                            .and(PIPELINE_ID.eq(pipelineId))
                            .and(NAME.eq(name)))
                    .fetchOne()
        }
    }

    fun incRunCount(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        with(TJingangMeta.T_JINGANG_META) {
            val value = dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId)
                            .and(PIPELINE_ID.eq(pipelineId))
                            .and(NAME.eq("jingang.run.count")))
                    .fetchOne()
                    ?.value?.toInt() ?: 0

            return dslContext.insertInto(this,
                    NAME,
                    PROJECT_ID,
                    PIPELINE_ID,
                    VALUE)
                    .values(
                            "jingang.run.count",
                            projectId,
                            pipelineId,
                            (value + 1).toString())
                    .onDuplicateKeyUpdate()
                    .set(VALUE, (value + 1).toString())
                    .execute()
        }
    }
}
