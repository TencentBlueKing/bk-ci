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
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelLock
import com.tencent.devops.process.service.template.v2.PipelineTemplatePersistenceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 创建流水线模版正式版本
 */
@Service
class PipelineTemplateReleaseCreateHandler @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplatePersistenceService: PipelineTemplatePersistenceService,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val redisOperation: RedisOperation
) : PipelineTemplateVersionCreateHandler {

    override fun support(context: PipelineTemplateVersionCreateContext): Boolean {
        return context.versionAction == PipelineVersionAction.CREATE_RELEASE
    }

    override fun handle(context: PipelineTemplateVersionCreateContext): DeployTemplateResult {
        with(context) {
            logger.info("handle template released version|$projectId|$templateId|$versionAction|$version")
            if (pTemplateResourceWithoutVersion.status != VersionStatus.RELEASED) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_STATUS_NOT_MATCHED,
                    params = arrayOf(VersionStatus.RELEASED.name, pTemplateResourceWithoutVersion.status.name)
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
        val resourceOnlyVersion = if (templateInfo == null) {
            val resourceOnlyVersion = pipelineTemplateGenerator.getDefaultVersion(
                versionStatus = pTemplateResourceWithoutVersion.status,
                versionName = customVersionName
            )
            pipelineTemplatePersistenceService.initializeTemplate(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion
            )
            resourceOnlyVersion
        } else {
            createReleaseVersion()
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
            versionAction = versionAction
        )
    }

    private fun PipelineTemplateVersionCreateContext.createReleaseVersion(): PTemplateResourceOnlyVersion {
        val resourceOnlyVersion = pipelineTemplateGenerator.generateReleaseVersion(
            projectId = projectId,
            templateId = templateId,
            newResource = pTemplateResourceWithoutVersion,
            newSetting = pTemplateSettingWithoutVersion,
            customVersionName = customVersionName
        )
        pipelineTemplatePersistenceService.createReleaseVersion(
            context = this,
            resourceOnlyVersion = resourceOnlyVersion
        )
        return resourceOnlyVersion
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateDraftReleaseHandler::class.java)
    }
}
