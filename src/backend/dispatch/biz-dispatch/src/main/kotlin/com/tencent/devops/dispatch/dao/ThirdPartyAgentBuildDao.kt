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

package com.tencent.devops.dispatch.dao

import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.TDispatchThirdpartyAgentBuild
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ThirdPartyAgentBuildDao {

    fun get(dslContext: DSLContext, buildId: String, vmSeqId: String): TDispatchThirdpartyAgentBuildRecord? {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .fetchOne()
        }
    }

    fun list(dslContext: DSLContext, buildId: String): Result<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .fetch()
        }
    }

    fun add(
        dslContext: DSLContext,
        projectId: String,
        agentId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        thirdPartyAgentWorkspace: String,
        pipelineName: String,
        buildNum: Int,
        taskName: String
    ): Int {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val now = LocalDateTime.now()
            val preRecord =
                dslContext.selectFrom(this).where(BUILD_ID.eq(buildId)).and(VM_SEQ_ID.eq(vmSeqId)).fetchAny()
            if (preRecord != null) { // 支持更新，让用户进行步骤重试时继续能使用
                dslContext.update(this)
                    .set(WORKSPACE, thirdPartyAgentWorkspace)
                    .set(CREATED_TIME, now)
                    .set(UPDATED_TIME, now)
                    .set(STATUS, PipelineTaskStatus.QUEUE.status)
                    .where(ID.eq(preRecord.id)).execute()
                return preRecord.id
            }
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                AGENT_ID,
                PIPELINE_ID,
                BUILD_ID,
                VM_SEQ_ID,
                STATUS,
                CREATED_TIME,
                UPDATED_TIME,
                WORKSPACE,
                PIPELINE_NAME,
                BUILD_NUM,
                TASK_NAME
            ).values(
                projectId,
                agentId,
                pipelineId,
                buildId,
                vmSeqId,
                PipelineTaskStatus.QUEUE.status,
                now,
                now,
                thirdPartyAgentWorkspace,
                pipelineName,
                buildNum,
                taskName
            ).execute()
        }
    }

    fun getPreBuildAgent(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        vmSeqId: String
    ): Result<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(STATUS.eq(PipelineTaskStatus.DONE.status))
                .orderBy(CREATED_TIME.desc())
                .limit(10)
                .fetch()
        }
    }

    fun updateStatus(
        dslContext: DSLContext,
        id: Int,
        status: PipelineTaskStatus
    ): Int {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.update(this)
                .set(STATUS, status.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getQueueBuilds(
        dslContext: DSLContext,
        agentId: String
    ): Result<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(AGENT_ID.eq(agentId))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
                .orderBy(UPDATED_TIME.asc())
                .fetch()
        }
    }

    fun getRunningAndQueueBuilds(
        dslContext: DSLContext,
        agentId: String
    ): Result<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(AGENT_ID.eq(agentId))
                .and(STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
                .fetch()
        }
    }

    fun getRunningBuilds(
        dslContext: DSLContext,
        agentId: String
    ): List<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(AGENT_ID.eq(agentId))
                .and(STATUS.eq(PipelineTaskStatus.RUNNING.status))
                .fetch()
        }
    }

    fun listAgentBuilds(
        dslContext: DSLContext,
        agentId: String,
        offset: Int,
        limit: Int
    ): List<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(AGENT_ID.eq(agentId))
                .orderBy(CREATED_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun countAgentBuilds(dslContext: DSLContext, agentId: String): Long {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectCount().from(this)
                .where(AGENT_ID.eq(agentId))
                .orderBy(CREATED_TIME.desc())
                .fetchOne(0, Long::class.java)
        }
    }
}
