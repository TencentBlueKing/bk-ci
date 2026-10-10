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
 * - 主键为 (BUILD_ID, KEY, CREATE_TIME)：CREATE_TIME 进主键是为了让本表能按
 *   CREATE_TIME 做 RANGE 分区（MySQL 要求分区列必须属于每一个唯一键），
 *   从而支持"按月 DROP PARTITION"的高效清理；
 * - 仅承担"溢出值"的物理存储，业务一致性逻辑由
 *   [com.tencent.devops.process.service.BuildVariableService] 把守。
 *
 * 关于写入的 upsert 语义（**重要**）：
 *  - 因为 CREATE_TIME（默认 CURRENT_TIMESTAMP(3)）进了主键，旧的
 *    `INSERT ... ON DUPLICATE KEY UPDATE` 在"同一 (BUILD_ID, KEY) 重复写入"时
 *    会因 CREATE_TIME 不同而**判定为不冲突 → 插入重复行**；
 *  - 因此 [save] 改为"**先 UPDATE，命中 0 行再 INSERT**"：既保证同一变量逻辑唯一，
 *    又保留首次写入的 CREATE_TIME（分区归属稳定）。写入均在
 *    [com.tencent.devops.process.service.BuildVariableService] 的
 *    PipelineBuildVarLock(buildId[, key]) RedisLock 内串行，无并发竞态。
 *
 * 注意：本 DAO 故意**不提供** `batchSave` 的单 SQL 批量实现。
 *  - 单个溢出值最大 4M（字符），N 条值合并成一条 INSERT 会迅速突破
 *    MySQL `max_allowed_packet`（生产环境通常 16M~64M）；
 *  - 因此 [batchSave] 采用"循环调用 [save]"实现，
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
            // CREATE_TIME 进主键后不能再用 ON DUPLICATE KEY UPDATE（新 CREATE_TIME 不冲突会插重复行）。
            // 改为"先 UPDATE 命中则结束，否则 INSERT"，保留首次 CREATE_TIME，保证逻辑唯一。
            val updated = dslContext.update(this)
                .set(VALUE, value)
                .set(VALUE_LENGTH, value.length)
                .set(VAR_TYPE, param.valueType?.name)
                .set(SENSITIVE, param.sensitive)
                .where(BUILD_ID.eq(buildId))
                .and(KEY.eq(param.key))
                .and(PROJECT_ID.eq(projectId))
                .execute()
            if (updated == 0) {
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
                    .execute()
            }
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
            // 正常情况下 (BUILD_ID, KEY) 仅一行；orderBy CREATE_TIME desc + limit(1) 仅作防御，
            // 避免历史脏数据（多 CREATE_TIME 行）导致 fetchOne 抛错，并始终取最新值。
            return dslContext.select(VALUE).from(this)
                .where(BUILD_ID.eq(buildId))
                .and(KEY.eq(key))
                .and(PROJECT_ID.eq(projectId))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
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
