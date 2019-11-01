package com.tencent.devops.process.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectPipelineService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineService: PipelineService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectPipelineService::class.java)
    }

    fun listPipelinesByProjectIds(
        projectIds: Set<String>,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode?
    ): Page<Pipeline> {
        val projectIdsStr = projectIds.fold("") { s1, s2 -> "$s1:$s2" }
        logger.info("listPipelinesByProjectIds|$projectIdsStr,$page,$pageSize,$channelCode")
        if (projectIds.isEmpty()) return Page(
            count = 0,
            page = PageUtil.getValidPage(page),
            pageSize = PageUtil.getValidPageSize(pageSize),
            totalPages = 1,
            records = listOf()
        )
        var totalCount = 0
        var pipelines = mutableListOf<Pipeline>()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            totalCount = pipelineService.getPipelineInfoNum(
                dslContext = context,
                projectIds = projectIds,
                channelCodes = mutableSetOf(channelCode ?: ChannelCode.BS)
            )!!
            val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
            pipelines = pipelineService.listPagedPipelines(
                dslContext = context,
                projectIds = projectIds,
                channelCodes = mutableSetOf(channelCode ?: ChannelCode.BS),
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )
        }
        // 排序
        pipelineService.sortPipelines(pipelines, PipelineSortType.UPDATE_TIME)
        return Page(
            page = PageUtil.getValidPage(page),
            pageSize = PageUtil.getValidPageSize(pageSize),
            count = totalCount.toLong(),
            records = pipelines
        )
    }
}