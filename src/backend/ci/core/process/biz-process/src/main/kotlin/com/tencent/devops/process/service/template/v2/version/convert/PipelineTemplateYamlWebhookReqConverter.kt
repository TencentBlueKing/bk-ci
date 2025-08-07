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
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateResourceWithoutVersion
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateVersionReq
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateYamlWebhookReq
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelInitializer
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.springframework.stereotype.Service

/**
 * 流水线模板分支推送请求转换
 */
@Service
class PipelineTemplateYamlWebhookReqConverter(
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateModelInitializer: PipelineTemplateModelInitializer
) : PipelineTemplateVersionReqConverter {
    override fun support(request: PipelineTemplateVersionReq): Boolean {
        return request is PipelineTemplateYamlWebhookReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        templateId: String?,
        version: Long?,
        request: PipelineTemplateVersionReq
    ): PipelineTemplateVersionCreateContext {
        request as PipelineTemplateYamlWebhookReq
        with(request) {
            val transferResult = pipelineTemplateGenerator.transfer(
                userId = userId,
                projectId = projectId,
                storageType = PipelineStorageType.YAML,
                templateType = null,
                templateModel = null,
                templateSetting = null,
                params = null,
                yaml = yaml
            )
            val (status, versionAction) = if (isDefaultBranch) {
                Pair(VersionStatus.RELEASED, PipelineVersionAction.CREATE_RELEASE)
            } else {
                Pair(VersionStatus.BRANCH, PipelineVersionAction.CREATE_BRANCH)
            }
            val templateSetting = transferResult.templateSetting
            val templateModel = transferResult.templateModel
            // 模版名称实际取值优先级：setting > model > fileName
            val templateName = if (templateModel is Model) {
                templateSetting.pipelineName.takeIf {
                    it.isNotBlank()
                } ?: templateModel.name.ifBlank {
                    yamlFileName
                }
            } else {
                // TODO PAC局部模版后续得看下名称要怎么处理
                yamlFileName
            }

            val (newTemplateId, templateInfo) = if (templateId == null) {
                val newTemplateId = pipelineTemplateGenerator.generateTemplateId()
                val templateInfo = PipelineTemplateInfoV2(
                    id = newTemplateId,
                    projectId = projectId,
                    name = templateName,
                    desc = templateSetting.desc,
                    mode = TemplateType.CUSTOMIZE,
                    type = transferResult.templateType,
                    enablePac = true,
                    creator = userId,
                    updater = userId,
                    latestVersionStatus = status
                )
                Pair(newTemplateId, templateInfo)
            } else {
                val templateInfo = pipelineTemplateInfoService.get(
                    projectId = projectId,
                    templateId = templateId
                )
                Pair(templateId, templateInfo)
            }
            pipelineTemplateModelInitializer.initTemplateModel(templateModel)
            val pTemplateResourceWithoutVersion = PTemplateResourceWithoutVersion(
                projectId = projectId,
                templateId = newTemplateId,
                params = transferResult.params,
                type = transferResult.templateType,
                model = if (templateModel is Model) {
                    templateModel.copy(
                        name = templateName
                    )
                } else {
                    templateModel
                },
                yaml = transferResult.yamlWithVersion?.yamlStr,
                description = description,
                status = status,
                creator = userId,
                updater = userId
            )
            val pTemplateSettingWithoutVersion = templateSetting.copy(
                projectId = projectId,
                pipelineId = newTemplateId,
                creator = userId,
                updater = userId
            )
            return PipelineTemplateVersionCreateContext(
                userId = userId,
                projectId = projectId,
                templateId = newTemplateId,
                version = version,
                versionAction = versionAction,
                newTemplate = templateId == null,
                pipelineTemplateInfo = templateInfo,
                pTemplateResourceWithoutVersion = pTemplateResourceWithoutVersion,
                pTemplateSettingWithoutVersion = pTemplateSettingWithoutVersion,
                enablePac = true,
                yamlFileInfo = yamlFileInfo,
                branchName = branchName
            )
        }
    }
}
