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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.pipeline.DeployTemplateResult
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceOnlyVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelLock
import com.tencent.devops.process.service.template.v2.PipelineTemplatePersistenceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 创建或更新流水线模版草稿版本
 */
@Service
class PipelineTemplateDraftSaveHandler @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplatePersistenceService: PipelineTemplatePersistenceService,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val redisOperation: RedisOperation
) : PipelineTemplateVersionCreateHandler {
    override fun support(context: PipelineTemplateVersionCreateContext): Boolean {
        return context.versionAction == PipelineVersionAction.SAVE_DRAFT
    }

    override fun handle(context: PipelineTemplateVersionCreateContext): DeployTemplateResult {
        with(context) {
            logger.info(
                "handle save template draft version|$projectId|$templateId|$versionAction|$version"
            )
            if (pTemplateResourceWithoutVersion.status != VersionStatus.COMMITTING) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_STATUS_NOT_MATCHED,
                    params = arrayOf(VersionStatus.COMMITTING.name, pTemplateResourceWithoutVersion.status.name)
                )
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
        val templateInfo = pipelineTemplateInfoService.getOrNull(
            projectId = projectId,
            templateId = templateId
        )
        val pTemplateResourceOnlyVersion = if (templateInfo == null) {
            val resourceOnlyVersion = pipelineTemplateGenerator.getDefaultVersion(
                versionStatus = pTemplateResourceWithoutVersion.status
            )
            pipelineTemplatePersistenceService.initializeTemplate(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion
            )
            resourceOnlyVersion
        } else {
            val draftResource = pipelineTemplateResourceService.getDraftVersionResource(
                projectId = projectId,
                templateId = templateId
            )
            if (draftResource == null) {
                createDraftVersion()
            } else {
                updateDraftVersion(draftResource)
            }
        }
        return DeployTemplateResult(
            projectId = projectId,
            userId = userId,
            version = pTemplateResourceOnlyVersion.version,
            templateId = templateId,
            templateName = pipelineTemplateInfo.name,
            number = pTemplateResourceOnlyVersion.number,
            versionNum = pTemplateResourceOnlyVersion.versionNum,
            versionName = pTemplateResourceOnlyVersion.versionName,
            versionAction = versionAction
        )
    }

    private fun PipelineTemplateVersionCreateContext.createDraftVersion(): PTemplateResourceOnlyVersion {
        val resourceOnlyVersion = pipelineTemplateGenerator.generateDraftVersion(
            projectId = projectId,
            templateId = templateId
        )
        pipelineTemplatePersistenceService.createDraftVersion(
            context = this,
            resourceOnlyVersion = resourceOnlyVersion
        )
        return resourceOnlyVersion
    }

    private fun PipelineTemplateVersionCreateContext.updateDraftVersion(
        draftResource: PipelineTemplateResource
    ): PTemplateResourceOnlyVersion {
        val resourceOnlyVersion = PTemplateResourceOnlyVersion(draftResource)
        pipelineTemplatePersistenceService.updateDraftVersion(
            context = this,
            resourceOnlyVersion = resourceOnlyVersion
        )
        return resourceOnlyVersion
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateDraftReleaseHandler::class.java)
    }
}
