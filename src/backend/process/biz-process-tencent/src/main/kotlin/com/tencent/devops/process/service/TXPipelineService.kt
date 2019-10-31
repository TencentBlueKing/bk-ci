package com.tencent.devops.process.service

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BkAuthServiceCode
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.quality.pojo.PipelineListRequest
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.app.PipelinePage
import com.tencent.devops.process.pojo.classify.PipelineViewAndPipelines
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByCreator
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByLabel
import com.tencent.devops.process.pojo.classify.PipelineViewFilterByName
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import com.tencent.devops.process.pojo.quality.QualityPipeline
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_FAVORITE_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException


/**
 *
 * @author irwinsun
 * @version 1.0
 */
@Service("newPipelineService")
class TXPipelineService @Autowired constructor(
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineService: PipelineService,
    private val processJmxApi: ProcessJmxApi
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TXPipelineService::class.java)
    }

    fun listQualityViewPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType,
        channelCode: ChannelCode,
        viewId: String,
        checkPermission: Boolean = true,
        filterByPipelineName: String? = null,
        filterByCreator: String? = null,
        filterByLabels: String? = null,
        callByApp: Boolean? = false,
        authPipelineIds: List<String> = emptyList(),
        skipPipelineIds: List<String> = emptyList()
    ): PipelineViewPipelinePage<QualityPipeline> {

        val watch = StopWatch()
        watch.start("perm_r_perm")
        val authPipelines = if (authPipelineIds.isEmpty()) {
            bkAuthPermissionApi.getUserResourceByPermission(
                userId, bsPipelineAuthServiceCode,
                AuthResourceType.PIPELINE_DEFAULT, projectId, AuthPermission.LIST,
                null
            )
        } else {
            authPipelineIds
        }
        watch.stop()

        watch.start("s_r_summary")
        val pipelineBuildSummary = pipelineRuntimeService.getBuildSummaryRecords(projectId, channelCode)
        watch.stop()

        watch.start("s_r_fav")
        val skipPipelineIdsNew = mutableListOf<String>()
        if (pipelineBuildSummary.isNotEmpty) {
            pipelineBuildSummary.forEach {
                skipPipelineIdsNew.add(it["PIPELINE_ID"] as String)
            }
        }
        if (skipPipelineIds.isNotEmpty()) {
            skipPipelineIdsNew.addAll(skipPipelineIds)
        }

        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        var slqLimit: SQLLimit? = null
        if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

        val offset = slqLimit?.offset ?: 0
        val limit = slqLimit?.limit ?: -1
        var count = 0L
        try {

            val list = if (pipelineBuildSummary.isNotEmpty) {

                val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
                val pipelines = pipelineService.buildPipelines(pipelineBuildSummary, favorPipelines, authPipelines)
                val allFilterPipelines =
                    pipelineService.filterViewPipelines(pipelines, filterByPipelineName, filterByCreator, filterByLabels)

                val hasPipelines = allFilterPipelines.isNotEmpty()

                if (!hasPipelines) {
                    return PipelineViewPipelinePage(pageNotNull, pageSizeNotNull, 0, emptyList())
                }

                val filterPipelines = when (viewId) {
                    PIPELINE_VIEW_FAVORITE_PIPELINES -> {
                        logger.info("User($userId) favorite pipeline ids($favorPipelines)")
                        allFilterPipelines.filter { favorPipelines.contains(it.pipelineId) }
                    }
                    PIPELINE_VIEW_MY_PIPELINES -> {
                        logger.info("User($userId) my pipelines")
                        allFilterPipelines.filter {
                            authPipelines.contains(it.pipelineId)
                        }
                    }
                    PIPELINE_VIEW_ALL_PIPELINES -> {
                        logger.info("User($userId) all pipelines")
                        allFilterPipelines
                    }
                    else -> {
                        logger.info("User($userId) filter view($viewId)")
                        pipelineService.filterViewPipelines(userId, projectId, allFilterPipelines, viewId)
                    }
                }

                val permissionList = filterPipelines.filter { it.hasPermission }
                pipelineService.sortPipelines(permissionList, sortType)
                count = permissionList.size.toLong()

                val toIndex =
                    if (limit == -1 || permissionList.size <= (offset + limit)) permissionList.size else offset + limit

                if (offset >= permissionList.size) listOf() else permissionList.subList(offset, toIndex)
            } else {
                emptyList()
            }
            watch.stop()

            val records = list.map {
                QualityPipeline(
                    it.projectId,
                    it.pipelineId,
                    it.pipelineName,
                    it.pipelineDesc,
                    it.taskCount,
                    it.buildCount,
                    it.latestBuildStartTime,
                    it.latestBuildEndTime
                )
            }
            return PipelineViewPipelinePage(pageNotNull, pageSizeNotNull, count, records)
        } finally {
            logger.info("listViewPipelines|[$projectId]|$userId|watch=$watch")
            processJmxApi.execute(ProcessJmxApi.LIST_NEW_PIPELINES, watch.totalTimeMillis)
        }
    }

    fun listPipelineInfo(userId: String, projectId: String, request: PipelineListRequest?): List<Pipeline> {
        return pipelineService.listPipelineInfo(userId, projectId, request?.pipelineId, request?.templateId)
    }
}
