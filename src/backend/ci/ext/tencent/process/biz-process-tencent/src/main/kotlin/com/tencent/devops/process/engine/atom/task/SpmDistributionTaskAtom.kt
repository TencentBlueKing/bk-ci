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

package com.tencent.devops.process.engine.atom.task

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Files
import com.google.gson.JsonParser
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.element.SpmDistributionElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_BUILD_TASK_CDN_FAIL
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.util.CommonUtils
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.LogOutputStream
import org.apache.commons.exec.PumpStreamHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class SpmDistributionTaskAtom @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val commonConfig: CommonConfig
) : IAtomTask<SpmDistributionElement> {
    @Value("\${cdntool.cmdpath}")
    private val cmdpath = "/data1/cdntool/cdntool"
    @Value("\${cdntool.master}")
    private val spmMaster = "spm-server.cdn.qq.com:8080"
    @Value("\${cdntool.querystatusurl}")
    private val querystatusurl = "http://spm.oa.com/cdntool/query_file_status.py"
    @Value("\${cdntool.rsyncip}")
    private val rsyncip = CommonUtils.getInnerIP()
    @Value("\${cdntool.rsyncport}")
    private val rsyncport = 873
    @Value("\${cdntool.rsyncmodule}")
    private val rsyncmodule = "landun_test"
    @Value("\${cdntool.rsyncuser}")
    private val rsyncuser = "root"
    @Value("\${cdntool.rsyncpwd}")
    private val rsyncpwd = "ITDev@server2"

    private var count = 0
    private val praser = JsonParser()

    private var buildId = ""
    private var projectId = ""
    private var pipelineId = ""

    override fun getParamElement(task: PipelineBuildTask): SpmDistributionElement {
        return JsonUtil.mapTo(task.taskParams, SpmDistributionElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: SpmDistributionElement, runVariables: Map<String, String>): AtomResponse {
        logger.info("Enter SpmDistributionDelegate run...")
        val searchUrl = "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/jfrog/api/service/search/aql"

        val cmdbAppId = param.cmdbAppId
        val cmdbAppName = parseVariable(param.cmdbAppName, runVariables)
        val rootPath = parseVariable(param.rootPath, runVariables)
        val secretKey = parseVariable(param.secretKey, runVariables)
        val regexPathsStr = parseVariable(param.regexPaths, runVariables)
        val isCustom = param.customize
        val maxRunningMins = param.maxRunningMins
        val userId = task.starter

        buildId = task.buildId
        projectId = task.projectId
        pipelineId = task.pipelineId
        val elementId = task.taskId

        val workspace = Files.createTempDir()
        var zipFile: File? = null

        try {
            regexPathsStr.split(",").forEach { regex ->
                val requestBody = getRequestBody(regex, isCustom)
                buildLogPrinter.addLine(buildId, "requestBody:$requestBody", elementId, task.containerHashId, task.executeCount ?: 1)
                val request = Request.Builder()
                        .url(searchUrl)
                        .post(RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), requestBody))
                        .build()

                OkhttpUtils.doHttp(request).use { response ->
                    val body = response.body()!!.string()

                    val results = praser.parse(body).asJsonObject["results"].asJsonArray
                    for (i in 0 until results.size()) {
                        count++
                        val obj = results[i].asJsonObject
                        val path = getPath(obj["path"].asString, obj["name"].asString, isCustom)
                        val url = getUrl(path, isCustom)
                        // val filePath = getFilePath(obj["path"].asString, obj["name"].asString, isCustom)
                        val destFile = File(workspace, obj["name"].asString)
                        OkhttpUtils.downloadFile(url, destFile)
                        logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
                    }
                }
            }
            logger.info("$count file(s) will be distribute...")
            buildLogPrinter.addLine(buildId, "$count file(s) will be distribute...", elementId, task.containerHashId, task.executeCount ?: 1)
            if (count == 0) throw TaskExecuteException(
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorType = ErrorType.USER,
                errorMsg = "No file to distribute"
            )
            zipFile = FileUtil.zipToCurrentPath(workspace)
            logger.info("Zip file: ${zipFile.canonicalPath}")
            buildLogPrinter.addLine(buildId, "Zip file: $zipFile", elementId, task.containerHashId, task.executeCount ?: 1)

            // 创建cdntool的配置文件
            val cdnToolConfFile = File(workspace, "cdntool.conf")
            cdnToolConfFile.writeText("bu_id = $cmdbAppId \n")
            cdnToolConfFile.appendText("bu_name = $cmdbAppName \n")
            cdnToolConfFile.appendText("master = $spmMaster \n")
            cdnToolConfFile.appendText("secret_key = $secretKey \n")
            cdnToolConfFile.appendText("bu_rtx = $userId \n")
            cdnToolConfFile.appendText("rsync_dir = /tmp/distribute/ \n")
            cdnToolConfFile.appendText("rsync_ip = $rsyncip \n")
            cdnToolConfFile.appendText("rsync_port = $rsyncport \n")
            cdnToolConfFile.appendText("rsync_module = $rsyncmodule \n")
            cdnToolConfFile.appendText("rsync_user = $rsyncuser \n")
            cdnToolConfFile.appendText("rsync_pwd = $rsyncpwd \n")

            // 执行cdntool命令
            val distributeFile = zipFile.canonicalPath.toString().substring("/tmp/distribute/".length)
            val cmd = "$cmdpath -c $workspace/cdntool.conf -f $distributeFile -o zip"
            logger.info("cdntool cmd: $cmd")
            val responseListStr = executeShell(cmd, File("/tmp/distribute/"))
            logger.info("spm cdntool return: $responseListStr")
            val response: Map<String, Any> = jacksonObjectMapper().readValue(responseListStr)
            if (response["ret"] != 0) {
                val msg = response["error"]
                logger.error("Spm return not 0,distribute to cdn failed. msg: $msg")
                buildLogPrinter.addRedLine(buildId, "分发CDN失败. msg: $msg", elementId, task.containerHashId, task.executeCount ?: 1)
                return AtomResponse(
                    buildStatus = BuildStatus.FAILED,
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorMsg = "分发CDN失败. msg: $msg"
                )
            }

            logger.info("Distribute to cdn request success, now get the process...")
            if (waitForDistribute(rootPath, distributeFile, cmdbAppId, maxRunningMins)) {
                logger.info("CDN distribute success.")
                buildLogPrinter.addLine(buildId, "CDN distribute success.", elementId, task.containerHashId, task.executeCount ?: 1)
            }

            buildLogPrinter.addLine(buildId, "Distribute to CDN done", elementId, task.containerHashId, task.executeCount ?: 1)
        } finally {
            workspace.deleteRecursively()
            zipFile?.delete()
        }
        return AtomResponse(BuildStatus.SUCCEED)
    }

    fun executeShell(command: String, workspace: File): String {
        val result = StringBuilder()
        val cmdLine = CommandLine.parse(command)
        val executor = DefaultExecutor()
        executor.workingDirectory = workspace
        val outputStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null)
                    return
                result.append(line)
            }
        }

        val errorStream = object : LogOutputStream() {
            override fun processLine(line: String?, level: Int) {
                if (line == null) {
                    return
                }
                result.append(line)
            }
        }
        executor.streamHandler = PumpStreamHandler(outputStream, errorStream)
        try {
            val exitCode = executor.execute(cmdLine)
            if (exitCode != 0) {
                throw TaskExecuteException(
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorType = ErrorType.USER,
                    errorMsg = "Script command execution failed with exit code($exitCode)"
                )
            }
        } catch (t: Throwable) {
            logger.warn("Fail to execute the command($command)", t)
            throw t
        }
        return result.toString()
    }

    private fun waitForDistribute(rootPath: String, distributePath: String, cmdbAppId: Int, timeout: Int): Boolean {
        logger.info("waiting for cdn done, timeout setting: ${timeout}s")
        val startTime = System.currentTimeMillis()
        while (!queryFileStatus(rootPath, distributePath, cmdbAppId)) {
            if (System.currentTimeMillis() - startTime > timeout * 1000) {
                logger.error("cdn distribute timeout")
                return false
            }
            Thread.sleep(3 * 1000)
        }
        return true
    }

    private fun queryFileStatus(rootPath: String, distributePath: String, cmdbAppId: Int): Boolean {
        var rootPathValue = rootPath
        if (!rootPathValue.startsWith("/")) {
            rootPathValue = "/$rootPath"
        }
        if (!rootPathValue.endsWith("/")) {
            rootPathValue = "$rootPathValue/"
        }
        val url = "$querystatusurl?compatible=on&buid=$cmdbAppId&filename=$rootPathValue$distributePath"
        logger.info("Get url: $url")
        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        OkhttpUtils.doHttp(request).use { response ->
            val body = response.body()!!.string()
            logger.info("Response body: $body")

            val responseJson = praser.parse(body).asJsonObject
            val retCode = responseJson["code"].asInt
            if (0 != retCode) {
                logger.error("Response failed. msg: ${responseJson["msg"].asString}")
                throw BuildTaskException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = ERROR_BUILD_TASK_CDN_FAIL.toInt(),
                    errorMsg = "分发CDN失败"
                )
            }

            val results = praser.parse(body).asJsonObject["file_list"].asJsonArray
            for (i in 0 until results.size()) {
                val obj = results[i].asJsonObject
                val finishRate = obj["finish_rate"].asString
                if ("100%" == finishRate) {
                    return true
                }
            }
        }

        return false
    }

    private fun getRequestBody(regex: String, isCustom: Boolean): String {
        val pathPair = getPathPair(regex)
        return if (isCustom) {
            "items.find(\n" +
                    "    {\n" +
                    "        \"repo\":{\"\$eq\":\"generic-local\"}, \"path\":{\"\$eq\":\"bk-custom/$projectId${pathPair.first}\"}, \"name\":{\"\$match\":\"${pathPair.second}\"}\n" +
                    "    }\n" +
                    ")"
        } else {
            "items.find(\n" +
                    "    {\n" +
                    "        \"repo\":{\"\$eq\":\"generic-local\"}, \"path\":{\"\$eq\":\"bk-archive/$projectId/$pipelineId/$buildId${pathPair.first}\"}, \"name\":{\"\$match\":\"${pathPair.second}\"}\n" +
                    "    }\n" +
                    ")"
        }
    }

    // aa/test/*.txt
    // first = /aa/test
    // second = *.txt
    private fun getPathPair(regex: String): Pair<String, String> {
        if (regex.endsWith("/")) return Pair("/" + regex.removeSuffix("/"), "*")
        val index = regex.lastIndexOf("/")

        if (index == -1) return Pair("", regex) // a.txt

        return Pair("/" + regex.substring(0, index), regex.substring(index + 1))
    }

    // 处理jfrog传回的路径
    private fun getPath(path: String, name: String, isCustom: Boolean): String {
        return if (isCustom) {
            path.substring(path.indexOf("/") + 1).removePrefix("/$projectId") + "/" + name
        } else {
            path.substring(path.indexOf("/") + 1).removePrefix("/$projectId/$pipelineId/$buildId") + "/" + name
        }
    }

    // 获取jfrog传回的url
    private fun getUrl(realPath: String, isCustom: Boolean): String {
        return if (isCustom) {
            "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/jfrog/storage/service/custom/$realPath"
        } else {
            "${HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)}/jfrog/storage/service/archive/$realPath"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SpmDistributionTaskAtom::class.java)
    }
}
