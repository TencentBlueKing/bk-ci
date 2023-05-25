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

import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.model.process.Tables.T_PIPELINE_BUILD_VAR
import org.jooq.DSLContext
import org.jooq.util.mysql.MySQLDSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class PipelineBuildVarDao @Autowired constructor() {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        name: String,
        value: Any
    ) {

        with(T_PIPELINE_BUILD_VAR) {
            dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                BUILD_ID,
                KEY,
                VALUE
            )
                .values(projectId, pipelineId, buildId, name, value.toString())
                .onDuplicateKeyUpdate()
                .set(PIPELINE_ID, pipelineId)
                .set(VALUE, value.toString())
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        name: String,
        value: Any,
        valueType: String? = null
    ): Int {
        with(T_PIPELINE_BUILD_VAR) {
            val baseStep = dslContext.update(this)
            if (valueType != null) {
                baseStep.set(VAR_TYPE, valueType)
            }
            return baseStep.set(VALUE, value.toString())
                .where(
                    BUILD_ID.eq(buildId).and(KEY.eq(name)).and(READ_ONLY.isNull.or(READ_ONLY.eq(false)))
                        .and(PROJECT_ID.eq(projectId))
                )
                .execute()
        }
    }

    fun getVars(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        key: String? = null
    ): MutableMap<String, String> {

        with(T_PIPELINE_BUILD_VAR) {
            val where = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
            if (key != null) {
                where.and(KEY.eq(key))
            }
            val result = where.fetch()
            val map = mutableMapOf<String, String>()
            result.forEach {
                map[it[KEY]] = it[VALUE]
            }
            return map
        }
    }

    fun getVarsWithType(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        key: String? = null
    ): List<BuildParameters> {

        with(T_PIPELINE_BUILD_VAR) {
            val where = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
            if (key != null) {
                where.and(KEY.eq(key))
            }
            val result = where.fetch()
            val list = mutableListOf<BuildParameters>()
            result.forEach {
                if (it.varType != null) {
                    list.add(BuildParameters(it.key, it.value, BuildFormPropertyType.valueOf(it.varType)))
                } else {
                    list.add(BuildParameters(it.key, it.value))
                }
            }
            return list
        }
    }

    fun deleteBuildVar(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        varName: String? = null,
        readOnly: Boolean? = null
    ): Int {
        return with(T_PIPELINE_BUILD_VAR) {
            val delete = dslContext.delete(this).where(BUILD_ID.eq(buildId).and(PROJECT_ID.eq(projectId)))
            if (!varName.isNullOrBlank()) {
                delete.and(KEY.eq(varName))
            }
            if (readOnly != null) {
                delete.and(READ_ONLY.eq(readOnly))
            }
            delete.execute()
        }
    }

    fun batchSave(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        variables: List<BuildParameters>
    ) {
        with(T_PIPELINE_BUILD_VAR) {
            val maxLength = VALUE.dataType.length()
            dslContext.insertInto(this, BUILD_ID, KEY, VALUE, PROJECT_ID, PIPELINE_ID, VAR_TYPE, READ_ONLY)
                .also {
                    variables.forEach { v ->
                        val valueString = v.value.toString()
                        if (valueString.length > maxLength) {
                            LOG.warn("$buildId|ABANDON_DATA|len[${v.key}]=${valueString.length}(max=$maxLength)")
                            return@forEach
                        }
                        it.values(
                            buildId,
                            v.key,
                            valueString,
                            projectId,
                            pipelineId,
                            v.valueType?.name ?: "STRING",
                            v.readOnly
                        )
                    }
                }
                .onDuplicateKeyUpdate()
                .set(VALUE, MySQLDSL.values(VALUE))
                .set(VAR_TYPE, MySQLDSL.values(VAR_TYPE))
                .execute()
        }
    }

    fun batchUpdate(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        variables: List<BuildParameters>
    ) {
        with(T_PIPELINE_BUILD_VAR) {
            variables.forEach { v ->
                val baseStep = dslContext.update(this)
                val valueType = v.valueType
                if (valueType != null) {
                    baseStep.set(VAR_TYPE, valueType.name)
                }
                baseStep.set(VALUE, v.value.toString()).where(
                    BUILD_ID.eq(buildId).and(KEY.eq(v.key)).and(
                        READ_ONLY.notEqual(true).and(PROJECT_ID.eq(projectId))
                    )
                ).execute()
            }
        }
    }

    fun deleteBuildVars(dslContext: DSLContext, projectId: String, pipelineId: String, buildId: String) {
        return with(T_PIPELINE_BUILD_VAR) {
            dslContext.delete(this).where(PROJECT_ID.eq(projectId)).and(BUILD_ID.eq(buildId))
                .and(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    fun deletePipelineBuildVar(dslContext: DSLContext, projectId: String, pipelineId: String) {
        return with(T_PIPELINE_BUILD_VAR) {
            dslContext.delete(this).where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId)).execute()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PipelineBuildVarDao::class.java)
    }
}
