package com.tencent.devops.process.service.pipeline.version.convert

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.engine.service.PipelineRepositoryVersionService
import com.tencent.devops.process.pojo.pipeline.version.PipelineRollbackDraftReq
import com.tencent.devops.process.pojo.pipeline.version.PipelineVersionCreateReq
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContextParam
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineRollbackDraftReqConverter @Autowired constructor(
    private val pipelineInfoService: PipelineInfoService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val dslContext: DSLContext,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineRepositoryVersionService: PipelineRepositoryVersionService,
    private val pipelineVersionCreateContextFactory: PipelineVersionCreateContextFactory,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
) : PipelineVersionCreateReqConverter {
    override fun support(request: PipelineVersionCreateReq): Boolean {
        return request is PipelineRollbackDraftReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        pipelineId: String?,
        version: Int?,
        request: PipelineVersionCreateReq
    ): PipelineVersionCreateContext {
        request as PipelineRollbackDraftReq
        if (pipelineId == null) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("pipelineId")
            )
        }
        if (version == null) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                params = arrayOf("version")
            )
        }
        val draftVersion = request.draftVersion
        logger.info(
            "Start to convert rollback request|$projectId|$pipelineId|$version|$draftVersion"
        )
        val pipelineInfo = pipelineInfoService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        // 获取目标的版本用于更新草稿
        val targetResource = if (draftVersion != null) {
            pipelineRepositoryVersionService.getPipelineDraftVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                draftVersion = draftVersion
            )
        } else {
            pipelineResourceVersionDao.getVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                includeDraft = true
            )
        } ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_VERSION_EXISTS_BY_ID,
            params = arrayOf(version.toString())
        )
        val targetSetting = targetResource.settingVersion?.let {
            pipelineSettingFacadeService.userGetSetting(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                version = it
            )
        } ?: pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val isPipelineInstanceFromTemplate = pipelineTemplateRelatedService.isPipelineInstanceFromTemplate(
            projectId = projectId,
            pipelineId = pipelineId
        )
        // 存量的实例化版本，不支持一键回滚
        if (isPipelineInstanceFromTemplate && targetResource.model.template == null) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_LEGACY_INSTANCE_CANNOT_ROLLBACK
            )
        }
        val contextParam = PipelineVersionCreateContextParam(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = pipelineInfo.channelCode,
            version = version,
            model = targetResource.model,
            yaml = targetResource.yaml,
            baseVersion = targetResource.baseVersion,
            pipelineSettingWithoutVersion = targetSetting,
            versionStatus = VersionStatus.COMMITTING,
            versionAction = PipelineVersionAction.SAVE_DRAFT,
            baseDraftVersion = draftVersion
        )

        return pipelineVersionCreateContextFactory.create(
            contextParam = contextParam
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRollbackDraftReqConverter::class.java)
    }
}
