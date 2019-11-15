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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.codecc.dao

import com.tencent.devops.model.plugin.tables.TPluginCodeccElement
import com.tencent.devops.model.plugin.tables.records.TPluginCodeccElementRecord
import com.tencent.devops.plugin.codecc.pojo.CodeccElementData
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class CodeccElementDao {

    fun save(dslContext: DSLContext, data: CodeccElementData): Int {
        with(TPluginCodeccElement.T_PLUGIN_CODECC_ELEMENT) {
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                PIPELINE_ID,
                TASK_NAME,
                TASK_CN_NAME,
                TASK_ID,
                IS_SYNC,
                SCAN_TYPE,
                LANGUAGE,
                PLATFORM,
                TOOLS,
                PY_VERSION,
                ESLINT_RC,
                CODE_PATH,
                SCRIPT_TYPE,
                SCRIPT,
                CHANNEL_CODE,
                UPDATE_USER_ID,
                IS_DELETE,
                UPDATE_TIME
            )
                .values(
                    data.projectId,
                    data.pipelineId,
                    data.taskName,
                    data.taskCnName,
                    data.taskId,
                    data.sync,
                    data.scanType,
                    data.language,
                    data.platform,
                    data.tools,
                    data.pythonVersion,
                    data.eslintRc,
                    data.codePath,
                    data.scriptType,
                    data.script,
                    data.channelCode,
                    data.updateUserId,
                    "0",
                    LocalDateTime.now()
                )
                .onDuplicateKeyUpdate()
                .set(TASK_NAME, data.taskName)
                .set(TASK_CN_NAME, data.taskCnName)
                .set(TASK_ID, data.taskId)
                .set(IS_SYNC, data.sync)
                .set(SCAN_TYPE, data.scanType)
                .set(LANGUAGE, data.language)
                .set(PLATFORM, data.platform)
                .set(TOOLS, data.tools)
                .set(PY_VERSION, data.pythonVersion)
                .set(ESLINT_RC, data.eslintRc)
                .set(CODE_PATH, data.codePath)
                .set(SCRIPT_TYPE, data.scriptType)
                .set(SCRIPT, data.script)
                .set(CHANNEL_CODE, data.channelCode)
                .set(UPDATE_USER_ID, data.updateUserId)
                .set(IS_DELETE, "0")
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        with(TPluginCodeccElement.T_PLUGIN_CODECC_ELEMENT) {
            return dslContext.update(this)
                .set(IS_DELETE, "1")
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, projectId: String, pipelineId: String): TPluginCodeccElementRecord? {
        with(TPluginCodeccElement.T_PLUGIN_CODECC_ELEMENT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId)))
                .fetchOne()
        }
    }
}