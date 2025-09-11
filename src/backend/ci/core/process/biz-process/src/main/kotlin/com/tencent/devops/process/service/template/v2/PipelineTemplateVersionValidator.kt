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

package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.template.ITemplateModel
import com.tencent.devops.common.pipeline.template.JobTemplateModel
import com.tencent.devops.common.pipeline.template.StageTemplateModel
import com.tencent.devops.common.pipeline.template.StepTemplateModel
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.store.pojo.atom.AtomCodeVersionReqItem
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineTemplateVersionValidator @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val client: Client
) {

    fun validate(context: PipelineTemplateVersionCreateContext) {
        with(context) {
            validateBasicInfo()
            validateModelInfo(
                userId = userId,
                projectId = projectId,
                templateModel = pTemplateResourceWithoutVersion.model,
                pipelineAsCodeSettings = pTemplateSettingWithoutVersion.pipelineAsCodeSettings,
                newTemplate = newTemplate
            )
        }
    }

    private fun PipelineTemplateVersionCreateContext.validateBasicInfo() {
        if (pipelineTemplateInfoService.isNameExist(
                projectId = projectId,
                templateName = pTemplateSettingWithoutVersion.pipelineName,
                excludeTemplateId = pTemplateResourceWithoutVersion.templateId
            )
        ) {
            throw ErrorCodeException(
                statusCode = Response.Status.CONFLICT.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NAME_EXISTS
            )
        }
    }

    fun validateModelInfo(
        userId: String,
        projectId: String,
        templateModel: ITemplateModel,
        pipelineAsCodeSettings: PipelineAsCodeSettings?,
        newTemplate: Boolean = false
    ) {
        if (templateModel is Model) {
            val pipelineDialect = pipelineAsCodeService.getPipelineDialect(
                projectId = projectId,
                asCodeSettings = pipelineAsCodeSettings
            )
            modelCheckPlugin.checkModelIntegrity(
                model = templateModel,
                projectId = projectId,
                userId = userId,
                isTemplate = true,
                pipelineDialect = pipelineDialect
            )
            // 只在更新操作时检查stage数量不为1
            if (!newTemplate && templateModel.stages.size <= 1) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_WITH_EMPTY_STAGE, params = arrayOf()
            )
        }
        checkTemplateAtomsForExplicitVersion(templateModel = templateModel, userId = userId)
    }

    /**
     * 检查模板中是否存在已下架、测试中插件(明确版本号)
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    private fun checkTemplateAtomsForExplicitVersion(templateModel: ITemplateModel, userId: String) {
        val codeVersions = mutableSetOf<AtomCodeVersionReqItem>()
        when (templateModel) {
            is Model -> {
                templateModel.stages.forEach { stage ->
                    stage.containers.forEach { container ->
                        container.elements.forEach nextElement@{ element ->
                            val atomCode = element.getAtomCode()
                            val version = element.version
                            if (version.contains("*")) {
                                return@nextElement
                            }
                            codeVersions.add(AtomCodeVersionReqItem(atomCode, version))
                        }
                    }
                }
            }

            is StageTemplateModel -> {
                templateModel.stages.forEach { stage ->
                    stage.containers.forEach { container ->
                        container.elements.forEach nextElement@{ element ->
                            val atomCode = element.getAtomCode()
                            val version = element.version
                            if (version.contains("*")) {
                                return@nextElement
                            }
                            codeVersions.add(AtomCodeVersionReqItem(atomCode, version))
                        }
                    }
                }
            }

            is JobTemplateModel -> {
                templateModel.containers.forEach { container ->
                    container.elements.forEach nextElement@{ element ->
                        val atomCode = element.getAtomCode()
                        val version = element.version
                        if (version.contains("*")) {
                            return@nextElement
                        }
                        codeVersions.add(AtomCodeVersionReqItem(atomCode, version))
                    }
                }
            }

            is StepTemplateModel -> {
                templateModel.container.elements.forEach nextElement@{ element ->
                    val atomCode = element.getAtomCode()
                    val version = element.version
                    if (version.contains("*")) {
                        return@nextElement
                    }
                    codeVersions.add(AtomCodeVersionReqItem(atomCode, version))
                }
            }
        }

        if (codeVersions.isNotEmpty()) {
            AtomUtils.checkTemplateRealVersionAtoms(
                codeVersions = codeVersions,
                userId = userId,
                client = client
            )
        }
    }
}
