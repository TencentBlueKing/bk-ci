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

package com.tencent.devops.process.service.template.v2.version.convert

import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.constant.PipelineTemplateConstant
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRollbackReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateVersionReq
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模板回滚请求转换
 */
@Service
class PipelineTemplateRollbackReqConverter @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService
) : PipelineTemplateVersionReqConverter {

    override fun support(request: PipelineTemplateVersionReq): Boolean {
        return request is PipelineTemplateRollbackReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        templateId: String?,
        version: Long?,
        request: PipelineTemplateVersionReq
    ): PipelineTemplateVersionCreateContext {
        logger.info(
            "Start to convert draft rollback request|$projectId|$templateId|$templateId|$version"
        )
        request as PipelineTemplateRollbackReq
        if (templateId == null) {
            throw IllegalArgumentException("templateId is null")
        }
        if (version == null) {
            throw IllegalArgumentException("version is null")
        }
        val draftVersion = request.draftVersion
        val pipelineTemplateInfo = pipelineTemplateInfoService.get(
            projectId = projectId,
            templateId = templateId
        )
        val targetResource = if (draftVersion != null) {
            pipelineTemplateResourceService.getByDraftVersion(
                projectId = projectId,
                templateId = templateId,
                version = version,
                draftVersion = draftVersion
            )
        } else {
            pipelineTemplateResourceService.get(
                projectId = projectId,
                templateId = templateId,
                version = version
            )
        }
        val targetSetting = if (draftVersion != null) {
            pipelineTemplateSettingService.getByDraftVersion(
                projectId = projectId,
                templateId = templateId,
                version = targetResource.version,
                draftVersion = draftVersion
            )
        } else {
            pipelineTemplateSettingService.get(
                projectId = projectId,
                templateId = templateId,
                settingVersion = targetResource.settingVersion
            )
        }
        // 草稿历史版本回滚,基准版本需使用原始的版本
        val baseResource = if (draftVersion != null) {
            targetResource.baseVersion?.let { baseVersion ->
                pipelineTemplateResourceService.get(
                    projectId = projectId,
                    templateId = templateId,
                    version = baseVersion
                )
            }
        } else {
            targetResource
        }
        val pTemplateResourceWithoutVersion = PTemplateResourceWithoutVersion(targetResource).copy(
            status = VersionStatus.COMMITTING,
            branchAction = null,
            sortWeight = PipelineTemplateConstant.COMMITTING_STATUS_VERSION_SORT_WIGHT,
            baseVersion = baseResource?.baseVersion,
            baseVersionName = baseResource?.baseVersionName,
            description = null
        )
        return PipelineTemplateVersionCreateContext(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            versionAction = PipelineVersionAction.SAVE_DRAFT,
            pipelineTemplateInfo = pipelineTemplateInfo,
            pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
            pTemplateSettingWithoutVersion = targetSetting,
            baseDraftVersion = draftVersion
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateRollbackReqConverter::class.java)
    }
}
