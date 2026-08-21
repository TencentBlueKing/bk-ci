package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AgentPipelineBuildTask
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AgentPipelineContainerBuild
import com.tencent.devops.dispatch.pojo.thirdpartyagent.JobIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.NodeInfo
import com.tencent.devops.dispatch.pojo.thirdpartyagent.PipelineIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildCountResp
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildHistory
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildView
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineReq
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.thirdpartyagent.BatchFetchNodeInfoData
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BatchFetchBuildRecordData
import com.tencent.devops.process.pojo.BatchFetchContainerRecordData
import com.tencent.devops.process.utils.KEY_STAGE
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId


@Suppress("NestedBlockDepth")
@Service
class ThirdPartyAgentBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao
) {
    // 支持跨项目引用环境和节点的查询，不用projectId
    fun fetchBuildPipelineView(
        userId: String,
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
            agentId = data.agentId,
            envId = envId,
            startTime = data.startTime,
            endTime = data.endTime,
            pipelineId = data.pipelineId,
            jobId = jobId,
            creator = data.creator,
            status = data.taskStatusList
        )
        val records = when (data.view) {
            TPAPipelineBuildView.PIPELINE -> thirdPartyAgentBuildDao.fetchAgentBuildPipeline(
                dslContext = dslContext,
                agentId = data.agentId,
                envId = envId,
                limit = limit,
                offset = offset,
                startTime = data.startTime,
                endTime = data.endTime,
                pipelineId = data.pipelineId,
                creator = data.creator,
                status = data.taskStatusList
            )

            TPAPipelineBuildView.JOB -> thirdPartyAgentBuildDao.fetchAgentBuildPipelineJob(
                dslContext = dslContext,
                agentId = data.agentId,
                envId = envId,
                limit = limit,
                offset = offset,
                startTime = data.startTime,
                endTime = data.endTime,
                pipelineId = data.pipelineId,
                jobId = data.jobId,
                creator = data.creator,
                status = data.taskStatusList
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
                    agentId = data.agentId,
                    envId = envId,
                    limit = limit,
                    offset = offset,
                    startTime = data.startTime,
                    endTime = data.endTime,
                    pipelineId = data.pipelineId,
                    creator = data.creator,
                    status = data.taskStatusList
                )
                val buildHistoryMap = client.get(ServiceBuildResource::class).batchFetchBuildRecordStatus(
                    data = BatchFetchBuildRecordData(buildIds = buildRecord.filter { !it.buildId.isNullOrBlank() }
                        .map { it.buildId!! }, executeCount = null)
                ).data?.groupBy { it.buildId } ?: emptyMap()
                buildRecord.forEach { record ->
                    val histories = buildHistoryMap[record.buildId]
                    val bh = histories?.let { list ->
                        if (record.executeCount == null) {
                            // record 的 executeCount 为空 则取 executeCount 最大的那条
                            list.maxByOrNull { it.executeCount }
                        } else {
                            // record 有 executeCount：一一对应，精确匹配相同 executeCount
                            list.firstOrNull { it.executeCount == record.executeCount }
                        }
                    }

                    record.buildHistory = bh?.let {
                        val startTime = it.startTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                        val endTime = it.endTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                        TPAPipelineBuildHistory(
                            userId = it.startUser,
                            buildNum = record.buildNum,
                            status = it.status,
                            totalTime = if (startTime == null || endTime == null) {
                                null
                            } else {
                                startTime - endTime
                            },
                            startTime = startTime,
                            endTime = endTime,
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
            agentId = agentId,
            envId = envId,
            startTime = startTime,
            endTime = endTime,
            pipelineId = pipelineId,
            jobId = jobId,
            creator = creator,
            status = status?.let { listOf(it) }
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
                    agentId = agentId,
                    envId = envId,
                    limit = limit,
                    offset = offset,
                    startTime = startTime,
                    endTime = endTime,
                    pipelineId = pipelineId,
                    jobId = jobId,
                    creator = creator,
                    status = status?.let { listOf(it) }
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

        val agentBuildCount = thirdPartyAgentBuildDao.countAgentBuildsByJob(
            dslContext = dslContext,
            agentId = agentId,
            envId = envId,
            pipelineId = pipelineId,
            jobId = jobId
        )
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuildsByJob(
            dslContext = dslContext,
            agentId = agentId,
            envId = envId,
            pipelineId = pipelineId,
            jobId = jobId,
            offset = offset,
            limit = limit
        )
        // 获取展示信息，不走鉴权，即使看到了跳转也没权限
        val builds = client.get(ServiceBuildResource::class).batchFetchBuildRecordStatus(
            data = BatchFetchBuildRecordData(buildIds = agentBuilds.map { it.buildId }, executeCount = null)
        ).data?.groupBy { it.buildId }
        val result = mutableListOf<AgentPipelineContainerBuild>()
        // 环境详情需要返回节点信息
        val nodeInfoMap = if (envId != null) {
            client.get(ServiceThirdPartyAgentResource::class).batchFetchNodeInfo(
                projectId = projectId,
                data = BatchFetchNodeInfoData(agentBuilds.map { it.agentId }.toSet())
            ).data?.associateBy { it.agentHashId }
        } else {
            null
        }
        agentBuilds.forEach { record ->
            val buildWithExecuteCount = builds?.get(record.buildId) ?: return@forEach
            val build = buildWithExecuteCount.let { list ->
                if (record.executeCount == null) {
                    // record 的 executeCount 为空 则取 executeCount 最大的那条
                    list.maxByOrNull { it.executeCount }
                } else {
                    // record 有 executeCount：一一对应，精确匹配相同 executeCount
                    list.firstOrNull { it.executeCount == record.executeCount }
                }
            } ?: return@forEach
            result.add(
                AgentPipelineContainerBuild(
                    buildId = build.buildId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    containerId = (record.vmSeqId ?: 0).toString(),
                    executeCount = build.executeCount,
                    status = build.status,
                    startTime = build.startTime,
                    endTime = build.endTime,
                    buildNum = record.buildNum ?: 0,
                    creator = record.startUser,
                    tasks = null,
                    nodeInfo = nodeInfoMap?.get(record.agentId)?.let { NodeInfo(it.agentHashId, it.nodeHashId) }
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
            agentId = agentId,
            envId = envId,
            pipelineId = pipelineId
        )
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuildGroupsByPipeline(
            dslContext = dslContext,
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
        val builds = client.get(ServiceBuildResource::class).batchFetchBuildRecordStatus(
            data = BatchFetchBuildRecordData(buildIds = agentBuilds.map { it.buildId }, executeCount = null)
        ).data?.groupBy { it.buildId }
        val result = mutableListOf<AgentPipelineContainerBuild>()
        // 环境详情需要返回节点信息
        val nodeInfoMap = if (envId != null) {
            client.get(ServiceThirdPartyAgentResource::class).batchFetchNodeInfo(
                projectId = projectId,
                data = BatchFetchNodeInfoData(agentBuilds.map { it.agentId }.toSet())
            ).data?.associateBy { it.agentHashId }
        } else {
            null
        }
        agentBuilds.groupBy { it.buildId }.forEach { (buildId, buildRecords) ->
            buildRecords.groupBy { it.executeCount }.forEach buildRecords@{ (executeCount, records) ->
                val buildWithExecuteCount = builds?.get(buildId) ?: return@forEach
                val build = buildWithExecuteCount.let { list ->
                    if (executeCount == null) {
                        // record 的 executeCount 为空 则取 executeCount 最大的那条
                        list.maxByOrNull { it.executeCount }
                    } else {
                        // record 有 executeCount：一一对应，精确匹配相同 executeCount
                        list.firstOrNull { it.executeCount == executeCount }
                    }
                } ?: return@buildRecords
                val record = records.first()
                result.add(
                    AgentPipelineContainerBuild(
                        buildId = build.buildId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        containerId = (record.vmSeqId ?: 0).toString(),
                        executeCount = build.executeCount,
                        status = build.status,
                        startTime = build.startTime,
                        endTime = build.endTime,
                        buildNum = record.buildNum ?: 0,
                        creator = record.startUser,
                        tasks = records.map {
                            AgentPipelineBuildTask(
                                taskName = it.taskName,
                                vmSeqId = it.vmSeqId,
                                stageId = it.stageId,
                                stageNumb = it.stageId?.toStageNumb() ?: it.stageId
                            )
                        },
                        nodeInfo = nodeInfoMap?.get(record.agentId)?.let { NodeInfo(it.agentHashId, it.nodeHashId) }
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
            agentId = agentId,
            envId = envId,
            buildId = buildId,
            executeCount = executeCount
        )
        val agentBuilds = thirdPartyAgentBuildDao.listAgentBuildGroupsByBuild(
            dslContext = dslContext,
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
        // 获取展示信息，不走鉴权，即使看到了跳转也没权限，这里展示job的状态
        val builds = client.get(ServiceBuildResource::class).fetchContainerRecordStatus(
            data = BatchFetchContainerRecordData(
                buildId = buildId,
                containerIds = agentBuilds.map { it.vmSeqId.toString() },
                executeCount = executeCount ?: 1
            )
        ).data?.associateBy { it.containerId }
        // 环境详情需要返回节点信息
        val nodeInfoMap = if (envId != null) {
            client.get(ServiceThirdPartyAgentResource::class).batchFetchNodeInfo(
                projectId = projectId,
                data = BatchFetchNodeInfoData(agentBuilds.map { it.agentId }.toSet())
            ).data?.associateBy { it.agentHashId }
        } else {
            null
        }
        val result = mutableListOf<AgentPipelineContainerBuild>()
        agentBuilds.forEach { record ->
            val build = builds?.get(record.vmSeqId.toString()) ?: return@forEach
            result.add(
                AgentPipelineContainerBuild(
                    buildId = buildId,
                    projectId = projectId,
                    pipelineId = record.pipelineId,
                    containerId = (record.vmSeqId ?: 0).toString(),
                    executeCount = build.executeCount,
                    status = build.status,
                    startTime = build.startTime,
                    endTime = build.endTime,
                    buildNum = record.buildNum ?: 0,
                    creator = record.startUser,
                    tasks = listOf(
                        AgentPipelineBuildTask(
                            taskName = record.taskName,
                            vmSeqId = record.vmSeqId,
                            stageId = record.stageId,
                            stageNumb = record.stageId?.toStageNumb() ?: record.stageId
                        )
                    ),
                    nodeInfo = nodeInfoMap?.get(record.agentId)?.let { NodeInfo(it.agentHashId, it.nodeHashId) }
                )
            )
        }

        return Page(pageNotNull, pageSizeNotNull, agentBuildCount, result)
    }

    fun fetchPipelineIdAndName(
        agentId: String?,
        envId: Long?,
        pipelineName: String?
    ): List<PipelineIdAndName> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        return thirdPartyAgentBuildDao.fetchPipelineIdAndName(
            dslContext = dslContext,
            agentId = agentId,
            envId = envId,
            pipelineName = pipelineName
        ).map { PipelineIdAndName(it.first, it.second) }
    }

    fun fetchJobIdAndName(
        agentId: String?,
        envId: Long?,
        jobName: String?
    ): List<JobIdAndName> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        return thirdPartyAgentBuildDao.fetchJobIdAndName(
            dslContext = dslContext,
            agentId = agentId,
            envId = envId,
            jobName = jobName
        ).map { JobIdAndName(it.first, it.second) }
    }

    fun fetchCreator(
        agentId: String?,
        envId: Long?,
        creator: String?
    ): List<String> {
        if (agentId.isNullOrBlank() && envId == null) {
            return emptyList()
        }
        return thirdPartyAgentBuildDao.fetchCreator(
            dslContext = dslContext,
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