package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildQueueBroadCastEvent
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.template.PipelineTemplateInfoDao
import com.tencent.devops.process.dao.template.PipelineTemplatePipelineVersionDao
import com.tencent.devops.process.dao.template.PipelineTemplateRelatedDao
import com.tencent.devops.process.dao.yaml.PipelineYamlVersionDao
import com.tencent.devops.process.engine.dao.PipelineBuildVersionDiffDao
import com.tencent.devops.process.pojo.BuildVersionDiffInfo
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersionCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedCommonCondition
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
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
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildVersionDiffService::class.java)
    }

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

    @Suppress("CyclomaticComplexMethod")
    fun onBuildQueue(event: PipelineBuildQueueBroadCastEvent) {
        try {
            with(event) {
                logger.info("Start to check build version diff|$projectId|$pipelineId|$buildId")
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
                if (!isPipelineInstanceFromTemplate) return

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
                /**
                 * 判断流水线依赖的模板版本是否发生动态变化（当满足以下所有条件时）：
                 * 1. 当前流水线模板引用类型为路径引用（PATH）
                 * 2. 先前流水线模板引用类型也为路径引用（PATH）
                 * 3. 当前流水线模板版本名与先前相同 或 当前流水线模板引用路径与先前相同
                 * 4. 当前流水线模板版本号与先前不同
                 */
                val refPipelineTemplateVersionChange = currTemplatePipeline.refType == TemplateRefType.PATH &&
                        prevTemplatePipeline.refType == TemplateRefType.PATH &&
                        currTemplatePipeline.inputTemplateRef == prevTemplatePipeline.inputTemplateRef &&
                        currTemplatePipeline.templateVersion != prevTemplatePipeline.templateVersion
                if (!refPipelineTemplateVersionChange) return

                val currTemplateVersionRef = pipelineYamlVersionDao.getPipelineYamlVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = templateInfo.id,
                    version = currTemplatePipeline.templateVersion.toInt()
                )?.commitId ?: run {
                    logger.warn(
                        "current template yaml version not found|" +
                                "$projectId|${templateInfo.id}|${currTemplatePipeline.templateVersion}"
                    )
                    return
                }
                val prevTemplateVersionRef = pipelineYamlVersionDao.getPipelineYamlVersion(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = templateInfo.id,
                    version = prevTemplatePipeline.templateVersion.toInt()
                )?.commitId ?: run {
                    logger.warn(
                        "prev template yaml version not found|" +
                                "$projectId|${templateInfo.id}|${prevTemplatePipeline.templateVersion}"
                    )
                    return
                }
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
        } catch (ex: Exception) {
            logger.warn("Failed to handle build queue event: $event", ex)
        }
    }
}
