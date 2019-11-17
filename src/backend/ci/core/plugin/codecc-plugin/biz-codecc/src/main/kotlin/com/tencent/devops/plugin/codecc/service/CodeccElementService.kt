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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.codecc.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.plugin.codecc.dao.CodeccElementDao
import com.tencent.devops.plugin.codecc.pojo.CodeccElementData
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CodeccElementService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val codeccElementDao: CodeccElementDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CodeccElementService::class.java)
    }

    fun saveEvent(pipelineModelAnalysisEvent: PipelineModelAnalysisEvent) {
        logger.info("deal with pipeline model event: $pipelineModelAnalysisEvent")
        with(pipelineModelAnalysisEvent) {
            // 已经删除流水线
            if (model.isBlank()) {
                codeccElementDao.delete(dslContext, projectId, pipelineId)
                return
            }

            val pipelineModel = objectMapper.readValue<Model>(model)
            val element = getModelCodeccElement(pipelineModel)
            // 已经删除了codecc原子
            if (element == null) {
                logger.info("codecc element is deleted for pipeline(${pipelineModelAnalysisEvent.pipelineId})")
                codeccElementDao.delete(dslContext, projectId, pipelineId)
            } else {
                logger.info("save codecc element data for pipeline(${pipelineModelAnalysisEvent.pipelineId})")
                val isSync = if (element.asynchronous != null && element.asynchronous!!) "0" else "1"
                val taskId = if (pipelineModelAnalysisEvent.channelCode == ChannelCode.CODECC.name) {
                    pipelineModel.name
                } else {
                    if (element is LinuxPaasCodeCCScriptElement) {
                        element.codeCCTaskId
                    } else {
                        ""
                    }
                }
                val elementData = CodeccElementData(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskName = element.codeCCTaskName ?: "",
                    taskCnName = pipelineModel.name,
                    taskId = taskId ?: "",
                    sync = isSync,
                    scanType = element.scanType ?: "",
                    language = element.languages.joinToString(","),
                    platform = element.compilePlat ?: "",
                    tools = element.tools?.joinToString(",") ?: "",
                    pythonVersion = element.pyVersion ?: "",
                    eslintRc = element.eslintRc ?: "",
                    codePath = element.path ?: "",
                    scriptType = element.scriptType.name,
                    script = element.script,
                    channelCode = channelCode,
                    updateUserId = userId
                )
                codeccElementDao.save(dslContext, elementData)
            }
        }
    }

    private fun getModelCodeccElement(model: Model): LinuxCodeCCScriptElement? {
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                val codeccElemet =
                    container.elements.filter { it is LinuxCodeCCScriptElement || it is LinuxPaasCodeCCScriptElement }
                if (codeccElemet.isNotEmpty()) return codeccElemet.first() as LinuxCodeCCScriptElement
            }
        }
        return null
    }

    fun getCodeccElement(projectId: String, pipelineId: String): CodeccElementData {
        val record = codeccElementDao.get(dslContext, projectId, pipelineId)
            ?: throw RuntimeException("not found codecc element for project($projectId), pipeline($pipelineId)")
        return CodeccElementData(
            projectId = projectId,
            pipelineId = pipelineId,
            taskName = record.taskName,
            taskCnName = record.taskCnName,
            taskId = record.taskId,
            sync = record.isSync,
            scanType = record.scanType,
            language = record.language,
            platform = record.platform,
            tools = record.tools,
            pythonVersion = record.pyVersion,
            eslintRc = record.eslintRc,
            codePath = record.codePath,
            scriptType = record.scriptType,
            script = record.script,
            channelCode = record.channelCode,
            updateUserId = record.updateUserId
        )
    }
}