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

import com.tencent.devops.model.process.tables.TPipelineGroup
import com.tencent.devops.model.process.tables.records.TPipelineGroupRecord
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

/**
 * 用户分组管理
 */
@Repository
class PipelineGroupDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        userId: String,
        id: Long? = null
    ): Long {
        logger.info("Create the pipeline group for project $projectId with name $name by user $userId")
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                CREATE_TIME,
                UPDATE_TIME,
                CREATE_USER,
                UPDATE_USER,
                ID
            )
                .values(
                    projectId,
                    name,
                    now,
                    now,
                    userId,
                    userId,
                    id
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        name: String,
        userId: String
    ): Boolean {
        logger.info("Update the pipeline group $groupId with name $name by user $userId")
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            return dslContext.update(this)
                .set(NAME, name)
                .set(UPDATE_USER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(groupId).and(PROJECT_ID.eq(projectId)))
                .execute() == 1
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        userId: String
    ): Boolean {
        logger.info("Delete the pipeline group $groupId by user $userId")
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(groupId).and(PROJECT_ID.eq(projectId)))
                .execute() == 1
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String
    ): Result<TPipelineGroupRecord> {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .fetch()
        }
    }

    fun count(
        dslContext: DSLContext,
        projectId: String
    ): Long {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun countByName(
        dslContext: DSLContext,
        projectId: String,
        name: String
    ): Long {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NAME.eq(name))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun get(dslContext: DSLContext, id: Long): TPipelineGroupRecord? {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun listByIds(dslContext: DSLContext, projectId: String, ids: Set<Long>): Result<TPipelineGroupRecord> {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ID.`in`(ids))
                .fetch()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long
    ): TPipelineGroupRecord? {
        with(TPipelineGroup.T_PIPELINE_GROUP) {
            return dslContext.selectFrom(this)
                .where(ID.eq(groupId).and(PROJECT_ID.eq(projectId)))
                .fetchOne()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineGroupDao::class.java)
    }
}
