package com.tencent.devops.process.service.pipeline.version.convert

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.version.PipelineRollbackReq
import com.tencent.devops.process.pojo.pipeline.version.PipelineVersionCreateReq
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContextParam
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineRollbackReqConverter @Autowired constructor(
    private val pipelineInfoService: PipelineInfoService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineVersionCreateContextFactory: PipelineVersionCreateContextFactory,
    private val pipelineRepositoryService: PipelineRepositoryService
) : PipelineVersionCreateReqConverter {
    override fun support(request: PipelineVersionCreateReq): Boolean {
        return request is PipelineRollbackReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        pipelineId: String?,
        version: Int?,
        request: PipelineVersionCreateReq
    ): PipelineVersionCreateContext {
        request as PipelineRollbackReq
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
            pipelineRepositoryService.getPipelineResourceByDraftVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                draftVersion = draftVersion
            )
        } else {
            pipelineRepositoryService.getPipelineResourceVersion(
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
        val targetSetting = if (draftVersion != null) {
            pipelineSettingFacadeService.getPipelineSettingByDraftVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                draftVersion = draftVersion
            )
        } else {
            targetResource.settingVersion?.let {
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
        private val logger = LoggerFactory.getLogger(PipelineRollbackReqConverter::class.java)
    }
}
