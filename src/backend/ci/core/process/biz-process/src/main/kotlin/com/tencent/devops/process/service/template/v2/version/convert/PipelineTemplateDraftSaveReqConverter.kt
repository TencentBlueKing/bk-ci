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

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.process.constant.PipelineTemplateConstant
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDraftSaveReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateVersionReq
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelInitializer
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 保存草稿请求转换
 */
@Service
class PipelineTemplateDraftSaveReqConverter @Autowired constructor(
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateModelInitializer: PipelineTemplateModelInitializer
) : PipelineTemplateVersionReqConverter {

    override fun support(request: PipelineTemplateVersionReq): Boolean {
        return request is PipelineTemplateDraftSaveReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        templateId: String?,
        version: Long?,
        request: PipelineTemplateVersionReq
    ): PipelineTemplateVersionCreateContext {
        request as PipelineTemplateDraftSaveReq
        with(request) {
            val transferResult = pipelineTemplateGenerator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = storageType,
                templateType = type,
                templateModel = model,
                params = if (storageType == PipelineStorageType.MODEL && type == PipelineTemplateType.PIPELINE) {
                    (model as Model).getTriggerContainer().params
                } else {
                    request.params
                },
                templateSetting = templateSetting,
                yaml = yaml,
                fallbackOnError = true
            )
            val templateInfo = if (templateId != null) {
                pipelineTemplateInfoService.get(
                    projectId = projectId,
                    templateId = templateId
                )
            } else {
                PipelineTemplateInfoV2(
                    id = pipelineTemplateGenerator.generateTemplateId(),
                    projectId = projectId,
                    name = transferResult.templateSetting.pipelineName,
                    desc = transferResult.templateSetting.desc,
                    mode = TemplateType.CUSTOMIZE,
                    type = transferResult.templateType,
                    enablePac = false,
                    creator = userId,
                    updater = userId,
                    latestVersionStatus = VersionStatus.COMMITTING
                )
            }

            baseVersion = baseVersion ?: templateInfo.releasedVersion.takeIf {
                templateInfo.latestVersionStatus != VersionStatus.COMMITTING
            }

            // 获取基础版本名称
            val baseResource = baseVersion?.let {
                pipelineTemplateResourceService.get(
                    projectId = projectId,
                    templateId = templateInfo.id,
                    version = it
                )
            }
            val (srcTemplateProjectId, srcTemplateId, srcTemplateVersion) =
                takeIf { templateInfo.mode == TemplateType.CONSTRAINT && baseResource != null }?.let {
                    with(baseResource!!) {
                        Triple(srcTemplateProjectId, srcTemplateId, srcTemplateVersion)
                    }
                } ?: Triple(null, null, null)

            logger.debug(
                "PipelineTemplateDraftSaveReqConverter|baseResource={},srcTemplateProjectId={}," +
                    "srcTemplateId={},srcTemplateVersion={},mode={}", baseResource, srcTemplateProjectId,
                srcTemplateId, srcTemplateVersion, templateInfo.mode
            )

            pipelineTemplateModelInitializer.initTemplateModel(transferResult.templateModel)
            val pTemplateResourceWithoutVersion = PTemplateResourceWithoutVersion(
                projectId = projectId,
                templateId = templateInfo.id,
                type = templateInfo.type,
                params = transferResult.params,
                model = transferResult.templateModel,
                yaml = transferResult.yamlWithVersion?.yamlStr,
                status = VersionStatus.COMMITTING,
                sortWeight = PipelineTemplateConstant.COMMITTING_STATUS_VERSION_SORT_WIGHT,
                srcTemplateProjectId = srcTemplateProjectId,
                srcTemplateId = srcTemplateId,
                srcTemplateVersion = srcTemplateVersion,
                baseVersion = baseVersion,
                baseVersionName = baseResource?.versionName,
                creator = userId,
                updater = userId
            )
            val pTemplateSettingWithoutVersion = transferResult.templateSetting.copy(
                projectId = projectId,
                pipelineId = templateInfo.id,
                creator = userId,
                updater = userId
            )
            return PipelineTemplateVersionCreateContext(
                userId = userId,
                projectId = projectId,
                templateId = templateInfo.id,
                version = version,
                versionAction = PipelineVersionAction.SAVE_DRAFT,
                pipelineTemplateInfo = templateInfo,
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateSettingWithoutVersion = pTemplateSettingWithoutVersion
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateDraftSaveReqConverter::class.java)
    }
}
