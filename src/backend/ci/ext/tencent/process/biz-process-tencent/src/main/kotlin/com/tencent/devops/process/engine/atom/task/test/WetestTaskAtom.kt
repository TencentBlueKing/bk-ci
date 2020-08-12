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

package com.tencent.devops.process.engine.atom.task.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Files
import com.tencent.devops.common.api.exception.TaskTimeoutExistException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.JfrogService
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.pipeline.element.WetestElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.pojo.wetest.WetestAutoTestRequest
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroup
import com.tencent.devops.plugin.pojo.wetest.WetestInstStatus
import com.tencent.devops.plugin.pojo.wetest.WetestTask
import com.tencent.devops.plugin.pojo.wetest.WetestTaskInst
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.service.PipelineUserService
import com.tencent.devops.process.util.CommonUtils
import com.tencent.devops.process.util.ExcelUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class WetestTaskAtom @Autowired constructor(
    private val jfrogService: JfrogService,
    private val pipelineUserService: PipelineUserService,
    private val buildLogPrinter: BuildLogPrinter,
    private val objectMapper: ObjectMapper
) : IAtomTask<WetestElement> {

    private lateinit var projectId: String
    private lateinit var pipelineId: String
    private lateinit var buildId: String
    private lateinit var elementId: String
    private lateinit var containerId: String
    private var executeCount: Int = 1
    private lateinit var accessId: String
    private lateinit var accessToken: String

    @Value("\${devopsGateway.idc:#{null}}")
    private lateinit var gatewayUrl: String

    override fun execute(task: PipelineBuildTask, param: WetestElement, runVariables: Map<String, String>): AtomResponse {
        return doExecute(task, param, runVariables)
    }

    private fun doExecute(task: PipelineBuildTask, param: WetestElement, runVariables: Map<String, String>): AtomResponse {
        projectId = task.projectId
        pipelineId = task.pipelineId
        buildId = task.buildId
        elementId = task.taskId
        containerId = task.containerHashId ?: ""
        executeCount = task.executeCount ?: 1

        val usersMap = pipelineUserService.listCreateUsers(setOf(pipelineId))
        val pipelineCreateUser = usersMap[pipelineId]
        if (pipelineCreateUser.isNullOrEmpty()) {
            buildLogPrinter.addRedLine(buildId, "获取流水线创建人失败", elementId, task.containerHashId, task.executeCount ?: 1)
            throw RuntimeException("获取流水线创建人失败， pipelineId = （$pipelineId）")
        }
        val (accessId, accessToken) = CommonUtils.getCredential(pipelineCreateUser!!)
        this.accessId = accessId
        this.accessToken = accessToken

        val apiHost = HomeHostUtil.innerApiHost()

        logger.info("param: $param")
        // 获取任务
        with(param) {
            val sourcePathStr = parseVariable(sourcePath, runVariables)
            val scriptPathStr = parseVariable(scriptPath, runVariables)
            val testAccountFileStr = parseVariable(testAccountFile, runVariables)
            val preTestApkFilesStr = parseVariable(preTestApkFiles, runVariables)

            buildLogPrinter.addLine(buildId, "详细结果可稍后前往查看：<a target='_blank' href=\"https://wetest.qq.com/console/report/cloud/\">查看详情</a>", elementId, task.containerHashId, task.executeCount ?: 1)
            buildLogPrinter.addLine(buildId, "正准备进行 $testType 测试", elementId, task.containerHashId, task.executeCount ?: 1)

            val request = Request.Builder()
                    .url("$apiHost/wetest/api/service/wetest/task/getTask?taskId=$taskId&projectId=$projectId")
                    .get()
                    .build()
            val wetestTask = OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                buildLogPrinter.addLine(buildId, "get task response: $data", elementId, task.containerHashId, task.executeCount ?: 1)
                objectMapper.readValue<Result<WetestTask>>(data).data ?: throw RuntimeException("在 $projectId 项目下面找不到 $taskId 的任务数据")
            }
            val isPrivateCloud = !wetestTask.mobileModelId.isBlank()
            val type = when {
                sourcePathStr.endsWith(".apk") -> "apk"
                sourcePathStr.endsWith(".ipa") -> "ipa"
                else -> throw RuntimeException("unsupported file type: $sourcePathStr")
            }

            // step 1
            val uploadResult = uploadApp(sourcePathStr, sourceType, type, task)
            val fileTaskId = uploadResult["apkid"] as? Int ?: uploadResult["ipaid"] as? Int
            ?: throw RuntimeException("上传文件到wetest失败!")

            // step 2
            val scriptTaskId = uploadScript(scriptPathStr, scriptSourceType, task)
            var weTestProjectIdStr = parseVariable(weTestProjectId, runVariables)
            val groupRequest = Request.Builder()
                    .url("$apiHost/wetest/api/service/wetest/emailgroup/$projectId/get?ID=$notifyType")
                    .get()
                    .build()
            val weTestGroupId = OkhttpUtils.doHttp(groupRequest).use { response ->
                val data = response.body()!!.string()
                buildLogPrinter.addLine(buildId, "get group response: $data", elementId, task.containerHashId, task.executeCount ?: 1)
                objectMapper.readValue<Result<WetestEmailGroup?>>(data).data!!.wetestGroupId
            }
            if (null != weTestGroupId) {
                weTestProjectIdStr = weTestGroupId
            }

            // step 3
            val preTestApkIds = if (!preTestApkFilesStr.isBlank() && !preTestArchiveType.isNullOrBlank()) {
                preTestApkFilesStr.split(",").map { it.trim() }.map {
                    val preParam = ArtifactorySearchParam(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        regexPath = preTestApkFiles!!,
                        custom = preTestArchiveType == "CUSTOMIZE",
                        executeCount = task.executeCount ?: 0,
                        elementId = task.taskId
                    )
                    val preTestUploadResult = uploadRes(preParam, "apk")
                    preTestUploadResult["apkid"] as? Int ?: preTestUploadResult["ipaid"] as? Int
                    ?: throw RuntimeException("上传文件到wetest失败!")
                }
            } else {
                listOf()
            }

            // step 4
            val accountMap = getAccountMap(testAccountFileStr, accountFileSourceType, task)
            val testId = autoTest(WetestAutoTestRequest(
                apkid = if (type == "apk") fileTaskId else null,
                ipaid = if (type == "ipa") fileTaskId else null,
                scriptid = scriptTaskId,
                toptype = if (isPrivateCloud) 0 else 1,
                topnum = if (isPrivateCloud) 0 else wetestTask.mobileCategory.removePrefix("TOP").trim().toInt(),
                testtype = testType,
                runtime = null,
                login = if (accountMap != null && accountMap.isNotEmpty()) "custom" else null,
                custom_account = accountMap,
                frametype = scriptType,
                models = if (isPrivateCloud) wetestTask.mobileModelId.split(",") else null,
                cloudid = null,
                projectid = if (NumberUtils.isDigits(weTestProjectIdStr)) weTestProjectIdStr.toInt() else null,
                comments = "",
                test_from = "bk-devops",
                extrainfo = getExtraInfo(preTestApkIds)
            ))
            val requestContent = objectMapper.writeValueAsString(WetestTaskInst(
                testId = testId,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                buildNo = runVariables[PIPELINE_BUILD_NUM]!!.toInt(),
                name = wetestTask.name,
                version = uploadResult["meta.versionName"] as? String ?: "",
                passingRate = "",
                taskId = wetestTask.id.toString(),
                testType = testType,
                scriptType = scriptType ?: "",
                synchronized = if (synchronized == "SYNC") "1" else "0",
                sourcePath = sourcePathStr,
                scriptPath = scriptPathStr ?: "",
                accountFile = testAccountFileStr ?: "",
                sourceType = sourceType,
                scriptSourceType = scriptSourceType ?: "",
                accountSourceType = accountFileSourceType ?: "",
                privateCloud = if (isPrivateCloud) "1" else "0",
                startUserId = pipelineCreateUser,
                beginTime = System.currentTimeMillis(),
                endTime = null,
                emailGroupId = param.notifyType.toLong()
            ))
            val saveTaskRequest = Request.Builder()
                    .url("$apiHost/wetest/api/service/wetest/task/saveTask")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestContent))
                    .build()
            OkhttpUtils.doHttp(saveTaskRequest).use { response ->
                val data = response.body()!!.string()
                buildLogPrinter.addLine(buildId, "save task response: $data", elementId, task.containerHashId, task.executeCount ?: 1)
            }
            buildLogPrinter.addLine(buildId, "成功提交wetest测试(testId: $testId)", elementId, task.containerHashId, task.executeCount ?: 1)

            // step 5
            // 私有云最长8小时;公有云快速兼容最长900秒,其余测试2小时
            val maxTimes = if (isPrivateCloud) 567 else 222
            if (synchronized == "SYNC") {
                var count = 1
                try {
                    while (true) {
                        val statusResult = queryTestStatus(accessId, accessToken, testId)
                        checkResult(statusResult, "wetest原子查询结果失败")
                        val testStatus = statusResult["teststatus"] as Map<*, *>
                        if (testStatus["isdone"] as Boolean) {
                            buildLogPrinter.addLine(buildId, "wetest测试成功，具体结果可以点击查看：<a target='_blank' href='https://wetest.qq.com/console/report/cloud'>查看详情</a>", elementId, task.containerHashId, task.executeCount ?: 1)
                            updateTaskInstStatus(testId, WetestInstStatus.SUCCESS)
                            break
                        }
                        buildLogPrinter.addLine(buildId, "测试进行中，请稍等...", elementId, task.containerHashId, task.executeCount ?: 1)
                        Thread.sleep(Math.min(5000L * count, 60 * 1000L)) // 最多等待1分钟
                        count++
                        if (count > maxTimes) throw TaskTimeoutExistException("wetest原子执行失败")
                    }
                } catch (e: TaskTimeoutExistException) {
                    buildLogPrinter.addRedLine(buildId, "wetest原子执行超时了", elementId, task.containerHashId, task.executeCount ?: 1)
                    updateTaskInstStatus(testId, WetestInstStatus.TIMEOUT)
                    throw e
                } catch (e: Throwable) {
                    buildLogPrinter.addRedLine(buildId, "wetest原子执行失败了", elementId, task.containerHashId, task.executeCount ?: 1)
                    updateTaskInstStatus(testId, WetestInstStatus.FAIL)
                    throw e
                }
            }

            // 存入流水线创建人id与testId,用来存入到measure
            task.taskParams["userId"] = pipelineCreateUser
            task.taskParams["testId"] = testId
        }
        return AtomResponse(BuildStatus.SUCCEED)
    }

    private fun getExtraInfo(preTestApkIds: List<Int>): String? {
        if (preTestApkIds.isEmpty()) return null
        return objectMapper.writeValueAsString(mapOf("pretestapks" to preTestApkIds))
    }

    private fun queryTestStatus(accessId: String, accessToken: String, testId: String): Map<String, Any> {
        val request = Request.Builder()
                .url("$gatewayUrl/wetest/api/service/wetest/task/queryTestStatus" +
                        "?accessId=${URLEncoder.encode(accessId, "utf-8")}&accessToken=${URLEncoder.encode(accessToken, "utf-8")}" +
                        "&testId=${URLEncoder.encode(testId, "utf-8")}")
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            buildLogPrinter.addLine(buildId, "query test status response: $data", elementId, containerId, executeCount)
            return objectMapper.readValue<Result<Map<String, Any>>>(data).data ?: mapOf()
        }
    }

    private fun updateTaskInstStatus(testId: String, status: WetestInstStatus) {
        val request = Request.Builder()
                .url("$gatewayUrl/wetest/api/service/wetest/task/updateTaskInstStatus?testId=${URLEncoder.encode(testId, "utf-8")}&status=${status.name}")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), ""))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            buildLogPrinter.addLine(buildId, "update task inst status $data", elementId, containerId, executeCount)
        }
    }

    override fun getParamElement(task: PipelineBuildTask): WetestElement {
        return JsonUtil.mapTo(task.taskParams, WetestElement::class.java)
    }

    private fun autoTest(wetestAutoTestRequest: WetestAutoTestRequest): String {
        val requestContent = objectMapper.writeValueAsString(wetestAutoTestRequest)
        val request = Request.Builder()
                .url("$gatewayUrl/wetest/api/service/wetest/task/autoTest" +
                        "?accessId=${URLEncoder.encode(accessId, "utf-8")}&accessToken=${URLEncoder.encode(accessToken, "utf-8")}")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestContent))
                .build()
        val result = OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            buildLogPrinter.addLine(buildId, "auto test response: $data", elementId, containerId, executeCount)
            objectMapper.readValue<Result<Map<String, Any>>>(data).data!!
        }
        checkResult(result, "启动任务失败:")
        return result["testid"] as String
    }

    private fun getAccountMap(testAccountFile: String?, sourceType: String?, task: PipelineBuildTask): List<List<String>>? {
        val accountExcelFile = Files.createTempDir().canonicalPath
        return if (!testAccountFile.isNullOrBlank()) {
            buildLogPrinter.addLine(buildId, "正在获取用户的数据: $testAccountFile($sourceType)", elementId, task.containerHashId, task.executeCount ?: 1)
            val excelFile = jfrogService.downloadFile(ArtifactorySearchParam(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                regexPath = testAccountFile!!,
                custom = sourceType == "CUSTOMIZE",
                executeCount = task.executeCount ?: 0,
                elementId = task.taskId
            ), accountExcelFile).firstOrNull()
                    ?: throw RuntimeException("account file can not be found: $accountExcelFile($sourceType)")
            excelFile.deleteOnExit()
            val list = ExcelUtils.getAccountFromExcel(excelFile.canonicalPath).map { listOf(it.key, it.value) }
            buildLogPrinter.addLine(buildId, "共获取用户的数据 ${list.size} 条", elementId, task.containerHashId, task.executeCount ?: 1)
            return list
        } else {
            null
        }
    }

    private fun uploadScript(scriptPath: String?, sourceType: String?, task: PipelineBuildTask): Int {
        return if (!scriptPath.isNullOrBlank()) {
            buildLogPrinter.addLine(buildId, "上传相应的脚本到wetest: $scriptPath($sourceType)", elementId, task.containerHashId, task.executeCount ?: 1)
            val param = ArtifactorySearchParam(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                regexPath = scriptPath!!,
                custom = sourceType == "CUSTOMIZE",
                executeCount = task.executeCount ?: 0,
                elementId = task.taskId
            )
            val scriptUploadResult = uploadRes(param, "script")
            checkResult(scriptUploadResult, "上传脚本到wetest失败!")
            scriptUploadResult["scriptid"] as? Int ?: throw RuntimeException("上传脚本到wetest失败!")
        } else {
            buildLogPrinter.addLine(buildId, "跳过上传相应的脚本到wetest步骤", elementId, task.containerHashId, task.executeCount ?: 1)
            0
        }
    }

    private fun uploadApp(sourcePath: String, sourceType: String, type: String, task: PipelineBuildTask): Map<String, Any> {
        buildLogPrinter.addLine(buildId, "上传相应的包到wetest: $sourcePath($sourceType)", elementId, task.containerHashId, task.executeCount ?: 1)
        val param = ArtifactorySearchParam(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            regexPath = sourcePath,
            custom = sourceType == "CUSTOMIZE",
            executeCount = task.executeCount ?: 1,
            elementId = task.taskId
        )
        val uploadResult = uploadRes(param, type)
        checkResult(uploadResult, "上传app包失败")
        return uploadResult
    }

    private fun uploadRes(param: ArtifactorySearchParam, type: String): Map<String, Any> {
        val requestContent = objectMapper.writeValueAsString(param)
        val request = Request.Builder()
                .url("$gatewayUrl/wetest/api/service/wetest/task/uploadRes?" +
                        "accessId=${URLEncoder.encode(accessId, "utf-8")}&accessToken=${URLEncoder.encode(accessToken, "utf-8")}&type=$type ")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestContent))
                .build()
        return OkhttpUtils.doLongHttp(request).use { response ->
            val data = response.body()!!.string()
            buildLogPrinter.addLine(buildId, "upload res response: $data", elementId, containerId, executeCount)
            objectMapper.readValue<Result<Map<String, Any>>>(data).data!!
        }
    }

    private fun checkResult(result: Map<String, Any>, errMsg: String) {
        if (!result.containsKey("ret") || result["ret"] as Int != 0) {
            val msg = result["msg"] as String?
            buildLogPrinter.addRedLine(buildId, "$errMsg : $msg", elementId, containerId, executeCount)
            throw RuntimeException(msg)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
