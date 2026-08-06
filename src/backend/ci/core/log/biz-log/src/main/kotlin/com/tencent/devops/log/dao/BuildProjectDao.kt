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

package com.tencent.devops.log.dao

import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

/**
 * 使用原生 SQL 操作 T_LOG_BUILD_PROJECT 表（无 JOOQ codegen 依赖）。
 * 表可能尚未创建（增量 DDL 未执行），所有 DB 操作均容忍失败并降级为 warn 日志。
 */
@Repository
class BuildProjectDao(
    private val dslContext: DSLContext
) {

    fun getProjectId(buildId: String): String? {
        return try {
            dslContext.fetch(
                "SELECT PROJECT_ID FROM T_LOG_BUILD_PROJECT WHERE BUILD_ID = ?",
                buildId
            ).firstOrNull()?.get("PROJECT_ID", String::class.java)
        } catch (e: Exception) {
            logger.warn("BuildProjectDao.getProjectId failed for buildId={}: {}", buildId, e.message)
            null
        }
    }

    fun upsert(buildId: String, projectId: String) {
        try {
            dslContext.execute(
                """INSERT INTO T_LOG_BUILD_PROJECT (BUILD_ID, PROJECT_ID)
                   VALUES (?, ?)
                   ON DUPLICATE KEY UPDATE PROJECT_ID = VALUES(PROJECT_ID)""",
                buildId,
                projectId
            )
        } catch (e: Exception) {
            logger.warn("BuildProjectDao.upsert failed for buildId={}, projectId={}: {}", buildId, projectId, e.message)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildProjectDao::class.java)
    }
}
