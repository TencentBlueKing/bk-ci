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

package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoDispatch
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.thirdPartyAgent.BuildJobType
import com.tencent.devops.model.dispatch.tables.TDispatchThirdpartyAgentBuild
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import org.jooq.JSON

@Repository
@Suppress("ALL")
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
        taskName: String,
        agentIp: String,
        nodeId: Long,
        dockerInfo: ThirdPartyAgentDockerInfoDispatch?,
        executeCount: Int?,
        containerHashId: String?
    ): Int {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val now = LocalDateTime.now()
            val preRecord =
                dslContext.selectFrom(this).where(BUILD_ID.eq(buildId)).and(VM_SEQ_ID.eq(vmSeqId)).fetchAny()
            if (preRecord != null) { // 支持更新，让用户进行步骤重试时继续能使用
                return dslContext.update(this)
                    .set(PROJECT_ID, projectId)
                    .set(AGENT_ID, agentId) // agentId 会变化存在于构建机集群的场景下出现飘移（合法）
                    .set(PIPELINE_ID, pipelineId)
                    .set(BUILD_ID, buildId)
                    .set(VM_SEQ_ID, vmSeqId)
                    .set(WORKSPACE, thirdPartyAgentWorkspace)
                    .set(UPDATED_TIME, now)
                    .set(STATUS, PipelineTaskStatus.QUEUE.status)
                    .set(AGENT_IP, agentIp)
                    .set(NODE_ID, nodeId)
                    .set(
                        DOCKER_INFO, if (dockerInfo == null) {
                            null
                        } else {
                            JSON.json(JsonUtil.toJson(dockerInfo, formatted = false))
                        }
                    )
                    .set(EXECUTE_COUNT, executeCount)
                    .set(CONTAINER_HASH_ID, containerHashId)
                    .where(ID.eq(preRecord.id)).execute()
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
                TASK_NAME,
                AGENT_IP,
                NODE_ID,
                DOCKER_INFO,
                EXECUTE_COUNT,
                CONTAINER_HASH_ID
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
                taskName,
                agentIp,
                nodeId,
                if (dockerInfo == null) {
                    null
                } else {
                    JSON.json(JsonUtil.toJson(dockerInfo, formatted = false))
                },
                executeCount,
                containerHashId
            ).execute()
        }
    }

    /**
     * 2 天之前的构建如果还在running获取queue的都置为失败
     */
    fun updateExpireBuilds(
        dslContext: DSLContext,
        ids: Set<Long>
    ): Int {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.update(this)
                .set(STATUS, PipelineTaskStatus.FAILURE.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.`in`(ids))
                .execute()
        }
    }

    fun getExpireBuilds(
        dslContext: DSLContext
    ): Result<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(STATUS.`in`(PipelineTaskStatus.QUEUE.status, PipelineTaskStatus.RUNNING.status))
                .and(UPDATED_TIME.lessThan(LocalDateTime.now().minusDays(2)))
                .fetch()
        }
    }

    fun getPreBuildAgentIds(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        size: Int
    ): List<String> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectDistinct(AGENT_ID) // 修复获取最近构建构建机超过10次不构建会被驱逐出最近构建机列表的BUG
                .from(this.forceIndex("IDX_PROJECT_PIPELINE_SEQ_STATUS_TIME"))
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(STATUS.eq(PipelineTaskStatus.DONE.status))
                .orderBy(CREATED_TIME.desc())
                .limit(size)
                .fetch(AGENT_ID, String::class.java)
        }
    }

    fun updateStatus(dslContext: DSLContext, id: Long, status: PipelineTaskStatus): Int {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.update(this)
                .set(STATUS, status.status)
                .set(UPDATED_TIME, LocalDateTime.now())
                .where(ID.eq(id))
                .execute()
        }
    }

    fun fetchOneQueueBuild(
        dslContext: DSLContext,
        agentId: String,
        buildType: BuildJobType
    ): TDispatchThirdpartyAgentBuildRecord? {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val select = dslContext.selectFrom(this.forceIndex("IDX_AGENTID_STATUS_UPDATE"))
                .where(AGENT_ID.eq(agentId))
                .and(STATUS.eq(PipelineTaskStatus.QUEUE.status))
            if (buildType == BuildJobType.DOCKER) {
                select.and(DOCKER_INFO.isNotNull)
            } else if (buildType == BuildJobType.BINARY) {
                select.and(DOCKER_INFO.isNull)
            }
            return select
                .orderBy(UPDATED_TIME.asc())
                .limit(1)
                .fetchAny()
        }
    }

    fun getRunningAndQueueBuilds(
        dslContext: DSLContext,
        agentId: String
    ): Result<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this.forceIndex("IDX_AGENTID_STATUS_UPDATE"))
                .where(AGENT_ID.eq(agentId))
                .and(DOCKER_INFO.isNull)
                .and(STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
                .fetch()
        }
    }

    fun getDockerRunningAndQueueBuilds(
        dslContext: DSLContext,
        agentId: String
    ): Result<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this.forceIndex("IDX_AGENTID_STATUS_UPDATE"))
                .where(AGENT_ID.eq(agentId))
                .and(DOCKER_INFO.isNotNull)
                .and(STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
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
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun getLastDockerBuild(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        vmSeqId: String
    ): TDispatchThirdpartyAgentBuildRecord? {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(DOCKER_INFO.isNotNull)
                .orderBy(CREATED_TIME.desc())
                .fetchAny()
        }
    }

    fun getDockerBuild(dslContext: DSLContext, buildId: String, vmSeqId: String): TDispatchThirdpartyAgentBuildRecord? {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(DOCKER_INFO.isNotNull)
                .fetchAny()
        }
    }
}
