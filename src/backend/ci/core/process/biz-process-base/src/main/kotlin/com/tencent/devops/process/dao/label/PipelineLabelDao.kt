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

import com.tencent.devops.model.process.tables.TPipelineLabel
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 流水线标签
 */
@Repository
class PipelineLabelDao {

    fun create(
        dslContext: DSLContext,
        groupId: Long,
        name: String,
        userId: String
    ) {
        logger.info("Create the pipeline label for group $groupId with name $name by user $userId")
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                GROUP_ID,
                NAME,
                CREATE_TIME,
                UPDATE_TIME,
                CREATE_USER,
                UPDATE_USER
            )
                .values(
                    groupId,
                    name,
                    now,
                    now,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        labelId: Long,
        name: String,
        userId: String
    ): Boolean {
        logger.info("Update the pipeline label $labelId with name $name by user $userId")
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.update(this)
                .set(NAME, name)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(UPDATE_USER, userId)
                .where(ID.eq(labelId))
                .execute() == 1
        }
    }

    fun deleteById(
        dslContext: DSLContext,
        labelId: Long,
        userId: String
    ): Boolean {
        logger.info("Delete the label $labelId by user $userId")
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(labelId))
                .execute() == 1
        }
    }

    fun deleteByGroupId(
        dslContext: DSLContext,
        groupId: Long,
        userId: String
    ): Int {
        logger.info("Delete the group $groupId by user $userId")
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.deleteFrom(this)
                .where(GROUP_ID.eq(groupId))
                .execute()
        }
    }

    fun getByGroupId(
        dslContext: DSLContext,
        groupId: Long
    ): Result<TPipelineLabelRecord> {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.selectFrom(this)
                .where(GROUP_ID.eq(groupId))
                .fetch()
        }
    }

    fun getByGroupIds(
        dslContext: DSLContext,
        groupId: Set<Long>
    ): Result<TPipelineLabelRecord> {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.selectFrom(this)
                .where(GROUP_ID.`in`(groupId))
                .fetch()
        }
    }

    fun getByIds(
        dslContext: DSLContext,
        ids: Set<Long>
    ): Result<TPipelineLabelRecord> {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(ids))
                .fetch()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineLabelDao::class.java)
    }
}