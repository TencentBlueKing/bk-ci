package com.tencent.devops.process.service.pipeline

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.store.api.template.ServiceTemplateResource
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
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val client: Client,
    private val clientTokenService: ClientTokenService
) {

    fun copyAcrossProject(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        pipelineId: String?
    ) {
        if (!pipelineId.isNullOrBlank()) {
            try {
                pipelineInfoDao.getPipelineInfo(
                    dslContext = dslContext,
                    projectId = sourceProjectId,
                    pipelineId = pipelineId,
                    delete = false
                )?.let { sourcePipelineInfo ->
                    copySinglePipeline(
                        userId = userId,
                        sourceProjectId = sourceProjectId,
                        targetProjectId = targetProjectId,
                        sourcePipelineInfo = sourcePipelineInfo
                    )
                } ?: logger.warn("get source pipeline failed|$sourceProjectId|$pipelineId")
            } catch (ignored: Exception) {
                logger.warn("get source pipeline failed|$sourceProjectId|$pipelineId", ignored)
            }
            return
        }
        copyAllPipelinesByPage(userId, sourceProjectId, targetProjectId)
    }

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

    private fun copyAllPipelinesByPage(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String
    ) {
        val pageSize = 100
        var offset = 0
        while (true) {
            val page = pipelineInfoDao.listPipelineInfoByProject(
                dslContext = dslContext,
                projectId = sourceProjectId,
                limit = pageSize,
                offset = offset,
                deleteFlag = false
            ) ?: break
            if (page.isEmpty()) {
                break
            }
            page.forEach { sourcePipelineInfo ->
                copySinglePipeline(
                    userId = userId,
                    sourceProjectId = sourceProjectId,
                    targetProjectId = targetProjectId,
                    sourcePipelineInfo = sourcePipelineInfo
                )
            }
            if (page.size < pageSize) {
                break
            }
            offset += pageSize
        }
    }

    private fun copySinglePipeline(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        sourcePipelineInfo: TPipelineInfoRecord
    ) {
        val copiedPipelineId = sourcePipelineInfo.pipelineId
        try {
            if (shouldSkipPipelineCopy(
                    targetProjectId = targetProjectId,
                    pipelineId = copiedPipelineId,
                    pipelineName = sourcePipelineInfo.pipelineName,
                    channelCode = resolveChannelCode(sourcePipelineInfo)
                )
            ) {
                logger.warn(
                    "pipeline already exists, skip copy|$sourceProjectId|$targetProjectId|$copiedPipelineId"
                )
                return
            }
            val sourceResource = pipelineRepositoryService.getPipelineResourceVersion(
                projectId = sourceProjectId,
                pipelineId = copiedPipelineId
            ) ?: run {
                logger.warn(
                    "source pipeline release version not found|$sourceProjectId|$copiedPipelineId"
                )
                return
            }
            val sourceSetting = pipelineRepositoryService.getSettingByPipelineVersion(
                projectId = sourceProjectId,
                pipelineId = copiedPipelineId,
                pipelineVersion = sourceResource.version
            ) ?: pipelineRepositoryService.getSetting(
                projectId = sourceProjectId,
                pipelineId = copiedPipelineId
            )
            val targetSetting = sourceSetting?.let {
                copyPipelineSetting(
                    sourceSetting = it,
                    targetProjectId = targetProjectId,
                    pipelineId = copiedPipelineId,
                    pipelineName = sourcePipelineInfo.pipelineName
                )
            }
            val pipelineOauthUser = pipelineRepositoryService.getPipelineOauthUser(
                projectId = sourceProjectId,
                pipelineId = copiedPipelineId
            )
            // 校验插件是否在目标项目可见
            client.get(ServiceTemplateResource::class).validateModelComponentVisibleDept(
                userId = userId,
                model = sourceResource.model,
                projectCode = targetProjectId
            )
            pipelineInfoFacadeService.createPipeline(
                userId = pipelineOauthUser ?: userId,
                projectId = targetProjectId,
                model = sourceResource.model,
                channelCode = resolveChannelCode(sourcePipelineInfo),
                setting = targetSetting,
                fixPipelineId = copiedPipelineId,
                instanceType = if (sourceResource.model.instanceFromTemplate == true) {
                    PipelineInstanceTypeEnum.CONSTRAINT.type
                } else {
                    PipelineInstanceTypeEnum.FREEDOM.type
                },
                versionStatus = sourceResource.status,
                branchName = sourceResource.versionName?.takeIf {
                    sourceResource.status == VersionStatus.BRANCH
                }
            )
            copyPipelineGroupMembersSafely(
                sourceProjectId = sourceProjectId,
                targetProjectId = targetProjectId,
                pipelineId = copiedPipelineId
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy pipeline failed|$sourceProjectId|$targetProjectId|$copiedPipelineId",
                ignored
            )
        }
    }

    private fun shouldSkipPipelineCopy(
        targetProjectId: String,
        pipelineId: String,
        pipelineName: String,
        channelCode: ChannelCode
    ): Boolean {
        if (pipelineInfoDao.getPipelineInfo(
                dslContext = dslContext,
                projectId = targetProjectId,
                pipelineId = pipelineId,
                delete = false
            ) != null
        ) {
            return true
        }
        return pipelineInfoFacadeService.isPipelineExist(
            projectId = targetProjectId,
            pipelineId = pipelineId,
            name = pipelineName,
            channelCode = channelCode
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

    private fun resolveChannelCode(sourcePipelineInfo: TPipelineInfoRecord): ChannelCode {
        return ChannelCode.getChannel(sourcePipelineInfo.channel) ?: ChannelCode.BS
    }

    private fun copyPipelineGroupMembersSafely(
        sourceProjectId: String,
        targetProjectId: String,
        pipelineId: String
    ) {
        try {
            client.get(ServiceResourceMemberResource::class).copyResourceGroupMembers(
                token = clientTokenService.getSystemToken() ?: "",
                sourceProjectCode = sourceProjectId,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                sourceResourceCode = pipelineId,
                targetProjectCode = targetProjectId,
                targetResourceCode = pipelineId
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy pipeline group members failed|$sourceProjectId|$targetProjectId|$pipelineId",
                ignored
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyService::class.java)
    }
}
