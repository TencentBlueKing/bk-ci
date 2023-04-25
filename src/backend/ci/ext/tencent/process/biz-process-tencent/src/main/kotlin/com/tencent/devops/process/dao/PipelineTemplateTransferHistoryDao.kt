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

package com.tencent.devops.process.dao

import com.tencent.devops.model.process.tables.TTemplateTransferHistory
import com.tencent.devops.model.process.tables.records.TTemplateTransferHistoryRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTemplateTransferHistoryDao {

    fun save(
        dslContext: DSLContext,
        projectId: String,
        templateId: String,
        userId: String,
        sourceVersion: Long,
        targetVersion: Long = 0,
        log: String?
    ): Int {
        with(TTemplateTransferHistory.T_TEMPLATE_TRANSFER_HISTORY) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(this,
                PROJECT_ID,
                TEMPLATE_ID,
                USER_ID,
                SOURCE_VERSION,
                TARGET_VERSION,
                LOG, UPDATE_TIME)
                .values(
                    projectId,
                    templateId,
                    userId,
                    sourceVersion,
                    targetVersion,
                    log,
                    now
                )
                .onDuplicateKeyUpdate()
                .set(USER_ID, userId)
                .set(SOURCE_VERSION, sourceVersion)
                .set(TARGET_VERSION, targetVersion)
                .set(LOG, log)
                .set(UPDATE_TIME, now)
                .execute()
        }
    }

    fun list(dslContext: DSLContext, projectId: String, templateIds: List<String>?, offset: Int, limit: Int = 50): Result<TTemplateTransferHistoryRecord> {
        with(TTemplateTransferHistory.T_TEMPLATE_TRANSFER_HISTORY) {
            val query = dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
            if (templateIds != null && templateIds.isNotEmpty()) {
                query.and(TEMPLATE_ID.`in`(templateIds))
            }
            return query.offset(offset).limit(limit).fetch()
        }
    }
}
