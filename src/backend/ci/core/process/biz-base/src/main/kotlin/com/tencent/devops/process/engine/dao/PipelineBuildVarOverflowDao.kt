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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.pipeline.pojo.BuildParameters
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

/**
 * 大变量溢出存储表的 DAO。
 *
 * 该表为新增表 [T_PIPELINE_BUILD_VAR_OVERFLOW]，DDL 见
 * `process/docs/T_PIPELINE_BUILD_VAR_OVERFLOW_MIGRATION.sql`。
 *
 * 设计要点：
 * - 不依赖 jOOQ 自动生成代码，完全通过 [DSL] 字段引用，避免不同环境
 *   未及时重新生成 model-process 模块的代码导致编译失败；
 * - 主键为 (BUILD_ID, KEY)，与主表 T_PIPELINE_BUILD_VAR 一致；
 * - 仅承担"溢出值"的物理存储，业务一致性逻辑由 [com.tencent.devops.process.service.BuildVariableService] 把守。
 */
@Repository
class PipelineBuildVarOverflowDao {

    private val table = DSL.table(DSL.name(TABLE_NAME))
    private val fBuildId = DSL.field(DSL.name("BUILD_ID"), String::class.java)
    private val fKey = DSL.field(DSL.name("KEY"), String::class.java)
    private val fValue = DSL.field(DSL.name("VALUE"), String::class.java)
    private val fValueLength = DSL.field(DSL.name("VALUE_LENGTH"), Int::class.java)
    private val fProjectId = DSL.field(DSL.name("PROJECT_ID"), String::class.java)
    private val fPipelineId = DSL.field(DSL.name("PIPELINE_ID"), String::class.java)
    private val fVarType = DSL.field(DSL.name("VAR_TYPE"), String::class.java)
    private val fReadOnly = DSL.field(DSL.name("READ_ONLY"), Boolean::class.java)
    private val fSensitive = DSL.field(DSL.name("SENSITIVE"), Boolean::class.java)

    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        param: BuildParameters
    ) {
        val value = param.value.toString()
        dslContext.insertInto(table)
            .set(fBuildId, buildId)
            .set(fKey, param.key)
            .set(fValue, value)
            .set(fValueLength, value.length)
            .set(fProjectId, projectId)
            .set(fPipelineId, pipelineId)
            .set(fVarType, param.valueType?.name)
            .set(fReadOnly, param.readOnly)
            .set(fSensitive, param.sensitive)
            .onDuplicateKeyUpdate()
            .set(fValue, value)
            .set(fValueLength, value.length)
            .set(fVarType, param.valueType?.name)
            .set(fSensitive, param.sensitive)
            .execute()
    }

    fun batchSave(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        params: List<BuildParameters>
    ) {
        if (params.isEmpty()) return
        params.forEach { save(dslContext, projectId, pipelineId, buildId, it) }
    }

    /** 按需加载单个大变量真实值。 */
    fun getValue(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        key: String
    ): String? {
        return dslContext.select(fValue).from(table)
            .where(fBuildId.eq(buildId))
            .and(fKey.eq(key))
            .and(fProjectId.eq(projectId))
            .fetchOne(fValue)
    }

    /** 一次取多个大变量值，主要用于"必须立即聚合"的极少数场景。 */
    fun getValues(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        keys: Collection<String>
    ): Map<String, String> {
        if (keys.isEmpty()) return emptyMap()
        val result = dslContext.select(fKey, fValue).from(table)
            .where(fBuildId.eq(buildId))
            .and(fProjectId.eq(projectId))
            .and(fKey.`in`(keys))
            .fetch()
        val map = mutableMapOf<String, String>()
        result.forEach { rec ->
            val k = rec.get(fKey)
            val v = rec.get(fValue)
            if (k != null && v != null) {
                map[k] = v
            }
        }
        return map
    }

    /** 仅返回此构建中存在溢出的 KEY 列表，用于构建懒加载快照。 */
    fun listKeys(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): Set<String> {
        return dslContext.select(fKey).from(table)
            .where(fBuildId.eq(buildId))
            .and(fProjectId.eq(projectId))
            .fetch(fKey)
            .filterNotNull()
            .toSet()
    }

    fun deleteByKey(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        key: String
    ): Int {
        return dslContext.deleteFrom(table)
            .where(fBuildId.eq(buildId))
            .and(fKey.eq(key))
            .and(fProjectId.eq(projectId))
            .execute()
    }

    fun deleteByBuildId(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): Int {
        return dslContext.deleteFrom(table)
            .where(fBuildId.eq(buildId))
            .and(fProjectId.eq(projectId))
            .execute()
    }

    fun deleteByBuildIds(
        dslContext: DSLContext,
        projectId: String,
        buildIds: List<String>
    ): Int {
        if (buildIds.isEmpty()) return 0
        return dslContext.deleteFrom(table)
            .where(fProjectId.eq(projectId))
            .and(fBuildId.`in`(buildIds))
            .execute()
    }

    fun deleteByPipelineId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        return dslContext.deleteFrom(table)
            .where(fProjectId.eq(projectId))
            .and(fPipelineId.eq(pipelineId))
            .execute()
    }

    companion object {
        const val TABLE_NAME = "T_PIPELINE_BUILD_VAR_OVERFLOW"
    }
}
