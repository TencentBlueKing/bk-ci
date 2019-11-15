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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement.ProjectLanguage
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.plugin.codecc.pojo.coverity.CodeccReport
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class PipelineCodeccService @Autowired constructor(
        private val client: Client,
        private val coverityApi: CodeccApi,
        private val pipelinePermissionService: PipelinePermissionService
) {

    fun getCodeccReport(
        userId: String,
        projectId: String,
        pipelineId: String,
        checkPermission: Boolean = true
    ): CodeccReport {

        if (checkPermission) {
            checkPermission(userId, projectId, pipelineId)
        }

        val model = client.get(ServicePipelineResource::class).get(userId, projectId, pipelineId, ChannelCode.BS).data
            ?: throw NotFoundException("流水线不存在")

        return try {
            getReport(model, projectId, pipelineId, userId)
        } catch (ignored: Exception) {
            logger.warn("Fail to parse the model($pipelineId)", ignored)
            throw RuntimeException("Fail to parse the mode of pipeline")
        } ?: throw OperationException("此流水线没有包含代码扫描原子")
    }

    private fun checkPermission(userId: String, projectId: String, pipelineId: String) {
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw PermissionForbiddenException("用户（$userId) 无权限获取流水线($pipelineId)Codecc报告")
        }
    }

    private fun getReport(model: Model, projectId: String, pipelineId: String, userId: String): CodeccReport? {
        model.stages.forEach { s ->
            s.containers.forEach c@{ c ->
                c.elements.forEach { e ->
                    if (e is LinuxPaasCodeCCScriptElement && e.codeCCTaskId != null) {
                        return coverityApi.getReport(projectId, pipelineId, e.codeCCTaskId!!, userId)
                    }
                }
            }
        }
        return null
    }

    fun updateCodeccTask(userId: String, projectId: String, pipelineId: String, pipelineName: String) {
        try {
            val element = getCodeccElement(projectId, pipelineId) ?: return
            val taskParams = element.taskParams
            val taskId = taskParams["codeCCTaskId"] ?: return
            val language = transferLanguage(taskParams["languages"].toString())
            val tools = taskParams["tools"]?.toString()

            val codeccElement = LinuxPaasCodeCCScriptElement(
                    "",
                    "",
                    "",
                    BuildScriptType.SHELL,
                    "",
                    "",
                    "",
                    taskId.toString(),
                    false,
                    taskParams["scanType"]?.toString() ?: "1",
                    "",
                    language
            )
            codeccElement.compilePlat = taskParams["compilePlat"]?.toString() ?: "LINUX"
            codeccElement.tools = if (!tools.isNullOrBlank()) JsonUtil.to(tools!!, object : TypeReference<List<String>>() {})
                                    else listOf("COVERITY")
            codeccElement.pyVersion = taskParams["pyVersion"]?.toString() ?: ""
            codeccElement.eslintRc = taskParams["eslintRc"]?.toString() ?: ""
            codeccElement.scanType = taskParams["scanType"]?.toString() ?: ""
            codeccElement.phpcsStandard = taskParams["phpcsStandard"]?.toString() ?: ""
            codeccElement.goPath = taskParams["goPath"]?.toString() ?: ""
            coverityApi.updateTask(
                pipelineName = pipelineName,
                userId = userId,
                element = codeccElement
            )
        } catch (ignored: Exception) {
            logger.error("update codecc task fail: ${ignored.message}", ignored)
        }
    }

    private fun transferLanguage(languageStr: String): List<ProjectLanguage> {
        return languageStr.trim().removePrefix("[").removeSuffix("]").split(",").map {
            ProjectLanguage.fromValue(it.trim())
        }
    }

    private fun getCodeccElement(projectId: String, pipelineId: String): PipelineModelTask? {
        val elementList = client.get(ServicePipelineTaskResource::class)
            .list(projectId, setOf(pipelineId)).data?.get(pipelineId) ?: listOf()
        return elementList.firstOrNull { it.classType == LinuxPaasCodeCCScriptElement.classType }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCodeccService::class.java)
    }
}

fun main(args: Array<String>) {
    val language = JsonUtil.to("[PYTHON, GOLANG]", object : TypeReference<List<ProjectLanguage>>() {})
    println(language)
}