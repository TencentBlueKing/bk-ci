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

package com.tencent.devops.environment.dao.thirdpartyagent

import com.tencent.devops.environment.pojo.thirdpartyagent.AgentPipelineRef
import com.tencent.devops.model.environment.tables.TAgentPipelineRef
import com.tencent.devops.model.environment.tables.records.TAgentPipelineRefRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AgentPipelineRefDao {
    fun list(dslContext: DSLContext, projectId: String, pipelineId: String): List<TAgentPipelineRefRecord> {
        with(TAgentPipelineRef.T_AGENT_PIPELINE_REF) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetch()
        }
    }

    fun listByNodeId(dslContext: DSLContext, projectId: String, nodeId: Long): List<TAgentPipelineRefRecord> {
        with(TAgentPipelineRef.T_AGENT_PIPELINE_REF) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NODE_ID.eq(nodeId))
                .fetch()
        }
    }

    fun batchDelete(dslContext: DSLContext, ids: List<Long>) {
        if (ids.isEmpty()) {
            return
        }

        with(TAgentPipelineRef.T_AGENT_PIPELINE_REF) {
            dslContext.deleteFrom(this)
                .where(ID.`in`(ids))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, agentPipelineRef: Collection<AgentPipelineRef>) {
        if (agentPipelineRef.isEmpty()) {
            return
        }
        dslContext.batch(agentPipelineRef.map {
            with(TAgentPipelineRef.T_AGENT_PIPELINE_REF) {
                dslContext.insertInto(
                    this,
                    NODE_ID,
                    AGENT_ID,
                    PROJECT_ID,
                    PIPELINE_ID,
                    PIEPLINE_NAME,
                    VM_SEQ_ID,
                    JOB_ID,
                    JOB_NAME,
                    LAST_BUILD_TIME
                ).values(
                    it.nodeId,
                    it.agentId,
                    it.projectId,
                    it.pipelineId,
                    it.pipelineName,
                    it.vmSeqId,
                    it.jobId,
                    it.jobName,
                    null
                )
            }
        }).execute()
    }

    fun countPipelineRef(dslContext: DSLContext, agentId: Long): Int {
        with(TAgentPipelineRef.T_AGENT_PIPELINE_REF) {
            return dslContext.selectCount()
                .from(this)
                .where(AGENT_ID.`in`(agentId))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun updateLastBuildTime(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        agentLongId: Long,
        time: LocalDateTime
    ) {
        with(TAgentPipelineRef.T_AGENT_PIPELINE_REF) {
            dslContext.update(this)
                .set(LAST_BUILD_TIME, time)
                .where(PROJECT_ID.eq(projectId))
                .and(AGENT_ID.eq(agentLongId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .execute()
        }
    }
}
