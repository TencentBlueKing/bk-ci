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

package com.tencent.devops.process.dao.label

import com.tencent.devops.model.process.tables.TPipelineLabel
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

/**
 * 流水线标签
 */
@Suppress("LongParameterList")
@Repository
class PipelineLabelDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        name: String,
        userId: String,
        id: Long? = null
    ) {
        logger.info("Create the pipeline label for group $groupId with name $name by user $userId")
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                PROJECT_ID,
                GROUP_ID,
                NAME,
                CREATE_TIME,
                UPDATE_TIME,
                CREATE_USER,
                UPDATE_USER,
                ID
            )
                .values(
                    projectId,
                    groupId,
                    name,
                    now,
                    now,
                    userId,
                    userId,
                    id
                )
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
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
                .where(ID.eq(labelId).and(PROJECT_ID.eq(projectId)))
                .execute() == 1
        }
    }

    fun deleteById(
        dslContext: DSLContext,
        projectId: String,
        labelId: Long,
        userId: String
    ): Boolean {
        logger.info("Delete the label $labelId by user $userId")
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(labelId).and(PROJECT_ID.eq(projectId)))
                .execute() == 1
        }
    }

    fun deleteByGroupId(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        userId: String
    ): Int {
        logger.info("Delete the group $groupId by user $userId")
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.deleteFrom(this)
                .where(GROUP_ID.eq(groupId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun countByGroupId(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long
    ): Long {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.selectCount().from(this)
                .where(GROUP_ID.eq(groupId).and(PROJECT_ID.eq(projectId)))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun countByGroupName(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        name: String
    ): Long {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.selectCount().from(this)
                .where(GROUP_ID.eq(groupId).and(PROJECT_ID.eq(projectId)).and(NAME.eq(name)))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getByGroupIds(
        dslContext: DSLContext,
        projectId: String,
        groupId: Set<Long>
    ): Result<TPipelineLabelRecord> {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.selectFrom(this)
                .where(GROUP_ID.`in`(groupId).and(PROJECT_ID.eq(projectId)))
                .fetch()
        }
    }

    fun getById(
        dslContext: DSLContext,
        projectId: String,
        id: Long
    ): TPipelineLabelRecord? {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .and(PROJECT_ID.eq(projectId))
                .fetchAny()
        }
    }

    fun getByIds(
        dslContext: DSLContext,
        projectId: String,
        ids: Set<Long>
    ): Result<TPipelineLabelRecord> {
        with(TPipelineLabel.T_PIPELINE_LABEL) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(ids).and(PROJECT_ID.eq(projectId)))
                .fetch()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineLabelDao::class.java)
    }
}
