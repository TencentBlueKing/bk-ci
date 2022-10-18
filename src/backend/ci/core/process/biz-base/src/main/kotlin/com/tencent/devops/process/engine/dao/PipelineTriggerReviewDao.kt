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

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.process.Tables.T_PIPELINE_TRIGGER_REVIEW
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineTriggerReviewDao {

    fun createReviewRecord(
        dslContext: DSLContext,
        buildId: String,
        pipelineId: String,
        projectId: String,
        reviewers: List<String>
    ): Int {
        with(T_PIPELINE_TRIGGER_REVIEW) {
            return dslContext.insertInto(this)
                .set(BUILD_ID, buildId)
                .set(PIPELINE_ID, pipelineId)
                .set(PROJECT_ID, projectId)
                .set(TRIGGER_REVIEWER, JsonUtil.toJson(reviewers, false))
                .execute()
        }
    }

    fun updateOperator(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String
    ): Int {
        with(T_PIPELINE_TRIGGER_REVIEW) {
            return dslContext.update(this)
                .set(TRIGGER_OPERATOR, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(
                    PROJECT_ID.eq(projectId).and(PIPELINE_ID.eq(pipelineId).and(BUILD_ID.eq(buildId)))
                )
                .execute()
        }
    }

    fun getTriggerReviewers(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<String>? {
        with(T_PIPELINE_TRIGGER_REVIEW) {
            val record = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(PROJECT_ID.eq(projectId))
                .fetchAny()
            return record?.triggerReviewer?.let { self ->
                JsonUtil.getObjectMapper().readValue(self) as List<String>
            }
        }
    }
}
