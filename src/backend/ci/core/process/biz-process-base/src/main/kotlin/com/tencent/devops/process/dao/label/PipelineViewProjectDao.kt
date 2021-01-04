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

import com.tencent.devops.model.process.tables.TPipelineViewProject
import com.tencent.devops.model.process.tables.records.TPipelineViewProjectRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 用户当前使用视图
 */
@Repository
class PipelineViewProjectDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        projectId: String,
        viewId: Long
    ) {
        with(TPipelineViewProject.T_PIPELINE_VIEW_PROJECT) {
            logger.info("Create the user view $viewId of project $projectId by user $userId")
            val now = LocalDateTime.now()
            dslContext.insertInto(
                this,
                VIEW_ID,
                PROJECT_ID,
                CREATE_TIME,
                UPDATE_TIME,
                CREATE_USER
            )
                .values(
                    viewId,
                    projectId,
                    now,
                    now,
                    userId
                )
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long,
        userId: String
    ): Int {
        logger.info("Delete the view $id by user $userId")
        with(TPipelineViewProject.T_PIPELINE_VIEW_PROJECT) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        userId: String,
        projectId: String
    ) {
        with(TPipelineViewProject.T_PIPELINE_VIEW_PROJECT) {
            dslContext.deleteFrom(this)
                .where(CREATE_USER.eq(userId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }

    fun deleteByView(
        dslContext: DSLContext,
        viewId: Long,
        userId: String
    ): Int {
        logger.info("Delete the view of viewId $viewId by user $userId")
        with(TPipelineViewProject.T_PIPELINE_VIEW_PROJECT) {
            return dslContext.deleteFrom(this)
                .where(VIEW_ID.eq(viewId))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        viewId: Long,
        userId: String
    ): Int {
        logger.info("Update the project view $projectId with viewId $viewId by user $userId")
        with(TPipelineViewProject.T_PIPELINE_VIEW_PROJECT) {
            return dslContext.update(this)
                .set(VIEW_ID, viewId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PROJECT_ID.eq(projectId))
                .and(CREATE_USER.eq(userId))
                .execute()
        }
    }

    fun get(
        dslContext: DSLContext,
        projectId: String,
        userId: String
    ): TPipelineViewProjectRecord? {
        with(TPipelineViewProject.T_PIPELINE_VIEW_PROJECT) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(CREATE_USER.eq(userId))
                .fetchOne()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineViewProjectDao::class.java)
    }
}