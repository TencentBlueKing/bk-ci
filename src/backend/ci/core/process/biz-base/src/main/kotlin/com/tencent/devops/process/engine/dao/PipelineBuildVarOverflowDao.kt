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
import com.tencent.devops.model.process.tables.TPipelineBuildVarOverflow
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * 大变量溢出存储表的 DAO。
 *
 * 该表为新增表 T_PIPELINE_BUILD_VAR_OVERFLOW，DDL 见
 * `process/docs/T_PIPELINE_BUILD_VAR_OVERFLOW_MIGRATION.sql`。
 *
 * 设计要点：
 * - 该表已在现网建好，jOOQ codegen 会生成
 *   [com.tencent.devops.model.process.tables.TPipelineBuildVarOverflow]，
 *   因此直接复用生成代码，与主表 T_PIPELINE_BUILD_VAR 的 DAO 风格保持一致；
 * - 主键为 (BUILD_ID, KEY)，与主表 T_PIPELINE_BUILD_VAR 一致；
 * - 仅承担"溢出值"的物理存储，业务一致性逻辑由
 *   [com.tencent.devops.process.service.BuildVariableService] 把守。
 *
 * 注意：本 DAO 故意**不提供** `batchSave` 的单 SQL 批量实现。
 *  - 单个溢出值最大 4M（字符），N 条值合并成一条 INSERT 会迅速突破
 *    MySQL `max_allowed_packet`（生产环境通常 16M~64M）；
 *  - 因此 [batchSave] 采用"循环调用 [save] + `ON DUPLICATE KEY UPDATE`"实现，
 *    单条 IO，单次最坏内存峰值 ≤ 4M，吞吐通过整条流水线"大变量数量本就有限"自然控制。
 */
@Repository
class PipelineBuildVarOverflowDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        param: BuildParameters
    ) {
        val value = param.value.toString()
        with(TPipelineBuildVarOverflow.T_PIPELINE_BUILD_VAR_OVERFLOW) {
            dslContext.insertInto(this)
                .set(BUILD_ID, buildId)
                .set(KEY, param.key)
                .set(VALUE, value)
                .set(VALUE_LENGTH, value.length)
                .set(PROJECT_ID, projectId)
                .set(PIPELINE_ID, pipelineId)
                .set(VAR_TYPE, param.valueType?.name)
                .set(READ_ONLY, param.readOnly)
                .set(SENSITIVE, param.sensitive)
                .onDuplicateKeyUpdate()
                .set(VALUE, value)
                .set(VALUE_LENGTH, value.length)
                .set(VAR_TYPE, param.valueType?.name)
                .set(SENSITIVE, param.sensitive)
                .execute()
        }
    }

    fun batchSave(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        params: List<BuildParameters>
    ) {
        if (params.isEmpty()) return
        // 见类注释：单条 4M 值不允许聚合，逐条入库防止超过 max_allowed_packet。
        params.forEach { save(dslContext, projectId, pipelineId, buildId, it) }
    }

    /** 按需加载单个大变量真实值。 */
    fun getValue(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        key: String
    ): String? {
        with(TPipelineBuildVarOverflow.T_PIPELINE_BUILD_VAR_OVERFLOW) {
            return dslContext.select(VALUE).from(this)
                .where(BUILD_ID.eq(buildId))
                .and(KEY.eq(key))
                .and(PROJECT_ID.eq(projectId))
                .fetchOne(VALUE)
        }
    }

    fun deleteByKey(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        key: String
    ): Int {
        with(TPipelineBuildVarOverflow.T_PIPELINE_BUILD_VAR_OVERFLOW) {
            return dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(KEY.eq(key))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteByBuildId(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): Int {
        with(TPipelineBuildVarOverflow.T_PIPELINE_BUILD_VAR_OVERFLOW) {
            return dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteByPipelineId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): Int {
        with(TPipelineBuildVarOverflow.T_PIPELINE_BUILD_VAR_OVERFLOW) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }
}
