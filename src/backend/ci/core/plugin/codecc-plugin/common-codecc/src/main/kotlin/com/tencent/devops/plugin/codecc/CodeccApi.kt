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

package com.tencent.devops.plugin.codecc

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.plugin.codecc.pojo.coverity.CodeccReport
import com.tencent.devops.plugin.codecc.pojo.coverity.CoverityResult
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.net.URLEncoder

open class CodeccApi constructor(
    private val codeccApiUrl: String,
    private val codeccApiProxyUrl: String,
    private val createPath: String = "/ms/task/api/service/task",
    private val updatePath: String = "/ms/task/api/service/task",
    private val existPath: String = "/ms/task/api/service/task/exists",
    private val deletePath: String = "/ms/task/api/service/task",
    private val report: String = "/api",
    private val getRuleSetsPath: String = "/ms/defect/api/service/checker/tools/{toolName}/pipelineCheckerSets"
) {

    companion object {
        private val objectMapper = JsonUtil.getObjectMapper()
        private val logger = LoggerFactory.getLogger(CodeccApi::class.java)
        private const val USER_NAME_HEADER = "X-DEVOPS-UID"
        private const val DEVOPS_PROJECT_ID = "X-DEVOPS-PROJECT-ID"
        private const val CONTENT_TYPE = "Content-Type"
        private const val CONTENT_TYPE_JSON = "application/json"
    }

    open fun createTask(
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        rtx: String,
        element: LinuxCodeCCScriptElement
    ): CoverityResult {
        with(element) {
            if (tools == null || tools!!.isEmpty() || languages.isEmpty()) return CoverityResult()
            val devopsToolParams = mutableListOf<DevOpsToolParams>()
            devopsToolParams.addAll(
                listOf(
                    DevOpsToolParams("compilePlat", compilePlat ?: "LINUX"),
                    DevOpsToolParams("scan_type", scanType ?: "1"),
                    DevOpsToolParams("phpcs_standard", phpcsStandard ?: ""),
                    DevOpsToolParams("go_path", goPath ?: ""),
                    DevOpsToolParams("py_version", pyVersion ?: ""),
                    DevOpsToolParams("ccn_threshold", ccnThreshold ?: ""),
                    DevOpsToolParams("needCodeContent", needCodeContent ?: ""),
                    DevOpsToolParams("eslint_rc", eslintRc ?: "")
                )
            )
            if (!element.projectBuildType.isNullOrBlank()) {
                devopsToolParams.add(DevOpsToolParams("PROJECT_BUILD_TYPE", projectBuildType!!))
                devopsToolParams.add(DevOpsToolParams("PROJECT_BUILD_COMMAND", projectBuildCommand ?: ""))
            }
            val body = mapOf(
                "pipelineId" to pipelineId,
                "pipelineName" to pipelineName,
                "devopsCodeLang" to objectMapper.writeValueAsString(languages),
                "devopsTools" to objectMapper.writeValueAsString(tools),
                "devopsToolParams" to devopsToolParams,
                "toolCheckerSets" to genToolChecker(element),
                "nameCn" to pipelineName,
                "projectBuildType" to scriptType.name,
                "projectBuildCommand" to script
            )
            logger.info("start to create task: $body")

            val header = mapOf(
                USER_NAME_HEADER to rtx,
                DEVOPS_PROJECT_ID to projectId,
                CONTENT_TYPE to CONTENT_TYPE_JSON
            )
            return getCodeccResult(taskExecution(body, createPath, header, "POST"))
        }
    }

    open fun updateTask(
        pipelineName: String,
        userId: String,
        element: LinuxPaasCodeCCScriptElement
    ) {
        with(element) {
            val devopsToolParams = mutableListOf(
                DevOpsToolParams("compilePlat", compilePlat ?: "LINUX"),
                DevOpsToolParams("scan_type", scanType ?: "1"),
                DevOpsToolParams("phpcs_standard", phpcsStandard ?: ""),
                DevOpsToolParams("go_path", goPath ?: ""),
                DevOpsToolParams("py_version", pyVersion ?: ""),
                DevOpsToolParams("ccn_threshold", ccnThreshold ?: ""),
                DevOpsToolParams("needCodeContent", needCodeContent ?: ""),
                DevOpsToolParams("eslint_rc", eslintRc ?: ""),
                DevOpsToolParams("SHELL", script)
            )
            if (!element.projectBuildType.isNullOrBlank()) {
                devopsToolParams.add(DevOpsToolParams("PROJECT_BUILD_TYPE", projectBuildType!!))
                devopsToolParams.add(DevOpsToolParams("PROJECT_BUILD_COMMAND", projectBuildCommand ?: ""))
            }
            if (codeCCTaskId.isNullOrBlank()) return
            val body = mapOf(
                "pipelineName" to pipelineName,
                "devopsCodeLang" to objectMapper.writeValueAsString(languages),
                "devopsTools" to objectMapper.writeValueAsString(tools ?: listOf<String>()),
                "taskId" to codeCCTaskId!!,
                "devopsToolParams" to devopsToolParams,
                "toolCheckerSets" to genToolChecker(element),
                "nameCn" to pipelineName,
                "projectBuildType" to scriptType.name,
                "projectBuildCommand" to script
            )
            logger.info("Update the coverity task($body)")
            val header = mapOf(
                USER_NAME_HEADER to userId,
                CONTENT_TYPE to CONTENT_TYPE_JSON
            )
            getCodeccResult(taskExecution(body, updatePath, header, "PUT"))
        }
    }

    open fun isTaskExist(taskId: String, userId: String): Boolean {
        logger.info("Check the coverity task if exist")
        val header = mapOf(CONTENT_TYPE to CONTENT_TYPE_JSON)
        val result = getCodeccResult(taskExecution(mapOf(), "$existPath/$taskId", header, "GET"))
        logger.info("Get the exist result($result)")
        return result.data == true
    }

    open fun deleteTask(taskId: String, rtx: String) {
        val body = emptyMap<String, String>()

        val headers = mapOf(
            "proj_id" to taskId,
            USER_NAME_HEADER to rtx
        )
        taskExecution(body, "$deletePath/$taskId", headers, "DELETE")
    }

    fun getRuleSets(projectId: String, userId: String, toolName: String): Result<Map<String, Any>> {
        val headers = mapOf(
            AUTH_HEADER_DEVOPS_USER_ID to userId,
            AUTH_HEADER_DEVOPS_PROJECT_ID to projectId
        )
        val result = taskExecution(
            body = mapOf(),
            path = getRuleSetsPath.replace("{toolName}", toolName),
            headers = headers,
            method = "GET"
        )
        return objectMapper.readValue(result)
    }

    fun getTaskInfo(taskEnName: String): Result<TaskDetailVO?> {
        val result = taskExecution(
            body = mapOf(),
            path = "/ms/task/api/service/task/taskInfo?nameEn=$taskEnName",
            method = "GET"
        )
        return objectMapper.readValue(result)
    }

    fun getTransferAuthor(taskId: String): Result<AuthorTransferVO?> {
        val result = taskExecution(
            body = mapOf(),
            headers = mapOf("X-DEVOPS-TASK-ID" to taskId),
            path = "/ms/defect/api/service/transferAuthor/list",
            method = "GET"
        )
        return objectMapper.readValue(result)
    }

    fun getFilterPath(taskId: String): Result<FilterPathOutVO?> {
        val result = taskExecution(
            body = mapOf(),
            path = "/ms/task/api/service/task/filter/path/$taskId",
            method = "GET"
        )
        return objectMapper.readValue(result)
    }

    private fun taskExecution(
        body: Map<String, Any>,
        path: String,
        headers: Map<String, String>? = null,
        method: String = "GET"
    ): String {
        val jsonBody = objectMapper.writeValueAsString(body)
        val requestBody = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"), jsonBody
        )

        val builder = Request.Builder()
            .url(getExecUrl(path))

        when (method) {
            "GET" -> {
            }
            "POST" -> {
                builder.post(requestBody)
            }
            "DELETE" -> {
                builder.delete(requestBody)
            }
            "PUT" -> {
                builder.put(requestBody)
            }
        }

        if (headers != null && headers.isNotEmpty()) {
            headers.forEach { (t, u) ->
                builder.addHeader(t, u)
            }
        }

        val request = builder.build()

        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to execute($path) task($body) because of ${response.message()} with response: $responseBody")
                throw RemoteServiceException("Fail to invoke codecc request")
            }
            logger.info("Get the task response body - $responseBody")
            return responseBody
        }
    }

    fun getExecUrl(path: String): String {
        val execUrl = if (codeccApiProxyUrl.isBlank()) {
            codeccApiUrl + path
        } else {
            "$codeccApiProxyUrl?url=${URLEncoder.encode(codeccApiUrl + path, "UTF-8")}"
        }
        logger.info("taskExecution url: $execUrl")
        return execUrl
    }

    private fun getCodeccResult(responseBody: String): CoverityResult {
        val result = objectMapper.readValue<CoverityResult>(responseBody)
        if (result.code != "0" || result.status != 0) throw TaskExecuteException(
            errorCode = ErrorCode.SYSTEM_SERVICE_ERROR,
            errorType = ErrorType.SYSTEM,
            errorMsg = "execute codecc task fail"
        )
        return result
    }

    open fun getReport(
        projectId: String,
        pipelineId: String,
        taskId: String,
        userId: String
    ): CodeccReport {
        try {
            val body = mapOf(
                "bs_project_id" to projectId,
                "pipeline_id" to pipelineId,
                "task_id" to taskId
            )
            val requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), objectMapper.writeValueAsString(body)
            )
            val builder = Request.Builder()
                .header(USER_NAME_HEADER, userId)
                .url(codeccApiUrl + report)
                .post(requestBody)

            val request = builder.build()

            OkhttpUtils.doHttp(request).use { response ->
                //            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.warn("Fail to execute($report) task($body) because of ${response.message()}")
                    throw RemoteServiceException("Fail to invoke codecc report")
                }
                val responseBody = response.body()!!.string()
                return CodeccReport(responseBody)
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to get the codecc report of ($projectId|$pipelineId)", ignored)
            throw TaskExecuteException(
                errorCode = ErrorCode.SYSTEM_SERVICE_ERROR,
                errorType = ErrorType.SYSTEM,
                errorMsg = "获取CodeCC报告失败"
            )
        }
    }

    fun getLanguageRuleSets(projectId: String, userId: String): Result<Map<String, Any>> {
        val headers = mapOf(
                AUTH_HEADER_DEVOPS_PROJECT_ID to projectId
        )
        val result = taskExecution(
                body = mapOf(),
                path = "/ms/defect/api/service/checkerSet/categoryList",
                headers = headers,
                method = "GET"
        )
        return objectMapper.readValue(result)
    }

    fun installCheckerSet(projectId: String, userId: String, type: String, checkerSetId: String): Result<Boolean> {
        val headers = mapOf(
            AUTH_HEADER_DEVOPS_PROJECT_ID to projectId,
            AUTH_HEADER_DEVOPS_USER_ID to userId
        )
        val body = mapOf(
            "type" to type,
            "projectId" to projectId
        )
        val result = taskExecution(
            body = body,
            path = "/ms/defect/api/service/checkerSet/$checkerSetId/relationships",
            headers = headers,
            method = "POST"
        )
        return objectMapper.readValue(result)
    }

    private fun genToolChecker(element: LinuxCodeCCScriptElement): List<ToolChecker> {
        return genToolRuleSet(element).map {
            ToolChecker(it.key, it.value)
        }
    }

    private fun genToolRuleSet(element: LinuxCodeCCScriptElement): Map<String, String> {
        val map = mutableMapOf<String, String>()
        with(element) {
            if (!coverityToolSetId.isNullOrBlank()) map["COVERITY"] = coverityToolSetId!!
            if (!klocworkToolSetId.isNullOrBlank()) map["KLOCWORK"] = klocworkToolSetId!!
            if (!cpplintToolSetId.isNullOrBlank()) map["CPPLINT"] = cpplintToolSetId!!
            if (!eslintToolSetId.isNullOrBlank()) map["ESLINT"] = eslintToolSetId!!
            if (!pylintToolSetId.isNullOrBlank()) map["PYLINT"] = pylintToolSetId!!
            if (!gometalinterToolSetId.isNullOrBlank()) map["GOML"] = gometalinterToolSetId!!
            if (!checkStyleToolSetId.isNullOrBlank()) map["CHECKSTYLE"] = checkStyleToolSetId!!
            if (!styleCopToolSetId.isNullOrBlank()) map["STYLECOP"] = styleCopToolSetId!!
            if (!detektToolSetId.isNullOrBlank()) map["DETEKT"] = detektToolSetId!!
            if (!phpcsToolSetId.isNullOrBlank()) map["PHPCS"] = phpcsToolSetId!!
            if (!sensitiveToolSetId.isNullOrBlank()) map["SENSITIVE"] = sensitiveToolSetId!!
            if (!occheckToolSetId.isNullOrBlank()) map["OCCHECK"] = occheckToolSetId!!
            if (!gociLintToolSetId.isNullOrBlank()) map["GOCILINT"] = gociLintToolSetId!!
            if (!woodpeckerToolSetId.isNullOrBlank()) map["WOODPECKER_SENSITIVE"] = woodpeckerToolSetId!!
            if (!horuspyToolSetId.isNullOrBlank()) map["HORUSPY"] = horuspyToolSetId!!
            if (!pinpointToolSetId.isNullOrBlank()) map["PINPOINT"] = pinpointToolSetId!!
        }
        return map
    }

    private data class DevOpsToolParams(
        val varName: String,
        val chooseValue: String
    )

    private data class ToolChecker(
        val toolName: String,
        val checkerSetId: String
    )

    data class TaskDetailVO(
        val scanType: Int,
        val notifyCustomInfo: NotifyCustomVO?,
        val newDefectJudge: NewDefectJudgeVO?
    )

    data class NotifyCustomVO(
        val rtxReceiverType: String?,
        val rtxReceiverList: Set<String>?,
        val botWebhookUrl: String?,
        val botRemindSeverity: Int?,
        val botRemaindTools: Set<String>?,
        val botRemindRange: Int?,
        val emailReceiverType: String?,
        val emailReceiverList: Set<String>?,
        val emailCCReceiverList: Set<String>?,
        val instantReportStatus: String?,
        val reportDate: Set<Int>?,
        val reportTime: Int?,
        val reportMinute: Int?,
        val reportTools: Set<String>?
    )

    data class NewDefectJudgeVO(
        val fromDate: String? = "",
        val fromDateTime: Long?
    )

    data class AuthorTransferVO(
        val taskId: Long?,
        val transferAuthorList: List<TransferAuthorPair>?
    )

    data class TransferAuthorPair(
        val sourceAuthor: String,
        val targetAuthor: String
    )

    data class FilterPathOutVO(
        val taskId: Long?,
        val filterPaths: List<String>?
    )
}
