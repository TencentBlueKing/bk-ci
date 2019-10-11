package com.tencent.devops.plugin.codecc.element

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz
import org.slf4j.LoggerFactory

@ElementBiz
class LinuxPaasCodeCCScriptElementBizPlugin constructor(
    private val coverityApi: CodeccApi
) : ElementBizPlugin<LinuxPaasCodeCCScriptElement> {

    companion object {
        private val logger = LoggerFactory.getLogger(LinuxPaasCodeCCScriptElementBizPlugin::class.java)
    }

    override fun elementClass(): Class<LinuxPaasCodeCCScriptElement> {
        return LinuxPaasCodeCCScriptElement::class.java
    }

    override fun check(element: LinuxPaasCodeCCScriptElement, appearedCnt: Int) {
        if (appearedCnt > 1) {
            throw IllegalArgumentException("只允许一个代码扫描原子")
        }
    }

    override fun afterCreate(
            element: LinuxPaasCodeCCScriptElement,
            projectId: String,
            pipelineId: String,
            pipelineName: String,
            userId: String,
            channelCode: ChannelCode
    ) {
        with(element) {
            if (languages.isEmpty()) {
                throw OperationException("工程语言不能为空")
            }
            try {
                if (!codeCCTaskId.isNullOrEmpty()) {
                    if (coverityApi.isTaskExist(codeCCTaskId!!, userId)) {
                        // Update the coverity
                        coverityApi.updateTask(pipelineName, userId, element)
                        return
                    }
                }
                // Create a new one
                val task = coverityApi.createTask(projectId, pipelineId, pipelineName, userId, element)
                logger.info("Create the coverity task($task)")
                val dataMap = task.data as Map<String, Any>
                codeCCTaskId = (dataMap["taskId"] as Int).toString()
                codeCCTaskName = dataMap["nameEn"] as String
                codeCCTaskCnName = pipelineName
            } catch (e: Exception) {
                logger.warn(
                    "Fail to create the coverity codecc task($projectId|$pipelineId|$pipelineName|$userId|$name)",
                    e
                )
                throw OperationException("代码检查任务创建失败，请联系【助手】")
            }
        }
    }

    override fun beforeDelete(element: LinuxPaasCodeCCScriptElement, userId: String, pipelineId: String?) {
        with(element) {
            logger.info("Start to delete the task($codeCCTaskId) in codecc by user $userId")
            if (codeCCTaskId.isNullOrEmpty()) {
                logger.warn("The codecc task id is empty")
                return
            }
            coverityApi.deleteTask(codeCCTaskId!!, userId)
            codeCCTaskId = null
        }
    }
}