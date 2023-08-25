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

package com.tencent.devops.process.service.transfer

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.transfer.PreviewResponse
import com.tencent.devops.process.pojo.transfer.TransferActionType
import com.tencent.devops.process.pojo.transfer.TransferBody
import com.tencent.devops.process.pojo.transfer.TransferMark
import com.tencent.devops.process.pojo.transfer.TransferResponse
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.yaml.modelTransfer.ElementTransfer
import com.tencent.devops.process.yaml.modelTransfer.ModelTransfer
import com.tencent.devops.process.yaml.modelTransfer.TransferMapper
import com.tencent.devops.process.yaml.modelTransfer.pojo.ModelTransferInput
import com.tencent.devops.process.yaml.modelTransfer.pojo.YamlTransferInput
import com.tencent.devops.process.yaml.pojo.TemplatePath
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v2.models.IPreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.step.PreStep
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplate
import com.tencent.devops.process.yaml.v2.parsers.template.YamlTemplateConf
import com.tencent.devops.process.yaml.v2.parsers.template.models.GetTemplateParam
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineTransferYamlService @Autowired constructor(
    private val modelTransfer: ModelTransfer,
    private val elementTransfer: ElementTransfer,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTransferYamlService::class.java)
        private const val TEMPLATE_ROOT_FILE = "TEMPLATE_ROOT_FILE"
        private val pipeline_key = listOf("stages", "jobs", "steps", "finally")
        private val trigger_key = listOf("on")
        private val notice_key = listOf("notices")
        private val setting_key = listOf("concurrency", "name", "version", "label")
    }

    fun getTemplate(param: GetTemplateParam<Any>): String {
        return ""
    }

    fun transfer(
        userId: String,
        projectId: String,
        pipelineId: String,
        actionType: TransferActionType,
        data: TransferBody
    ): TransferResponse {
        val watcher = Watcher(id = "yaml and model transfer watcher")
        try {
            when (actionType) {
                TransferActionType.FULL_MODEL2YAML -> {
                    watcher.start("step_1|FULL_MODEL2YAML start")
                    val yml = modelTransfer.model2yaml(
                        ModelTransferInput(
                            data.modelAndSetting.model,
                            data.modelAndSetting.setting,
                            YamlVersion.Version.V3_0
                        )
                    )
                    watcher.start("step_2|mergeYaml")
                    val newYaml = TransferMapper.mergeYaml(data.oldYaml, TransferMapper.toYaml(yml))
                    watcher.stop()
                    logger.info(watcher.toString())
                    return TransferResponse(newYaml = newYaml)
                }
                TransferActionType.FULL_YAML2MODEL -> {
                    watcher.start("step_1|FULL_YAML2MODEL start")
                    val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
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
                        userId, projectId, pipelineInfo, pYml
                    )
                    val model = modelTransfer.yaml2Model(input)
                    val setting = modelTransfer.yaml2Setting(input)

                    logger.info(watcher.toString())
                    return TransferResponse(modelAndSetting = PipelineModelAndSetting(model, setting))
                }
            }
        } catch (t: Throwable) {
            logger.warn("PAC|TRANSFER|transferAction")
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
        val yml = elementTransfer.element2YamlStep(data) ?: throw ErrorCodeException(errorCode = "")
        return TransferMapper.toYaml(yml)
    }

    fun yamlTaskTransfer(
        userId: String,
        projectId: String,
        pipelineId: String,
        yaml: String
    ): Element {
        val tYml = TransferMapper.getObjectMapper()
            .readValue(yaml, object : TypeReference<PreStep>() {})
        return elementTransfer.yaml2element(ScriptYmlUtils.preStepToStep(tYml), null)
    }

    fun buildPreview(
        userId: String,
        projectId: String,
        pipelineId: String,
        version: Int?
    ): PreviewResponse {
        val resource = getPipelineResource(projectId, pipelineId, version)
            ?: return PreviewResponse("")
        val setting = pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = resource.settingVersion ?: version ?: 1
        )
        val modelAndSetting = PipelineModelAndSetting(
            setting = setting,
            model = resource.model
        )
        val yaml = resource.yaml ?: transfer(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            actionType = TransferActionType.FULL_MODEL2YAML,
            data = TransferBody(modelAndSetting)
        ).newYaml ?: return PreviewResponse("")

        val pipelineIndex = mutableListOf<TransferMark>()
        val triggerIndex = mutableListOf<TransferMark>()
        val noticeIndex = mutableListOf<TransferMark>()
        val settingIndex = mutableListOf<TransferMark>()
        TransferMapper.getYamlLevelOneIndex(yaml).forEach { (key, value) ->
            if (key in pipeline_key) pipelineIndex.add(value)
            if (key in trigger_key) triggerIndex.add(value)
            if (key in notice_key) noticeIndex.add(value)
            if (key in setting_key) settingIndex.add(value)
        }
        return PreviewResponse(yaml, pipelineIndex, triggerIndex, noticeIndex, settingIndex)
    }

    private fun getPipelineResource(
        projectId: String,
        pipelineId: String,
        version: Int?
    ): PipelineResourceVersion? {
        // 如果指定版本号则获取对应版本内容，如果未指定则获取最新内容
        return pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        )
    }
}
