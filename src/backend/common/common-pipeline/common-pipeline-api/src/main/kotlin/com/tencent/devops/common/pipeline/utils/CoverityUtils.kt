package com.tencent.devops.common.pipeline.utils

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CodeccReportException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.pojo.coverity.CodeccReport
import com.tencent.devops.common.pipeline.pojo.coverity.CoverityProjectType
import com.tencent.devops.common.pipeline.pojo.coverity.CoverityResult
import com.tencent.devops.common.pipeline.pojo.coverity.ProjectLanguage
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

object CoverityUtils {

    private const val CODE_CC_DEV_URL = "http://10.123.16.48"
    private const val CODE_CC_TEST_URL = "http://10.123.24.177"
    private const val CODE_CC_PROD_URL = "http://api.codecc.oa.com"

    private val url: String
    private const val createPath = "/blueShield/createTask"
    private const val updatePath = "/blueShield/updateTask"
    private const val existPath = "/blueShield/checkTaskExists"
    private const val deletePath = "/blueShield/deleteTask"
    private const val report = "/blueShield/codeCheckReport"
    private const val getRuleSetsPath = "/blueShield/getRuleSets"
    private val objectMapper = JsonUtil.getObjectMapper()
    private val logger = LoggerFactory.getLogger(CoverityUtils::class.java)

    private const val USER_NAME_HEADER = "username"

//    private val okHttpClient = okhttp3.OkHttpClient.Builder()
//            .connectTimeout(5L, TimeUnit.SECONDS)
//            .readTimeout(30L, TimeUnit.SECONDS)
//            .writeTimeout(30L, TimeUnit.SECONDS)
//            .build()

    init {
        val profile = System.getProperty("spring.profiles.active")
        url = if (profile.isNullOrEmpty()) {
            CODE_CC_TEST_URL
        } else {
            val p = profile.split(",")
            if (p.isEmpty()) {
                CODE_CC_TEST_URL
            } else {
                when {
                    p.contains("prod") -> CODE_CC_PROD_URL
                    p.contains("test") -> CODE_CC_TEST_URL
                    else -> CODE_CC_DEV_URL
                }
            }
        }
        logger.info("Get the codecc url($url) and profile($profile)")
    }

    val map = mapOf(
            ProjectLanguage.C.name to CoverityProjectType.COMPILE,
            ProjectLanguage.C_PLUS_PLUSH.name to CoverityProjectType.COMPILE,
            ProjectLanguage.C_CPP.name to CoverityProjectType.COMPILE,
            ProjectLanguage.OBJECTIVE_C.name to CoverityProjectType.COMPILE,
            ProjectLanguage.OC.name to CoverityProjectType.COMPILE,
            ProjectLanguage.C_SHARP.name to CoverityProjectType.COMPILE,
            ProjectLanguage.JAVA.name to CoverityProjectType.COMPILE,
            ProjectLanguage.PYTHON.name to CoverityProjectType.UN_COMPILE,
            ProjectLanguage.JAVASCRIPT.name to CoverityProjectType.UN_COMPILE,
            ProjectLanguage.JS.name to CoverityProjectType.UN_COMPILE,
            ProjectLanguage.PHP.name to CoverityProjectType.UN_COMPILE,
            ProjectLanguage.RUBY.name to CoverityProjectType.UN_COMPILE,
            ProjectLanguage.LUA.name to CoverityProjectType.UN_COMPILE,
            ProjectLanguage.GOLANG.name to CoverityProjectType.COMBINE,
            ProjectLanguage.SWIFT.name to CoverityProjectType.COMBINE,
            ProjectLanguage.TYPESCRIPT.name to CoverityProjectType.UN_COMPILE,
            ProjectLanguage.KOTLIN.name to CoverityProjectType.COMPILE,
            ProjectLanguage.OTHERS.name to CoverityProjectType.UN_COMPILE
    )
    // Check the coverity project type by the project language
    /**
     * C/C++                	编译型
     * Objective-C/C++			编译型
     * C#						编译型
     * Java 					编译型
     * Python					非编译型
     * JavaScript				非编译型
     * PHP						非编译型
     * Ruby					    非编译型
     */
    fun projectType(languages: List<String>): CoverityProjectType {
        if (languages.isEmpty()) {
            return CoverityProjectType.UN_COMPILE
        }

        var type = map[languages[0]]

        languages.forEach {
            val currentType = map[it]
            if (type != null) {
                if (currentType != null && type != currentType) {
                    return CoverityProjectType.COMBINE
                }
            } else {
                type = currentType
            }
        }

        return type ?: CoverityProjectType.UN_COMPILE
    }

    fun languages2String(languages: List<ProjectLanguage>): String {
        if (languages.isEmpty())
            return ""

        return languages.joinToString(",") {
            it.value
        }
    }

    fun languages2List(languages: String): List<ProjectLanguage> {
        if (languages.isEmpty()) {
            return listOf()
        }

        return languages.split(",").map {
            ProjectLanguage.fromValue(it.trim())
        }.toList()
    }

    fun createTask(
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        rtx: String,
        languages: List<ProjectLanguage>,
        compilePlat: String,
        tools: List<String>,
        pyVersion: String,
        eslintRc: String,
        scanType: String,
        phpcsStandard: String,
        goPath: String,
        ccnThreshold: Int?,
        needCodeContent: String?,
        toolRuleSet: Map<String, String>
    ): CoverityResult {
        if (tools.isEmpty() || languages.isEmpty()) return CoverityResult()
        val result = create(projectId, pipelineId, pipelineName, rtx, languages,
            compilePlat, tools, pyVersion, eslintRc, scanType, phpcsStandard, goPath, ccnThreshold, needCodeContent, toolRuleSet)

        if (result.res != 0) {
            logger.warn("Fail to create the coverity task because of ${result.msg}")
            // The task is already exist
            if (result.res == 1011) {
                // Delete it
                val taskInfo = result.task_info
                if (taskInfo == null) {
                    logger.warn("The task info is empty")
                    throw RuntimeException("The task info is empty")
                }
                logger.info("Delete the task($taskInfo) as already exist")
                deleteTask(taskInfo.task_id, rtx)
                val r = create(projectId, pipelineId, pipelineName, rtx, languages,
                    compilePlat, tools, pyVersion, eslintRc, scanType, phpcsStandard, goPath, ccnThreshold, needCodeContent, toolRuleSet)
                if (r.res != 0) {
                    logger.warn("Fail to create the coverity task because of ${r.msg}")
                    throw RuntimeException("Fail to create the task")
                }
                return r
            }
            throw RuntimeException("Fail to create the task because of $result")
        }
        return result
    }

    fun updateTask(
        pipelineName: String,
        userId: String,
        taskId: String,
        languages: List<ProjectLanguage>,
        compilePlat: String,
        tools: List<String>,
        pyVersion: String,
        eslintRc: String,
        scanType: String,
        phpcsStandard: String,
        goPath: String,
        ccnThreshold: Int?,
        needCodeContent: String?,
        toolRuleSet: Map<String, String>
    ) {
        val body = mutableMapOf<String, Any>(
                "task_id" to taskId,
                "bs_pipeline_name" to pipelineName,
                "bs_task_name" to pipelineName,
                "code_lang" to objectMapper.writeValueAsString(languages),
                "tools" to objectMapper.writeValueAsString(tools),
                "compile_plat" to compilePlat,
                "scan_type" to scanType,
                "phpcs_standard" to phpcsStandard,
                "go_path" to goPath
        )
        if (pyVersion.isNotEmpty()) body["py_version"] = pyVersion
        if (eslintRc.isNotEmpty()) body["eslint_rc"] = eslintRc
        if (ccnThreshold != null) body["ccn_threshold"] = ccnThreshold
        if (!needCodeContent.isNullOrBlank()) body["needCodeContent"] = needCodeContent!!
        if (toolRuleSet.isNotEmpty()) {
            body["tool_rule_set"] = toolRuleSet.map { ToolRuleSet(it.key, it.value) }
        }
        logger.info("Update the coverity task($body)")
        try {
            val header = mapOf(USER_NAME_HEADER to userId)
            val result = taskExecution(body, updatePath, header)
            if (result.res != 0) {
                logger.warn("Fail to update the coverity task($body) because of ${result.msg}")
            }
        } catch (e: Exception) {
            logger.warn("Fail to update the coverity task", e)
        }
    }

    private fun create(
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        rtx: String,
        languages: List<ProjectLanguage>,
        compilePlat: String,
        tools: List<String>,
        pyVersion: String,
        eslintRc: String,
        scanType: String,
        phpcsStandard: String,
        goPath: String,
        ccnThreshold: Int?,
        needCodeContent: String?,
        toolRuleSet: Map<String, String>
    ): CoverityResult {
        val body = mutableMapOf<String, Any>(
                "bs_project_id" to projectId,
                "bs_pipeline_id" to pipelineId,
                "bs_pipeline_name" to pipelineName,
                "rtx" to rtx,
                "bs_task_name" to pipelineName,
                "code_lang" to objectMapper.writeValueAsString(languages),
                "compile_plat" to compilePlat,
                "tools" to objectMapper.writeValueAsString(tools),
                "scan_type" to scanType,
                "phpcs_standard" to phpcsStandard,
                "go_path" to goPath
        )
        if (pyVersion.isNotEmpty()) body["py_version"] = pyVersion
        if (eslintRc.isNotEmpty()) body["eslint_rc"] = eslintRc
        if (ccnThreshold != null) body["ccn_threshold"] = ccnThreshold
        if (!needCodeContent.isNullOrBlank()) body["needCodeContent"] = needCodeContent!!
        if (toolRuleSet.isNotEmpty()) {
            body["tool_rule_set"] = toolRuleSet.map { ToolRuleSet(it.key, it.value) }
        }
        logger.info("create the coverity task($body)")
        val header = mapOf(USER_NAME_HEADER to rtx)
        return taskExecution(body, createPath, header)
    }

    fun isTaskExist(taskId: String, userId: String): Boolean {
        val body = mapOf(
                "task_id" to taskId
        )
        logger.info("Check the coverity task($body) if exist")
        val header = mapOf(USER_NAME_HEADER to userId)
        val result = taskExecution(body, existPath, header)
        logger.info("Get the exist result($result)")
        return result.res == 0
    }

    fun deleteTask(taskId: String, rtx: String): Boolean {
        val body = mapOf(
                "task_id" to taskId
        )

        val headers = mapOf(
                "proj_id" to taskId,
                USER_NAME_HEADER to rtx
        )
        val result = taskExecution(body, deletePath, headers)

        if (result.res != 0) {
            logger.warn("Fail to delete the task($taskId) by $rtx because of $result")
            return false
        }
        return true
    }

    fun getRuleSets(projectId: String, userId: String, toolName: String): Result<Map<String, Any>> {
        val headers = mapOf(
                USER_NAME_HEADER to userId
        )
        val result = taskExecution("$getRuleSetsPath?bsProjectId=$projectId&toolName=$toolName", headers)
        return objectMapper.readValue(result)
    }

    fun getReport(projectId: String, pipelineId: String, taskId: String, userId: String): CodeccReport {
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
                    .url(url + report)
                    .post(requestBody)

            val request = builder.build()
            OkhttpUtils.doHttp(request).use { response ->
//            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.warn("Fail to execute($report) task($body) because of ${response.message()}")
                    throw RuntimeException("Fail to invoke codecc report")
                }
                val responseBody = response.body()!!.string()
                return CodeccReport(responseBody)
            }
        } catch (t: Throwable) {
            logger.warn("Fail to get the codecc report of ($projectId|$pipelineId)", t)
            throw CodeccReportException("获取CodeCC报告失败")
        }
    }

    private fun taskExecution(path: String, headers: Map<String, String>? = null): String {
        logger.info("taskExecution url: ${url + path}")
        val builder = Request.Builder()
                .url(url + path)
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

    private fun taskExecution(body: Map<String, Any>, path: String, headers: Map<String, String>? = null): CoverityResult {
        val requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), objectMapper.writeValueAsString(body)
        )
        val builder = Request.Builder()
                .url(url + path)
                .post(requestBody)

        if (headers != null && headers.isNotEmpty()) {
            headers.forEach { t, u ->
                builder.addHeader(t, u)
            }
        }

        val request = builder.build()
        OkhttpUtils.doHttp(request).use { response ->
//        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                logger.warn("Fail to execute($path) task($body) because of ${response.message()}")
                throw RuntimeException("Fail to invoke codecc request")
            }
            val responseBody = response.body()!!.string()
            logger.info("Get the task response body - $responseBody")
            return objectMapper.readValue(responseBody)
        }
    }

    private data class ToolRuleSet(
        @JsonProperty("tool_name")
        val toolName: String,
        @JsonProperty("rule_set_id")
        val ruleSetId: String
    )
}