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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.CommonMessageCode.ELEMENT_NOT_SUPPORT_TRANSFER
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.TemplateModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertBody
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertResponse
import com.tencent.devops.common.pipeline.pojo.transfer.PositionResponse
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.FULL_MODEL2YAML
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.FULL_YAML2MODEL
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.TEMPLATE_MODEL2YAML_JOB
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.TEMPLATE_MODEL2YAML_PIPELINE
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.TEMPLATE_MODEL2YAML_STAGE
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.TEMPLATE_MODEL2YAML_STEP
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.TEMPLATE_YAML2MODEL_JOB
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.TEMPLATE_YAML2MODEL_PIPELINE
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.TEMPLATE_YAML2MODEL_STAGE
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.TEMPLATE_YAML2MODEL_STEP
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType.YAML_INSERT_TASK
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.TransferMark
import com.tencent.devops.common.pipeline.pojo.transfer.TransferResponse
import com.tencent.devops.common.pipeline.pojo.transfer.YamlWithVersion
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.process.dao.yaml.PipelineYamlInfoDao
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineInfoService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.yaml.pojo.TemplatePath
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.transfer.ElementTransfer
import com.tencent.devops.process.yaml.transfer.ModelTransfer
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import com.tencent.devops.process.yaml.transfer.TemplateModelTransfer
import com.tencent.devops.process.yaml.transfer.TransferMapper
import com.tencent.devops.process.yaml.transfer.YamlIndexService
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspect
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectLoader
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.transfer.pojo.TemplateModelTransferInput
import com.tencent.devops.process.yaml.transfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.transfer.schema.CodeSchemaCheck
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYamlParser
import com.tencent.devops.process.yaml.v3.models.ITemplateFilter
import com.tencent.devops.process.yaml.v3.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v3.parsers.template.YamlTemplateConf
import com.tencent.devops.process.yaml.v3.parsers.template.models.GetTemplateParam
import com.tencent.devops.process.yaml.v3.utils.ScriptYmlUtils
import com.tencent.devops.repository.api.ServiceRepositoryResource
import java.util.LinkedList
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineTransferYamlService @Autowired constructor(
    private val dslContext: DSLContext,
    private val modelTransfer: ModelTransfer,
    private val templateModelTransfer: TemplateModelTransfer,
    private val elementTransfer: ElementTransfer,
    private val pipelineSettingVersionService: PipelineSettingVersionService,
    private val pipelineInfoDao: PipelineInfoDao,
    private val yamlIndexService: YamlIndexService,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val client: Client,
    private val yamlSchemaCheck: CodeSchemaCheck,
    private val pipelineInfoService: PipelineInfoService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTransferYamlService::class.java)
        private const val TEMPLATE_ROOT_FILE = "TEMPLATE_ROOT_FILE"
        val pipeline_key = listOf(
            ITemplateFilter::stages.name,
            ITemplateFilter::jobs.name,
            ITemplateFilter::steps.name,
            ITemplateFilter::finally.name,
            ITemplateFilter::extends.name
        )
        val trigger_key = listOf("on")
        val notice_key = listOf(IPreTemplateScriptBuildYamlParser::notices.name)
        val setting_key = listOf(
            IPreTemplateScriptBuildYamlParser::concurrency.name,
            IPreTemplateScriptBuildYamlParser::name.name,
            IPreTemplateScriptBuildYamlParser::version.name,
            IPreTemplateScriptBuildYamlParser::label.name,
            IPreTemplateScriptBuildYamlParser::desc.name,
            IPreTemplateScriptBuildYamlParser::syntaxDialect.name
        )
        private const val DEFAULT_REPO_ALIAS_NAME = "self"
    }

    fun getTemplate(param: GetTemplateParam<Any>): String {
        throw PipelineTransferException(
            CommonMessageCode.YAML_NOT_VALID,
            arrayOf("Templates are not currently supported")
        )
    }

    fun transfer(
        userId: String,
        projectId: String,
        pipelineId: String?,
        actionType: TransferActionType,
        data: TransferBody,
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList(),
        editPermission: Boolean? = null,
        archiveFlag: Boolean? = false
    ): TransferResponse {
        val watcher = Watcher(id = "yaml and model transfer watcher")
        // #8161 蓝盾PAC默认使用V3版本的YAML语言
        val defaultVersion = YamlVersion.V3_0
        try {
            val model = data.modelAndSetting?.model
            // 无编辑权限需要对流水线插件敏感参数做处理
            if (editPermission == false && model != null) {
                val elementSensitiveParamInfos = AtomUtils.getModelElementSensitiveParamInfos(projectId, model, client)
                model.stages.forEach { stage ->
                    stage.containers.forEach { container ->
                        container.elements.forEach { e ->
                            elementSensitiveParamInfos?.let {
                                pipelineInfoService.transferSensitiveParam(e, it)
                            }
                        }
                    }
                }
            }
            actionType.check(data)
            PipelineTransferAspectLoader.sharedEnvTransfer(aspects)
            val pipelineInfo = pipelineId?.let {
                pipelineInfoDao.convert(
                    t = pipelineInfoDao.getPipelineInfo(
                        dslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT),
                        projectId = projectId,
                        pipelineId = pipelineId
                    ),
                    templateId = null
                )
            }
            when (actionType) {
                FULL_MODEL2YAML -> {
                    return fullModel2Yaml(
                        watcher = watcher,
                        aspects = aspects,
                        userId = userId,
                        data = data,
                        pipelineInfo = pipelineInfo,
                        defaultVersion = defaultVersion
                    )
                }

                FULL_YAML2MODEL -> {
                    return fullYaml2Model(
                        watcher = watcher,
                        aspects = aspects,
                        data = data,
                        pipelineId = pipelineId,
                        projectId = projectId,
                        userId = userId
                    )
                }

                TEMPLATE_MODEL2YAML_PIPELINE -> {
                    return templateModel2YamlPipeline(
                        watcher = watcher,
                        aspects = aspects,
                        userId = userId,
                        projectId = projectId,
                        data = data,
                        defaultVersion = defaultVersion
                    )
                }

                TEMPLATE_YAML2MODEL_PIPELINE -> {
                    return templateYaml2ModelPipeline(
                        watcher = watcher,
                        aspects = aspects,
                        data = data,
                        projectId = projectId,
                        userId = userId
                    )
                }

                TEMPLATE_MODEL2YAML_STAGE -> TODO()
                TEMPLATE_YAML2MODEL_STAGE -> TODO()
                TEMPLATE_MODEL2YAML_JOB -> TODO()
                TEMPLATE_YAML2MODEL_JOB -> TODO()
                TEMPLATE_MODEL2YAML_STEP -> TODO()
                TEMPLATE_YAML2MODEL_STEP -> TODO()
                YAML_INSERT_TASK -> return TransferResponse()
            }
        } finally {
            watcher.stop()
        }
    }

    private fun fullModel2Yaml(
        watcher: Watcher,
        aspects: LinkedList<IPipelineTransferAspect>,
        userId: String,
        data: TransferBody,
        pipelineInfo: PipelineInfo?,
        defaultVersion: YamlVersion
    ): TransferResponse {
        watcher.start("step_1|FULL_MODEL2YAML start")
        val invalidElement = mutableListOf<String>()
        val invalidNameSpaceElement = mutableListOf<String>()
        aspects.addAll(
            PipelineTransferAspectLoader.checkInvalidElement(
                invalidElement, invalidNameSpaceElement
            )
        )
        val response = modelTransfer.model2yaml(
            ModelTransferInput(
                userId = userId,
                model = data.modelAndSetting!!.model,
                setting = data.modelAndSetting!!.setting,
                pipelineInfo = pipelineInfo,
                version = defaultVersion,
                aspectWrapper = PipelineTransferAspectWrapper(aspects)
            )
        )
        if (invalidElement.isNotEmpty()) {
            throw PipelineTransferException(
                ELEMENT_NOT_SUPPORT_TRANSFER,
                arrayOf(invalidElement.joinToString("\n- ", "- "))
            )
        }
        if (invalidNameSpaceElement.isNotEmpty()) {
            throw PipelineTransferException(
                ELEMENT_NOT_SUPPORT_TRANSFER,
                arrayOf(invalidNameSpaceElement.joinToString("\n- ", "- "))
            )
        }
        watcher.start("step_2|mergeYaml")
        val newYaml = TransferMapper.mergeYaml(data.oldYaml, TransferMapper.toYaml(response))
        watcher.stop()
        logger.info(watcher.toString())
        return TransferResponse(
            yamlWithVersion = YamlWithVersion(yamlStr = newYaml, versionTag = defaultVersion.tag)
        )
    }

    private fun fullYaml2Model(
        watcher: Watcher,
        aspects: LinkedList<IPipelineTransferAspect>,
        data: TransferBody,
        pipelineId: String?,
        projectId: String,
        userId: String
    ): TransferResponse {
        watcher.start("step_1|FULL_YAML2MODEL start")
        PipelineTransferAspectLoader.yaml2ModelAspects(aspects)
        yamlSchemaCheck.check(data.oldYaml)
        val pipelineInfo = pipelineId?.let {
            pipelineInfoDao.convert(
                t = pipelineInfoDao.getPipelineInfo(
                    dslContext = dslContext,
                    projectId = projectId,
                    pipelineId = pipelineId
                ),
                templateId = null
            )
        }
        watcher.start("step_2|parse template")
        val pYml = loadYaml(data.oldYaml)
        watcher.start("step_3|transfer start")
        val input = YamlTransferInput(
            userId = userId,
            projectCode = projectId,
            pipelineInfo = pipelineInfo,
            yaml = pYml,
            yamlFileName = data.yamlFileName,
            aspectWrapper = PipelineTransferAspectWrapper(aspects)
        )
        val model = modelTransfer.yaml2Model(input)
        val setting = modelTransfer.yaml2Setting(input)

        logger.info(watcher.toString())
        return TransferResponse(
            yamlWithVersion = YamlWithVersion(
                yamlStr = data.oldYaml, versionTag = input.yaml.version
            ),
            modelAndSetting = PipelineModelAndSetting(model, setting)
        )
    }

    private fun templateModel2YamlPipeline(
        watcher: Watcher,
        aspects: LinkedList<IPipelineTransferAspect>,
        userId: String,
        projectId: String,
        data: TransferBody,
        defaultVersion: YamlVersion
    ): TransferResponse {
        watcher.start("step_1|TEMPLATE_MODEL2YAML_PIPELINE start")
        val invalidElement = mutableListOf<String>()
        val invalidNameSpaceElement = mutableListOf<String>()
        aspects.addAll(PipelineTransferAspectLoader.checkInvalidElement(invalidElement, invalidNameSpaceElement))
        val response = templateModelTransfer.templateModel2yaml(
            TemplateModelTransferInput(
                userId = userId,
                projectId = projectId,
                model = data.templateModelAndSetting!!.templateModel,
                setting = data.templateModelAndSetting!!.setting,
                version = defaultVersion,
                aspectWrapper = PipelineTransferAspectWrapper(aspects)
            )
        )
        if (invalidElement.isNotEmpty()) {
            throw PipelineTransferException(
                ELEMENT_NOT_SUPPORT_TRANSFER,
                arrayOf(invalidElement.joinToString("\n- ", "- "))
            )
        }
        watcher.start("step_2|mergeYaml")
        val newYaml = TransferMapper.mergeYaml(data.oldYaml, TransferMapper.toYaml(response))
        watcher.stop()
        logger.info(watcher.toString())
        return TransferResponse(
            yamlWithVersion = YamlWithVersion(yamlStr = newYaml, versionTag = defaultVersion.tag)
        )
    }

    private fun templateYaml2ModelPipeline(
        watcher: Watcher,
        aspects: LinkedList<IPipelineTransferAspect>,
        data: TransferBody,
        projectId: String,
        userId: String
    ): TransferResponse {
        watcher.start("step_1|TEMPLATE_YAML2MODEL_PIPELINE start")
        PipelineTransferAspectLoader.yaml2ModelAspects(aspects)
        yamlSchemaCheck.check(data.oldYaml)
        watcher.start("step_2|parse template")
        val pYml = loadYaml(data.oldYaml)
        watcher.start("step_3|transfer start")
        val input = YamlTransferInput(
            userId = userId,
            projectCode = projectId,
            pipelineInfo = null,
            yaml = pYml,
            yamlFileName = data.yamlFileName,
            aspectWrapper = PipelineTransferAspectWrapper(aspects),
            templateType = Model::class.java
        )
        val model = templateModelTransfer.yaml2TemplateModel(input)
        val setting = modelTransfer.yaml2Setting(input)

        logger.info(watcher.toString())
        return TransferResponse(
            yamlWithVersion = YamlWithVersion(
                yamlStr = data.oldYaml, versionTag = input.yaml.version
            ),
            templateModelAndSetting = TemplateModelAndSetting(model, setting)
        )
    }

    fun loadYaml(yaml: String): IPreTemplateScriptBuildYamlParser {
        val pYml = TransferMapper.getObjectMapper()
            .readValue(yaml, object : TypeReference<IPreTemplateScriptBuildYamlParser>() {})
        pYml.replaceTemplate { templateFilter ->
            YamlTemplate(
                yamlObject = templateFilter,
                filePath = TemplatePath(TEMPLATE_ROOT_FILE),
                extraParameters = this,
                getTemplateMethod = ::getTemplate,
                nowRepo = null,
                repo = null,
                resourcePoolMapExt = null,
                // TODO #8161 留给模板时再考虑
                conf = YamlTemplateConf(
                    useOldParametersExpression = false
                )
            ).replace()
        }
        return pYml
    }

    fun modelTaskTransfer(
        userId: String,
        projectId: String,
        pipelineId: String,
        data: Element
    ): String {
        val yml = elementTransfer.element2YamlStep(data, projectId) ?: throw PipelineTransferException(
            CommonMessageCode.ELEMENT_NOT_SUPPORT_TRANSFER,
            arrayOf("${data.getClassType()}(${data.name})")
        )
        return TransferMapper.toYaml(listOf(yml))
    }

    fun yamlTaskTransfer(
        userId: String,
        projectId: String,
        pipelineId: String,
        yaml: String
    ): Element {
        val tYml = TransferMapper.getObjectMapper()
            .readValue(yaml, object : TypeReference<PreStep>() {})
        return elementTransfer.yaml2element(userId, ScriptYmlUtils.preStepToStep(tYml), null)
    }

    /**
     * @param force 是否强制更新
     */
    fun buildPreview(
        userId: String,
        projectId: String,
        pipelineId: String,
        resource: PipelineResourceVersion,
        editPermission: Boolean? = null,
        archiveFlag: Boolean? = false,
        force: Boolean = false,
        isEncryptParamsValue: Boolean? = false
    ): PreviewResponse {
        val setting = pipelineSettingVersionService.getPipelineSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            detailInfo = null,
            version = resource.settingVersion ?: 1,
            archiveFlag = archiveFlag
        )
        val modelAndSetting = PipelineModelAndSetting(
            setting = setting,
            model = resource.model
        )
        val pipelineIndex = mutableListOf<TransferMark>()
        val triggerIndex = mutableListOf<TransferMark>()
        val noticeIndex = mutableListOf<TransferMark>()
        val settingIndex = mutableListOf<TransferMark>()
        val yaml = if (force || editPermission == false || resource.yaml.isNullOrBlank() || isEncryptParamsValue == true) {
            transfer(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                actionType = FULL_MODEL2YAML,
                data = TransferBody(modelAndSetting),
                editPermission = editPermission,
                archiveFlag = archiveFlag
            ).yamlWithVersion?.yamlStr ?: return PreviewResponse("")
        } else {
            resource.yaml
        }
        try {
            TransferMapper.getYamlLevelOneIndex(yaml!!).forEach { (key, value) ->
                if (key in pipeline_key) pipelineIndex.add(value)
                if (key in trigger_key) triggerIndex.add(value)
                if (key in notice_key) noticeIndex.add(value)
                if (key in setting_key) settingIndex.add(value)
            }
        } catch (ignore: Throwable) {
            logger.warn("TRANSFER_YAML|$projectId|$userId", ignore)
        }
        return PreviewResponse(yaml!!, pipelineIndex, triggerIndex, noticeIndex, settingIndex)
    }

    fun position(
        userId: String,
        projectId: String,
        line: Int,
        column: Int,
        yaml: String
    ): PositionResponse {
        logger.debug("check position |$line|$column|$yaml")
        val pYml = YamlUtil.getObjectMapper().readValue(yaml, object : TypeReference<ITemplateFilter>() {})
        return yamlIndexService.position(
            userId = userId, line = line, column = column, yaml = yaml, preYaml = pYml
        )
    }

    fun modelTaskInsert(
        userId: String,
        projectId: String,
        pipelineId: String,
        line: Int,
        column: Int,
        data: ElementInsertBody
    ): ElementInsertResponse {
        logger.debug("check position |$projectId|$line|$column|$data")
        return yamlIndexService.modelTaskInsert(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            line = line,
            column = column,
            data = data
        )
    }

    private fun getRepoAliasName(projectId: String, pipelineId: String?): String {
        if (pipelineId.isNullOrBlank()) {
            return DEFAULT_REPO_ALIAS_NAME
        }
        return pipelineYamlInfoDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.let {
            client.get(ServiceRepositoryResource::class).get(
                projectId = projectId, repositoryId = it.repoHashId, repositoryType = RepositoryType.ID
            ).data?.aliasName
        } ?: DEFAULT_REPO_ALIAS_NAME
    }
}
