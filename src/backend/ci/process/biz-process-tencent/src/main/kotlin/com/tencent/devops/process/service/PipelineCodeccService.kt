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

package com.tencent.devops.process.service

import com.tencent.devops.common.pipeline.pojo.coverity.CoverityResult
import com.tencent.devops.common.pipeline.pojo.coverity.ProjectLanguage
import com.tencent.devops.common.pipeline.pojo.element.atom.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.utils.CoverityUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.constant.WebsocketCode
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.common.websocket.enum.NotityLevel
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.common.websocket.pojo.NotifyPost
import com.tencent.devops.common.websocket.pojo.WebSocketType
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.websocket.EditPageBuild
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCodeccService @Autowired constructor(
    private val pipelineTaskService: PipelineTaskService,
    private val websocketDispatcher: WebSocketDispatcher,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    fun updateCodeccTask(userId: String, setting: PipelineSetting) {
        val element = getCodeccElement(setting.projectId, setting.pipelineId) ?: return
        updateTask(element, setting.pipelineName, userId)
    }

    fun updateCodeccTask(userId: String, projectId: String, pipelineId: String, pipelineName: String) {
        val element = getCodeccElement(projectId, pipelineId) ?: return
        logger.info("[$pipelineId]-updateCodeccTask,element:{$element}")
        updateTask(element, pipelineName, userId)
    }

    fun getCodeccElement(projectId: String, pipelineId: String): PipelineModelTask? {
        val elementList = pipelineTaskService.list(projectId, setOf(pipelineId))[pipelineId] ?: listOf()
        return elementList.firstOrNull { it.classType == LinuxPaasCodeCCScriptElement.classType }
    }

    fun createTask(
        projectId: String,
        pipelineId: String,
        userId: String,
        pipelineName: String,
        element: LinuxPaasCodeCCScriptElement?,
        variables: Map<String, Any>?
    ): CoverityResult {
        try {
            // Create a new one
            val task = CoverityUtils.createTask(
                projectId,
                pipelineId,
                pipelineName,
                userId,
                element!!.languages,
                element.compilePlat ?: "LINUX",
                element.tools ?: listOf("COVERITY"),
                element.pyVersion ?: "",
                element.eslintRc ?: "",
                element.scanType ?: "1",
                element.phpcsStandard ?: "",
                element.goPath ?: "",
                element.ccnThreshold,
                null,
                genToolSet(variables ?: mutableMapOf())
            )
            return task
        } catch (e: Exception) {
            val post = NotifyPost(
                module = "process",
                message = e.message!!,
                level = NotityLevel.HIGH_LEVEL.getLevel(),
                dealUrl = EditPageBuild().buildPage(
                    BuildPageInfo(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = null,
                        atomId = null
                    )
                ),
                code = WebsocketCode.CODECC_ADD_ERROR,
                webSocketType = WebSocketType.changWebType(WebSocketType.CODECC),
                page = ""
            )
            logger.warn("[$pipelineId]调用codecc add返回异常。webSocket推送异常信息[$post]")
            throw e
        }
    }

    private fun updateTask(element: PipelineModelTask, pipelineName: String, userId: String) {
        try {
            logger.info("[${element.pipelineId}],update task start]")
            val taskParams = element.taskParams
            val taskId = taskParams["codeCCTaskId"] ?: return
            val language = solveJsonArr(taskParams["languages"]?.toString()).map { ProjectLanguage.valueOf(it) }
            val tools = solveJsonArr(taskParams["tools"]?.toString())
            val needCodeContent = taskParams["needCodeContent"]?.toString()
            CoverityUtils.updateTask(
                pipelineName,
                userId,
                taskId.toString(),
                language,
                taskParams["compilePlat"]?.toString() ?: "LINUX",
                if (tools.isNotEmpty()) tools else listOf("COVERITY"),
                taskParams["pyVersion"]?.toString() ?: "",
                taskParams["eslintRc"]?.toString() ?: "",
                taskParams["scanType"]?.toString() ?: "1",
                taskParams["phpcsStandard"]?.toString() ?: "",
                taskParams["goPath"]?.toString() ?: "",
                taskParams["ccnThreshold"] as? Int,
                needCodeContent,
                genToolSet(taskParams)
            )
            logger.info("[${element.pipelineId}],update task end]")
        } catch (e: Exception) {
            logger.error("update codecc task fail: ${e.message}", e)
        }
    }

    private fun genToolSet(taskParams: Map<String, Any>): Map<String, String> {
        val map = mutableMapOf<String, String>()

        val coverityToolSetId = taskParams["coverityToolSetId"] as? String
        val klocworkToolSetId = taskParams["klocworkToolSetId"] as? String
        val cpplintToolSetId = taskParams["cpplintToolSetId"] as? String
        val eslintToolSetId = taskParams["eslintToolSetId"] as? String
        val pylintToolSetId = taskParams["pylintToolSetId"] as? String
        val gometalinterToolSetId = taskParams["gometalinterToolSetId"] as? String
        val checkStyleToolSetId = taskParams["checkStyleToolSetId"] as? String
        val styleCopToolSetId = taskParams["styleCopToolSetId"] as? String
        val detektToolSetId = taskParams["detektToolSetId"] as? String
        val phpcsToolSetId = taskParams["phpcsToolSetId"] as? String
        val sensitiveToolSetId = taskParams["sensitiveToolSetId"] as? String
        val occheckToolSetId = taskParams["occheckToolSetId"] as? String

        if (!coverityToolSetId.isNullOrBlank()) map["coverityToolSetId"] = coverityToolSetId!!
        if (!klocworkToolSetId.isNullOrBlank()) map["klocworkToolSetId"] = klocworkToolSetId!!
        if (!cpplintToolSetId.isNullOrBlank()) map["cpplintToolSetId"] = cpplintToolSetId!!
        if (!eslintToolSetId.isNullOrBlank()) map["eslintToolSetId"] = eslintToolSetId!!
        if (!pylintToolSetId.isNullOrBlank()) map["pylintToolSetId"] = pylintToolSetId!!
        if (!gometalinterToolSetId.isNullOrBlank()) map["gometalinterToolSetId"] = gometalinterToolSetId!!
        if (!checkStyleToolSetId.isNullOrBlank()) map["checkStyleToolSetId"] = checkStyleToolSetId!!
        if (!styleCopToolSetId.isNullOrBlank()) map["styleCopToolSetId"] = styleCopToolSetId!!
        if (!detektToolSetId.isNullOrBlank()) map["detektToolSetId"] = detektToolSetId!!
        if (!phpcsToolSetId.isNullOrBlank()) map["phpcsToolSetId"] = phpcsToolSetId!!
        if (!sensitiveToolSetId.isNullOrBlank()) map["sensitiveToolSetId"] = sensitiveToolSetId!!
        if (!occheckToolSetId.isNullOrBlank()) map["occheckToolSetId"] = occheckToolSetId!!

        return map
    }

    private fun solveJsonArr(toolString: String?): List<String> {
        if (toolString.isNullOrBlank()) return listOf()
        return toolString!!.trim().removePrefix("[").removeSuffix("]").split(",").map { it.trim() }
    }
}
