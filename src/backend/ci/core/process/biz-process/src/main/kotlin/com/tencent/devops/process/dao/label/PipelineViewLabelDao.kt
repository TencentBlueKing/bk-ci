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

package com.tencent.devops.process.dao.label

import com.tencent.devops.model.process.tables.TPipelineViewLabel
import com.tencent.devops.model.process.tables.records.TPipelineViewLabelRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 视图和标签绑定表
 */
@Repository
class PipelineViewLabelDao {

    fun addLabel(
        dslContext: DSLContext,
        viewId: Long,
        labelId: Long
    ) {
        logger.info("Add label $labelId with view $viewId")
        with(TPipelineViewLabel.T_PIPELINE_VIEW_LABEL) {
            dslContext.insertInto(
                this,
                VIEW_ID,
                LABEL_ID,
                CREATE_TIME
            )
                .values(
                    viewId,
                    labelId,
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun addLabels(
        dslContext: DSLContext,
        viewId: Long,
        labelId: Set<Long>
    ) {
        logger.info("Add label $labelId with view $viewId")
        with(TPipelineViewLabel.T_PIPELINE_VIEW_LABEL) {
            dslContext.batch(
                labelId.map {
                    dslContext.insertInto(
                        this,
                        VIEW_ID,
                        LABEL_ID,
                        CREATE_TIME
                    )
                        .values(
                            viewId,
                            it,
                            LocalDateTime.now()
                        )
                }
            ).execute()
        }
    }

    /**
     * 视图和标签断开关联
     */
    fun detachLabel(
        dslContext: DSLContext,
        labelId: Long,
        userId: String
    ) {
        logger.info("Detach label $labelId by user $userId")
        with(TPipelineViewLabel.T_PIPELINE_VIEW_LABEL) {
            dslContext.deleteFrom(this)
                .where(LABEL_ID.eq(labelId))
                .execute()
        }
    }

    fun detachLabel(
        dslContext: DSLContext,
        labelId: Set<Long>,
        userId: String
    ) {
        logger.info("Detach label $labelId by user $userId")
        with(TPipelineViewLabel.T_PIPELINE_VIEW_LABEL) {
            dslContext.deleteFrom(this)
                .where(LABEL_ID.`in`(labelId))
                .execute()
        }
    }

    fun detachLabelByView(
        dslContext: DSLContext,
        viewId: Long,
        userId: String
    ) {
        logger.info("Detach the labels by view $viewId by user $userId")
        with(TPipelineViewLabel.T_PIPELINE_VIEW_LABEL) {
            dslContext.deleteFrom(this)
                .where(VIEW_ID.eq(viewId))
                .execute()
        }
    }

    fun getLabels(
        dslContext: DSLContext,
        viewId: Long
    ): Result<TPipelineViewLabelRecord> {
        with(TPipelineViewLabel.T_PIPELINE_VIEW_LABEL) {
            return dslContext.selectFrom(this)
                .where(VIEW_ID.eq(viewId))
                .fetch()
        }
    }

    fun getLabels(
        dslContext: DSLContext,
        viewId: Set<Long>
    ): Result<TPipelineViewLabelRecord> {
        with(TPipelineViewLabel.T_PIPELINE_VIEW_LABEL) {
            return dslContext.selectFrom(this)
                .where(VIEW_ID.`in`(viewId))
                .fetch()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineViewLabelDao::class.java)
    }
}