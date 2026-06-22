package com.tencent.devops.process.service.view

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.enums.Logic
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineViewCopyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewService: PipelineViewService,
    private val client: Client,
    private val clientTokenService: ClientTokenService
) {

    fun copyAcrossProject(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        viewName: String?
    ) {
        if (!viewName.isNullOrBlank()) {
            pipelineViewDao.fetchAnyByName(
                dslContext = dslContext,
                projectId = sourceProjectId,
                name = viewName,
                isProject = true
            )?.let { sourceView ->
                copySingleView(
                    userId = userId,
                    sourceProjectId = sourceProjectId,
                    targetProjectId = targetProjectId,
                    sourceView = sourceView
                )
            } ?: logger.warn("get source pipeline view failed|$sourceProjectId|$viewName")
            return
        }
        copyAllViewsByPage(userId, sourceProjectId, targetProjectId)
    }

    private fun copyAllViewsByPage(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String
    ) {
        val pageSize = 100
        var offset = 0
        while (true) {
            val page = pipelineViewDao.listByPage(
                dslContext = dslContext,
                projectId = sourceProjectId,
                isProject = true,
                viewName = null,
                limit = pageSize,
                offset = offset
            )
            if (page.isEmpty()) {
                break
            }
            page.forEach { sourceView ->
                copySingleView(
                    userId = userId,
                    sourceProjectId = sourceProjectId,
                    targetProjectId = targetProjectId,
                    sourceView = sourceView
                )
            }
            if (page.size < pageSize) {
                break
            }
            offset += pageSize
        }
    }

    private fun copySingleView(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        sourceView: TPipelineViewRecord
    ) {
        try {
            if (pipelineViewDao.fetchAnyByName(
                    dslContext = dslContext,
                    projectId = targetProjectId,
                    name = sourceView.name,
                    isProject = true
                ) != null
            ) {
                logger.warn(
                    "pipeline view already exists, skip copy|$sourceProjectId|$targetProjectId|${sourceView.name}"
                )
                return
            }
            val targetViewId = pipelineViewService.addView(
                userId = userId,
                projectId = targetProjectId,
                pipelineView = PipelineViewForm(
                    name = sourceView.name,
                    projected = true,
                    viewType = sourceView.viewType,
                    logic = Logic.of(sourceView.logic),
                    filters = pipelineViewService.getFilters(
                        filterByName = sourceView.filterByPipeineName,
                        filterByCreator = sourceView.filterByCreator,
                        filters = sourceView.filters
                    ),
                    pipelineIds = if (sourceView.viewType == PipelineViewType.STATIC) emptyList() else null
                )
            )
            copyPipelineGroupMembersSafely(
                sourceProjectId = sourceProjectId,
                targetProjectId = targetProjectId,
                sourceViewId = sourceView.id,
                targetViewId = targetViewId
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy pipeline view failed|$sourceProjectId|$targetProjectId|${sourceView.name}",
                ignored
            )
        }
    }

    private fun copyPipelineGroupMembersSafely(
        sourceProjectId: String,
        targetProjectId: String,
        sourceViewId: Long,
        targetViewId: Long
    ) {
        try {
            client.get(ServiceResourceMemberResource::class).copyResourceGroupMembers(
                token = clientTokenService.getSystemToken() ?: "",
                sourceProjectCode = sourceProjectId,
                resourceType = AuthResourceType.PIPELINE_GROUP.value,
                sourceResourceCode = HashUtil.encodeLongId(sourceViewId),
                targetResourceCode = HashUtil.encodeLongId(targetViewId),
                targetProjectCode = targetProjectId
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy pipeline group members failed|$sourceProjectId|$targetProjectId|" +
                    "$sourceViewId|$targetViewId",
                ignored
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineViewCopyService::class.java)
    }
}
