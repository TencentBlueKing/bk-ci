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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_SENSITIVE_VALUE
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class PipelineBuildSensitiveValueDao @Autowired constructor() {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        value: String
    ) {

        with(T_PIPELINE_BUILD_SENSITIVE_VALUE) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VALUE
            )
                .values(projectId, pipelineId, buildId, value)
                .onDuplicateKeyUpdate()
                .set(PIPELINE_ID, pipelineId)
                .set(VALUE, value)
                .execute()
        }
    }

    fun batchSave(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        values: Set<String>
    ) {
        with(T_PIPELINE_BUILD_SENSITIVE_VALUE) {
            val maxLength = VALUE.dataType.length()
            values.forEach { v ->
                if (v.length > maxLength) {
                    LOG.error("$buildId|ABANDON_DATA|len[${v.length}(max=$maxLength)")
                    return@forEach
                }
                dslContext.insertInto(this)
                    .set(PROJECT_ID, projectId)
                    .set(PIPELINE_ID, pipelineId)
                    .set(BUILD_ID, buildId)
                    .set(VALUE, v)
                    .onDuplicateKeyUpdate()
                    .set(VALUE, v)
                    .execute()
            }
        }
    }

    fun batchUpdate(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        values: List<String>
    ) {
        with(T_PIPELINE_BUILD_SENSITIVE_VALUE) {
            values.forEach { v ->
                dslContext.update(this)
                    .set(BUILD_ID, buildId)
                    .set(VALUE, v)
                    .where(
                        BUILD_ID.eq(buildId).and(VALUE.eq(v)).and(PROJECT_ID.eq(projectId))
                    ).execute()
            }
        }
    }

    fun getValues(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): List<String> {

        with(T_PIPELINE_BUILD_SENSITIVE_VALUE) {
            val where = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
            val result = where.fetch()
            val list = mutableListOf<String>()
            result.forEach {
                list.add(it[VALUE])
            }
            return list
        }
    }

    fun deletePipelineSensitiveValues(dslContext: DSLContext, projectId: String, pipelineId: String) {
        return with(T_PIPELINE_BUILD_SENSITIVE_VALUE) {
            dslContext.delete(this).where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PipelineBuildSensitiveValueDao::class.java)
    }
}
