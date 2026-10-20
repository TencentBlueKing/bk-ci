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
import com.tencent.devops.environment.pojo.AllCreateNodeEnv
import com.tencent.devops.model.dispatch.tables.TDispatchThirdpartyAgentBuild
import com.tencent.devops.model.dispatch.tables.records.TDispatchThirdpartyAgentBuildRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.Result
import org.jooq.impl.DSL
import org.jooq.impl.DSL.countDistinct
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
            // 通过排序取最新
            return dsl.orderBy(ID.desc()).limit(1).fetchAny()
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
            // 通过排序取最新
            return dsl.orderBy(ID.desc()).limit(1).fetchAny()
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
        val t = TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD.`as`("t")
        val dsl = dslContext.select(t.BUILD_ID, t.STATUS)
            .from(t)
            .where(t.AGENT_ID.eq(agentId))
            .and(t.STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
            .and(notExistsNewerRecord(dslContext, t))
        if (hasDocker) {
            dsl.and(t.DOCKER_INFO.isNotNull)
        } else {
            dsl.and(t.DOCKER_INFO.isNull)
        }
        return dsl.fetch().map { Pair(it[t.BUILD_ID], it[t.STATUS]) }
    }

    /**
     * 判断 t 这一行是否是同一 (agentId, buildId, vmSeqId) 下的最新一行（ID 自增，最大即最新）。
     * 不使用 "ID IN (SELECT MAX(ID) ... GROUP BY ...)"：那种写法要回表扫 agent 的全部历史记录再分组，
     * 历史多时达到百万行级别；这里先由外层按 AGENT_ID + STATUS 取出少量运行中/排队中的行，
     * 再逐行用 IDX_BVE(BUILD_ID, VM_SEQ_ID, ...) 确认后面没有更新的记录，结果集一致。
     */
    private fun notExistsNewerRecord(
        dslContext: DSLContext,
        t: TDispatchThirdpartyAgentBuild
    ): Condition {
        val n = TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD.`as`("n")
        return DSL.notExists(
            dslContext.selectOne()
                .from(n)
                .where(n.BUILD_ID.eq(t.BUILD_ID))
                .and(n.VM_SEQ_ID.eq(t.VM_SEQ_ID))
                .and(n.AGENT_ID.eq(t.AGENT_ID))
                .and(n.ID.gt(t.ID))
        )
    }

    /**
     * 批量统计多个 agent 当前 运行中+排队中 的任务数，一次 SQL 代替按 agent 循环调用 [getRunningAndQueueBuilds]。
     * 统计口径与 [getRunningAndQueueBuilds] 保持一致：每个 (agentId, buildId, vmSeqId) 只取最新一行。
     * @return agentId -> Pair(普通任务数, Docker 任务数)；没有任务的 agent 不会出现在结果中，调用方按 0 处理
     */
    fun countRunningAndQueueByAgents(
        dslContext: DSLContext,
        agentIds: Set<String>
    ): Map<String, Pair<Int, Int>> {
        if (agentIds.isEmpty()) {
            return emptyMap()
        }
        val t = TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD.`as`("t")
        // 用条件求和一次拿到普通和 Docker 两种计数
        val normalCnt = DSL.sum(DSL.`when`(t.DOCKER_INFO.isNull, 1).otherwise(0)).`as`("NORMAL_CNT")
        val dockerCnt = DSL.sum(DSL.`when`(t.DOCKER_INFO.isNotNull, 1).otherwise(0)).`as`("DOCKER_CNT")
        return dslContext.select(t.AGENT_ID, normalCnt, dockerCnt)
            .from(t)
            .where(t.AGENT_ID.`in`(agentIds))
            .and(t.STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
            .and(notExistsNewerRecord(dslContext, t))
            .groupBy(t.AGENT_ID)
            .fetch()
            .associate {
                it.value1() to Pair(it.value2()?.toInt() ?: 0, it.value3()?.toInt() ?: 0)
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

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
    fun countAgentBuildsByJob(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String?,
        jobId: String?,
        startTime: Long?,
        endTime: Long?
    ): Long {
        if (agentId.isNullOrBlank() && envId == null) {
            return 0
        }
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val conditions = mutableListOf(buildProjectCondition(projectId, envId)).apply {
                if (agentId != null) add(AGENT_ID.eq(agentId))
                if (envId != null) add(ENV_ID.eq(envId))
                if (pipelineId != null) add(PIPELINE_ID.eq(pipelineId))
                if (jobId != null) add(JOB_ID.eq(jobId))
                if (startTime != null) {
                    add(
                        CREATED_TIME.ge(
                            LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(startTime),
                                ZoneId.systemDefault()
                            )
                        )
                    )
                }
                if (endTime != null) {
                    add(
                        CREATED_TIME.le(
                            LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(endTime),
                                ZoneId.systemDefault()
                            )
                        )
                    )
                }
            }
            return dslContext.selectCount().from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
    fun listAgentBuildsByJob(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String?,
        jobId: String?,
        offset: Int,
        limit: Int,
        startTime: Long?,
        endTime: Long?
    ): List<TDispatchThirdpartyAgentBuildRecord> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val conditions = mutableListOf(buildProjectCondition(projectId, envId)).apply {
                if (agentId != null) add(AGENT_ID.eq(agentId))
                if (envId != null) add(ENV_ID.eq(envId))
                if (pipelineId != null) add(PIPELINE_ID.eq(pipelineId))
                if (jobId != null) add(JOB_ID.eq(jobId))
                if (startTime != null) {
                    add(
                        CREATED_TIME.ge(
                            LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(startTime),
                                ZoneId.systemDefault()
                            )
                        )
                    )
                }
                if (endTime != null) {
                    add(
                        CREATED_TIME.le(
                            LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(endTime),
                                ZoneId.systemDefault()
                            )
                        )
                    )
                }
            }
            return dslContext.selectFrom(this)
                .where(conditions)
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

    /**
     * 统计某个 job 在环境下（全集群）运行中+排队中的任务数，每个 (buildId, vmSeqId) 只取最新一行。
     * 不使用 "ID IN (SELECT MAX(ID) ... GROUP BY ...)"：那种写法要回表扫该 job 的全部历史再分组。
     * 不强制索引，由优化器结合数据分布选择，避免目标 job 记录少时仍扫描整条流水线的历史索引。
     */
    fun countProjectJobRunningAndQueueAll(
        dslContext: DSLContext,
        pipelineId: String,
        envId: Long,
        jobId: String,
        projectId: String
    ): Long {
        val t = TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD.`as`("t")
        val n = TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD.`as`("n")
        return dslContext.selectCount()
            .from(t)
            .where(jobEnvCondition(t, projectId, pipelineId, jobId, envId))
            .and(t.STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
            .and(
                notExistsNewerJobRecord(
                    dslContext, t, n, jobEnvCondition(n, projectId, pipelineId, jobId, envId)
                )
            )
            .fetchOne(0, Long::class.java)!!
    }

    /**
     * 统计某个 job 在指定 agent 上运行中+排队中的任务数，口径与原写法一致：
     * 在 "命中过滤条件（含 agentIds）的行" 里，每个 (buildId, vmSeqId) 只取最新一行，不按 agent 分组。
     * 外层由优化器自行选择 IDX_AGENTID_STATUS_UPDATE，只扫这些 agent 的活跃行。
     */
    fun countAgentsJobRunningAndQueueAll(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        envId: Long,
        jobId: String,
        agentIds: Set<String>
    ): Map<String, Int> {
        if (agentIds.isEmpty()) {
            return emptyMap()
        }
        val t = TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD.`as`("t")
        val n = TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD.`as`("n")
        val cnt = DSL.count().`as`("COUNT")
        return dslContext.select(t.AGENT_ID, cnt)
            .from(t)
            .where(t.AGENT_ID.`in`(agentIds))
            .and(jobEnvCondition(t, projectId, pipelineId, jobId, envId))
            .and(t.STATUS.`in`(PipelineTaskStatus.RUNNING.status, PipelineTaskStatus.QUEUE.status))
            .and(
                notExistsNewerJobRecord(
                    dslContext, t, n,
                    n.AGENT_ID.`in`(agentIds).and(jobEnvCondition(n, projectId, pipelineId, jobId, envId))
                )
            )
            .groupBy(t.AGENT_ID)
            .fetch()
            .associate { it.value1() to it.value2() }
    }

    private fun jobEnvCondition(
        table: TDispatchThirdpartyAgentBuild,
        projectId: String,
        pipelineId: String,
        jobId: String,
        envId: Long
    ): Condition = table.PROJECT_ID.eq(projectId)
        .and(table.PIPELINE_ID.eq(pipelineId))
        .and(table.JOB_ID.eq(jobId))
        .and(table.ENV_ID.eq(envId))

    /**
     * 判断 t 是否是 "满足 nFilter 的行" 中同一 (buildId, vmSeqId) 下的最新一行。
     * 与 [notExistsNewerRecord] 的区别：job 维度的原口径不按 AGENT_ID 分组，且只在满足过滤条件的行里比较。
     */
    private fun notExistsNewerJobRecord(
        dslContext: DSLContext,
        t: TDispatchThirdpartyAgentBuild,
        n: TDispatchThirdpartyAgentBuild,
        nFilter: Condition
    ): Condition = DSL.notExists(
        dslContext.selectOne()
            .from(n)
            .where(n.BUILD_ID.eq(t.BUILD_ID))
            .and(n.VM_SEQ_ID.eq(t.VM_SEQ_ID))
            .and(n.ID.gt(t.ID))
            .and(nFilter)
    )

    /**
     * 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
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
            val conditions = mutableListOf(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                conditions.add(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                conditions.add(ENV_ID.eq(envId))
            }
            if (startTime != null) {
                conditions.add(
                    CREATED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (endTime != null) {
                conditions.add(
                    CREATED_TIME.le(LocalDateTime.ofInstant(Instant.ofEpochSecond(endTime), ZoneId.systemDefault()))
                )
            }
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (!jobId.isNullOrBlank()) {
                conditions.add(JOB_ID.eq(jobId))
            }
            if (!creator.isNullOrBlank()) {
                conditions.add(START_USER.eq(creator))
            }
            if (status != null) {
                conditions.add(STATUS.`in`(status.map { it.status }))
            }
            conditions.add(JOB_ID.isNotNull)
            val result = dslContext.select(
                DSL.countDistinct(PIPELINE_ID).`as`("PIPELINE_COUNT"),
                DSL.countDistinct(PIPELINE_ID, JOB_ID).`as`("JOB_COUNT"),
                DSL.countDistinct(BUILD_ID, EXECUTE_COUNT).`as`("BUILD_COUNT")
            ).from(this).where(conditions).fetchOne()
            return Triple(
                result?.get("PIPELINE_COUNT", Long::class.java) ?: 0L,
                result?.get("JOB_COUNT", Long::class.java) ?: 0L,
                result?.get("BUILD_COUNT", Long::class.java) ?: 0L
            )
        }
    }

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
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
            val conditions = mutableListOf(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                conditions.add(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                conditions.add(ENV_ID.eq(envId))
            }
            if (startTime != null) {
                conditions.add(
                    CREATED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (endTime != null) {
                conditions.add(
                    CREATED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (!creator.isNullOrBlank()) {
                conditions.add(START_USER.eq(creator))
            }
            if (status != null) {
                conditions.add(STATUS.`in`(status.map { it.status }))
            }
            return dslContext.select(
                PIPELINE_ID,
                PIPELINE_NAME,
                DSL.countDistinct(BUILD_ID, EXECUTE_COUNT).`as`("BUILD_COUNT"),
                PROJECT_ID,
                // 分组内按 ID 降序取最新一条的 BUILD_ID（ID 自增，最大即最新）
                DSL.field(
                    "SUBSTRING_INDEX(GROUP_CONCAT({0} ORDER BY {1} DESC SEPARATOR ','), ',', 1)",
                    String::class.java,
                    BUILD_ID,
                    ID
                ).`as`("LAST_BUILD_ID")
            ).from(this).where(conditions)
                .groupBy(PIPELINE_ID)
                .orderBy(DSL.max(ID).desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map {
                    TPAPipelineBuild(
                        projectId = it.value4(),
                        pipelineId = it.value1(),
                        pipelineName = it.value2(),
                        jobId = null,
                        jobName = null,
                        buildCount = it.value3() as Int,
                        lastBuildTime = null,
                        avgTimeInterval = null,
                        lastContainerId = null,
                        stageId = null,
                        stageNumb = null,
                        buildId = it.value5(),
                        executeCount = null
                    )
                }
        }
    }

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
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
            val conditions = mutableListOf(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                conditions.add(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                conditions.add(ENV_ID.eq(envId))
            }
            if (startTime != null) {
                conditions.add(
                    CREATED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (endTime != null) {
                conditions.add(
                    CREATED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (!creator.isNullOrBlank()) {
                conditions.add(START_USER.eq(creator))
            }
            if (status != null) {
                conditions.add(STATUS.`in`(status.map { it.status }))
            }
            return dslContext.select(
                PIPELINE_ID,
                PIPELINE_NAME,
                BUILD_ID,
                EXECUTE_COUNT,
                BUILD_NUM,
                PROJECT_ID
            ).from(this).where(conditions)
                .groupBy(PIPELINE_ID, BUILD_ID, EXECUTE_COUNT)
                .orderBy(DSL.max(ID).desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map {
                    TPAPipelineBuild(
                        projectId = it.value6(),
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

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
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
            val conditions = mutableListOf(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                conditions.add(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                conditions.add(ENV_ID.eq(envId))
            }
            // 新增条件查询
            if (startTime != null) {
                conditions.add(
                    CREATED_TIME.ge(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(startTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (endTime != null) {
                conditions.add(
                    CREATED_TIME.le(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(endTime),
                            ZoneId.systemDefault()
                        )
                    )
                )
            }
            if (!pipelineId.isNullOrBlank()) {
                conditions.add(PIPELINE_ID.eq(pipelineId))
            }
            if (!jobId.isNullOrBlank()) {
                conditions.add(JOB_ID.eq(jobId))
            }
            if (!creator.isNullOrBlank()) {
                conditions.add(START_USER.eq(creator))
            }
            if (status != null) {
                conditions.add(STATUS.`in`(status.map { it.status }))
            }
            conditions.add(JOB_ID.isNotNull)
            return dslContext.select(
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
                PROJECT_ID
            ).from(this).where(conditions)
                .groupBy(PIPELINE_ID, JOB_ID)
                .orderBy(DSL.max(ID).desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map {
                    TPAPipelineBuild(
                        projectId = it.value10(),
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

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
    fun fetchPipelineIdAndName(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineName: String?
    ): List<Pair<String, String>> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val conditions = mutableListOf(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                conditions.add(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                conditions.add(ENV_ID.eq(envId))
            }
            if (!pipelineName.isNullOrBlank()) {
                conditions.add(PIPELINE_NAME.like("%$pipelineName%"))
            }
            return dslContext.select(
                PIPELINE_ID,
                PIPELINE_NAME
            ).from(this).where(conditions)
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

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
    fun fetchJobIdAndName(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        jobName: String?
    ): List<Pair<String, String>> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val conditions = mutableListOf(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                conditions.add(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                conditions.add(ENV_ID.eq(envId))
            }
            if (!jobName.isNullOrBlank()) {
                conditions.add(TASK_NAME.like("%$jobName%"))
            }
            return dslContext.select(
                JOB_ID,
                TASK_NAME
            ).from(this).where(conditions)
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

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
    fun fetchCreator(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        creator: String?
    ): List<String> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val conditions = mutableListOf(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                conditions.add(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                conditions.add(ENV_ID.eq(envId))
            }
            if (!creator.isNullOrBlank()) {
                conditions.add(START_USER.like("%$creator%"))
            }
            return dslContext.selectDistinct(
                START_USER
            ).from(this).where(conditions)
                .and(START_USER.isNotNull)
                .groupBy(START_USER)
                .orderBy(ID.desc())
                .fetch()
                .map {
                    it.value1()
                }
        }
    }

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
    fun countAgentBuildGroupsByPipeline(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String,
        status: List<PipelineTaskStatus>?,
        startTime: Long?,
        endTime: Long?
    ): Long {
        if (agentId.isNullOrBlank() && envId == null) {
            return 0
        }
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.select(countDistinct(BUILD_ID, EXECUTE_COUNT)).from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (status != null) {
                dsl.and(STATUS.`in`(status.map { it.status }))
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
            return dsl.fetchOne(0, Long::class.java) ?: 0L
        }
    }

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
    fun listAgentBuildGroupsByPipeline(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String,
        offset: Int,
        limit: Int,
        status: List<PipelineTaskStatus>?,
        startTime: Long?,
        endTime: Long?
    ): List<Pair<String, Int>> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.select(BUILD_ID, EXECUTE_COUNT).from(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .and(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            if (status != null) {
                dsl.and(STATUS.`in`(status.map { it.status }))
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
            return dsl.groupBy(BUILD_ID, EXECUTE_COUNT).orderBy(DSL.max(ID).desc()).limit(limit).offset(offset).fetch()
                .map { Pair(it.value1(), it.value2()) }
        }
    }

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
    fun countAgentBuildGroupsByBuild(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        buildId: String,
        executeCount: Int?,
        status: List<PipelineTaskStatus>?,
        startTime: Long?,
        endTime: Long?
    ): Long {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectCount().from(this)
                .where(BUILD_ID.eq(buildId))
                .and(buildProjectCondition(projectId, envId))
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
            if (status != null) {
                dsl.and(STATUS.`in`(status.map { it.status }))
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
            return dsl.fetchOne(0, Long::class.java) ?: 0L
        }
    }

    // 普通环境和节点支持跨项目查询，所有创作环境按 projectId 隔离
    fun listAgentBuildGroupsByBuild(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        buildId: String,
        executeCount: Int?,
        offset: Int,
        limit: Int,
        status: List<PipelineTaskStatus>?,
        startTime: Long?,
        endTime: Long?
    ): List<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(BUILD_ID.eq(buildId))
                .and(buildProjectCondition(projectId, envId))
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
            if (status != null) {
                dsl.and(STATUS.`in`(status.map { it.status }))
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
            return dsl.orderBy(ID.desc()).limit(limit).offset(offset).fetch()
        }
    }

    fun fetchAgentBuildsByBuildId(
        dslContext: DSLContext,
        projectId: String,
        agentId: String?,
        envId: Long?,
        buildIdList: Set<String>
    ): List<TDispatchThirdpartyAgentBuildRecord> {
        with(TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD) {
            val dsl = dslContext.selectFrom(this)
                .where(BUILD_ID.`in`(buildIdList))
                .and(buildProjectCondition(projectId, envId))
            if (!agentId.isNullOrBlank()) {
                dsl.and(AGENT_ID.eq(agentId))
            }
            if (envId != null) {
                dsl.and(ENV_ID.eq(envId))
            }
            return dsl.fetch()
        }
    }

    /**
     * 所有创作环境的 ID 在各项目间相同，必须同时按项目过滤；普通环境和节点保留跨项目查看能力。
     */
    private fun buildProjectCondition(projectId: String, envId: Long?): Condition =
        if (envId != null && AllCreateNodeEnv.hasId(envId)) {
            TDispatchThirdpartyAgentBuild.T_DISPATCH_THIRDPARTY_AGENT_BUILD.PROJECT_ID.eq(projectId)
        } else {
            DSL.noCondition()
        }
}
