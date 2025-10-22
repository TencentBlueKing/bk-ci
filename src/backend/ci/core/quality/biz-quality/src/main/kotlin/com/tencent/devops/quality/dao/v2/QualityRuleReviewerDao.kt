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

package com.tencent.devops.quality.dao.v2

import com.tencent.devops.model.quality.tables.TQualityRuleReviewer
import com.tencent.devops.model.quality.tables.records.TQualityRuleReviewerRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QualityRuleReviewerDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        ruleId: Long,
        reviewer: String
    ) {
        with(TQualityRuleReviewer.T_QUALITY_RULE_REVIEWER) {
            dslContext.insertInto(
                this,
                this.PROJECT_ID,
                this.PIPELINE_ID,
                this.BUILD_ID,
                this.RULE_ID,
                this.REVIEWER,
                this.REVIEW_TIME
            ).values(
                projectId,
                pipelineId,
                buildId,
                ruleId,
                reviewer,
                LocalDateTime.now()
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        ruleId: Long,
        reviewer: String
    ) {
        with(TQualityRuleReviewer.T_QUALITY_RULE_REVIEWER) {
            dslContext.update(this)
                .set(REVIEWER, reviewer)
                .set(REVIEW_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .and(RULE_ID.eq(ruleId))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        ruleId: Long
    ): TQualityRuleReviewerRecord? {
        with(TQualityRuleReviewer.T_QUALITY_RULE_REVIEWER) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(BUILD_ID.eq(buildId))
                .and(RULE_ID.eq(ruleId))
                .fetchOne()
        }
    }
}
