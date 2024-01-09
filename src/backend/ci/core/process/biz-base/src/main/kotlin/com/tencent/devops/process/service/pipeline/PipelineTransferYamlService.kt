/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertBody
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertResponse
import com.tencent.devops.common.pipeline.pojo.transfer.PositionResponse
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.common.pipeline.pojo.transfer.PreviewResponse
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.common.pipeline.pojo.transfer.TransferMark
import com.tencent.devops.common.pipeline.pojo.transfer.TransferResponse
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineYamlInfoDao
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.yaml.transfer.ElementTransfer
import com.tencent.devops.process.yaml.transfer.ModelTransfer
import com.tencent.devops.process.yaml.transfer.PipelineTransferException
import com.tencent.devops.process.yaml.transfer.TransferMapper
import com.tencent.devops.process.yaml.transfer.YamlIndexService
import com.tencent.devops.process.yaml.transfer.aspect.IPipelineTransferAspect
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectLoader
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.transfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.transfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.transfer.schema.YamlSchemaCheck
import com.tencent.devops.process.yaml.pojo.TemplatePath
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v3.models.ITemplateFilter
import com.tencent.devops.process.yaml.v3.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v3.parsers.template.YamlTemplateConf
import com.tencent.devops.process.yaml.v3.parsers.template.models.GetTemplateParam
import com.tencent.devops.process.yaml.v3.utils.ScriptYmlUtils
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.LinkedList

@Suppress("ALL")
@Service
class PipelineTransferYamlService @Autowired constructor(
    private val dslContext: DSLContext,
    private val modelTransfer: ModelTransfer,
    private val elementTransfer: ElementTransfer,
    private val pipelineSettingVersionService: PipelineSettingVersionService,
    private val pipelineInfoDao: PipelineInfoDao,
    private val yamlIndexService: YamlIndexService,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val client: Client,
    private val yamlSchemaCheck: YamlSchemaCheck
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTransferYamlService::class.java)
        private const val TEMPLATE_ROOT_FILE = "TEMPLATE_ROOT_FILE"
        private val pipeline_key = listOf("stages", "jobs", "steps", "finally")
        private val trigger_key = listOf("on")
        private val notice_key = listOf("notices")
        private val setting_key = listOf("concurrency", "name", "version", "label")
        private const val DEFAULT_REPO_ALIAS_NAME = "self"
    }

    fun getTemplate(param: GetTemplateParam<Any>): String {
        return ""
    }

    fun transfer(
        userId: String,
        projectId: String,
        pipelineId: String?,
        actionType: TransferActionType,
        data: TransferBody,
        aspects: LinkedList<IPipelineTransferAspect>? = null
    ): TransferResponse {
        val watcher = Watcher(id = "yaml and model transfer watcher")
        try {
            when (actionType) {
                TransferActionType.FULL_MODEL2YAML -> {
                    watcher.start("step_1|FULL_MODEL2YAML start")
                    val invalidElement = mutableListOf<String>()
                    val defaultAspects = PipelineTransferAspectLoader.checkInvalidElement(invalidElement)
                    val response = modelTransfer.model2yaml(
                        ModelTransferInput(
                            userId = userId,
                            model = data.modelAndSetting!!.model,
                            setting = data.modelAndSetting!!.setting,
                            version = YamlVersion.Version.V3_0,
                            aspectWrapper = PipelineTransferAspectWrapper(aspects ?: defaultAspects)
                        )
                    )
                    val newYaml = TransferMapper.mergeYaml(data.oldYaml, TransferMapper.toYaml(response))
                    if (invalidElement.isNotEmpty()) {
                        throw PipelineTransferException(
                            ELEMENT_NOT_SUPPORT_TRANSFER,
                            arrayOf(invalidElement.joinToString("\n- ", "- "))
                        )
                    }
                    watcher.start("step_2|mergeYaml")
                    watcher.stop()
                    logger.info(watcher.toString())
                    return TransferResponse(newYaml = newYaml)
                }

                TransferActionType.FULL_YAML2MODEL -> {
                    watcher.start("step_1|FULL_YAML2MODEL start")
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
                    val defaultAspects = PipelineTransferAspectLoader.initByDefaultTriggerOn(
                        {
                            getRepoAliasName(projectId = projectId, pipelineId)
                        }
                    )
                    val pYml = TransferMapper.getObjectMapper()
                        .readValue(data.oldYaml, object : TypeReference<IPreTemplateScriptBuildYaml>() {})
                    watcher.start("step_2|parse template")
                    pYml.replaceTemplate { templateFilter ->
                        YamlTemplate(
                            yamlObject = templateFilter,
                            filePath = TemplatePath(TEMPLATE_ROOT_FILE),
                            extraParameters = this,
                            getTemplateMethod = ::getTemplate,
                            nowRepo = null,
                            repo = null,
                            resourcePoolMapExt = null,
                            conf = YamlTemplateConf(
                                useOldParametersExpression = false // todo
                            )
                        ).replace()
                    }
                    watcher.start("step_3|transfer start")
                    val input = YamlTransferInput(
                        userId = userId,
                        projectCode = projectId,
                        pipelineInfo = pipelineInfo,
                        yaml = pYml,
                        aspectWrapper = PipelineTransferAspectWrapper(aspects ?: defaultAspects)
                    )
                    val model = modelTransfer.yaml2Model(input)
                    val setting = modelTransfer.yaml2Setting(input)

                    logger.info(watcher.toString())
                    return TransferResponse(modelAndSetting = PipelineModelAndSetting(model, setting))
                }
            }
        } finally {
            watcher.stop()
        }
        return TransferResponse()
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

    fun buildPreview(
        userId: String,
        projectId: String,
        pipelineId: String,
        resource: PipelineResourceVersion
    ): PreviewResponse {
        val setting = pipelineSettingVersionService.getPipelineSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            detailInfo = null,
            version = resource.settingVersion ?: 1
        )
        val modelAndSetting = PipelineModelAndSetting(
            setting = setting,
            model = resource.model
        )
        val pipelineIndex = mutableListOf<TransferMark>()
        val triggerIndex = mutableListOf<TransferMark>()
        val noticeIndex = mutableListOf<TransferMark>()
        val settingIndex = mutableListOf<TransferMark>()
        val yaml = resource.yaml ?: transfer(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            actionType = TransferActionType.FULL_MODEL2YAML,
            data = TransferBody(modelAndSetting)
        ).newYaml ?: return PreviewResponse("")
        try {
            TransferMapper.getYamlLevelOneIndex(yaml).forEach { (key, value) ->
                if (key in pipeline_key) pipelineIndex.add(value)
                if (key in trigger_key) triggerIndex.add(value)
                if (key in notice_key) noticeIndex.add(value)
                if (key in setting_key) settingIndex.add(value)
            }
        } catch (ignore: Throwable) {
            logger.warn("TRANSFER_YAML|$projectId|$userId", ignore)
        }
        return PreviewResponse(yaml, pipelineIndex, triggerIndex, noticeIndex, settingIndex)
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
