package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AgentPipelineBuildTask
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AgentPipelineContainerBuild
import com.tencent.devops.dispatch.pojo.thirdpartyagent.JobIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.PipelineIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildCountResp
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildHistory
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildView
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineReq
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.utils.KEY_STAGE
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import kotlin.collections.get

@Service
class ThirdPartyAgentBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao
) {
    fun fetchBuildPipelineView(
        userId: String,
        projectId: String,
        envId: Long?,
        data: TPAPipelineReq
    ): TPAPipelineBuildCountResp {
        if (data.agentId.isNullOrBlank() && data.envId == null) {
            return TPAPipelineBuildCountResp(0L, 0L, 0L, Page(0, 0, 0, emptyList()))
        }
        val pageNotNull = data.page ?: 0
        val pageSizeNotNull = data.pageSize ?: 10
        val sqlLimit = if (pageSizeNotNull != -1) {
            PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        } else {
            null
        }
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 100
        val jobId = if (data.view == TPAPipelineBuildView.JOB) data.jobId else null
        val (pipelineCount, jobCount, buildCount) = thirdPartyAgentBuildDao.countAgentBuildPipelineJob(
            dslContext = dslContext,
            projectId = projectId,
            agentId = data.agentId,
            envId = envId,
            startTime = data.startTime,
            endTime = data.endTime,
            pipelineId = data.pipelineId,
            jobId = jobId,
            creator = data.creator,
            status = data.taskStatus
        )
        val records = when (data.view) {
            TPAPipelineBuildView.PIPELINE -> thirdPartyAgentBuildDao.fetchAgentBuildPipeline(
                dslContext = dslContext,
                projectId = projectId,
                agentId = data.agentId,
                envId = envId,
                limit = limit,
                offset = offset,
                startTime = data.startTime,
                endTime = data.endTime,
                pipelineId = data.pipelineId,
                creator = data.creator,
                status = data.taskStatus
            )

            TPAPipelineBuildView.JOB -> thirdPartyAgentBuildDao.fetchAgentBuildPipelineJob(
                dslContext = dslContext,
                projectId = projectId,
                agentId = data.agentId,
                envId = envId,
                limit = limit,
                offset = offset,
                startTime = data.startTime,
                endTime = data.endTime,
                pipelineId = data.pipelineId,
                jobId = data.jobId,
                creator = data.creator,
                status = data.taskStatus
            ).let { list ->
                // 处理下stage序号计算
                list.forEach { item ->
                    item.stageNumb = item.stageId?.toStageNumb() ?: item.stageId
                }
                list
            }

            TPAPipelineBuildView.BUILD -> {
                val buildRecord = thirdPartyAgentBuildDao.fetchAgentBuildPipelineBuild(
                    dslContext = dslContext,
                    projectId = projectId,
                    agentId = data.agentId,
                    envId = envId,
                    limit = limit,
                    offset = offset,
                    startTime = data.startTime,
                    endTime = data.endTime,
                    pipelineId = data.pipelineId,
                    creator = data.creator,
                    status = data.taskStatus
                )
                val buildHistoryMap = client.get(ServiceBuildResource::class).getBatchBuildStatus(
                    projectId = projectId,
                    buildId = buildRecord.filter { !it.buildId.isNullOrBlank() }.map { it.buildId!! }.toSet(),
                    withExecuteCount = true
                ).data?.groupBy { it.id } ?: emptyMap()
                buildRecord.forEach { record ->
                    val histories = buildHistoryMap[record.buildId]
                    val bh = histories?.let { list ->
                        if (record.executeCount == null) {
                            // record 的 executeCount 为空：优先取 BuildHistory 中 executeCount 最大的那条
                            // 没有则取 executeCount 也为空的，
                            list.maxByOrNull { it.executeCount ?: -1 } ?: list.firstOrNull { it.executeCount == null }
                        } else {
                            // record 有 executeCount：一一对应，精确匹配相同 executeCount
                            list.firstOrNull { it.executeCount == record.executeCount }
                        }
                    }
                    record.buildHistory = bh?.let {
                        TPAPipelineBuildHistory(
                            userId = it.userId,
                            buildNum = it.buildNum,
                            status = it.status,
                            totalTime = it.totalTime,
                            executeTime = it.executeTime,
                            startTime = it.startTime,
                            endTime = it.endTime,
                            executeCount = it.executeCount
                        )
                    }
                }
                buildRecord
            }
        }
        val count = when (data.view) {
            TPAPipelineBuildView.PIPELINE -> pipelineCount
            TPAPipelineBuildView.JOB -> jobCount
            TPAPipelineBuildView.BUILD -> buildCount
        }
        return TPAPipelineBuildCountResp(
            pipelineCount = pipelineCount,
            jobCount = jobCount,
            buildCount = buildCount,
            result = Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = count,
                records = records
            )
        )
    }

    @Deprecated("fetchBuildPipelineView")
    fun fetchBuildPipeline(
        projectId: String,
        agentId: String?,
        envId: Long?,
        page: Int?,
        pageSize: Int?,
        startTime: Long?,
        endTime: Long?,
        pipelineId: String?,
        jobId: String?,
        creator: String?,
        status: PipelineTaskStatus?
    ): TPAPipelineBuildCountResp {
        if (agentId.isNullOrBlank() && envId == null) {
            return TPAPipelineBuildCountResp(0L, 0L, 0L, Page(0, 0, 0, emptyList()))
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 10
        val sqlLimit = if (pageSizeNotNull != -1) {
            PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        } else {
            null
        }
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 100
        val (pipelineCount, jobCount, buildCount) = thirdPartyAgentBuildDao.countAgentBuildPipelineJob(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            startTime = startTime,
            endTime = endTime,
            pipelineId = pipelineId,
            jobId = jobId,
            creator = creator,
            status = status
        )
        return TPAPipelineBuildCountResp(
            pipelineCount = pipelineCount,
            jobCount = jobCount,
            buildCount = buildCount,
            result = Page(
                page = pageNotNull,
                pageSize = pageSizeNotNull,
                count = jobCount,
                records = thirdPartyAgentBuildDao.fetchAgentBuildPipelineJob(
                    dslContext = dslContext,
                    projectId = projectId,
                    agentId = agentId,
                    envId = envId,
                    limit = limit,
                    offset = offset,
                    startTime = startTime,
                    endTime = endTime,
                    pipelineId = pipelineId,
                    jobId = jobId,
                    creator = creator,
                    status = status
                ).let { list ->
                    // 处理下stage序号计算
                    list.forEach { item ->
                        item.stageNumb = item.stageId?.toStageNumb() ?: item.stageId
                    }
                    list
                }
            )
        )
    }

    fun fetchAgentBuildsByJob(
        userId: String,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String,
        jobId: String,
        page: Int?,
        pageSize: Int?
    ): Page<AgentPipelineContainerBuild> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val offset = sqlLimit.offset
        val limit = sqlLimit.limit

        val agentBuildCount = thirdPartyAgentBuildDao.countAgentBuildsProject(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            pipelineId = pipelineId,
            jobId = jobId
        )
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuildsByProject(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            pipelineId = pipelineId,
            jobId = jobId,
            offset = offset,
            limit = limit
        )
        // 获取展示信息，不走鉴权，即使看到了跳转也没权限
        val builds = client.get(ServiceBuildResource::class).getBatchBuildStatus(
            projectId = projectId,
            buildId = agentBuilds.map { it.buildId }.toSet(),
            withExecuteCount = true
        ).data?.groupBy { it.id }
        val result = mutableListOf<AgentPipelineContainerBuild>()
        agentBuilds.forEach { record ->
            val buildWithExecuteCount = builds?.get(record.buildId) ?: return@forEach
            val build = buildWithExecuteCount.let { list ->
                if (record.executeCount == null) {
                    // record 的 executeCount 为空：优先取 BuildHistory 中 executeCount 也为空的，
                    // 没有则取 executeCount 最大的那条
                    list.firstOrNull { it.executeCount == null }
                        ?: list.maxByOrNull { it.executeCount ?: -1 }
                } else {
                    // record 有 executeCount：一一对应，精确匹配相同 executeCount
                    list.firstOrNull { it.executeCount == record.executeCount }
                }
            } ?: return@forEach
            result.add(
                AgentPipelineContainerBuild(
                    buildId = build.id,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    containerId = (record.vmSeqId ?: 0).toString(),
                    executeCount = build.executeCount ?: 1,
                    status = build.status,
                    startTime = Instant.ofEpochMilli(build.startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
                    endTime = build.endTime?.let { ed ->
                        Instant.ofEpochMilli(ed)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    },
                    buildNum = build.buildNum ?: 0,
                    creator = build.userId
                )
            )
        }
        return Page(pageNotNull, pageSizeNotNull, agentBuildCount, result)
    }

    fun fetchAgentBuildsByPipeline(
        userId: String,
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineId: String,
        page: Int?,
        pageSize: Int?
    ): Page<AgentPipelineContainerBuild> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val offset = sqlLimit.offset
        val limit = sqlLimit.limit

        val agentBuildCount = thirdPartyAgentBuildDao.countAgentBuildGroupsByPipeline(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            pipelineId = pipelineId
        )
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuildGroupsByPipeline(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            pipelineId = pipelineId,
            offset = offset,
            limit = limit
        )
        if (agentBuilds.isEmpty()) {
            return Page(pageNotNull, pageSizeNotNull, agentBuildCount, emptyList())
        }
        // 获取展示信息，不走鉴权，即使看到了跳转也没权限
        val builds = client.get(ServiceBuildResource::class).getBatchBuildStatus(
            projectId = projectId,
            buildId = agentBuilds.map { it.buildId }.toSet(),
            withExecuteCount = true
        ).data?.groupBy { it.id }
        val result = mutableListOf<AgentPipelineContainerBuild>()
        agentBuilds.groupBy { it.buildId }.forEach { (buildId, buildRecords) ->
            buildRecords.groupBy { it.executeCount }.forEach buildRecords@{ (executeCount, records) ->
                val buildWithExecuteCount = builds?.get(buildId) ?: return@forEach
                val build = buildWithExecuteCount.let { list ->
                    if (executeCount == null) {
                        // record 的 executeCount 为空：优先取 BuildHistory 中 executeCount 也为空的，
                        // 没有则取 executeCount 最大的那条
                        list.firstOrNull { it.executeCount == null }
                            ?: list.maxByOrNull { it.executeCount ?: -1 }
                    } else {
                        // record 有 executeCount：一一对应，精确匹配相同 executeCount
                        list.firstOrNull { it.executeCount == executeCount }
                    }
                } ?: return@buildRecords
                val record = records.first()
                result.add(
                    AgentPipelineContainerBuild(
                        buildId = build.id,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        containerId = (record.vmSeqId ?: 0).toString(),
                        executeCount = build.executeCount ?: 1,
                        status = build.status,
                        startTime = Instant.ofEpochMilli(build.startTime)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime(),
                        endTime = build.endTime?.let { ed ->
                            Instant.ofEpochMilli(ed)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                        },
                        buildNum = build.buildNum ?: 0,
                        creator = build.userId,
                        tasks = records.map {
                            AgentPipelineBuildTask(
                                taskName = it.taskName,
                                vmSeqId = it.vmSeqId,
                                stageId = it.stageId,
                                stageNumb = it.stageId?.toStageNumb() ?: it.stageId
                            )
                        }
                    )
                )
            }
        }
        return Page(pageNotNull, pageSizeNotNull, agentBuildCount, result)
    }

    fun fetchAgentBuildsByBuild(
        userId: String,
        projectId: String,
        agentId: String?,
        envId: Long?,
        buildId: String,
        executeCount: Int?,
        page: Int?,
        pageSize: Int?
    ): Page<AgentPipelineContainerBuild> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val offset = sqlLimit.offset
        val limit = sqlLimit.limit

        val agentBuildCount = thirdPartyAgentBuildDao.countAgentBuildGroupsByBuild(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            buildId = buildId,
            executeCount = executeCount
        )
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuildGroupsByBuild(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            buildId = buildId,
            executeCount = executeCount,
            offset = offset,
            limit = limit
        )
        if (agentBuilds.isEmpty()) {
            return Page(pageNotNull, pageSizeNotNull, agentBuildCount, emptyList())
        }
        // 获取展示信息，不走鉴权，即使看到了跳转也没权限
        val builds = client.get(ServiceBuildResource::class).getBatchBuildStatus(
            projectId = projectId,
            buildId = agentBuilds.map { it.buildId }.toSet(),
            withExecuteCount = true
        ).data?.groupBy { it.id }
        val result = mutableListOf<AgentPipelineContainerBuild>()
        agentBuilds.forEach { record ->
            val buildWithExecuteCount = builds?.get(buildId) ?: return@forEach
            val build = buildWithExecuteCount.let { list ->
                if (executeCount == null) {
                    // record 的 executeCount 为空：优先取 BuildHistory 中 executeCount 也为空的，
                    // 没有则取 executeCount 最大的那条
                    list.firstOrNull { it.executeCount == null }
                        ?: list.maxByOrNull { it.executeCount ?: -1 }
                } else {
                    // record 有 executeCount：一一对应，精确匹配相同 executeCount
                    list.firstOrNull { it.executeCount == executeCount }
                }
            } ?: return@forEach
            result.add(
                AgentPipelineContainerBuild(
                    buildId = build.id,
                    projectId = projectId,
                    pipelineId = record.pipelineId,
                    containerId = (record.vmSeqId ?: 0).toString(),
                    executeCount = build.executeCount ?: 1,
                    status = build.status,
                    startTime = Instant.ofEpochMilli(build.startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
                    endTime = build.endTime?.let { ed ->
                        Instant.ofEpochMilli(ed)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime()
                    },
                    buildNum = build.buildNum ?: 0,
                    creator = build.userId,
                    tasks = listOf(
                        AgentPipelineBuildTask(
                            taskName = record.taskName,
                            vmSeqId = record.vmSeqId,
                            stageId = record.stageId,
                            stageNumb = record.stageId?.toStageNumb() ?: record.stageId
                        )

                    )
                )
            )
        }

        return Page(pageNotNull, pageSizeNotNull, agentBuildCount, result)
    }

    fun fetchPipelineIdAndName(
        projectId: String,
        agentId: String?,
        envId: Long?,
        pipelineName: String?
    ): List<PipelineIdAndName> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        return thirdPartyAgentBuildDao.fetchPipelineIdAndName(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            pipelineName = pipelineName
        ).map { PipelineIdAndName(it.first, it.second) }
    }

    fun fetchJobIdAndName(
        projectId: String,
        agentId: String?,
        envId: Long?,
        jobName: String?
    ): List<JobIdAndName> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        return thirdPartyAgentBuildDao.fetchJobIdAndName(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            jobName = jobName
        ).map { JobIdAndName(it.first, it.second) }
    }

    fun fetchCreator(
        projectId: String,
        agentId: String?,
        envId: Long?,
        creator: String?
    ): List<String> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        return thirdPartyAgentBuildDao.fetchCreator(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            creator = creator
        )
    }

    companion object {
        private fun String.toStageNumb() =
            this.removePrefix("$KEY_STAGE-").toIntOrNull()?.let { it - 1 }?.toString()

        private val logger = LoggerFactory.getLogger(ThirdPartyAgentBuildService::class.java)
    }
}