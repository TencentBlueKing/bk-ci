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

package com.tencent.devops.process.dao.`var`

import com.tencent.devops.model.process.tables.TPipelinePublicVarGroupReleaseRecord
import com.tencent.devops.process.pojo.`var`.po.PipelinePublicVarGroupReleaseRecordPO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelinePublicVarGroupReleseRecordDao {

    fun batchInsert(dslContext: DSLContext, records: List<PipelinePublicVarGroupReleaseRecordPO>) {
        with(TPipelinePublicVarGroupReleaseRecord.T_PIPELINE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            val insertValuesStep = records.map { record ->
                dslContext.insertInto(
                    this,
                    ID,
                    PROJECT_ID,
                    GROUP_NAME,
                    VERSION,
                    PUBLISHER,
                    PUB_TIME,
                    DESC,
                    CONTENT,
                    CREATOR,
                    MODIFIER,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    record.id,
                    record.projectId,
                    record.groupName,
                    record.version,
                    record.publisher,
                    record.pubTime,
                    record.desc,
                    record.content,
                    record.creator,
                    record.modifier,
                    record.createTime,
                    record.updateTime
                )
            }
            dslContext.batch(insertValuesStep).execute()
        }
    }

    fun deleteByGroupName(dslContext: DSLContext, projectId: String, groupName: String) {
        with(TPipelinePublicVarGroupReleaseRecord.T_PIPELINE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .execute()
        }
    }

    fun countByGroupName(dslContext: DSLContext, projectId: String, groupName: String): Long {
        with(TPipelinePublicVarGroupReleaseRecord.T_PIPELINE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .fetchOne(0, Long::class.java) ?: 0
        }
    }

    fun listByGroupNamePage(
        dslContext: DSLContext,
        projectId: String,
        groupName: String,
        page: Int,
        pageSize: Int
    ): List<PipelinePublicVarGroupReleaseRecordPO> {
        with(TPipelinePublicVarGroupReleaseRecord.T_PIPELINE_PUBLIC_VAR_GROUP_RELEASE_RECORD) {
            val offset = (page - 1) * pageSize
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(GROUP_NAME.eq(groupName))
                .orderBy(PUB_TIME.desc())
                .offset(offset)
                .limit(pageSize)
                .fetch { record ->
                    PipelinePublicVarGroupReleaseRecordPO(
                        id = record.id,
                        projectId = record.projectId,
                        groupName = record.groupName,
                        version = record.version,
                        publisher = record.publisher,
                        pubTime = record.pubTime,
                        desc = record.desc,
                        content = record.content,
                        creator = record.creator,
                        modifier = record.modifier,
                        createTime = record.createTime,
                        updateTime = record.updateTime
                    )
                }
        }
    }
}