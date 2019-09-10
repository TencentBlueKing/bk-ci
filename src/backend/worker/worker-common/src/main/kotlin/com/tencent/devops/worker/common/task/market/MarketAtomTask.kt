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

package com.tencent.devops.worker.common.task.market

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.archive.element.ReportArchiveElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.worker.common.WORKSPACE_ENV
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskFactory
import com.tencent.devops.worker.common.utils.ArchiveUtils
import com.tencent.devops.worker.common.utils.BatScriptUtil
import com.tencent.devops.worker.common.utils.ShellUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

/**
 * 构建脚本任务
 */
open class MarketAtomTask : ITask() {

    private val atomApi = ApiFactory.create(AtomArchiveSDKApi::class)

    private val outputFile = "output.json"

    private val inputFile = "input.json"

    private val sdkFile = ".sdk.json"

    private lateinit var atomExecuteFile: File

    @Suppress("UNCHECKED_CAST")
    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        logger.info("buildTask is:$buildTask,buildVariables is:$buildVariables,workspacePath is:${workspace.absolutePath}")
        val taskParams = buildTask.params ?: mapOf()
        val atomName = taskParams["name"] as String
        val atomCode = taskParams["atomCode"] as String
        val atomVersion = taskParams["version"] as String
        val data = taskParams["data"] ?: "{}"
        val map = JsonUtil.toMutableMapSkipEmpty(data)

        logger.info("Start to execute the plugin task($atomName)($atomCode)")
        // 获取插件基本信息
        val atomEnvResult = atomApi.getAtomEnv(buildVariables.projectId, atomCode, atomVersion)
        logger.info("atomEnvResult is:$atomEnvResult")
        val atomData =
            atomEnvResult.data ?: throw ExecuteException("can not found $atomName: ${atomEnvResult.message}")

        // val atomWorkspace = File("${workspace.absolutePath}/${atomCode}_${buildTask.taskId}_data")
        val atomWorkspace = Files.createTempDirectory("${atomCode}_${buildTask.taskId}_data").toFile()

        if (!atomWorkspace.exists() && !atomWorkspace.mkdirs()) {
            throw ExecuteException("create directory fail! please check ${atomWorkspace.absolutePath}")
        }

        cleanOutput(atomWorkspace)

        val runtimeVariables = if (buildTask.buildVariable != null) {
            buildVariables.variables.plus(buildTask.buildVariable!!)
        } else {
            buildVariables.variables
        }

        val command = StringBuilder()

        // 解析输出字段模板
        val props = JsonUtil.toMutableMapSkipEmpty(atomData.props!!)

        // 解析输入参数

        val inputTemplate =
            if (props["input"] != null) {
                props["input"] as Map<String, Map<String, Any>>
            } else {
                mutableMapOf()
            }

        val systemVariables = mapOf(WORKSPACE_ENV to workspace.absolutePath)

        val atomParams = mutableMapOf<String, String>()
        try {
            val inputMap = map["input"] as Map<String, Any>?
            inputMap?.forEach { (name, value) ->
                // 只有构建机插件才有workspace变量
                if (buildTask.type == MarketBuildAtomElement.classType) {
                    atomParams[name] = EnvUtils.parseEnv(value.toString(), systemVariables)
                } else {
                    atomParams[name] = value.toString()
                }
            }
        } catch (ignored: Throwable) {
            logger.error("plugin input illegal! ", ignored)
            throw InvalidParamException("plugin input illegal!")
        }

        atomParams["bkWorkspace"] =
            if (buildTask.type != MarketBuildAtomElement.classType) {
                // 无构建环境插件的workspace取临时文件的路径
                atomWorkspace.absolutePath
            } else {
                workspace.absolutePath
            }

        val outputTemplate =
            if (props["output"] != null) {
                props["output"] as Map<String, Map<String, Any>>
            } else {
                mutableMapOf()
            }

        // 输出参数的用户命名空间：防止重名窘况
        val namespace: String? = map["namespace"] as String?

        printInput(atomData, atomParams, inputTemplate)

        if (atomData.target.isBlank()) {
            throw ParamBlankException("can not found any plugin cmd!")
        }

        // 查询插件的敏感信息
        val atomSensitiveConfResult = atomApi.getAtomSensitiveConf(atomCode)
        logger.info("atomCode is:$atomCode ,atomSensitiveConfResult is:$atomSensitiveConfResult")
        val atomSensitiveConfList = atomSensitiveConfResult.data
        val atomSensitiveConfMap = mutableMapOf<String, String>()
        atomSensitiveConfList?.forEach {
            atomSensitiveConfMap[it.fieldName] = it.fieldValue
        }
        val atomSensitiveDataMap = mapOf("bkSensitiveConfInfo" to atomSensitiveConfMap)
        writeInputFile(atomWorkspace, runtimeVariables.plus(atomParams).plus(atomSensitiveDataMap))

        writeSdkEnv(atomWorkspace, buildTask, buildVariables)

        val javaFile = getJavaFile()
        val environment = runtimeVariables.plus(
            mapOf(
                DIR_ENV to atomWorkspace.absolutePath,
                INPUT_ENV to inputFile,
                OUTPUT_ENV to outputFile,
                JAVA_PATH_ENV to javaFile.absolutePath
            )
        )

        var error: Throwable? = null
        try {
            // 下载atom执行文件
            atomExecuteFile = downloadAtomExecuteFile(atomData.pkgPath, atomWorkspace)

            checkSha1(atomExecuteFile, atomData.shaContent!!)

            val preCmds = mutableListOf<String>()
            if (!atomData.preCmd.isNullOrBlank()) {
                if (atomData.preCmd!!.contains(Regex("^\\s*\\[[\\w\\s\\S\\W]*\\]\\s*$"))) {
                    preCmds.addAll(JsonUtil.to(atomData.preCmd!!, object : TypeReference<List<String>>() {}))
                } else {
                    preCmds.add(atomData.preCmd!!)
                }
            }

            when {
                AgentEnv.getOS() == OSType.WINDOWS -> {
                    if (preCmds.isNotEmpty()) {
                        preCmds.forEach { cmd ->
                            command.append("\r\n$cmd\r\n")
                        }
                    }
                    command.append("\r\n${atomData.target.replace("\$bk_java_path", "%bk_java_path%")}\r\n")
                    BatScriptUtil.execute(
                        buildVariables.buildId,
                        command.toString(),
                        environment,
                        atomWorkspace,
                        null
                    )
                }
                AgentEnv.getOS() == OSType.LINUX || AgentEnv.getOS() == OSType.MAC_OS -> {
                    if (preCmds.isNotEmpty()) {
                        preCmds.forEach { cmd ->
                            command.append("\n$cmd\n")
                        }
                    }
                    command.append("\n${atomData.target}\n")
                    ShellUtil.execute(
                        buildVariables.buildId,
                        command.toString(),
                        atomWorkspace,
                        buildVariables.buildEnvs,
                        environment,
                        null
                    )
                }
            }
        } catch (e: Throwable) {
            error = e
        } finally {
            output(buildTask, atomWorkspace, buildVariables, outputTemplate, namespace)
            if (error != null)
                throw error
        }
    }

    private fun printInput(
        atomData: AtomEnv,
        atomParams: MutableMap<String, String>,
        inputTemplate: Map<String, Map<String, Any>>
    ) {
        LoggerService.addNormalLine("Task           :${atomData.atomName}")
        if (!atomData.summary.isNullOrBlank()) {
            LoggerService.addNormalLine("Description    :${atomData.summary}")
        }
        LoggerService.addNormalLine("Version        :${atomData.version}")
        LoggerService.addNormalLine("Author         :${atomData.creator}")
        if (!atomData.docsLink.isNullOrBlank()) {
            LoggerService.addNormalLine("Help           :[More Information](${atomData.docsLink}) ")
        }

        val atomStatus = AtomStatusEnum.getAtomStatus(atomData.atomStatus)
        if (atomStatus == AtomStatusEnum.UNDERCARRIAGED) {
            LoggerService.addYellowLine(
                "[警告]该插件已被下架，有可能无法正常工作！\n[WARNING]The plugin has been removed and may not work properly."
            )
        } else if (atomStatus == AtomStatusEnum.UNDERCARRIAGING) {
            LoggerService.addYellowLine(
                "[警告]该插件处于下架过渡期，后续可能无法正常工作！\n[WARNING]The plugin is in the transition period and may not work properly in the future."
            )
        }

        atomParams.forEach { (key, value) ->
            if (inputTemplate[key] != null) {
                val def = inputTemplate[key] as Map<String, Any>
                val sensitiveFlag = def["isSensitive"]
                if (sensitiveFlag != null && sensitiveFlag.toString() == "true") {
                    LoggerService.addYellowLine("input(sensitive): (${def["label"]})$key=******")
                } else {
                    LoggerService.addNormalLine("input(normal): (${def["label"]})$key=$value")
                }
            } else {
                LoggerService.addYellowLine("input(except): $key=$value")
            }
        }
    }

    private fun writeSdkEnv(workspace: File, buildTask: BuildTask, buildVariables: BuildVariables) {
        val inputFileFile = File(workspace, sdkFile)
        val sdkEnv: SdkEnv = when (BuildEnv.getBuildType()) {
            BuildType.AGENT, BuildType.DOCKER -> {
                SdkEnv(
                    buildType = BuildEnv.getBuildType(),
                    projectId = buildVariables.projectId,
                    agentId = AgentEnv.getAgentId(),
                    secretKey = AgentEnv.getAgentSecretKey(),
                    buildId = buildTask.buildId,
                    vmSeqId = buildTask.vmSeqId,
                    gateway = AgentEnv.getGateway()
                )
            }
        }
        logger.info("sdkEnv is:$sdkEnv")
        inputFileFile.writeText(JsonUtil.toJson(sdkEnv))
    }

    data class SdkEnv(
        val buildType: BuildType,
        val projectId: String,
        val agentId: String,
        val secretKey: String,
        val gateway: String,
        val buildId: String,
        val vmSeqId: String
    )

    private fun writeInputFile(
        workspace: File,
        inputVariables: Map<String, Any>
    ) {
//        logger.info("runtimeVariables is:$runtimeVariables") // 有敏感信息
        val inputFileFile = File(workspace, inputFile)
        inputFileFile.writeText(JsonUtil.toJson(inputVariables))
    }

    private fun output(
        buildTask: BuildTask,
        atomWorkspace: File,
        buildVariables: BuildVariables,
        outputTemplate: Map<String, Map<String, Any>>,
        namespace: String?
    ) {
        val atomResult = readOutputFile(atomWorkspace)
        deletePluginFile()
        val success: Boolean
        if (atomResult == null) {
            LoggerService.addYellowLine("No output")
        } else {
            when {
                atomResult.status == "success" -> {
                    success = true
                    LoggerService.addNormalLine("success: ${atomResult.message ?: ""}")
                }
                else -> {
                    success = false
                }
            }

            val outputData = atomResult.data
            val env = mutableMapOf<String, String>()
            outputData?.forEach { varKey, output ->
                val type = output["type"]
                val key = if (!namespace.isNullOrBlank()) {
                    "${namespace}_$varKey" // 用户前缀_插件输出变量名
                } else {
                    varKey
                }
                /*
                "data":{          # default模板的数据格式如下：
                    "outVar1": {
                        "type": "string",
                        "value": "testaaaaa"
                    },
                    "outVar2": {
                        "type": "artifact",
                        "value": ["xxx.html", "yyy.css"] # 本地文件路径，指定后，agent自动将这些文件归档到仓库
                    },
                    "outVar3": {
                        "type": "report",
                        "label": "",  # 报告别名，用于产出物报告界面标识当前报告
                        "path": "",   # 报告目录所在路径，相对于工作空间
                        "target": "", # 报告入口文件
                    }
                }
                 */
                when (type) {
                    "string" -> env[key] = output["value"] as String
                    "report" -> env[key] = archiveReport(buildTask, output, buildVariables, atomWorkspace)
                    "artifact" -> env[key] = archiveArtifact(output, atomWorkspace, buildVariables)
                }
                if (outputTemplate.containsKey(varKey)) {
                    val outPutDefine = outputTemplate[varKey]
                    val sensitiveFlag = outPutDefine!!["isSensitive"] as Boolean? ?: false
                    if (sensitiveFlag) {
                        LoggerService.addNormalLine("output(sensitive): $key=******")
                    } else {
                        LoggerService.addNormalLine("output(normal): $key=${env[key]}")
                    }
                } else {
                    LoggerService.addYellowLine("output(except): $key=${env[key]}")
                }
            }

            if (atomResult.type == "default") {
                if (env.isNotEmpty()) {
                    addEnv(env)
                }
            }

            if (!success) {
                throw ExecuteException("${atomResult.status}: ${atomResult.message}")
            }
        }
    }

    private fun deletePluginFile() {
        logger.info("delete plugin file: ${atomExecuteFile.name} ${atomExecuteFile.delete()}")
        logger.info("delete plugin input: $inputFile ${File(inputFile).delete()}")
        logger.info("delete plugin output: $outputFile ${File(outputFile).delete()}")
    }

    /**
     * 上传归档
     */
    @Suppress("UNCHECKED_CAST")
    private fun archiveArtifact(
        output: Map<String, Any>,
        atomWorkspace: File,
        buildVariables: BuildVariables
    ): String {
        var oneArtifact = ""
        try {
            val artifacts = output["value"] as List<String>
            artifacts.forEach { artifact ->
                oneArtifact = artifact
                ArchiveUtils.archivePipelineFiles(artifact, atomWorkspace, buildVariables)
            }
        } catch (e: Exception) {
            LoggerService.addRedLine("获取输出构件[artifact]值错误：${e.message}")
            logger.error("获取输出构件[artifact]值错误", e)
        }
        return oneArtifact
    }

    /**
     * 上传报告
     */
    private fun archiveReport(
        buildTask: BuildTask,
        output: Map<String, Any>,
        buildVariables: BuildVariables,
        atomWorkspace: File
    ): String {
        val params = mutableMapOf<String, String>()
        if (buildTask.params != null) {
            params.putAll(buildTask.params!!)
        }
        val resultData: String
        val reportType = output["reportType"] ?: ReportTypeEnum.INTERNAL.name // 报告类型，如果用户不传则默认为平台内置类型
        params["reportType"] = reportType.toString()
        if (reportType == ReportTypeEnum.INTERNAL.name) {
            params["fileDir"] = output["path"] as String
            val target = output["target"] as String
            params["indexFile"] = target
            resultData = target
        } else {
            val url = output["url"] as String
            params["reportUrl"] = url
            resultData = url
        }
        params["reportName"] = output["label"] as String
        val reportArchTask = BuildTask(
            buildTask.buildId,
            buildTask.vmSeqId,
            buildTask.status,
            buildTask.taskId,
            buildTask.elementId,
            buildTask.elementName,
            buildTask.type,
            params,
            buildTask.buildVariable
        )
        logger.info("reportArchTask is:$reportArchTask,buildVariables is:$buildVariables,atomWorkspacePath is:${atomWorkspace.absolutePath}")

        TaskFactory.create(ReportArchiveElement.classType).run(
            buildTask = reportArchTask,
            buildVariables = buildVariables, workspace = atomWorkspace
        )

        return resultData
    }

    private fun cleanOutput(workspace: File) {
        val outputFile = File(workspace, outputFile)
        if (!outputFile.exists()) {
            return
        }
        outputFile.writeText("")
    }

    private fun readOutputFile(workspace: File): AtomResult? {
        val f = File(workspace, outputFile)
        if (!f.exists()) {
            return null
        }
        if (f.isDirectory) {
            return null
        }

        val json = f.readText()
        if (json.isEmpty()) {
            return null
        }
        logger.info("drop output=${f.delete()}")
        return JsonUtil.to(json, AtomResult::class.java)
    }

    private fun checkSha1(file: File, sha1: String) {
        val fileSha1 = ShaUtils.sha1(file.readBytes())
        if (fileSha1 != sha1) {
            throw ExecuteException("Plugin File Sha1 is wrong! wrong sha1: $fileSha1")
        }
    }

    private fun downloadAtomExecuteFile(atomFilePath: String, workspace: File): File {
        try {
            // 取插件文件名
            val lastFx = atomFilePath.lastIndexOf("/")
            val file = if (lastFx > 0) {
                File(workspace, atomFilePath.substring(lastFx + 1))
            } else {
                File(workspace, atomFilePath)
            }
            atomApi.downloadAtom(atomFilePath, file)
            return file
        } catch (ignored: Throwable) {
            logger.error("download plugin execute file fail:", ignored)
            LoggerService.addRedLine("download plugin execute file fail: ${ignored.message}")
            throw ExecuteException("download plugin execute file fail $ignored")
        }
    }

    private fun getJavaFile() = File(System.getProperty("java.home"), "/bin/java")

    companion object {
        private const val DIR_ENV = "bk_data_dir"
        private const val INPUT_ENV = "bk_data_input"
        private const val OUTPUT_ENV = "bk_data_output"
        private const val JAVA_PATH_ENV = "bk_java_path"
        private val logger = LoggerFactory.getLogger(MarketAtomTask::class.java)
    }
}
