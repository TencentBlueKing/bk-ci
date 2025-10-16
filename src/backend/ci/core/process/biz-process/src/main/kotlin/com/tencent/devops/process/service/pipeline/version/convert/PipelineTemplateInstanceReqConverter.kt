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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.TemplateVariable
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.template.PipelineTemplateType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.utils.TemplateInstanceUtil
import com.tencent.devops.process.pojo.pipeline.PipelineResourceWithoutVersion
import com.tencent.devops.process.pojo.pipeline.PipelineTemplateInstanceBasicInfo
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileInfo
import com.tencent.devops.process.pojo.pipeline.version.PipelineTemplateInstanceReq
import com.tencent.devops.process.pojo.pipeline.version.PipelineVersionCreateReq
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.pipeline.version.PipelineResourceFactory
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.process.yaml.PipelineYamlService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 模版实例化创建请求转换
 */
@Service
class PipelineTemplateInstanceReqConverter(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService,
    private val stageTagService: StageTagService,
    private val pipelineIdGenerator: PipelineIdGenerator,
    private val pipelineResourceFactory: PipelineResourceFactory,
    private val pipelineVersionGenerator: PipelineVersionGenerator,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineYamlService: PipelineYamlService,
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val pipelineInfoService: PipelineInfoService
) : PipelineVersionCreateReqConverter {
    override fun support(request: PipelineVersionCreateReq) = request is PipelineTemplateInstanceReq

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun convert(
        userId: String,
        projectId: String,
        pipelineId: String?,
        version: Int?,
        request: PipelineVersionCreateReq
    ): PipelineVersionCreateContext {
        request as PipelineTemplateInstanceReq
        with(request) {
            if (enablePac) {
                if (targetAction == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("targetAction")
                    )
                }
                if (repoHashId == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("repoHashId")
                    )
                }
                if (filePath.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("filePath")
                    )
                }
                if (targetAction == CodeTargetAction.COMMIT_TO_BRANCH && targetBranch == null) {
                    throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("targetBranch")
                    )
                }
            }
            if (templateRefType == TemplateRefType.PATH) {
                if (!enablePac) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TEMPLATE_PATH_REF_PIPELINE_NEED_PAC
                    )
                }
                if (templateRef.isNullOrEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TEMPLATE_PATH_REF_TEMPLATE_REF_NOT_EMPTY,
                    )
                }
            }

            logger.info(
                "Start to convert template instance request|$projectId|$pipelineId|$templateId|$templateVersion"
            )

            // 生成流水线ID
            val newPipelineId = pipelineId ?: pipelineIdGenerator.getNextId()
            // 异步创建实例化,在请求时会创建线ID,所以不能根据pipelineId为空判断是否是创建流水线
            val pipelineInfo = pipelineInfoService.getPipelineInfo(projectId = projectId, pipelineId = newPipelineId)

            // 生成流水线基本信息
            val pipelineBasicInfo = pipelineResourceFactory.createPipelineBasicInfo(
                projectId = projectId,
                pipelineId = newPipelineId,
                channelCode = ChannelCode.BS,
                pipelineName = pipelineName,
                pipelineDesc = null
            )

            // 获取版本状态
            val (versionStatus, branchName) = pipelineVersionGenerator.getInstanceStatusAndBranchName(
                projectId = projectId,
                pipelineId = newPipelineId,
                templateId = templateId,
                templateVersion = templateVersion,
                enablePac = enablePac,
                repoHashId = repoHashId,
                targetAction = targetAction,
                targetBranch = targetBranch
            )

            val templateInfo = pipelineTemplateInfoService.get(projectId = projectId, templateId = templateId)
            if (templateInfo.type != PipelineTemplateType.PIPELINE) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_INSTANCE_NEED_PIPELINE_TYPE,
                )
            }
            val templateResource = pipelineTemplateResourceService.get(
                projectId = projectId,
                templateId = templateId,
                version = templateVersion
            )
            val templateModel = templateResource.model
            if (templateModel !is Model) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_TYPE_MODEL_TYPE_NOT_MATCH
                )
            }

            // 生成实例化流水线model
            val templatePath = templateRefType?.takeIf { it == TemplateRefType.PATH }?.let {
                pipelineYamlService.getPipelineYamlInfo(
                    projectId = projectId,
                    pipelineId = templateId
                )?.filePath ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_PATH_REF_TEMPLATE_NEED_PAC
                )
            }

            // 前端会把所有的参数都传过来，这里只需要保留流水线自定义的参数,ui方式实例化,参数默认都是自定义
            // 以下变量为流水线自身的，不跟随模板，会对模板的变量默认值，进行覆盖。
            val templateVariables = params?.filter {
                overrideTemplateField?.overrideParam(it.id) ?: true
            }?.map { TemplateVariable(it) }

            // 前端会把所有的触发器都传过来,这里只需要保留流水线自定义的触发器,ui方式实例化,触发器默认继承模版
            // 以下触发器配置为流水线自定义的触发器，不跟随模板，会对流水线模板的触发器配置进行覆盖
            val overrideTemplateTriggerConfigs = triggerConfigs?.filter {
                it.stepId != null && overrideTemplateField?.overrideTrigger(it.stepId!!) ?: false
            }

            // 是否覆盖推荐版本号
            val recommendedVersion = TemplateInstanceUtil.getRecommendedVersion(
                buildNo = buildNo,
                params = params ?: emptyList(),
                overrideTemplateField = overrideTemplateField
            )

            val pipelineModelRef = pipelineResourceFactory.createPipelineModelRef(
                name = pipelineName,
                desc = null,
                refType = templateRefType,
                templateId = templateId,
                templateVersionName = templateResource.versionName,
                templatePath = templatePath,
                templateRef = templateRef,
                templateVariables = templateVariables,
                triggerConfigs = overrideTemplateTriggerConfigs,
                recommendedVersion = recommendedVersion,
                overrideTemplateField = overrideTemplateField
            )
            val pipelineSettingWithoutVersion = getPipelineSetting(
                projectId = projectId,
                pipelineId = newPipelineId,
                templateSettingVersion = templateResource.settingVersion,
                enablePac = enablePac
            )
            // 转换成yaml
            val newYaml = pipelineVersionGenerator.model2yaml(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                modelAndSetting = PipelineModelAndSetting(
                    model = pipelineModelRef,
                    setting = pipelineSettingWithoutVersion
                ),
                oldYaml = null
            )

            // 生成实例化model
            val defaultStageTagId = stageTagService.getDefaultStageTag().data?.id

            val instanceModel = TemplateInstanceUtil.instanceModel(
                templateResource = templateResource,
                pipelineName = pipelineName,
                defaultStageTagId = defaultStageTagId,
                templateVariables = templateVariables,
                overrideTemplateTriggerConfigs = overrideTemplateTriggerConfigs,
                recommendedVersion = recommendedVersion,
                overrideTemplateField = overrideTemplateField,
                template = pipelineModelRef.template
            )

            val pipelineResourceWithoutVersion = PipelineResourceWithoutVersion(
                projectId = projectId,
                pipelineId = newPipelineId,
                model = instanceModel,
                yaml = newYaml?.yamlStr,
                yamlVersion = newYaml?.versionTag,
                creator = userId,
                createTime = LocalDateTime.now(),
                updater = userId,
                updateTime = LocalDateTime.now(),
                status = versionStatus,
                branchAction = BranchVersionAction.ACTIVE.takeIf {
                    versionStatus == VersionStatus.BRANCH
                },
                description = description,
            )

            val pipelineDialect = pipelineAsCodeService.getPipelineDialect(
                projectId = projectId,
                asCodeSettings = pipelineSettingWithoutVersion.pipelineAsCodeSettings
            )
            val pipelineModelBasicInfo = pipelineResourceFactory.createPipelineModelBasicInfo(
                model = instanceModel,
                projectId = projectId,
                pipelineId = newPipelineId,
                userId = userId,
                create = pipelineId == null,
                versionStatus = versionStatus,
                channelCode = ChannelCode.BS,
                pipelineDialect = pipelineDialect
            )

            val templateInstanceBasicInfo = PipelineTemplateInstanceBasicInfo(
                templateId = templateId,
                templateName = templateInfo.name,
                templateVersion = templateVersion,
                templateVersionName = templateResource.versionName,
                templateSettingVersion = templateResource.settingVersion,
                instanceModel = instanceModel,
                instanceType = PipelineInstanceTypeEnum.CONSTRAINT,
                refType = templateRefType
            )

            return PipelineVersionCreateContext(
                userId = userId,
                projectId = projectId,
                pipelineId = newPipelineId,
                versionAction = PipelineVersionAction.TEMPLATE_INSTANCE,
                pipelineInfo = pipelineInfo,
                pipelineBasicInfo = pipelineBasicInfo,
                pipelineModelBasicInfo = pipelineModelBasicInfo,
                pipelineResourceWithoutVersion = pipelineResourceWithoutVersion,
                pipelineSettingWithoutVersion = pipelineSettingWithoutVersion,
                templateInstanceBasicInfo = templateInstanceBasicInfo,
                resetBuildNo = resetBuildNo,
                enablePac = enablePac,
                yamlFileInfo = enablePac.takeIf { it }?.let {
                    PipelineYamlFileInfo(
                        repoHashId = repoHashId!!,
                        filePath = filePath!!,
                    )
                },
                targetAction = targetAction,
                targetBranch = targetBranch,
                branchName = branchName
            )
        }
    }

    private fun PipelineTemplateInstanceReq.getPipelineSetting(
        projectId: String,
        pipelineId: String,
        templateSettingVersion: Int,
        enablePac: Boolean
    ): PipelineSetting {
        val templateSetting = pipelineTemplateSettingService.get(
            projectId = projectId,
            templateId = templateId,
            settingVersion = templateSettingVersion
        )
        val pipelineSetting = if (useTemplateSetting) {
            templateSetting.copy(
                pipelineId = pipelineId,
                pipelineName = pipelineName
            )
        } else {
            pipelineRepositoryService.getSetting(
                projectId = projectId,
                pipelineId = pipelineId
            )?.copy(
                pipelineName = pipelineName
            ) ?: pipelineRepositoryService.createDefaultSetting(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                channelCode = ChannelCode.BS
            )
        }
        val pacSetting = enablePac.takeIf { it }?.let {
            val pipelineAsCodeSettings =
                pipelineSetting.pipelineAsCodeSettings?.copy(enable = true) ?: PipelineAsCodeSettings(enable = true)
            pipelineSetting.copy(pipelineAsCodeSettings = pipelineAsCodeSettings)
        } ?: pipelineSetting
        return pacSetting
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateInstanceReqConverter::class.java)
    }
}
