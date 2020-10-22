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

package com.tencent.devops.plugin.codecc.element

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
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
        channelCode: ChannelCode,
        create: Boolean
    ) {
        with(element) {
            if (languages.isEmpty()) {
                throw OperationException("工程语言不能为空")
            }
            try {
                if ((!create) && (!codeCCTaskId.isNullOrEmpty())) {
                    if (coverityApi.isTaskExist(codeCCTaskId!!, userId)) {
                        // Update the coverity
                        coverityApi.updateTask(pipelineName, userId, element)
                        return
                    }
                }
                // Create a new one
                val task = coverityApi.createTask(projectId, pipelineId, pipelineName, userId, element)
                // 返回data可能是map，也可能是true
                logger.info("Create the coverity task($task)")
                if (task.data is Map<*, *>) {
                    val dataMap = task.data as Map<String, Any>
                    codeCCTaskId = (dataMap["taskId"] as Int).toString()
                    codeCCTaskName = dataMap["nameEn"] as String
                    codeCCTaskCnName = pipelineName
                }
            } catch (e: Exception) {
                logger.warn(
                    "Fail to create the coverity codecc task($projectId|$pipelineId|$pipelineName|$userId|$name)",
                    e
                )
                throw OperationException("代码检查任务创建失败，请联系【助手】")
            }
        }
    }

    override fun beforeDelete(element: LinuxPaasCodeCCScriptElement, param: BeforeDeleteParam) {
        with(element) {
            logger.info("Start to delete the codecc task($codeCCTaskId) in codecc by user ${param.userId}")
            if (codeCCTaskId.isNullOrEmpty()) {
                logger.warn("The codecc task id is empty")
                return
            }
            coverityApi.deleteTask(codeCCTaskId!!, param.userId)
            codeCCTaskId = null
        }
    }
}