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

package com.tencent.devops.stream.v1.dao

import com.tencent.devops.model.stream.tables.TGitRequestEventNotBuild
import com.tencent.devops.model.stream.tables.records.TGitRequestEventNotBuildRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class V1GitRequestEventNotBuildDao {

    fun save(
        dslContext: DSLContext,
        eventId: Long,
        originYaml: String?,
        parsedYaml: String? = null,
        normalizedYaml: String?,
        reason: String?,
        reasonDetail: String?,
        pipelineId: String?,
        filePath: String?,
        gitProjectId: Long,
        version: String?,
        branch: String?
    ): Long {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            val record = dslContext.insertInto(
                this,
                EVENT_ID,
                ORIGIN_YAML,
                PIPELINE_ID,
                FILE_PATH,
                PARSED_YAML,
                NORMALIZED_YAML,
                REASON,
                REASON_DETAIL,
                GIT_PROJECT_ID,
                CREATE_TIME,
                VERSION,
                BRANCH
            ).values(
                eventId,
                originYaml,
                pipelineId,
                filePath,
                parsedYaml,
                normalizedYaml,
                reason,
                reasonDetail,
                gitProjectId,
                LocalDateTime.now(),
                version,
                branch
            ).returning(ID)
                .fetchOne()!!
            return record.id
        }
    }

    fun getRequestNoBuildsByEventId(
        dslContext: DSLContext,
        eventId: Long,
        gitProjectId: Long
    ): List<TGitRequestEventNotBuildRecord> {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.selectFrom(this)
                .where(EVENT_ID.eq(eventId))
                .and(GIT_PROJECT_ID.eq(gitProjectId))
                .fetch()
        }
    }

    fun getListByEventIds(
        dslContext: DSLContext,
        eventIds: Set<Long>
    ): List<TGitRequestEventNotBuildRecord> {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.selectFrom(this)
                .where(EVENT_ID.`in`(eventIds))
                .fetch()
        }
    }

    fun deleteNotBuildByPipelineIds(
        dslContext: DSLContext,
        pipelineIds: Set<String>
    ): Int {
        with(TGitRequestEventNotBuild.T_GIT_REQUEST_EVENT_NOT_BUILD) {
            return dslContext.deleteFrom(this)
                .where(PIPELINE_ID.`in`(pipelineIds))
                .execute()
        }
    }
}
