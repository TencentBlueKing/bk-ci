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

package com.tencent.devops.misc.dao.project

import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistory
import com.tencent.devops.misc.pojo.project.ProjectDataMigrateHistoryQueryParam
import com.tencent.devops.model.project.tables.TProjectDataMigrateHistory
import com.tencent.devops.model.project.tables.records.TProjectDataMigrateHistoryRecord
import com.tencent.devops.process.utils.PIPELINE_ID
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ProjectDataMigrateHistoryDao {

    fun add(dslContext: DSLContext, userId: String, projectDataMigrateHistory: ProjectDataMigrateHistory) {
        with(TProjectDataMigrateHistory.T_PROJECT_DATA_MIGRATE_HISTORY) {
            dslContext.insertInto(
                this,
                ID,
                PROJECT_ID,
                PIPELINE_ID,
                MODULE_CODE,
                SOURCE_CLUSTER_NAME,
                SOURCE_DATA_SOURCE_NAME,
                TARGET_CLUSTER_NAME,
                TARGET_DATA_SOURCE_NAME,
                TARGET_DATA_TAG,
                CREATOR,
                MODIFIER
            )
                .values(
                    projectDataMigrateHistory.id,
                    projectDataMigrateHistory.projectId,
                    projectDataMigrateHistory.pipelineId,
                    projectDataMigrateHistory.moduleCode.name,
                    projectDataMigrateHistory.sourceClusterName,
                    projectDataMigrateHistory.sourceDataSourceName,
                    projectDataMigrateHistory.targetClusterName,
                    projectDataMigrateHistory.targetDataSourceName,
                    projectDataMigrateHistory.targetDataTag,
                    userId,
                    userId
                ).onDuplicateKeyUpdate()
                .set(SOURCE_CLUSTER_NAME, projectDataMigrateHistory.sourceClusterName)
                .set(SOURCE_DATA_SOURCE_NAME, projectDataMigrateHistory.sourceDataSourceName)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun getLatestProjectDataMigrateHistory(
        dslContext: DSLContext,
        queryParam: ProjectDataMigrateHistoryQueryParam
    ): TProjectDataMigrateHistoryRecord? {
        with(TProjectDataMigrateHistory.T_PROJECT_DATA_MIGRATE_HISTORY) {
            val conditions = mutableListOf<Condition>()
            conditions.add(PROJECT_ID.eq(queryParam.projectId))
            val pipelineId = queryParam.pipelineId
            if (pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.isNull)
            } else {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            conditions.add(MODULE_CODE.eq(queryParam.moduleCode.name))
            conditions.add(TARGET_CLUSTER_NAME.eq(queryParam.targetClusterName))
            conditions.add(TARGET_DATA_SOURCE_NAME.eq(queryParam.targetDataSourceName))
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }
}
