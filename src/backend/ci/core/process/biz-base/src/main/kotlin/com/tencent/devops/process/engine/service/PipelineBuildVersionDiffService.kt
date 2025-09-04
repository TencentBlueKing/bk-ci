package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.template.PipelineTemplateInfoDao
import com.tencent.devops.process.dao.template.PipelineTemplatePipelineVersionDao
import com.tencent.devops.process.dao.template.PipelineTemplateRelatedDao
import com.tencent.devops.process.dao.yaml.PipelineYamlVersionDao
import com.tencent.devops.process.engine.dao.PipelineBuildVersionDiffDao
import com.tencent.devops.process.pojo.BuildVersionDiffInfo
import com.tencent.devops.process.pojo.template.TemplateRefType
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersionCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedCommonCondition
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class PipelineBuildVersionDiffService(
    val dslContext: DSLContext,
    val pipelineBuildVersionDiffDao: PipelineBuildVersionDiffDao,
    val pipelineRuntimeService: PipelineRuntimeService,
    val pipelineTemplateRelatedDao: PipelineTemplateRelatedDao,
    val pipelineTemplatePipelineVersionDao: PipelineTemplatePipelineVersionDao,
    val pipelineTemplateInfoDao: PipelineTemplateInfoDao,
    val pipelineYamlVersionDao: PipelineYamlVersionDao
) {
    fun list(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<BuildVersionDiffInfo> {
        return pipelineBuildVersionDiffDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
    }

    fun onBuildQueue(event: PipelineBuildQueueBroadCastEvent) {
        with(event) {
            val currBuildInfo = pipelineRuntimeService.getBuildInfo(
                projectId = projectId, pipelineId = pipelineId, buildId = buildId
            ) ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
            if (currBuildInfo.buildNum == 1 || currBuildInfo.versionChange != null || currBuildInfo.debug) {
                return
            }
            val prevBuildInfo = pipelineRuntimeService.getBuildInfoByBuildNum(
                projectId = projectId,
                pipelineId = pipelineId,
                buildNum = currBuildInfo.buildNum - 1
            ) ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
            pipelineRuntimeService.updateBuildVersionChangeFlag(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                versionChange = currBuildInfo.version != prevBuildInfo.version
            )
            val isPipelineInstanceFromTemplate = pipelineTemplateRelatedDao.get(
                dslContext = dslContext,
                condition = PipelineTemplateRelatedCommonCondition(
                    projectId = projectId,
                    pipelineId = pipelineId
                )
            )?.let { it.instanceType == PipelineInstanceTypeEnum.CONSTRAINT } ?: false

            if (isPipelineInstanceFromTemplate) {
                val currTemplatePipeline = pipelineTemplatePipelineVersionDao.get(
                    dslContext = dslContext,
                    condition = PTemplatePipelineVersionCommonCondition(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        pipelineVersion = currBuildInfo.version
                    )
                )
                val prevTemplatePipeline = pipelineTemplatePipelineVersionDao.get(
                    dslContext = dslContext,
                    condition = PTemplatePipelineVersionCommonCondition(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        pipelineVersion = prevBuildInfo.version
                    )
                )

                if (currTemplatePipeline == null || prevTemplatePipeline == null)
                    return

                val templateInfo = pipelineTemplateInfoDao.get(
                    dslContext = dslContext,
                    templateId = currTemplatePipeline.templateId
                )!!

                val refPipelineTemplateVersionChange = currTemplatePipeline.refType == TemplateRefType.PATH &&
                    prevTemplatePipeline.refType == TemplateRefType.PATH &&
                    currTemplatePipeline.templateVersionName == prevTemplatePipeline.templateVersionName &&
                    currTemplatePipeline.templateVersion != prevTemplatePipeline.templateVersion

                val currTemplateVersionRef = pipelineYamlVersionDao.getPipelineYamlVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = currTemplatePipeline.templateVersion.toInt()
                )!!.commitId

                val prevTemplateVersionRef = pipelineYamlVersionDao.getPipelineYamlVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    version = prevTemplatePipeline.templateVersion.toInt()
                )!!.commitId

                if (refPipelineTemplateVersionChange) {
                    pipelineBuildVersionDiffDao.create(
                        dslContext = dslContext,
                        buildVersionDiffInfo = BuildVersionDiffInfo(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            templateType = PipelineTemplateType.PIPELINE,
                            templateName = templateInfo.name,
                            templateId = templateInfo.id,
                            templateVersionName = currTemplatePipeline.templateVersionName,
                            currTemplateVersion = currTemplatePipeline.templateVersion,
                            prevTemplateVersion = prevTemplatePipeline.templateVersion,
                            currTemplateVersionRef = currTemplateVersionRef,
                            prevTemplateVersionRef = prevTemplateVersionRef
                        )
                    )
                }
            }
        }
    }
}
