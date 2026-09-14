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

import com.tencent.devops.model.process.tables.TPipelineBuildHistoryParamOverflow
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

/**
 * 构建启动参数大值溢出表 DAO（长期载体）。
 *
 * 该表 T_PIPELINE_BUILD_HISTORY_PARAM_OVERFLOW 见
 * `process/docs/T_PIPELINE_BUILD_HISTORY_PARAM_OVERFLOW_MIGRATION.sql`。
 *
 * 设计要点：
 * - 主键 (BUILD_ID, KEY)，**不分区**：保留周期与构建历史一致，随历史按 buildId 清理 / 归档迁移；
 *   因此可安全使用 `INSERT ... ON DUPLICATE KEY UPDATE`（不像 VAR 溢出表那样 CREATE_TIME 进主键）；
 * - 仅承担"启动参数大值"的物理存储，引用协议（写引用 / 读解析）的一致性由
 *   [com.tencent.devops.process.service.BuildStartupParamOverflowService] 把守；
 * - 单条 VALUE 最大 4M（字符），故 [batchSave] 逐条写入，避免突破 MySQL `max_allowed_packet`。
 */
@Repository
class PipelineBuildHistoryParamOverflowDao {

    @Suppress("LongParameterList")
    fun save(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        key: String,
        value: String,
        debug: Boolean
    ) {
        with(TPipelineBuildHistoryParamOverflow.T_PIPELINE_BUILD_HISTORY_PARAM_OVERFLOW) {
            dslContext.insertInto(this)
                .set(BUILD_ID, buildId)
                .set(KEY, key)
                .set(VALUE, value)
                .set(VALUE_LENGTH, value.length)
                .set(PROJECT_ID, projectId)
                .set(PIPELINE_ID, pipelineId)
                .set(DEBUG, debug)
                .onDuplicateKeyUpdate()
                .set(VALUE, value)
                .set(VALUE_LENGTH, value.length)
                .execute()
        }
    }

    fun getValue(
        dslContext: DSLContext,
        projectId: String,
        buildId: String,
        key: String
    ): String? {
        with(TPipelineBuildHistoryParamOverflow.T_PIPELINE_BUILD_HISTORY_PARAM_OVERFLOW) {
            return dslContext.select(VALUE).from(this)
                .where(BUILD_ID.eq(buildId))
                .and(KEY.eq(key))
                .and(PROJECT_ID.eq(projectId))
                .fetchAny(VALUE)
        }
    }

    fun deleteByBuildId(
        dslContext: DSLContext,
        projectId: String,
        buildId: String
    ): Int {
        with(TPipelineBuildHistoryParamOverflow.T_PIPELINE_BUILD_HISTORY_PARAM_OVERFLOW) {
            return dslContext.deleteFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }
}
