package com.tencent.devops.plugin.codecc

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CodeccReportException
import com.tencent.devops.common.api.exception.RemoteServiceException
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

open class CodeccApi constructor(
        private val codeccApiUrl: String,
        private val createPath: String = "/blueShield/createTask",
        private val updatePath: String = "/blueShield/updateTask",
        private val existPath: String = "/blueShield/checkTaskExists",
        private val deletePath: String = "/blueShield/deleteTask",
        private val report: String = "/blueShield/codeCheckReport",
        private val getRuleSetsPath: String = "/blueShield/getRuleSets"
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
            devopsToolParams.addAll(listOf(DevOpsToolParams("compilePlat", compilePlat ?: "LINUX"),
                    DevOpsToolParams("scan_type", scanType ?: "1"),
                    DevOpsToolParams("phpcs_standard", phpcsStandard ?: ""),
                    DevOpsToolParams("go_path", goPath ?:""),
                    DevOpsToolParams("py_version", pyVersion ?: ""),
                    DevOpsToolParams("eslint_rc", eslintRc ?: "")))
            if (!element.projectBuildType.isNullOrBlank()) {
                devopsToolParams.add(DevOpsToolParams("PROJECT_BUILD_TYPE", projectBuildType!!))
                devopsToolParams.add(DevOpsToolParams("PROJECT_BUILD_COMMAND", projectBuildCommand ?: ""))
            }
            val body = mapOf(
                    "pipelineId" to pipelineId,
                    "pipelineName" to pipelineName,
                    "devopsCodeLang" to objectMapper.writeValueAsString(languages),
                    "devopsTools" to objectMapper.writeValueAsString(tools),
                    "devopsToolParams" to devopsToolParams
            )
            val header = mapOf(USER_NAME_HEADER to rtx,
                    DEVOPS_PROJECT_ID to projectId,
                    CONTENT_TYPE to CONTENT_TYPE_JSON)
            return taskExecution(body, createPath, header, "POST")
        }
    }

    open fun updateTask(
            pipelineName: String,
            userId: String,
            element: LinuxPaasCodeCCScriptElement
    ) {
        with(element) {
            val devopsToolParams = mutableListOf(DevOpsToolParams("compilePlat", compilePlat ?: "LINUX"),
                DevOpsToolParams("scan_type", scanType ?: "1"),
                DevOpsToolParams("phpcs_standard", phpcsStandard ?: ""),
                DevOpsToolParams("go_path", goPath ?:""),
                DevOpsToolParams("py_version", pyVersion ?: ""),
                DevOpsToolParams("eslint_rc", eslintRc ?: ""))
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
                    "devopsToolParams" to devopsToolParams
            )
            logger.info("Update the coverity task($body)")
            val header = mapOf(USER_NAME_HEADER to userId,
                    CONTENT_TYPE to CONTENT_TYPE_JSON)
            taskExecution(body, updatePath, header, "PUT")
        }
    }

    open fun isTaskExist(taskId: String, userId: String): Boolean {
        logger.info("Check the coverity task if exist")
        val header = mapOf(CONTENT_TYPE to CONTENT_TYPE_JSON)
        val result = taskExecution(mapOf(), "$existPath/$taskId", header, "GET")
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
            USER_NAME_HEADER to userId
        )
        val result = taskExecution("$getRuleSetsPath?bsProjectId=$projectId&toolName=$toolName", headers)
        return objectMapper.readValue(result)
    }


    private fun taskExecution(path: String, headers: Map<String, String>? = null): String {
        logger.info("taskExecution url: ${codeccApiUrl + path}")
        val builder = Request.Builder()
            .url(codeccApiUrl + path)
            .get()

        if (headers != null && headers.isNotEmpty()) {
            headers.forEach { t, u ->
                builder.addHeader(t, u)
            }
        }

        val request = builder.build()
        OkhttpUtils.doHttp(request).use { response ->
            //        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.warn("Fail to execute($path) because of ${response.message()}")
                throw RuntimeException("Fail to invoke codecc request")
            }
            val responseBody = response.body()!!.string()
            logger.info("Get the task response body - $responseBody")
            return responseBody
        }
    }

    private fun taskExecution(
            body: Map<String, Any>,
            path: String,
            headers: Map<String, String>? = null,
            method: String = "GET"
    ): CoverityResult {
        val jsonBody = objectMapper.writeValueAsString(body)
        val requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), jsonBody
        )

        val builder = Request.Builder()
                .url(codeccApiUrl + path)

        when (method) {
            "GET" -> {}
            "POST" -> { builder.post(requestBody) }
            "DELETE" -> { builder.delete(requestBody) }
            "PUT" -> { builder.put(requestBody) }
        }

        if (headers != null && headers.isNotEmpty()) {
            headers.forEach { (t, u) ->
                builder.addHeader(t, u)
            }
        }

        val request = builder.build()

        OkhttpUtils.doHttp(request).use { response ->
            if (!response.isSuccessful) {
                logger.warn("Fail to execute($path) task($body) because of ${response.message()}")
                throw RemoteServiceException("Fail to invoke codecc request")
            }
            val responseBody = response.body()!!.string()
            logger.info("Get the task response body - $responseBody")

            val result = objectMapper.readValue<CoverityResult>(responseBody)
            if (result.code != "0" || result.status != 0) throw RuntimeException("execute codecc task fail in path($path) - method($method):\n$headers\n$body")
            return result
        }
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
            throw CodeccReportException("获取CodeCC报告失败")
        }
    }

    private data class DevOpsToolParams(
            val varName: String,
            val chooseValue: String
    )
}