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
                    buildId = buildRecord.filter { !it.buildId.isNullOrBlank() }.map { it.buildId!! }.toSet()
                ).data?.associateBy { it.id } ?: emptyMap()
                buildRecord.forEach {
                    it.buildHistory = buildHistoryMap[it.buildId]?.let { bh ->
                        TPAPipelineBuildHistory(
                            userId = bh.userId,
                            buildNum = bh.buildNum,
                            status = bh.status,
                            totalTime = bh.totalTime,
                            executeTime = bh.executeTime,
                            startTime = bh.startTime,
                            endTime = bh.endTime
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
        val builds = client.get(ServiceBuildResource::class).batchGetBuildStatus(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildIdSet = agentBuilds.map { it.buildId }.toSet()
        ).data?.associateBy { it.id }
        val result = mutableListOf<AgentPipelineContainerBuild>()
        agentBuilds.forEach {
            val build = builds?.get(it.buildId) ?: return@forEach
            result.add(
                AgentPipelineContainerBuild(
                    buildId = build.id,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    containerId = (it.vmSeqId ?: 0).toString(),
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

        val agentBuildCount = thirdPartyAgentBuildDao.countAgentBuildGroupsByBuild(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            pipelineId = pipelineId,
            buildId = null
        )
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuildGroupsByBuild(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            pipelineId = pipelineId,
            buildId = null,
            offset = offset,
            limit = limit
        )
        if (agentBuilds.isEmpty()) {
            return Page(pageNotNull, pageSizeNotNull, agentBuildCount, emptyList())
        }
        val builds = client.get(ServiceBuildResource::class).batchGetBuildStatus(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildIdSet = agentBuilds.map { it.buildId }.toSet()
        ).data?.associateBy { it.id }
        val result = mutableListOf<AgentPipelineContainerBuild>()
        agentBuilds.groupBy { it.buildId }.forEach { (buildId, records) ->
            val build = builds?.get(buildId) ?: return@forEach
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
        return Page(pageNotNull, pageSizeNotNull, agentBuildCount, result)
    }

    fun fetchAgentBuildsByBuild(
        userId: String,
        projectId: String,
        agentId: String?,
        envId: Long?,
        buildId: String,
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
            pipelineId = null,
            buildId = buildId
        )
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuildGroupsByBuild(
            dslContext = dslContext,
            projectId = projectId,
            agentId = agentId,
            envId = envId,
            pipelineId = null,
            buildId = buildId,
            offset = offset,
            limit = limit
        )
        if (agentBuilds.isEmpty()) {
            return Page(pageNotNull, pageSizeNotNull, agentBuildCount, emptyList())
        }
        val builds = agentBuilds.groupBy { it.pipelineId }
            .flatMap { (pipelineId, records) ->
                client.get(ServiceBuildResource::class).batchGetBuildStatus(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildIdSet = records.map { it.buildId }.toSet()
                ).data ?: emptyList()
            }.associateBy { it.id }
        val result = mutableListOf<AgentPipelineContainerBuild>()
        agentBuilds.groupBy { it.buildId }.forEach { (buildId, records) ->
            val build = builds[buildId] ?: return@forEach
            val record = records.first()
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
        private fun String.toStageNumb() = this.removePrefix("$KEY_STAGE-").toIntOrNull()?.let { it - 1 }?.toString()

        private val logger = LoggerFactory.getLogger(ThirdPartyAgentBuildService::class.java)
    }
}