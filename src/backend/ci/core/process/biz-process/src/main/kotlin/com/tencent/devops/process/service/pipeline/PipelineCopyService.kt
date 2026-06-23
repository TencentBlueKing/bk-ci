package com.tencent.devops.process.service.pipeline

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.view.PipelineViewService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCopyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewService: PipelineViewService,
    private val client: Client,
    private val clientTokenService: ClientTokenService
) {

    fun fixInstanceSetting(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        sourcePipelineId: String,
        targetPipelineId: String
    ): Boolean {
        try {
            val targetPipelineInfo = pipelineInfoDao.getPipelineInfo(
                dslContext = dslContext,
                projectId = targetProjectId,
                pipelineId = targetPipelineId,
                delete = false
            ) ?: run {
                logger.warn(
                    "fix constraint instance setting skip, target pipeline not found|" +
                        "$sourceProjectId|$targetProjectId|$sourcePipelineId|$targetPipelineId"
                )
                return false
            }
            if (pipelineSettingDao.getSetting(
                    dslContext = dslContext,
                    projectId = targetProjectId,
                    pipelineId = targetPipelineId
                ) != null
            ) {
                logger.info(
                    "fix constraint instance setting skip, setting exists|" +
                        "$targetProjectId|$targetPipelineId"
                )
                return false
            }
            val sourceResource = pipelineRepositoryService.getPipelineResourceVersion(
                projectId = sourceProjectId,
                pipelineId = sourcePipelineId
            ) ?: run {
                logger.warn(
                    "fix constraint instance setting skip, source pipeline not found|" +
                        "$sourceProjectId|$sourcePipelineId"
                )
                return false
            }
            if (sourceResource.model.instanceFromTemplate != true) {
                logger.info(
                    "fix constraint instance setting skip, not constraint instance|" +
                        "$sourceProjectId|$sourcePipelineId"
                )
                return false
            }
            val sourceSetting = pipelineRepositoryService.getSettingByPipelineVersion(
                projectId = sourceProjectId,
                pipelineId = sourcePipelineId,
                pipelineVersion = sourceResource.version
            ) ?: pipelineRepositoryService.getSetting(
                projectId = sourceProjectId,
                pipelineId = sourcePipelineId
            ) ?: run {
                logger.warn(
                    "fix constraint instance setting skip, source setting not found|" +
                        "$sourceProjectId|$sourcePipelineId"
                )
                return false
            }
            val targetSetting = copyPipelineSetting(
                sourceSetting = sourceSetting,
                targetProjectId = targetProjectId,
                pipelineId = targetPipelineId,
                pipelineName = targetPipelineInfo.pipelineName
            )
            val createUserId = pipelineRepositoryService.getPipelineOauthUser(
                projectId = sourceProjectId,
                pipelineId = sourcePipelineId
            ) ?: userId
            pipelineSettingFacadeService.saveSetting(
                userId = createUserId,
                projectId = targetProjectId,
                pipelineId = targetPipelineId,
                setting = targetSetting,
                checkPermission = false,
                updateLabels = false
            )
            logger.info(
                "fix constraint instance setting success|$sourceProjectId|$targetProjectId|" +
                    "$sourcePipelineId|$targetPipelineId"
            )
            return true
        } catch (ignored: Exception) {
            logger.warn(
                "fix constraint instance setting failed|$sourceProjectId|$targetProjectId|" +
                    "$sourcePipelineId|$targetPipelineId",
                ignored
            )
            return false
        }
    }

    fun copyLabelsAcrossProject(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        labelId: String?
    ) {
        if (!labelId.isNullOrBlank()) {
            try {
                pipelineLabelDao.getById(
                    dslContext = dslContext,
                    projectId = sourceProjectId,
                    id = HashUtil.decodeIdToLong(labelId)
                )?.let { sourceLabel ->
                    copySingleLabel(
                        userId = userId,
                        sourceProjectId = sourceProjectId,
                        targetProjectId = targetProjectId,
                        sourceLabel = sourceLabel
                    )
                } ?: logger.warn("get source pipeline label failed|$sourceProjectId|$labelId")
            } catch (ignored: Exception) {
                logger.warn("get source pipeline label failed|$sourceProjectId|$labelId", ignored)
            }
            return
        }
        copyAllLabelsByGroup(userId, sourceProjectId, targetProjectId)
    }

    fun copyViewsAcrossProject(
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

    private fun copyPipelineSetting(
        sourceSetting: PipelineSetting,
        targetProjectId: String,
        pipelineId: String,
        pipelineName: String
    ): PipelineSetting {
        return sourceSetting.copy(
            projectId = targetProjectId,
            pipelineId = pipelineId,
            pipelineName = pipelineName
        )
    }

    private fun copyAllLabelsByGroup(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String
    ) {
        pipelineGroupDao.list(dslContext, sourceProjectId).forEach { sourceGroup ->
            pipelineLabelDao.getByGroupIds(
                dslContext = dslContext,
                projectId = sourceProjectId,
                groupId = setOf(sourceGroup.id)
            ).forEach { sourceLabel ->
                copySingleLabel(
                    userId = userId,
                    sourceProjectId = sourceProjectId,
                    targetProjectId = targetProjectId,
                    sourceLabel = sourceLabel
                )
            }
        }
    }

    private fun copySingleLabel(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        sourceLabel: TPipelineLabelRecord
    ) {
        try {
            val sourceGroup = pipelineGroupDao.get(
                dslContext = dslContext,
                projectId = sourceProjectId,
                groupId = sourceLabel.groupId
            ) ?: run {
                logger.warn(
                    "get source pipeline label group failed|$sourceProjectId|${sourceLabel.groupId}"
                )
                return
            }
            val targetGroup = getOrCreateTargetGroup(
                userId = userId,
                targetProjectId = targetProjectId,
                groupName = sourceGroup.name
            )
            if (pipelineLabelDao.getByName(
                    dslContext = dslContext,
                    projectId = targetProjectId,
                    groupId = targetGroup.id,
                    name = sourceLabel.name
                ) != null
            ) {
                logger.warn(
                    "pipeline label already exists, skip copy|$sourceProjectId|$targetProjectId|${sourceLabel.name}"
                )
                return
            }
            pipelineGroupService.addLabel(
                userId = userId,
                projectId = targetProjectId,
                pipelineLabel = PipelineLabelCreate(
                    groupId = HashUtil.encodeLongId(targetGroup.id),
                    name = sourceLabel.name
                )
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy pipeline label failed|$sourceProjectId|$targetProjectId|${sourceLabel.name}",
                ignored
            )
        }
    }

    private fun getOrCreateTargetGroup(
        userId: String,
        targetProjectId: String,
        groupName: String
    ) = pipelineGroupDao.getByName(
        dslContext = dslContext,
        projectId = targetProjectId,
        name = groupName
    ) ?: run {
        pipelineGroupService.addGroup(
            userId = userId,
            pipelineGroup = PipelineGroupCreate(
                projectId = targetProjectId,
                name = groupName
            )
        )
        pipelineGroupDao.getByName(
            dslContext = dslContext,
            projectId = targetProjectId,
            name = groupName
        ) ?: throw IllegalStateException("create pipeline label group failed|$targetProjectId|$groupName")
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
            copyViewGroupMembersSafely(
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

    private fun copyViewGroupMembersSafely(
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
        private val logger = LoggerFactory.getLogger(PipelineCopyService::class.java)
    }
}
