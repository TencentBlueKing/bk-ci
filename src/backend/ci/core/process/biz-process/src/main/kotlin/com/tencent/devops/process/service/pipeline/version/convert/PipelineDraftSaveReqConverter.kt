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

package com.tencent.devops.process.service.pipeline.version.convert

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.PipelineStorageType
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.utils.TemplateInstanceUtil
import com.tencent.devops.process.pojo.pipeline.version.PipelineDraftSaveReq
import com.tencent.devops.process.pojo.pipeline.version.PipelineVersionCreateReq
import com.tencent.devops.process.service.pipeline.PipelineModelParser
import com.tencent.devops.process.service.pipeline.version.PipelineResourceFactory
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.yaml.PipelineYamlService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 流水线草稿保存转换器
 */
@Service
class PipelineDraftSaveReqConverter(
    private val pipelineIdGenerator: PipelineIdGenerator,
    private val pipelineResourceFactory: PipelineResourceFactory,
    private val pipelineVersionGenerator: PipelineVersionGenerator,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val pipelineVersionCreateContextFactory: PipelineVersionCreateContextFactory,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val client: Client,
    private val pipelineModelParser: PipelineModelParser
) : PipelineVersionCreateReqConverter {
    override fun support(request: PipelineVersionCreateReq): Boolean {
        return request is PipelineDraftSaveReq
    }

    override fun convert(
        userId: String,
        projectId: String,
        pipelineId: String?,
        version: Int?,
        request: PipelineVersionCreateReq
    ): PipelineVersionCreateContext {
        request as PipelineDraftSaveReq
        with(request) {
            logger.info(
                "Start to convert draft release request|$projectId|$pipelineId|$version|$storageType|$baseVersion"
            )
            val (modelAndSetting, yamlWithVersion) = if (storageType == PipelineStorageType.YAML) {
                if (yaml.isNullOrEmpty()) {
                    throw IllegalArgumentException("yaml can not be empty")
                }
                pipelineVersionGenerator.yaml2model(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    yaml = yaml!!,
                    aspects = AtomUtils.checkElementCanPauseBeforeRun(client, projectId)
                )
            } else {
                if (modelAndSetting == null) {
                    throw IllegalArgumentException("modelAndSetting can not be null")
                }
                val newModel = createPipelineModel(projectId = projectId, pipelineId = pipelineId)
                val newModelAndSetting = PipelineModelAndSetting(
                    model = newModel,
                    setting = modelAndSetting!!.setting
                )
                val newYaml = pipelineVersionGenerator.model2yaml(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    modelAndSetting = newModelAndSetting,
                    oldYaml = pipelineId?.let {
                        pipelineRepositoryService.getPipelineResourceVersion(
                            projectId = projectId,
                            pipelineId = it,
                            version = request.baseVersion,
                            includeDraft = true
                        )?.yaml
                    } ?: ""
                )
                Pair(newModelAndSetting, newYaml)
            }
            // 生成流水线ID
            val newPipelineId = pipelineId ?: pipelineIdGenerator.getNextId()
            val pipelineSettingWithoutVersion = modelAndSetting.setting.copy(
                projectId = projectId,
                pipelineId = newPipelineId
            )

            // 通过路径引用的方式,模版yaml文件所属的仓库ID应与流水线相同
            val pipelineYamlInfo = pipelineYamlService.getPipelineYamlInfo(
                projectId = projectId,
                pipelineId = newPipelineId
            )
            return pipelineVersionCreateContextFactory.create(
                userId = userId,
                projectId = projectId,
                pipelineId = newPipelineId,
                channelCode = ChannelCode.BS,
                version = version,
                model = modelAndSetting.model,
                yaml = yamlWithVersion?.yamlStr,
                baseVersion = baseVersion,
                pipelineSettingWithoutVersion = pipelineSettingWithoutVersion,
                versionStatus = VersionStatus.COMMITTING,
                versionAction = PipelineVersionAction.SAVE_DRAFT,
                repoHashId = pipelineYamlInfo?.repoHashId
            )
        }
    }

    private fun PipelineDraftSaveReq.createPipelineModel(
        projectId: String,
        pipelineId: String?
    ): Model {
        // 前端传过来的model是完整的model,如果是模版实例化的,需要转换成引用的方式
        val model = modelAndSetting!!.model
        val triggerContainer = model.getTriggerContainer()
        val overrideTemplateField = model.overrideTemplateField

        // 前端传过来的是所有的触发器,triggerConfigs只需要保留流水线自定义的
        val triggerConfigs = TemplateInstanceUtil.getTriggerConfigs(
            elements = triggerContainer.elements,
            overrideTemplateField = overrideTemplateField,
        )

        return if (model.template != null) {
            val template = model.template!!
            val templateResource = pipelineModelParser.parseTemplateDescriptor(
                projectId = projectId,
                descriptor = model.template!!
            )
            val templateModel = templateResource.model
            if (templateModel !is Model) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_TYPE_MODEL_TYPE_NOT_MATCH
                )
            }
            val templateTrigger = templateModel.getTriggerContainer()
            // 前端传过来的参数是所有的参数,templateVariables只需要流水线自定义的值
            val templateVariables = TemplateInstanceUtil.getTemplateVariables(
                pipelineParams = triggerContainer.params,
                templateParams = templateTrigger.params,
                overrideTemplateField = overrideTemplateField
            )

            val recommendedVersion = TemplateInstanceUtil.getRecommendedVersion(
                pipelineBuildNo = triggerContainer.buildNo,
                pipelineParams = triggerContainer.params,
                templateBuildNo = templateTrigger.buildNo,
                overrideTemplateField = overrideTemplateField
            )

            pipelineResourceFactory.createPipelineModelRef(
                name = model.name,
                desc = model.desc,
                refType = template.templateRefType,
                templatePath = template.templatePath,
                templateRef = template.templateRef,
                templateId = template.templateId,
                templateVersionName = template.templateVersionName,
                templateVariables = templateVariables,
                triggerConfigs = triggerConfigs,
                recommendedVersion = recommendedVersion,
                overrideTemplateField = overrideTemplateField
            )
        } else {
            val pipelineTemplateRelated = pipelineId?.let {
                pipelineTemplateRelatedService.get(projectId = projectId, pipelineId = pipelineId)
            }
            if (pipelineTemplateRelated == null ||
                pipelineTemplateRelated.instanceType != PipelineInstanceTypeEnum.CONSTRAINT
            ) {
                return model
            }
            val templateResource = pipelineTemplateResourceService.get(
                projectId = projectId,
                templateId = pipelineTemplateRelated.templateId,
                version = pipelineTemplateRelated.version
            )
            val templateModel = templateResource.model
            if (templateModel !is Model) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_TYPE_MODEL_TYPE_NOT_MATCH
                )
            }
            val templateTrigger = templateModel.getTriggerContainer()
            // 前端传过来的参数是所有的参数,templateVariables只需要流水线自定义的值
            val templateVariables = TemplateInstanceUtil.getTemplateVariables(
                pipelineParams = triggerContainer.params,
                templateParams = templateTrigger.params,
                overrideTemplateField = overrideTemplateField
            )

            val recommendedVersion = TemplateInstanceUtil.getRecommendedVersion(
                pipelineBuildNo = triggerContainer.buildNo,
                pipelineParams = triggerContainer.params,
                templateBuildNo = templateTrigger.buildNo,
                overrideTemplateField = overrideTemplateField
            )
            pipelineResourceFactory.createPipelineModelRef(
                name = model.name,
                desc = model.desc,
                refType = TemplateRefType.ID,
                templateId = pipelineTemplateRelated.templateId,
                templateVersionName = pipelineTemplateRelated.versionName,
                templateVariables = templateVariables,
                triggerConfigs = triggerConfigs,
                recommendedVersion = recommendedVersion,
                overrideTemplateField = overrideTemplateField
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineDraftSaveReqConverter::class.java)
    }
}
