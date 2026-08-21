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

package com.tencent.devops.dispatch.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfoDispatch
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuild
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AgentBuildInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.BuildJobType
import com.tencent.devops.model.dispatch.tables.TDispatchThirdpartyAgentBuild
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.Result
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Repository
@Suppress("ALL")
class ThirdPartyAgentBuildDao {

    fun get(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int?
    ): TDispatchThirdpartyAgentBuildRecord? {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
            if (executeCount == null) {
                dsl.and(EXECUTE_COUNT.isNull)
            } else {
                dsl.and(EXECUTE_COUNT.eq(executeCount))
            }
            return dsl // 通过排序取最新
                .orderBy(ID.desc())
                .limit(1).fetchAny()
        }
    }

    fun getWithExecuteCount(
        dslContext: DSLContext,
        buildId: String,
        vmSeqId: String,
        executeCount: Int?
    ): TDispatchThirdpartyAgentBuildRecord? {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
            if (executeCount != null) {
                dsl.and(EXECUTE_COUNT.eq(executeCount).or(EXECUTE_COUNT.isNull))
            }
            return dsl// 通过排序取最新
                .orderBy(ID.desc())
                .limit(1).fetchAny()
        }
    }

    fun list(
        dslContext: DSLContext,
        buildId: String,
        executeCount: Int?
    ): Result<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
            if (executeCount != null) {
                dsl.and(EXECUTE_COUNT.eq(executeCount).or(EXECUTE_COUNT.isNull))
            }
            return dsl.fetch()
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
        containerHashId: String?,
        envId: Long?,
        ignoreEnvAgentIds: Set<String>?,
        jobId: String?,
        startUser: String?,
        stageId: String?
    ): Int {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val now = LocalDateTime.now()
            val ignoreEnvAgentIdsJson = if (ignoreEnvAgentIds.isNullOrEmpty()) {
                null
            } else {
                JSON.json(JsonUtil.toJson(ignoreEnvAgentIds, false))
            }
            val preRecord = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(
                    if (executeCount == null) {
                        EXECUTE_COUNT.isNull
                    } else {
                        EXECUTE_COUNT.eq(executeCount)
                    }
                )
                .fetchAny()
            if (preRecord != null) { // 支持更新，让用户进行步骤重试时继续能使用
                return dslContext.update(this)
                    .set(PROJECT_ID, projectId)
                    .set(AGENT_ID, agentId) // agentId 会变化存在于构建机集群的场景下出现飘移（合法）
                    .set(PIPELINE_ID, pipelineId)
                    .set(BUILD_ID, buildId)
                    .set(VM_SEQ_ID, vmSeqId)
                    .set(WORKSPACE, thirdPartyAgentWorkspace)
                    .set(UPDATED_TIME, now)
                    .set(
                        TIME_INTERVAL, field(
                            "TIMESTAMPDIFF(SECOND, {0}, {1})",
                            Long::class.java,
                            CREATED_TIME,
                            now
                        )
                    )
                    .set(STATUS, PipelineTaskStatus.QUEUE.status)
                    .set(AGENT_IP, agentIp)
                    .set(NODE_ID, nodeId)
                    .set(
                        DOCKER_INFO,
                        if (dockerInfo == null) {
                            null
                        } else {
                            JSON.json(JsonUtil.toJson(dockerInfo, formatted = false))
                        }
                    )
                    .set(EXECUTE_COUNT, executeCount)
                    .set(CONTAINER_HASH_ID, containerHashId)
                    .set(ENV_ID, envId)
                    .set(IGNORE_ENV_AGENT_IDS, ignoreEnvAgentIdsJson)
                    .set(JOB_ID, jobId)
                    .set(START_USER, startUser)
                    .set(STAGE_ID, stageId)
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
                CONTAINER_HASH_ID,
                ENV_ID,
                IGNORE_ENV_AGENT_IDS,
                JOB_ID,
                START_USER,
                STAGE_ID
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
                containerHashId,
                envId,
                ignoreEnvAgentIdsJson,
                jobId,
                startUser,
                stageId
            ).execute()
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

    fun updateStatus(dslContext: DSLContext, id: Long, status: PipelineTaskStatus, timeInterval: Long?): Int {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val now = LocalDateTime.now()
            val dsl = dslContext.update(this)
                .set(STATUS, status.status)
                .set(UPDATED_TIME, now)
            if (timeInterval != null) {
                dsl.set(TIME_INTERVAL, timeInterval)
            } else {
                dsl.set(
                    TIME_INTERVAL, timeInterval ?: field(
                        "TIMESTAMPDIFF(SECOND, {0}, {1})",
                        Long::class.java,
                        CREATED_TIME,
                        now
                    )
                )
            }
            return dsl.where(ID.eq(id)).execute()
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
        agentId: String,
        hasDocker: Boolean
    ): List<Pair<String, Int>> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            // 子查询：该 agent 下每个 (buildId, vmSeqId) 取最新一行（ID 自增，最大即最新）
            val latestIds = dslContext.select(DSL.max(ID))
                .from(this)
                .where(AGENT_ID.eq(agentId))
                .groupBy(BUILD_ID, VM_SEQ_ID)

            val dsl = dslContext.select(BUILD_ID, STATUS)
                .from(this)
                .where(ID.`in`(latestIds))
            if (hasDocker) {
                dsl.and(DOCKER_INFO.isNotNull)
            } else {
                dsl.and(DOCKER_INFO.isNull)
            }
            return dsl.and(STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
                .fetch()
                .map { Pair(it[BUILD_ID], it[STATUS]) }
        }
    }

    // 这个方法在有executeCount前就是按build维度的，需要聚合下
    fun listAgentBuilds(
        dslContext: DSLContext,
        agentId: String,
        status: String?,
        pipelineId: String?,
        jobId: String?,
        offset: Int,
        limit: Int
    ): List<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            // 过滤条件抽出来，子查询和主查询共用
            val conditions = mutableListOf<Condition>()
            conditions.add(AGENT_ID.eq(agentId))
            if (status != null) {
                conditions.add(STATUS.eq(PipelineTaskStatus.parse(status).status))
            }
            if (pipelineId != null) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (jobId != null) {
                conditions.add(JOB_ID.eq(jobId))
            }

            // 子查询：每个 (buildId, vmSeqId) 组取主键最大的一条（最新插入 = 最新执行）
            val latestIds = dslContext.select(DSL.max(ID))
                .from(this)
                .where(conditions)
                .groupBy(BUILD_ID, VM_SEQ_ID)

            return dslContext.selectFrom(this)
                .where(conditions)
                .and(ID.`in`(latestIds))
                .orderBy(CREATED_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun listLatestBuildPipelines(
        dslContext: DSLContext,
        agentIds: List<String>
    ): List<AgentBuildInfo> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val sub = dslContext.select(DSL.max(ID).`as`("max_id"))
                .from(this).where(AGENT_ID.`in`(agentIds)).groupBy(AGENT_ID).asTable("sub")
            return dslContext.select(
                PROJECT_ID,
                AGENT_ID,
                PIPELINE_ID,
                PIPELINE_NAME,
                BUILD_ID,
                BUILD_NUM,
                VM_SEQ_ID,
                TASK_NAME,
                STATUS,
                CREATED_TIME,
                UPDATED_TIME,
                WORKSPACE
            ).from(this)
                .join(sub).on(ID.eq(sub.field("max_id", Long::class.java)))
                .orderBy(CREATED_TIME.desc())
                .fetch().map {
                    AgentBuildInfo(
                        projectId = it.value1(),
                        agentId = it.value2(),
                        pipelineId = it.value3(),
                        pipelineName = it.value4(),
                        buildId = it.value5(),
                        buildNum = it.value6(),
                        vmSeqId = it.value7(),
                        taskName = it.value8(),
                        status = PipelineTaskStatus.toStatus(it.value9()).name,
                        createdTime = it.value10().timestamp(),
                        updatedTime = it.value11().timestamp(),
                        workspace = it.value12() ?: ""
                    )
                }
        }
    }

    // 这个方法在有executeCount前就是按build维度的，需要聚合下
    fun countAgentBuilds(
        dslContext: DSLContext,
        agentId: String,
        status: String?,
        pipelineId: String?,
        jobId: String?
    ): Long {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val conditions = mutableListOf<Condition>()
            conditions.add(AGENT_ID.eq(agentId))
            if (status != null) {
                conditions.add(STATUS.eq(PipelineTaskStatus.parse(status).status))
            }
            if (pipelineId != null) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (jobId != null) {
                conditions.add(JOB_ID.eq(jobId))
            }

            return dslContext.select(DSL.countDistinct(BUILD_ID, VM_SEQ_ID))
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun countAgentBuildsByJob(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String?,
        jobId: String?
    ): Long {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId))
                .let {
                    if (agentId != null) it.and(AGENT_ID.eq(agentId)) else it
                }
                .let {
                    if (envId != null) it.and(ENV_ID.eq(envId)) else it
                }
                .let {
                    if (pipelineId != null) it.and(PIPELINE_ID.eq(pipelineId)) else it
                }
                .let {
                    if (jobId != null) it.and(JOB_ID.eq(jobId)) else it
                }
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun listAgentBuildsByJob(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String?,
        jobId: String?,
        offset: Int,
        limit: Int
    ): List<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .let {
                    if (agentId != null) it.and(AGENT_ID.eq(agentId)) else it
                }
                .let {
                    if (envId != null) it.and(ENV_ID.eq(envId)) else it
                }
                .let {
                    if (pipelineId != null) it.and(PIPELINE_ID.eq(pipelineId)) else it
                }
                .let {
                    if (jobId != null) it.and(JOB_ID.eq(jobId)) else it
                }
                .orderBy(CREATED_TIME.desc())
                .limit(offset, limit)
                .fetch()
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
                // 通过排序取最新
                .orderBy(ID.desc())
                .limit(1)
                .fetchAny()
        }
    }

    fun getDockerBuild(dslContext: DSLContext, buildId: String, vmSeqId: String): TDispatchThirdpartyAgentBuildRecord? {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(VM_SEQ_ID.eq(vmSeqId))
                .and(DOCKER_INFO.isNotNull)
                // 通过排序取最新
                .orderBy(ID.desc())
                .limit(1)
                .fetchAny()
        }
    }

    fun countProjectJobRunningAndQueueAll(
        dslContext: DSLContext,
        pipelineId: String,
        envId: Long,
        jobId: String,
        projectId: String
    ): Long {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            // 子查询：每个 (buildId, vmSeqId) 取最新一行的主键（ID 自增，最大即最新）
            val latestIds = dslContext.select(DSL.max(ID))
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(JOB_ID.eq(jobId))
                .and(ENV_ID.eq(envId))
                .groupBy(BUILD_ID, VM_SEQ_ID)

            // 只在“每组最新行”里数 RUNNING/QUEUE
            return dslContext.selectCount()
                .from(this)
                .where(ID.`in`(latestIds))
                .and(STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun countAgentsJobRunningAndQueueAll(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        envId: Long,
        jobId: String,
        agentIds: Set<String>
    ): Map<String, Int> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            // 子查询：命中过滤条件的行里，每个 (buildId, vmSeqId) 取最新一行
            val latestIds = dslContext.select(DSL.max(ID))
                .from(this)
                .where(AGENT_ID.`in`(agentIds))
                .and(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(JOB_ID.eq(jobId))
                .and(ENV_ID.eq(envId))
                .groupBy(BUILD_ID, VM_SEQ_ID)

            return dslContext.select(AGENT_ID, DSL.count().`as`("COUNT"))
                .from(this)
                .where(ID.`in`(latestIds))
                .and(STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
                .groupBy(AGENT_ID)
                .fetch()
                .map { it[AGENT_ID] to (it["COUNT"] as Int) }
                .toMap()
        }
    }

    /**
     * @return PIPELINE_COUNT,JOB_COUNT,BUILD_COUNT
     */
    fun countAgentBuildPipelineJob(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        startTime: Long?,
        endTime: Long?,
        pipelineId: String?,
        jobId: String?,
        creator: String?,
        status: List<PipelineTaskStatus>?
    ): Triple<Long, Long, Long> {
        if (agentId.isNullOrBlank() && envId == null) {
            return Triple(0L, 0L, 0L)
        }
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.select(
                DSL.countDistinct(PIPELINE_ID).`as`("PIPELINE_COUNT"),
                DSL.countDistinct(PIPELINE_ID, JOB_ID).`as`("JOB_COUNT"),
                DSL.countDistinct(BUILD_ID, EXECUTE_COUNT).`as`("BUILD_COUNT")
            ).from(this)
                .where(PROJECT_ID.eq(projectId))

            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (startTime != null) {
                dsl.and(
                    CREATED_TIME.ge(
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(startTime), ZoneId.systemDefault())
                    )
                )
            }
            if (endTime != null) {
                dsl.and(
                    CREATED_TIME.le(
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(endTime), ZoneId.systemDefault())
                    )
                )
            }
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!jobId.isNullOrBlank()) {
                dsl.and(JOB_ID.eq(jobId))
            }
            if (!creator.isNullOrBlank()) {
                dsl.and(START_USER.eq(creator))
            }
            if (status != null) {
                dsl.and(STATUS.`in`(status.map { it.status }))
            }

            val result = dsl.and(JOB_ID.isNotNull).fetchOne()
            return Triple(
                result?.get("PIPELINE_COUNT", Long::class.java) ?: 0L,
                result?.get("JOB_COUNT", Long::class.java) ?: 0L,
                result?.get("BUILD_COUNT", Long::class.java) ?: 0L
            )
        }
    }

    fun fetchAgentBuildPipeline(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        limit: Int,
        offset: Int,
        startTime: Long?,
        endTime: Long?,
        pipelineId: String?,
        creator: String?,
        status: List<PipelineTaskStatus>?
    ): List<TPAPipelineBuild> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.select(
                PIPELINE_ID,
                PIPELINE_NAME,
                DSL.countDistinct(BUILD_ID, EXECUTE_COUNT).`as`("BUILD_COUNT"),
                DSL.max(CREATED_TIME).`as`("LAST_BUILD_TIME"),
                DSL.avg(TIME_INTERVAL).`as`("AVG_TIME_INTERVAL")
            ).from(this).where(PROJECT_ID.eq(projectId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (startTime != null) {
                dsl.and(
                    CREATED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (endTime != null) {
                dsl.and(
                    CREATED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!creator.isNullOrBlank()) {
                dsl.and(START_USER.eq(creator))
            }
            if (status != null) {
                dsl.and(STATUS.`in`(status.map { it.status }))
            }
            return dsl.and(JOB_ID.isNotNull)
                .groupBy(PIPELINE_ID)
                .orderBy(DSL.max(ID).desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map {
                    TPAPipelineBuild(
                        pipelineId = it.value1(),
                        pipelineName = it.value2(),
                        jobId = null,
                        jobName = null,
                        buildCount = it.value3() as Int,
                        lastBuildTime = it.value4(),
                        avgTimeInterval = it.value5()?.toLong(),
                        lastContainerId = null,
                        stageId = null,
                        stageNumb = null,
                        buildId = null,
                        executeCount = null
                    )
                }
        }
    }

    fun fetchAgentBuildPipelineBuild(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        limit: Int,
        offset: Int,
        startTime: Long?,
        endTime: Long?,
        pipelineId: String?,
        creator: String?,
        status: List<PipelineTaskStatus>?
    ): List<TPAPipelineBuild> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.select(
                PIPELINE_ID,
                PIPELINE_NAME,
                BUILD_ID,
                EXECUTE_COUNT,
                BUILD_NUM
            ).from(this).where(PROJECT_ID.eq(projectId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (startTime != null) {
                dsl.and(
                    CREATED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (endTime != null) {
                dsl.and(
                    CREATED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!creator.isNullOrBlank()) {
                dsl.and(START_USER.eq(creator))
            }
            if (status != null) {
                dsl.and(STATUS.`in`(status.map { it.status }))
            }
            return dsl.and(JOB_ID.isNotNull)
                .groupBy(PIPELINE_ID, BUILD_ID, EXECUTE_COUNT)
                .orderBy(DSL.max(ID).desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map {
                    TPAPipelineBuild(
                        pipelineId = it.value1(),
                        pipelineName = it.value2(),
                        jobId = null,
                        jobName = null,
                        buildCount = 0,
                        lastBuildTime = null,
                        avgTimeInterval = null,
                        lastContainerId = null,
                        stageId = null,
                        stageNumb = null,
                        buildId = it.value3(),
                        executeCount = it.value4(),
                        buildNum = it.value5()
                    )
                }
        }
    }

    fun fetchAgentBuildPipelineJob(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        limit: Int,
        offset: Int,
        startTime: Long?,
        endTime: Long?,
        pipelineId: String?,
        jobId: String?,
        creator: String?,
        status: List<PipelineTaskStatus>?
    ): List<TPAPipelineBuild> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.select(
                PIPELINE_ID,
                PIPELINE_NAME,
                JOB_ID,
                TASK_NAME,
                DSL.count().`as`("BUILD_COUNT"),
                DSL.max(CREATED_TIME).`as`("LAST_BUILD_TIME"),
                DSL.avg(TIME_INTERVAL).`as`("AVG_TIME_INTERVAL"),
                DSL.field(
                    "SUBSTRING_INDEX(GROUP_CONCAT({0} ORDER BY {1} DESC SEPARATOR ','), ',', 1)",
                    Long::class.java,
                    VM_SEQ_ID,
                    ID
                ).`as`("LAST_VM_SEQ_ID"),
                DSL.field(
                    "SUBSTRING_INDEX(GROUP_CONCAT({0} ORDER BY {1} DESC SEPARATOR ','), ',', 1)",
                    String::class.java,
                    STAGE_ID,
                    ID
                ).`as`("STAGE_ID"),
            ).from(this).where(PROJECT_ID.eq(projectId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            // 新增条件查询
            if (startTime != null) {
                dsl.and(
                    CREATED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (endTime != null) {
                dsl.and(
                    CREATED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (!pipelineId.isNullOrBlank()) {
                dsl.and(PIPELINE_ID.eq(pipelineId))
            }
            if (!jobId.isNullOrBlank()) {
                dsl.and(JOB_ID.eq(jobId))
            }
            if (!creator.isNullOrBlank()) {
                dsl.and(START_USER.eq(creator))
            }
            if (status != null) {
                dsl.and(STATUS.`in`(status.map { it.status }))
            }
            return dsl.and(JOB_ID.isNotNull)
                .groupBy(PIPELINE_ID, JOB_ID)
                .orderBy(DSL.max(ID).desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map {
                    TPAPipelineBuild(
                        pipelineId = it.value1(),
                        pipelineName = it.value2(),
                        jobId = it.value3(),
                        jobName = it.value4(),
                        buildCount = it.value5() as Int,
                        lastBuildTime = it.value6(),
                        avgTimeInterval = it.value7()?.toLong(),
                        lastContainerId = it.value8(),
                        stageId = it.value9(),
                        stageNumb = null,
                        buildId = null,
                        executeCount = null
                    )
                }
        }
    }

    fun fetchPipelineIdAndName(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineName: String?
    ): List<Pair<String, String>> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.select(
                PIPELINE_ID,
                PIPELINE_NAME
            ).from(this).where(PROJECT_ID.eq(projectId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (!pipelineName.isNullOrBlank()) {
                dsl.and(PIPELINE_NAME.like("%$pipelineName%"))
            }
            return dsl.and(JOB_ID.isNotNull)
                .groupBy(PIPELINE_ID)
                .orderBy(ID.desc())
                .fetch()
                .map {
                    Pair(
                        it.value1(),
                        it.value2()
                    )
                }
        }
    }

    fun fetchJobIdAndName(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        jobName: String?
    ): List<Pair<String, String>> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.select(
                JOB_ID,
                TASK_NAME
            ).from(this).where(PROJECT_ID.eq(projectId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (!jobName.isNullOrBlank()) {
                dsl.and(TASK_NAME.like("%$jobName%"))
            }
            return dsl.and(JOB_ID.isNotNull)
                .groupBy(JOB_ID)
                .orderBy(ID.desc())
                .fetch()
                .map {
                    Pair(
                        it.value1(),
                        it.value2()
                    )
                }
        }
    }

    fun fetchCreator(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        creator: String?
    ): List<String> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectDistinct(
                START_USER
            ).from(this).where(PROJECT_ID.eq(projectId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (!creator.isNullOrBlank()) {
                dsl.and(START_USER.like("%$creator%"))
            }
            return dsl.and(JOB_ID.isNotNull)
                .and(START_USER.isNotNull)
                .groupBy(START_USER)
                .orderBy(ID.desc())
                .fetch()
                .map {
                    it.value1()
                }
        }
    }

    fun countAgentBuildGroupsByPipeline(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String
    ): Long {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectCount().from(this)
                .where(PIPELINE_ID.eq(pipelineId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            return dsl.and(PROJECT_ID.eq(projectId)).fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun listAgentBuildGroupsByPipeline(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String,
        offset: Int,
        limit: Int
    ): List<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            return dsl.and(PROJECT_ID.eq(projectId)).orderBy(ID.desc()).limit(limit).offset(offset).fetch()
        }
    }

    fun countAgentBuildGroupsByBuild(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        buildId: String,
        executeCount: Int?
    ): Long {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectCount().from(this)
                .where(BUILD_ID.eq(buildId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (executeCount == null) {
                dsl.and(EXECUTE_COUNT.isNull)
            } else {
                dsl.and(EXECUTE_COUNT.eq(executeCount))
            }
            return dsl.and(PROJECT_ID.eq(projectId)).fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun listAgentBuildGroupsByBuild(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        buildId: String,
        executeCount: Int?,
        offset: Int,
        limit: Int
    ): List<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (executeCount == null) {
                dsl.and(EXECUTE_COUNT.isNull)
            } else {
                dsl.and(EXECUTE_COUNT.eq(executeCount))
            }
            return dsl.and(PROJECT_ID.eq(projectId)).orderBy(ID.desc()).limit(limit).offset(offset).fetch()
        }
    }
}
