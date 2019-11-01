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
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.common.pipeline.element.WetestElement
import com.tencent.devops.plugin.pojo.wetest.*
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
import org.springframework.amqp.rabbit.core.RabbitTemplate
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
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper
) : IAtomTask<WetestElement> {

    private lateinit var projectId: String
    private lateinit var pipelineId: String
    private lateinit var buildId: String
    private lateinit var elementId: String
    private var executeCount: Int = 1
    private lateinit var accessId: String
    private lateinit var accessToken: String

    @Value("\${gateway.service:#{null}}")
    private lateinit var gatewayUrl: String

    override fun execute(task: PipelineBuildTask, param: WetestElement, runVariables: Map<String, String>): AtomResponse {
        return doExecute(task, param, runVariables)
    }

    private fun doExecute(task: PipelineBuildTask, param: WetestElement, runVariables: Map<String, String>): AtomResponse {
        projectId = task.projectId
        pipelineId = task.pipelineId
        buildId = task.buildId
        elementId = task.taskId
        executeCount = task.executeCount ?: 1

        val usersMap = pipelineUserService.listCreateUsers(setOf(pipelineId))
        val pipelineCreateUser = usersMap[pipelineId]
        if (pipelineCreateUser.isNullOrEmpty()) {
            LogUtils.addRedLine(rabbitTemplate, buildId, "获取流水线创建人失败", elementId, task.containerHashId,task.executeCount ?: 1)
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

            LogUtils.addLine(rabbitTemplate, buildId, "详细结果可稍后前往查看：<a target='_blank' href=\"https://wetest.qq.com/console/report/cloud/\">查看详情</a>", elementId, task.containerHashId,task.executeCount ?: 1)
            LogUtils.addLine(rabbitTemplate, buildId, "正准备进行 $testType 测试", elementId, task.containerHashId,task.executeCount ?: 1)

            val request = Request.Builder()
                    .url("$apiHost/wetest/api/service/wetest/task/getTask?taskId=$taskId&projectId=$projectId")
                    .get()
                    .build()
            val wetestTask = OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()!!.string()
                LogUtils.addLine(rabbitTemplate, buildId, "get task response: $data", elementId, task.containerHashId,task.executeCount ?: 1)
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
                LogUtils.addLine(rabbitTemplate, buildId, "get group response: $data", elementId, task.containerHashId,task.executeCount ?: 1)
                objectMapper.readValue<Result<WetestEmailGroup?>>(data).data!!.wetestGroupId
            }
            if (null != weTestGroupId) {
                weTestProjectIdStr = weTestGroupId
            }

            // step 3
            val preTestApkIds = if (!preTestApkFilesStr.isBlank() && !preTestArchiveType.isNullOrBlank()) {
                preTestApkFilesStr.split(",").map { it.trim() }.map {
                    val preParam = ArtifactorySearchParam(projectId,
                            pipelineId,
                            buildId,
                            preTestApkFiles!!,
                            preTestArchiveType == "CUSTOMIZE",
                            task.executeCount ?: 0,
                            task.taskId)
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
                    if (type == "apk") fileTaskId else null,
                    if (type == "ipa") fileTaskId else null,
                    scriptTaskId,
                    if (isPrivateCloud) 0 else 1,
                    if (isPrivateCloud) 0 else wetestTask.mobileCategory.removePrefix("TOP").trim().toInt(),
                    testType,
                    null,
                    if (accountMap != null && accountMap.isNotEmpty()) "custom" else null,
                    accountMap,
                    scriptType,
                    if (isPrivateCloud) wetestTask.mobileModelId.split(",") else null,
                    null,
                    if (NumberUtils.isDigits(weTestProjectIdStr)) weTestProjectIdStr.toInt() else null,
                    "",
                    "bk-devops",
                    getExtraInfo(preTestApkIds)
            ))
            val requestContent = objectMapper.writeValueAsString(WetestTaskInst(
                    testId,
                    projectId,
                    pipelineId,
                    buildId,
                    runVariables[PIPELINE_BUILD_NUM]!!.toInt(),
                    wetestTask.name,
                    uploadResult["meta.versionName"] as? String ?: "",
                    "",
                    wetestTask.id.toString(),
                    testType,
                    scriptType ?: "",
                    if (synchronized == "SYNC") "1" else "0",
                    sourcePathStr,
                    scriptPathStr ?: "",
                    testAccountFileStr ?: "",
                    sourceType,
                    scriptSourceType ?: "",
                    accountFileSourceType ?: "",
                    if (isPrivateCloud) "1" else "0",
                    pipelineCreateUser,
                    System.currentTimeMillis(),
                    null,
                    param.notifyType.toLong()
            ))
            val saveTaskRequest = Request.Builder()
                    .url("$apiHost/wetest/api/service/wetest/task/saveTask")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestContent))
                    .build()
            OkhttpUtils.doHttp(saveTaskRequest).use { response ->
                val data = response.body()!!.string()
                LogUtils.addLine(rabbitTemplate, buildId, "save task response: $data", elementId, task.containerHashId,task.executeCount ?: 1)
            }
            LogUtils.addLine(rabbitTemplate, buildId, "成功提交wetest测试(testId: $testId)", elementId, task.containerHashId,task.executeCount ?: 1)

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
                            LogUtils.addLine(rabbitTemplate, buildId, "wetest测试成功，具体结果可以点击查看：<a target='_blank' href='https://wetest.qq.com/console/report/cloud'>查看详情</a>", elementId, task.containerHashId,task.executeCount ?: 1)
                            updateTaskInstStatus(testId, WetestInstStatus.SUCCESS)
                            break
                        }
                        LogUtils.addLine(rabbitTemplate, buildId, "测试进行中，请稍等...", elementId, task.containerHashId,task.executeCount ?: 1)
                        Thread.sleep(Math.min(5000L * count, 60 * 1000L)) // 最多等待1分钟
                        count++
                        if (count > maxTimes) throw TaskTimeoutExistException("wetest原子执行失败")
                    }
                } catch (e: TaskTimeoutExistException) {
                    LogUtils.addRedLine(rabbitTemplate, buildId, "wetest原子执行超时了", elementId, task.containerHashId,task.executeCount ?: 1)
                    updateTaskInstStatus(testId, WetestInstStatus.TIMEOUT)
                    throw e
                } catch (e: Throwable) {
                    LogUtils.addRedLine(rabbitTemplate, buildId, "wetest原子执行失败了", elementId, task.containerHashId,task.executeCount ?: 1)
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
                .url("http://$gatewayUrl/wetest/api/service/wetest/task/queryTestStatus" +
                        "?accessId=${URLEncoder.encode(accessId, "utf-8")}&accessToken=${URLEncoder.encode(accessToken, "utf-8")}" +
                        "&testId=${URLEncoder.encode(testId, "utf-8")}")
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            LogUtils.addLine(rabbitTemplate, buildId, "query test status response: $data", elementId, executeCount)
            return objectMapper.readValue<Result<Map<String, Any>>>(data).data ?: mapOf()
        }
    }

    private fun updateTaskInstStatus(testId: String, status: WetestInstStatus) {
        val request = Request.Builder()
                .url("http://$gatewayUrl/wetest/api/service/wetest/task/updateTaskInstStatus?testId=${URLEncoder.encode(testId, "utf-8")}&status=${status.name}")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), ""))
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            LogUtils.addLine(rabbitTemplate, buildId, "update task inst status $data", elementId, executeCount)
        }
    }

    override fun getParamElement(task: PipelineBuildTask): WetestElement {
        return JsonUtil.mapTo(task.taskParams, WetestElement::class.java)
    }

    private fun autoTest(wetestAutoTestRequest: WetestAutoTestRequest): String {
        val requestContent = objectMapper.writeValueAsString(wetestAutoTestRequest)
        val request = Request.Builder()
                .url("http://$gatewayUrl/wetest/api/service/wetest/task/autoTest" +
                        "?accessId=${URLEncoder.encode(accessId, "utf-8")}&accessToken=${URLEncoder.encode(accessToken, "utf-8")}")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestContent))
                .build()
        val result = OkhttpUtils.doHttp(request).use { response ->
            val data = response.body()!!.string()
            LogUtils.addLine(rabbitTemplate, buildId, "auto test response: $data", elementId, executeCount)
            objectMapper.readValue<Result<Map<String, Any>>>(data).data!!
        }
        checkResult(result, "启动任务失败:")
        return result["testid"] as String
    }

    private fun getAccountMap(testAccountFile: String?, sourceType: String?, task: PipelineBuildTask): List<List<String>>? {
        val accountExcelFile = Files.createTempDir().canonicalPath
        return if (!testAccountFile.isNullOrBlank()) {
            LogUtils.addLine(rabbitTemplate, buildId, "正在获取用户的数据: $testAccountFile($sourceType)", elementId, task.containerHashId,task.executeCount ?: 1)
            val excelFile = jfrogService.downloadFile(ArtifactorySearchParam(
                    projectId,
                    pipelineId,
                    buildId,
                    testAccountFile!!,
                    sourceType == "CUSTOMIZE",
                    task.executeCount ?: 0,
                    task.taskId
            ), accountExcelFile).firstOrNull()
                    ?: throw RuntimeException("account file can not be found: $accountExcelFile($sourceType)")
            excelFile.deleteOnExit()
            val list = ExcelUtils.getAccountFromExcel(excelFile.canonicalPath).map { listOf(it.key, it.value) }
            LogUtils.addLine(rabbitTemplate, buildId, "共获取用户的数据 ${list.size} 条", elementId, task.containerHashId,task.executeCount ?: 1)
            return list
        } else {
            null
        }
    }

    private fun uploadScript(scriptPath: String?, sourceType: String?, task: PipelineBuildTask): Int {
        return if (!scriptPath.isNullOrBlank()) {
            LogUtils.addLine(rabbitTemplate, buildId, "上传相应的脚本到wetest: $scriptPath($sourceType)", elementId, task.containerHashId,task.executeCount ?: 1)
            val param = ArtifactorySearchParam(
                    projectId,
                    pipelineId,
                    buildId,
                    scriptPath!!,
                    sourceType == "CUSTOMIZE",
                    task.executeCount ?: 0,
                    task.taskId
            )
            val scriptUploadResult = uploadRes(param, "script")
            checkResult(scriptUploadResult, "上传脚本到wetest失败!")
            scriptUploadResult["scriptid"] as? Int ?: throw RuntimeException("上传脚本到wetest失败!")
        } else {
            LogUtils.addLine(rabbitTemplate, buildId, "跳过上传相应的脚本到wetest步骤", elementId, task.containerHashId,task.executeCount ?: 1)
            0
        }
    }

    private fun uploadApp(sourcePath: String, sourceType: String, type: String, task: PipelineBuildTask): Map<String, Any> {
        LogUtils.addLine(rabbitTemplate, buildId, "上传相应的包到wetest: $sourcePath($sourceType)", elementId, task.containerHashId,task.executeCount ?: 1)
        val param = ArtifactorySearchParam(
                projectId,
                pipelineId,
                buildId,
                sourcePath,
                sourceType == "CUSTOMIZE",
                task.executeCount ?: 1,
                task.taskId
        )
        val uploadResult = uploadRes(param, type)
        checkResult(uploadResult, "上传app包失败")
        return uploadResult
    }

    private fun uploadRes(param: ArtifactorySearchParam, type: String): Map<String, Any> {
        val requestContent = objectMapper.writeValueAsString(param)
        val request = Request.Builder()
                .url("http://$gatewayUrl/wetest/api/service/wetest/task/uploadRes?" +
                        "accessId=${URLEncoder.encode(accessId, "utf-8")}&accessToken=${URLEncoder.encode(accessToken, "utf-8")}&type=$type ")
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestContent))
                .build()
        return OkhttpUtils.doLongHttp(request).use { response ->
            val data = response.body()!!.string()
            LogUtils.addLine(rabbitTemplate, buildId, "upload res response: $data", elementId, executeCount)
            objectMapper.readValue<Result<Map<String, Any>>>(data).data!!
        }
    }

    private fun checkResult(result: Map<String, Any>, errMsg: String) {
        if (!result.containsKey("ret") || result["ret"] as Int != 0) {
            val msg = result["msg"] as String?
            LogUtils.addRedLine(rabbitTemplate, buildId, "$errMsg : $msg", elementId, executeCount)
            throw RuntimeException(msg)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

//    private fun sendEmail(isSuccess: Boolean, notifyType: Int) {
//        val title = if (isSuccess) "wetest执行成功" else "wetest执行失败"
//        val host = HomeHostUtil.innerServerHost()
//        val url = "$host/console/pipeline/$projectId/$pipelineId/detail/$buildId"
//        val content = "蓝盾流水线执行wetest扫描任务结束，具体可以点击：<br><br>" +
//                "<a href=\"$url\">查看详情</a>"
//        val templateParams = mapOf(
//                "templateTitle" to title,
//                "templateContent" to content,
//                "projectName" to (projectOauthTokenService.getProjectName(projectId) ?: ""),
//                "logoUrl" to logoUrl,
//                "titleUrl" to titleUrl
//        )
//        val message = EmailNotifyMessage().apply {
//            format = EnumEmailFormat.HTML
//            body = NotifyUtils.parseMessageTemplate(NotifyTemplateUtils.EMAIL_BODY, templateParams)
//            this.title = title
//        }
//        val receivers = client.get(ServiceWetestEmailGroupResource::class).get(projectId, notifyType).data!!.userInternal.split(",").toSet()
//        message.addAllReceivers(receivers)
//        client.get(ServiceNotifyResource::class).sendEmailNotify(message)
//    }
}
