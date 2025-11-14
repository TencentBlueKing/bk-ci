/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service.template.v2.version.hander

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.pipeline.DeployTemplateResult
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseReqSource
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileReleaseResult
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceOnlyVersion
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupReferDTO
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelLock
import com.tencent.devops.process.service.template.v2.PipelineTemplatePersistenceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.process.service.`var`.PublicVarGroupReferInfoService
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

/**
 * 发布流水线模版草稿版本
 */
@Service
class PipelineTemplateDraftReleaseHandler @Autowired constructor(
    private val pipelineTemplatePersistenceService: PipelineTemplatePersistenceService,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val redisOperation: RedisOperation,
    @Lazy private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val publicVarGroupReferInfoService: PublicVarGroupReferInfoService
) : PipelineTemplateVersionCreateHandler {
    override fun support(context: PipelineTemplateVersionCreateContext) =
        context.versionAction == PipelineVersionAction.RELEASE_DRAFT

    override fun handle(context: PipelineTemplateVersionCreateContext): DeployTemplateResult {
        logger.info("Template draft version released with context={}", JsonUtil.toJson(context, false))
        with(context) {
            if (version == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("version")
                )
            }
            if (enablePac) {
                if (targetAction == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("targetAction")
                    )
                }
                if (yamlFileInfo == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("yamlFileInfo")
                    )
                }
                if (pTemplateResourceWithoutVersion.yaml == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("yaml content")
                    )
                }
                if (pTemplateResourceWithoutVersion.description == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("description")
                    )
                }
            }
            val lock = PipelineTemplateModelLock(redisOperation = redisOperation, templateId = templateId)
            try {
                lock.lock()
                return doHandle()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineTemplateVersionCreateContext.doHandle(): DeployTemplateResult {
        val draftResource = pipelineTemplateResourceService.get(
            projectId = projectId, templateId = templateId, version = version!!
        )
        if (draftResource.status != VersionStatus.COMMITTING) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_RELEASE_MUST_DRAFT_VERSION)
        }
        val templateSetting = pipelineTemplateSettingService.get(
            projectId = projectId, templateId = templateId, settingVersion = draftResource.settingVersion
        )
        val (versionStatus, resourceOnlyVersion) = pipelineTemplateGenerator.generateDraftReleaseVersion(
            projectId = projectId,
            templateId = templateId,
            draftResource = draftResource,
            draftSetting = templateSetting,
            customVersionName = customVersionName,
            enablePac = enablePac,
            repoHashId = yamlFileInfo?.repoHashId,
            targetAction = targetAction,
            targetBranch = branchName
        )
        validateReleaseYamlFile(resourceOnlyVersion = resourceOnlyVersion)
        if (versionStatus == VersionStatus.RELEASED) {
            pipelineTemplatePersistenceService.releaseDraft2ReleaseVersion(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion
            )
        } else {
            pipelineTemplatePersistenceService.releaseDraft2BranchVersion(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion
            )
        }

        // 发布yaml文件
        val yamlFileReleaseResult = releaseYamlFile(resourceOnlyVersion = resourceOnlyVersion)
        (pTemplateResourceWithoutVersion.model as? Model)?.let {
            publicVarGroupReferInfoService.handleVarGroupReferBus(
                PublicVarGroupReferDTO(
                    userId = userId,
                    projectId = projectId,
                    model = it,
                    referId = templateId,
                    referType = PublicVerGroupReferenceTypeEnum.TEMPLATE,
                    referName = pipelineTemplateInfo.name,
                    referVersion = resourceOnlyVersion.version.toInt(),
                    referVersionName = resourceOnlyVersion.versionName ?: ""
                )
            )
        }
        return DeployTemplateResult(
            projectId = projectId,
            userId = userId,
            version = resourceOnlyVersion.version,
            templateId = templateId,
            templateName = pipelineTemplateInfo.name,
            number = resourceOnlyVersion.number,
            versionNum = resourceOnlyVersion.versionNum,
            versionName = resourceOnlyVersion.versionName,
            targetUrl = yamlFileReleaseResult?.pullRequestUrl,
            versionAction = versionAction
        )
    }

    private fun PipelineTemplateVersionCreateContext.releaseYamlFile(
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ): PipelineYamlFileReleaseResult? {
        if (!enablePac) {
            return null
        }
        val yamlFileReleaseReq = PipelineYamlFileReleaseReq(
            userId = userId,
            projectId = projectId,
            pipelineId = templateId,
            pipelineName = pipelineTemplateInfo.name,
            version = resourceOnlyVersion.version.toInt(),
            versionName = resourceOnlyVersion.versionName,
            repoHashId = yamlFileInfo!!.repoHashId,
            filePath = yamlFileInfo.filePath,
            content = pTemplateResourceWithoutVersion.yaml!!,
            commitMessage = pTemplateResourceWithoutVersion.description
                ?: "update template ${pipelineTemplateInfo.name}",
            targetAction = targetAction!!,
            targetBranch = branchName
        )
        return pipelineYamlFacadeService.releaseYamlFile(
            yamlFileReleaseReq = yamlFileReleaseReq
        )
    }

    private fun PipelineTemplateVersionCreateContext.validateReleaseYamlFile(
        resourceOnlyVersion: PTemplateResourceOnlyVersion
    ) {
        if (!enablePac) {
            return
        }
        val yamlFileReleaseReq = PipelineYamlFileReleaseReq(
            userId = userId,
            projectId = projectId,
            pipelineId = templateId,
            pipelineName = pipelineTemplateInfo.name,
            version = resourceOnlyVersion.version.toInt(),
            versionName = resourceOnlyVersion.versionName,
            repoHashId = yamlFileInfo!!.repoHashId,
            filePath = yamlFileInfo.filePath,
            content = pTemplateResourceWithoutVersion.yaml!!,
            commitMessage = pTemplateResourceWithoutVersion.description
                ?: "update template ${pipelineTemplateInfo.name}",
            targetAction = targetAction!!,
            targetBranch = branchName,
            source = PipelineYamlFileReleaseReqSource.TEMPLATE
        )
        pipelineYamlFacadeService.validateReleaseYamlFile(
            yamlFileReleaseReq = yamlFileReleaseReq
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateDraftReleaseHandler::class.java)
    }
}
