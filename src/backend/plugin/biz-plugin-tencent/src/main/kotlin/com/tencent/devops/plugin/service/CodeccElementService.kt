package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.event.pojo.pipeline.PipelineModelAnalysisEvent
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.LinuxPaasCodeCCScriptElement
import com.tencent.devops.plugin.dao.CodeccElementDao
import com.tencent.devops.plugin.pojo.codecc.CodeccElementData
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
                        projectId,
                        pipelineId,
                        element.codeCCTaskName ?: "",
                        pipelineModel.name,
                        taskId ?: "",
                        isSync,
                        element.scanType ?: "",
                        element.languages.joinToString(","),
                        element.compilePlat ?: "",
                        element.tools?.joinToString(",") ?: "",
                        element.pyVersion ?: "",
                        element.eslintRc ?: "",
                        element.path ?: "",
                        element.scriptType.name,
                        element.script,
                        channelCode,
                        userId
                )
                codeccElementDao.save(dslContext, elementData)
            }
        }
    }

    private fun getModelCodeccElement(model: Model): LinuxCodeCCScriptElement? {
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                val codeccElemet = container.elements.filter { it is LinuxCodeCCScriptElement || it is LinuxPaasCodeCCScriptElement }
                if (codeccElemet.isNotEmpty()) return codeccElemet.first() as LinuxCodeCCScriptElement
            }
        }
        return null
    }

    fun getCodeccElement(projectId: String, pipelineId: String): CodeccElementData? {
        val record = codeccElementDao.get(dslContext, projectId, pipelineId) ?: return null
        return CodeccElementData(
                projectId,
                pipelineId,
                record.taskName,
                record.taskCnName,
                record.taskId,
                record.isSync,
                record.scanType,
                record.language,
                record.platform,
                record.tools,
                record.pyVersion,
                record.eslintRc,
                record.codePath,
                record.scriptType,
                record.script,
                record.channelCode,
                record.updateUserId
        )
    }
}