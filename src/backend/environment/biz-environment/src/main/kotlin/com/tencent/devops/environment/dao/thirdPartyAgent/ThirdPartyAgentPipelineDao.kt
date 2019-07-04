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

package com.tencent.devops.environment.dao.thirdPartyAgent

import com.tencent.devops.environment.pojo.thirdPartyAgent.pipeline.PipelineStatus
import com.tencent.devops.model.environment.tables.TEnvironmentAgentPipeline
import com.tencent.devops.model.environment.tables.records.TEnvironmentAgentPipelineRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ThirdPartyAgentPipelineDao {

    fun add(
        dslContext: DSLContext,
        agentId: Long,
        projectId: String,
        userId: String,
        status: PipelineStatus,
        pipeline: String
    ): Long {
        with(TEnvironmentAgentPipeline.T_ENVIRONMENT_AGENT_PIPELINE) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                AGENT_ID,
                PROJECT_ID,
                USER_ID,
                CREATED_TIME,
                UPDATED_TIME,
                PIPELINE,
                STATUS
            )
                .values(
                    agentId,
                    projectId,
                    userId,
                    now,
                    now,
                    pipeline,
                    status.status
                )
                .returning().fetchOne().id
        }
    }

    fun getPipeline(
        dslContext: DSLContext,
        agentId: Long,
        status: PipelineStatus
    ): TEnvironmentAgentPipelineRecord? {
        with(TEnvironmentAgentPipeline.T_ENVIRONMENT_AGENT_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(AGENT_ID.eq(agentId))
                .and(STATUS.eq(status.status))
                .orderBy(CREATED_TIME.asc())
                .limit(1)
                .fetchOne()
        }
    }

    fun getPipeline(
        dslContext: DSLContext,
        id: Long,
        agentId: Long
    ): TEnvironmentAgentPipelineRecord? {
        with(TEnvironmentAgentPipeline.T_ENVIRONMENT_AGENT_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .and(AGENT_ID.eq(agentId))
                .fetchOne()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        id: Long,
        status: PipelineStatus,
        response: String
    ): Int {
        with(TEnvironmentAgentPipeline.T_ENVIRONMENT_AGENT_PIPELINE) {
            return dslContext.update(this)
                .set(STATUS, status.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .set(RESPONSE, response)
                .where(ID.eq(id))
                .execute()
        }
    }
}