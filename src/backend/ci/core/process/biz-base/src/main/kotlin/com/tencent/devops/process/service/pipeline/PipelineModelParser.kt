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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.TemplateDescriptor
import com.tencent.devops.common.pipeline.enums.TemplateRefType
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.template.PipelineTemplateInfoDao
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.dao.template.PipelineTemplateSettingDao
import com.tencent.devops.process.dao.yaml.PipelineYamlInfoDao
import com.tencent.devops.process.engine.utils.TemplateInstanceUtil
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模版模型解析器
 */
@Service
class PipelineModelParser @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineTemplateInfoDao: PipelineTemplateInfoDao,
    private val pipelineTemplateResourceDao: PipelineTemplateResourceDao,
    private val pipelineTemplateSettingDao: PipelineTemplateSettingDao,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineYamlVersionResolver: PipelineYamlVersionResolver
) {

    /**
     * 解析模版编排,将模版引用转换成具体的编排
     */
    fun parseModel(
        projectId: String,
        pipelineId: String,
        model: Model,
        branchName: String? = null
    ): Model {
        return if (model.template != null) {
            val templateResource = parseTemplateDescriptor(
                projectId = projectId,
                descriptor = model.template!!,
                pipelineId = pipelineId,
                branchName = branchName
            )
            TemplateInstanceUtil.instanceModel(
                model = model,
                templateResource = templateResource
            )
        } else {
            model
        }
    }

    fun parseModelAndSetting(
        projectId: String,
        pipelineId: String,
        model: Model,
        setting: PipelineSetting,
        branchName: String? = null
    ): PipelineModelAndSetting {
        return if (model.template != null) {
            val templateResource = parseTemplateDescriptor(
                projectId = projectId,
                descriptor = model.template!!,
                pipelineId = pipelineId,
                branchName = branchName
            )
            val templateSetting = pipelineTemplateSettingDao.get(
                dslContext = dslContext,
                projectId = projectId,
                templateId = templateResource.templateId,
                settingVersion = templateResource.settingVersion
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
            )
            val instanceModel = TemplateInstanceUtil.instanceModel(
                model = model,
                templateResource = templateResource
            )
            val instanceSetting = TemplateInstanceUtil.instanceSetting(
                setting = setting,
                templateSetting = templateSetting,
                overrideTemplateField = model.overrideTemplateField
            )
            PipelineModelAndSetting(
                model = instanceModel,
                setting = instanceSetting
            )
        } else {
            PipelineModelAndSetting(
                model = model,
                setting = setting
            )
        }
    }

    /*fun parseTemplateModel(
        projectId: String,
        model: ITemplateModel
    ): ITemplateModel {
        return when (model) {
            is Model -> {
                parseModel(projectId = projectId, model = model)
            }

            is StageTemplateModel -> {
                model.copy(stages = parseStages(projectId = projectId, stages = model.stages))
            }

            is JobTemplateModel -> {
                model.copy(containers = parseContainers(projectId = projectId, containers = model.containers))
            }

            is StepTemplateModel -> {
                val newElements = parseElements(projectId = projectId, elements = model.container.elements)
                val newContainer = model.container.copyElements(newElements)
                model.copy(container = newContainer)
            }

            else -> model
        }
    }

    private fun parseStages(
        projectId: String,
        stages: List<Stage>
    ): List<Stage> {
        val newStages = mutableListOf<Stage>()
        stages.forEach { stage ->
            val newStage = if (stage.fromTemplate == true) {
                parseStageTemplate(projectId = projectId, stage = stage)
            } else {
                val newContainers = parseContainers(projectId = projectId, containers = stage.containers)
                listOf(stage.copy(containers = newContainers))
            }
            newStages.addAll(newStage)
        }
        return newStages
    }

    private fun parseContainers(
        projectId: String,
        containers: List<Container>
    ): List<Container> {
        val newContainers = mutableListOf<Container>()
        containers.forEach { container ->
            val newContainer = if (container is JobTemplateContainer) {
                parseJobTemplateContainer(
                    projectId = projectId,
                    container = container
                )
            } else {
                val newElements = parseElements(projectId = projectId, elements = container.elements)
                listOf(container.copyElements(newElements))
            }
            newContainers.addAll(newContainer)
        }
        return newContainers
    }

    private fun parseElements(
        projectId: String,
        elements: List<Element>
    ): List<Element> {
        val newElements = mutableListOf<Element>()
        elements.forEach { element ->
            val newElement = if (element is StepTemplateElement) {
                parseStepTemplateElement(
                    projectId = projectId,
                    element = element
                )
            } else {
                listOf(element)
            }
            newElements.addAll(newElement)
        }
        return newElements
    }

    private fun parseStageTemplate(
        projectId: String,
        stage: Stage
    ): List<Stage> {
        val templateModel = getPipelineTemplateResource(
            projectId = projectId,
            descriptor = stage
        ).model
        if (templateModel !is StageTemplateModel) {
            // 模型不匹配
            throw ErrorCodeException(
                errorCode = ""
            )
        }
        return templateModel.stages
    }

    private fun parseJobTemplateContainer(
        projectId: String,
        container: JobTemplateContainer
    ): List<Container> {
        val templateModel = getPipelineTemplateResource(
            projectId = projectId,
            descriptor = container
        ).model
        if (templateModel !is JobTemplateModel) {
            // 模型不匹配
            throw ErrorCodeException(
                errorCode = ""
            )
        }
        return templateModel.containers
    }

    private fun parseStepTemplateElement(
        projectId: String,
        element: StepTemplateElement
    ): List<Element> {
        val templateModel = getPipelineTemplateResource(
            projectId = projectId,
            descriptor = element
        ).model
        if (templateModel !is StepTemplateModel) {
            // 模型不匹配
            throw ErrorCodeException(
                errorCode = ""
            )
        }
        return templateModel.container.elements
    }*/

    /**
     * @param repoHashId 仓库hashId,当通过模版路径引用时，必须传入
     * @param branchName 触发分支名称,当webhook触发时才有值
     */
    @Suppress("CyclomaticComplexMethod")
    fun parseTemplateDescriptor(
        projectId: String,
        descriptor: TemplateDescriptor,
        pipelineId: String? = null,
        repoHashId: String? = null,
        branchName: String? = null
    ): PipelineTemplateResource {
        with(descriptor) {
            return when (templateRefType) {
                // 通过模版ID方式引用
                TemplateRefType.ID -> {
                    if (templateVersionName.isNullOrEmpty()) {
                        throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_TEMPLATE_VERSION_NAME_NOT_EMPTY
                        )
                    }
                    logger.info(
                        "parse template descriptor by version name|$projectId|$templateId|$templateVersionName"
                    )
                    pipelineTemplateInfoDao.get(
                        dslContext = dslContext,
                        projectId = projectId,
                        templateId = templateId!!
                    ) ?: throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS,
                        params = arrayOf(templateId!!)
                    )
                    pipelineTemplateResourceDao.getLatestRecord(
                        dslContext = dslContext,
                        projectId = projectId,
                        templateId = templateId!!,
                        versionName = templateVersionName!!
                    ) ?: throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TEMPLATE_VERSION_BY_ID_NOT_FOUND,
                        params = arrayOf(templateId!!, templateVersionName!!)
                    )
                }

                // 通过模版路径方式引用
                TemplateRefType.PATH -> {
                    val finalRepoHashId = when {
                        !repoHashId.isNullOrEmpty() -> repoHashId
                        !pipelineId.isNullOrEmpty() -> {
                            pipelineYamlInfoDao.get(
                                dslContext = dslContext,
                                projectId = projectId,
                                pipelineId = pipelineId,
                            )?.repoHashId
                        }

                        else -> null
                    } ?: throw ErrorCodeException(
                        errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                        params = arrayOf("repoHashId")
                    )
                    logger.info(
                        "parse template descriptor by path|$projectId|$finalRepoHashId|$templatePath|$templateRef"
                    )
                    // 1. 获取yaml文件绑定的模版
                    val pipelineYamlInfo = pipelineYamlInfoDao.get(
                        dslContext = dslContext,
                        projectId = projectId,
                        repoHashId = finalRepoHashId,
                        filePath = templatePath!!
                    ) ?: throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_YAML_FOR_TEMPLATE_NOT_FOUND,
                        params = arrayOf(templatePath!!)
                    )
                    // 2. 获取yaml文件对应的模版版本
                    /**
                     * 1. 如果指定分支，则使用指定分支
                     * 2. 如果没有指定分支,当webhook触发时,使用触发的分支,否则使用默认分支
                     */
                    val ref = templateRef?.takeIf { it.isNotEmpty() } ?: branchName?.takeIf { it.isNotEmpty() }
                    val pipelineYamlVersion = pipelineYamlVersionResolver.resolveTemplateRefVersion(
                        projectId = projectId,
                        repoHashId = finalRepoHashId,
                        filePath = templatePath!!,
                        ref = ref
                    )
                    logger.info(
                        "parse template descriptor result by path|$projectId|$finalRepoHashId|" +
                                "${pipelineYamlInfo.pipelineId}|${pipelineYamlVersion.version}"
                    )

                    pipelineTemplateInfoDao.get(
                        dslContext = dslContext,
                        projectId = projectId,
                        templateId = pipelineYamlInfo.pipelineId
                    )
                    pipelineTemplateResourceDao.getLatestRecord(
                        dslContext = dslContext,
                        projectId = projectId,
                        templateId = pipelineYamlInfo.pipelineId,
                        version = pipelineYamlVersion.version.toLong()
                    ) ?: throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TEMPLATE_VERSION_BY_PATH_NOT_FOUND
                    )
                }

                else -> {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TEMPLATE_REF_TYPE
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineModelParser::class.java)
    }
}
