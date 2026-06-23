package com.tencent.devops.process.service.pipeline

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import com.tencent.devops.model.process.tables.records.TPipelineViewRecord
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceUpdateInfo
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.task.copy.PipelineDependencyReplaceService
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
    private val pipelineResourceDao: PipelineResourceDao,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineTemplateResourceDao: PipelineTemplateResourceDao,
    private val pipelineDependencyReplaceService: PipelineDependencyReplaceService,
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

    fun fixPipelineSubPipelineProject(
        projectId: String,
        pipelineIds: List<String>,
        sourceSubProjectId: String,
        targetSubProjectId: String
    ) {
        pipelineIds.forEach { pipelineId ->
            try {
                fixSinglePipelineSubPipelineProject(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    sourceSubProjectId = sourceSubProjectId,
                    targetSubProjectId = targetSubProjectId
                )
            } catch (ignored: Exception) {
                logger.warn(
                    "fix pipeline sub pipeline project failed|$projectId|$pipelineId|" +
                        "$sourceSubProjectId|$targetSubProjectId",
                    ignored
                )
            }
        }
    }

    fun fixTemplateSubPipelineProject(
        projectId: String,
        templateIds: List<String>,
        sourceSubProjectId: String,
        targetSubProjectId: String
    ) {
        templateIds.forEach { templateId ->
            try {
                fixSingleTemplateSubPipelineProject(
                    projectId = projectId,
                    templateId = templateId,
                    sourceSubProjectId = sourceSubProjectId,
                    targetSubProjectId = targetSubProjectId
                )
            } catch (ignored: Exception) {
                logger.warn(
                    "fix template sub pipeline project failed|$projectId|$templateId|" +
                        "$sourceSubProjectId|$targetSubProjectId",
                    ignored
                )
            }
        }
    }

    private fun fixSinglePipelineSubPipelineProject(
        projectId: String,
        pipelineId: String,
        sourceSubProjectId: String,
        targetSubProjectId: String
    ) {
        val pipelineInfoRecord = pipelineInfoDao.getPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            delete = false
        ) ?: run {
            logger.warn("fix pipeline sub pipeline project skip, pipeline not found|$projectId|$pipelineId")
            return
        }
        val pipelineInfo = pipelineInfoDao.convert(pipelineInfoRecord, null) ?: run {
            logger.warn("fix pipeline sub pipeline project skip, pipeline info invalid|$projectId|$pipelineId")
            return
        }
        fixPipelineReleaseResource(
            projectId = projectId,
            pipelineId = pipelineId,
            sourceSubProjectId = sourceSubProjectId,
            targetSubProjectId = targetSubProjectId
        )
        fixPipelineVersionResources(
            projectId = projectId,
            pipelineId = pipelineId,
            pipelineInfo = pipelineInfo,
            sourceSubProjectId = sourceSubProjectId,
            targetSubProjectId = targetSubProjectId
        )
    }

    private fun fixPipelineReleaseResource(
        projectId: String,
        pipelineId: String,
        sourceSubProjectId: String,
        targetSubProjectId: String
    ) {
        val releaseResource = pipelineResourceDao.getReleaseVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: run {
            logger.warn(
                "fix pipeline sub pipeline project skip, release resource not found|$projectId|$pipelineId"
            )
            return
        }
        val (fixedModel, changed) = pipelineDependencyReplaceService.fixSubPipelineProjectInModel(
            model = releaseResource.model,
            projectId = projectId,
            sourceSubProjectId = sourceSubProjectId,
            targetSubProjectId = targetSubProjectId
        )
        if (!changed) {
            logger.info(
                "fix pipeline sub pipeline project skip, release resource no change|$projectId|$pipelineId"
            )
            return
        }
        pipelineResourceDao.updateModel(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = releaseResource.version,
            model = fixedModel
        )
        logger.info(
            "fix pipeline sub pipeline project release resource success|$projectId|$pipelineId|" +
                "${releaseResource.version}|$sourceSubProjectId|$targetSubProjectId"
        )
    }

    private fun fixPipelineVersionResources(
        projectId: String,
        pipelineId: String,
        pipelineInfo: PipelineInfo,
        sourceSubProjectId: String,
        targetSubProjectId: String
    ) {
        val pageSize = 100
        var offset = 0
        while (true) {
            val versionList = pipelineResourceVersionDao.listPipelineVersion(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineInfo = pipelineInfo,
                includeDraft = true,
                offset = offset,
                limit = pageSize
            )
            if (versionList.isEmpty()) {
                break
            }
            versionList.forEach { versionSimple ->
                try {
                    fixSinglePipelineVersionResource(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        version = versionSimple.version,
                        sourceSubProjectId = sourceSubProjectId,
                        targetSubProjectId = targetSubProjectId
                    )
                } catch (ignored: Exception) {
                    logger.warn(
                        "fix pipeline sub pipeline project version failed|$projectId|$pipelineId|" +
                            "${versionSimple.version}|$sourceSubProjectId|$targetSubProjectId",
                        ignored
                    )
                }
            }
            if (versionList.size < pageSize) {
                break
            }
            offset += pageSize
        }
    }

    private fun fixSinglePipelineVersionResource(
        projectId: String,
        pipelineId: String,
        version: Int,
        sourceSubProjectId: String,
        targetSubProjectId: String
    ) {
        val versionResource = pipelineResourceVersionDao.getVersionResource(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            includeDraft = true
        ) ?: run {
            logger.warn(
                "fix pipeline sub pipeline project skip, version resource not found|" +
                    "$projectId|$pipelineId|$version"
            )
            return
        }
        val (fixedModel, changed) = pipelineDependencyReplaceService.fixSubPipelineProjectInModel(
            model = versionResource.model,
            projectId = projectId,
            sourceSubProjectId = sourceSubProjectId,
            targetSubProjectId = targetSubProjectId
        )
        if (!changed) {
            logger.info(
                "fix pipeline sub pipeline project skip, version resource no change|" +
                    "$projectId|$pipelineId|$version"
            )
            return
        }
        pipelineResourceVersionDao.updateModel(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            model = fixedModel
        )
        logger.info(
            "fix pipeline sub pipeline project version resource success|$projectId|$pipelineId|$version|" +
                "$sourceSubProjectId|$targetSubProjectId"
        )
    }

    private fun fixSingleTemplateSubPipelineProject(
        projectId: String,
        templateId: String,
        sourceSubProjectId: String,
        targetSubProjectId: String
    ) {
        val templateResources = pipelineTemplateResourceDao.list(
            dslContext = dslContext,
            commonCondition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId
            )
        )
        if (templateResources.isEmpty()) {
            logger.warn("fix template sub pipeline project skip, template resource not found|$projectId|$templateId")
            return
        }
        templateResources.forEach { templateResource ->
            try {
                fixSingleTemplateResource(
                    projectId = projectId,
                    templateId = templateId,
                    templateResourceVersion = templateResource.version,
                    model = templateResource.model,
                    sourceSubProjectId = sourceSubProjectId,
                    targetSubProjectId = targetSubProjectId
                )
            } catch (ignored: Exception) {
                logger.warn(
                    "fix template sub pipeline project version failed|$projectId|$templateId|" +
                        "${templateResource.version}|$sourceSubProjectId|$targetSubProjectId",
                    ignored
                )
            }
        }
    }

    private fun fixSingleTemplateResource(
        projectId: String,
        templateId: String,
        templateResourceVersion: Long,
        model: ITemplateModel,
        sourceSubProjectId: String,
        targetSubProjectId: String
    ) {
        if (model !is Model) {
            logger.info(
                "fix template sub pipeline project skip, unsupported template model type|" +
                    "$projectId|$templateId|$templateResourceVersion|${model.javaClass.simpleName}"
            )
            return
        }
        val (fixedModel, changed) = pipelineDependencyReplaceService.fixSubPipelineProjectInModel(
            model = model,
            projectId = projectId,
            sourceSubProjectId = sourceSubProjectId,
            targetSubProjectId = targetSubProjectId
        )
        if (!changed) {
            logger.info(
                "fix template sub pipeline project skip, template resource no change|" +
                    "$projectId|$templateId|$templateResourceVersion"
            )
            return
        }
        pipelineTemplateResourceDao.update(
            dslContext = dslContext,
            record = PipelineTemplateResourceUpdateInfo(model = fixedModel),
            commonCondition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
                version = templateResourceVersion
            )
        )
        logger.info(
            "fix template sub pipeline project success|$projectId|$templateId|$templateResourceVersion|" +
                "$sourceSubProjectId|$targetSubProjectId"
        )
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
