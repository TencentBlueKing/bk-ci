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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.task.market

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.bkrepo.repository.pojo.token.TokenType
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.annotation.SkipLogField
import com.tencent.devops.common.api.constant.ARTIFACT
import com.tencent.devops.common.api.constant.ARTIFACTORY_TYPE
import com.tencent.devops.common.api.constant.LABEL
import com.tencent.devops.common.api.constant.LOCALE_LANGUAGE
import com.tencent.devops.common.api.constant.PATH
import com.tencent.devops.common.api.constant.REPORT
import com.tencent.devops.common.api.constant.REPORT_TYPE
import com.tencent.devops.common.api.constant.STRING
import com.tencent.devops.common.api.constant.TYPE
import com.tencent.devops.common.api.constant.URL
import com.tencent.devops.common.api.constant.VALUE
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.factory.BkDiskLruFileCacheFactory
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.ObjectReplaceEnvVarUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.archive.element.ReportArchiveElement
import com.tencent.devops.common.pipeline.EnvReplacementParser
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.dialect.IPipelineDialect
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.webhook.pojo.code.BK_CI_RUN
import com.tencent.devops.dispatch.constants.ENV_PUBLIC_HOST_MAX_ATOM_FILE_CACHE_SIZE
import com.tencent.devops.dispatch.constants.ENV_THIRD_HOST_MAX_ATOM_FILE_CACHE_SIZE
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.process.utils.PIPELINE_ATOM_CODE
import com.tencent.devops.process.utils.PIPELINE_ATOM_NAME
import com.tencent.devops.process.utils.PIPELINE_ATOM_TIMEOUT
import com.tencent.devops.process.utils.PIPELINE_ATOM_VERSION
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_STEP_ID
import com.tencent.devops.process.utils.PIPELINE_TASK_NAME
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ATOM_POST_ENTRY_PARAM
import com.tencent.devops.store.pojo.common.ATOM_POST_PARENT_ELEMENT_ID
import com.tencent.devops.store.pojo.common.ATOM_POST_PARENT_TASK_ID
import com.tencent.devops.store.pojo.common.KEY_TARGET
import com.tencent.devops.store.pojo.common.enums.BuildHostTypeEnum
import com.tencent.devops.worker.common.BK_CI_ATOM_EXECUTE_ENV_PATH
import com.tencent.devops.worker.common.CI_TOKEN_CONTEXT
import com.tencent.devops.worker.common.CommonEnv
import com.tencent.devops.worker.common.JAVA_PATH_ENV
import com.tencent.devops.worker.common.JOB_OS_CONTEXT
import com.tencent.devops.worker.common.PIPELINE_SCRIPT_ATOM_CODE
import com.tencent.devops.worker.common.WORKSPACE_CONTEXT
import com.tencent.devops.worker.common.WORKSPACE_ENV
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.api.archive.ArtifactoryBuildResourceApi
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.api.atom.StoreSdkApi
import com.tencent.devops.worker.common.api.quality.QualityGatewaySDKApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_ATOM_HAS_BEEN_REMOVED
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_ATOM_IS_IN_THE_TRANSITION_PERIOD_OF_DELISTING
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BK_GET_OUTPUT_ARTIFACTVALUE_ERROR
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.exception.TaskExecuteExceptionDecorator
import com.tencent.devops.worker.common.expression.SpecialFunctions
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.service.CIKeywordsService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskFactory
import com.tencent.devops.worker.common.utils.ArchiveUtils
import com.tencent.devops.worker.common.utils.BatScriptUtil
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.CredentialUtils.parseCredentialValue
import com.tencent.devops.worker.common.utils.FileUtils
import com.tencent.devops.worker.common.utils.ShellUtil
import com.tencent.devops.worker.common.utils.TaskUtil
import com.tencent.devops.worker.common.utils.TemplateAcrossInfoUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 构建脚本任务
 */
@Suppress("ALL")
open class MarketAtomTask : ITask() {

    private val atomApi = ApiFactory.create(AtomArchiveSDKApi::class)

    private val storeApi = ApiFactory.create(StoreSdkApi::class)

    private val archiveApi = ApiFactory.create(ArchiveSDKApi::class)

    private val outputFile = "output.json"

    private val inputFile = "input.json"

    private val sdkFile = ".sdk.json"

    private val paramFile = ".param.json"

    private val atomExecuteFileDir = "store${File.separator}cache${File.separator}plugins"

    private val qualityGatewayResourceApi = ApiFactory.create(QualityGatewaySDKApi::class)

    @Suppress("UNCHECKED_CAST")
    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val projectId = buildVariables.projectId
        val taskName = taskParams["name"] as String
        val atomCode = taskParams["atomCode"] as String
        val atomVersion = taskParams["version"] as String
        val data = taskParams["data"] ?: "{}"
        val map = JsonUtil.toMutableMap(data)
        // 该插件执行的工作空间绝对路径
        val workspacePath = workspace.absolutePath
        // 输出参数的用户命名空间：防止重名窘况
        val namespace: String? = map["namespace"] as String?
        logger.info(
            "${buildTask.buildId}|RUN_ATOM|taskName=$taskName|ver=$atomVersion|code=$atomCode" +
                    "|workspace=$workspacePath"
        )

        // 获取插件基本信息
        val atomEnvResult = atomApi.getAtomEnv(
            projectCode = buildVariables.projectId,
            atomCode = atomCode,
            atomVersion = atomVersion,
            osName = AgentEnv.getOS().name,
            osArch = System.getProperty("os.arch")
        )
        logger.info("atomEnvResult is:$atomEnvResult")
        val atomData =
            atomEnvResult.data ?: throw TaskExecuteException(
                errorMsg = "can not found $taskName: ${atomEnvResult.message}",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )

        // val atomWorkspace = File("${workspace.absolutePath}/${atomCode}_${buildTask.taskId}_data")
        val atomTmpSpace = Files.createTempDirectory("${atomCode}_${buildTask.taskId}_data").toFile()
        buildTask.elementVersion = atomData.version
        if (!atomTmpSpace.exists() && !atomTmpSpace.mkdirs()) {
            throw TaskExecuteException(
                errorMsg = "create directory fail! please check ${atomTmpSpace.absolutePath}",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }

        cleanOutput(atomTmpSpace)

        // 将Job传入的流水线变量先进行凭据替换
        // 插件接收的流水线参数 = Job级别参数 + Task调度时参数 + 本插件上下文 + 编译机环境参数
        val acrossInfo by lazy { TemplateAcrossInfoUtil.getAcrossInfo(buildVariables.variables, buildTask.taskId) }
        var variables = buildVariables.variables.plus(buildTask.buildVariable ?: emptyMap()).map {
            it.key to it.value.parseCredentialValue(
                context = buildTask.buildVariable,
                acrossProjectId = acrossInfo?.targetProjectId
            )
        }.toMap()

        // 解析输入输出字段模板
        val props = JsonUtil.toMutableMap(atomData.props!!)
        val inputTemplate = props["input"]?.let { it as Map<String, Map<String, Any>> } ?: mutableMapOf()
        val outputTemplate = props["output"]?.let { props["output"] as Map<String, Map<String, Any>> } ?: mutableMapOf()

        // 插件SDK输入 = 所有变量 + 预置变量 + 敏感信息 + 处理后的插件参数
        // 增加插件名称和任务名称变量，设置是否是测试版本的标识
        val bkWorkspacePath = if (buildTask.containerType != VMBuildContainer.classType) {
            // 无构建环境下运行的插件的workspace取临时文件的路径
            atomTmpSpace.absolutePath
        } else {
            workspacePath
        }
        variables = variables.plus(
            mapOf(
                "bkWorkspace" to Paths.get(bkWorkspacePath).normalize().toString(),
                "testVersionFlag" to if (AtomStatusEnum.TESTING.name == atomData.atomStatus) "Y" else "N",
                PIPELINE_ATOM_NAME to atomData.atomName,
                PIPELINE_ATOM_CODE to atomData.atomCode,
                PIPELINE_ATOM_VERSION to atomData.version,
                PIPELINE_TASK_NAME to taskName,
                PIPELINE_ATOM_TIMEOUT to TaskUtil.getTimeOut(buildTask).toString(),
                LOCALE_LANGUAGE to (AgentEnv.getLocaleLanguage())
            )
        )

        val dialect = PipelineDialectUtil.getPipelineDialect(variables[PIPELINE_DIALECT])
        // 解析并打印插件执行传入的所有参数
        val inputParams = map["input"]?.let { input ->
            parseInputParams(
                inputMap = input as Map<String, Any>,
                variables = variables.plus(getContainerVariables(buildTask, buildVariables, workspacePath)),
                acrossInfo = acrossInfo,
                dialect = dialect
            )
        } ?: emptyMap()
        printInput(atomData, inputParams, inputTemplate)

        if (atomData.target?.isBlank() == true) {
            throw TaskExecuteException(
                errorMsg = "can not found any plugin cmd",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }

        buildTask.stepId?.let { variables = variables.plus(PIPELINE_STEP_ID to it) }

        val inputVariables = if (dialect.supportUseExpression()) {
            // 如果开启PAC,插件入参增加旧变量，防止开启PAC后,插件获取参数失败
            PipelineVarUtil.mixOldVarAndNewVar(variables.toMutableMap())
        } else {
            variables
        }.plus(inputParams).toMutableMap<String, Any>()
        val atomSensitiveConfWriteSwitch = System.getProperty("BK_CI_ATOM_PRIVATE_CONFIG_WRITE_SWITCH")?.toBoolean()
        if (atomSensitiveConfWriteSwitch != false) {
            // 开关关闭则不再写入插件私有配置到input.json中
            inputVariables.putAll(getAtomSensitiveConfMap(atomCode))
        }
        writeSdkEnv(atomTmpSpace, buildTask, buildVariables)
        writeParamEnv(atomCode, atomTmpSpace, workspace, buildTask, buildVariables)

        // 环境变量 = 所有插件变量 + Worker端执行插件依赖的预置变量
        val runtimeVariables = variables.plus(
            mapOf(
                DIR_ENV to atomTmpSpace.absolutePath,
                INPUT_ENV to inputFile,
                OUTPUT_ENV to outputFile,
                JAVA_PATH_ENV to AgentEnv.getRuntimeJdkPath()
            )
        ).toMutableMap()

        var error: Throwable? = null
        try {
            LoggerService.addFoldStartLine("[Install plugin]")
            // 获取插件执行包文件
            val atomExecuteFile = getAtomExecuteFile(
                atomData = atomData,
                atomTmpSpace = atomTmpSpace,
                workspace = workspace,
                projectId = projectId,
                buildId = buildTask.buildId
            )
            // 检查插件包的完整性
            checkSha1(atomExecuteFile, atomData.shaContent!!)
            val buildHostType = if (BuildEnv.isThirdParty()) BuildHostTypeEnum.THIRD else BuildHostTypeEnum.PUBLIC
            val atomLanguage = atomData.language!!
            val atomDevLanguageEnvVarsResult = atomApi.getAtomDevLanguageEnvVars(
                atomLanguage, buildHostType.name, AgentEnv.getOS().name
            )
            logger.info("atomCode is:$atomCode ,atomDevLanguageEnvVarsResult is:$atomDevLanguageEnvVarsResult")
            val atomDevLanguageEnvVars = atomDevLanguageEnvVarsResult.data
            val systemEnvVariables = TaskUtil.getTaskEnvVariables(buildVariables, buildTask.taskId)
            atomDevLanguageEnvVars?.forEach {
                systemEnvVariables[it.envKey] = it.envValue
            }

            // #7023 找回重构导致的逻辑丢失： runtime 覆盖 system 环境变量
            systemEnvVariables.forEach { runtimeVariables.putIfAbsent(it.key, it.value) }
            val preCmd = atomData.preCmd
            val buildEnvs = buildVariables.buildEnvs
            LoggerService.addFoldEndLine("-----")
            LoggerService.addNormalLine("")
            val atomRunConditionHandleService = AtomRunConditionFactory.createAtomRunConditionHandleService(
                language = atomLanguage
            )
            atomData.runtimeVersion?.let {
                // 准备插件运行环境
                atomRunConditionHandleService.prepareRunEnv(
                    osType = AgentEnv.getOS(),
                    language = atomLanguage,
                    runtimeVersion = it,
                    workspace = workspace
                )
                val atomExecutePath = System.getProperty(BK_CI_ATOM_EXECUTE_ENV_PATH)
                atomExecutePath?.let {
                    runtimeVariables[BK_CI_ATOM_EXECUTE_ENV_PATH] = atomExecutePath
                }
            }
            // 获取插件post操作入口参数
            val additionalOptions = taskParams[Element::additionalOptions.name]
            var postEntryParam: String? = null
            if (additionalOptions != null) {
                val additionalOptionMap = JsonUtil.toMutableMap(additionalOptions)
                val elementPostInfoMap = additionalOptionMap["elementPostInfo"] as? Map<String, Any>
                postEntryParam = elementPostInfoMap?.get(ATOM_POST_ENTRY_PARAM)?.toString()
                val parentTaskId = elementPostInfoMap?.get(ATOM_POST_PARENT_ELEMENT_ID)?.toString()
                if (!parentTaskId.isNullOrBlank()) {
                    inputVariables[ATOM_POST_PARENT_TASK_ID] = parentTaskId
                }
            }

            writeInputFile(atomTmpSpace, inputVariables)
            val atomTarget = atomRunConditionHandleService.handleAtomTarget(
                target = atomData.target!!,
                osType = AgentEnv.getOS(),
                postEntryParam = postEntryParam
            )
            val runCmds = mutableListOf<String>()
            if (!preCmd.isNullOrBlank()) {
                runCmds.addAll(CommonUtils.strToList(preCmd))
            }
            runCmds.add(atomTarget)
            // 运行阶段单独处理执行失败错误
            try {
                val errorMessage = "Fail to run the plugin"
                when (AgentEnv.getOS()) {
                    OSType.WINDOWS -> {
                        val script = runCmds.joinToString(
                            separator = "\r\n"
                        ) { "\r\n$it" }
                        BatScriptUtil.execute(
                            buildId = buildVariables.buildId,
                            script = script,
                            runtimeVariables = runtimeVariables,
                            dir = atomTmpSpace,
                            workspace = workspace,
                            errorMessage = errorMessage,
                            jobId = buildVariables.jobId,
                            stepId = buildTask.stepId,
                            taskId = buildTask.taskId
                        )
                    }
                    OSType.LINUX, OSType.MAC_OS -> {
                        val script = runCmds.joinToString(
                            separator = "\n"
                        ) { "\n$it" }
                        ShellUtil.execute(
                            buildId = buildVariables.buildId,
                            script = script,
                            dir = atomTmpSpace,
                            workspace = workspace,
                            buildEnvs = buildEnvs,
                            runtimeVariables = runtimeVariables,
                            errorMessage = errorMessage,
                            jobId = buildVariables.jobId,
                            stepId = buildTask.stepId,
                            taskId = buildTask.taskId
                        )
                    }
                    else -> {
                    }
                }
            } catch (t: Throwable) {
                logger.warn("Market atom execution exit with StackTrace:\n", t)
                throw TaskExecuteException(
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL,
                    errorMsg = "Market atom execution exit with StackTrace: ${t.message}"
                )
            }
        } catch (e: Throwable) {
            error = TaskExecuteExceptionDecorator.decorate(e)
        } finally {
            output(buildTask, atomTmpSpace, File(bkWorkspacePath), buildVariables, outputTemplate, namespace, atomCode)
            atomData.finishKillFlag?.let { addFinishKillFlag(it) }
            if (error != null) {
                throw if (error is TaskExecuteException) {
                    error
                } else {
                    val defaultMessage = StringBuilder("Market atom env load exit with StackTrace:\n")
                    defaultMessage.append(error.toString())
                    error.stackTrace.forEach {
                        with(it) { defaultMessage.append("\n    at $className.$methodName($fileName:$lineNumber)") }
                    }
                    TaskExecuteException(
                        errorType = ErrorType.SYSTEM,
                        errorCode = ErrorCode.SYSTEM_INNER_TASK_ERROR,
                        errorMsg = defaultMessage.toString()
                    )
                }
            }
        }
    }

    private fun getAtomExecuteFile(
        atomData: AtomEnv,
        atomTmpSpace: File,
        workspace: File,
        projectId: String,
        buildId: String
    ): File {
        // 取插件文件名
        val atomFilePath = atomData.pkgPath!!
        val lastFx = atomFilePath.lastIndexOf("/")
        val atomExecuteFile = if (lastFx > 0) {
            File(atomTmpSpace, atomFilePath.substring(lastFx + 1))
        } else {
            File(atomTmpSpace, atomFilePath)
        }
        if (atomExecuteFile.exists() && atomExecuteFile.length() > 0) {
            return atomExecuteFile
        }
        val atomExecuteFileName = atomExecuteFile.name
        // 从缓存中获取插件执行包文件
        val cacheDirPrefix = if (BuildEnv.isThirdParty()) {
            // 如果是第三方构建机，插件包缓存放入构建机的公共区域
            System.getProperty("user.dir")
        } else {
            // 如果是公共构建机，插件包缓存放入流水线的工作空间上一级目录中
            // 如果workspace路径是相对路径.，workspace.parentFile会为空，故需用file对象包装一下
            File(workspace.parentFile, "cache").absolutePath
        }
        val fileCacheDir = "$cacheDirPrefix${File.separator}$atomExecuteFileDir"
        // 获取构建机缓存文件区域大小
        val maxFileCacheSize = if (BuildEnv.isThirdParty()) {
            AgentEnv.getEnvProp(ENV_THIRD_HOST_MAX_ATOM_FILE_CACHE_SIZE)?.toLong()
                ?: DEFAULT_THIRD_HOST_MAX_ATOM_FILE_CACHE_SIZE
        } else {
            AgentEnv.getEnvProp(ENV_PUBLIC_HOST_MAX_ATOM_FILE_CACHE_SIZE)?.toLong()
                ?: DEFAULT_PUBLIC_HOST_MAX_ATOM_FILE_CACHE_SIZE
        }
        logger.info("getDiskLruFileCache fileCacheDir:$fileCacheDir,maxFileCacheSize:$maxFileCacheSize")
        val bkDiskLruFileCache = BkDiskLruFileCacheFactory.getDiskLruFileCache(fileCacheDir, maxFileCacheSize)
        val fileCacheKey = "${atomData.atomCode}-${atomData.version}-$atomExecuteFileName"
        bkDiskLruFileCache.get(fileCacheKey, atomExecuteFile)
        try {
            if (!atomExecuteFile.exists() || atomExecuteFile.length() < 1) {
                logger.info("local file[$atomExecuteFileName] is not exist,start downloading from the repo!")
                val cacheFlag = atomData.atomStatus !in setOf(
                    AtomStatusEnum.TESTING.name,
                    AtomStatusEnum.CODECCING.name,
                    AtomStatusEnum.AUDITING.name
                )

                downloadAtomExecuteFile(
                    projectId = projectId,
                    atomFilePath = atomFilePath,
                    atomExecuteFile = atomExecuteFile,
                    authFlag = atomData.authFlag ?: true,
                    queryCacheFlag = cacheFlag
                )

                val shouldCache = atomData.authFlag != true && checkSha1(
                    atomExecuteFile, atomData.shaContent!!
                ) && cacheFlag && !fileCacheDir.contains(buildId)

                if (shouldCache) {
                    // 无需鉴权的插件包且插件包内容是完整的才放入缓存中
                    // 未发布的插件版本内容可能会变化故也不存入缓存、工作空间如果以构建ID为维度没法实现资源复用故也不存入缓存
                    bkDiskLruFileCache.put(fileCacheKey, atomExecuteFile)
                }
            } else if (atomData.authFlag == true) {
                bkDiskLruFileCache.remove(fileCacheKey)
            }
        } finally {
            bkDiskLruFileCache.close()
        }
        return atomExecuteFile
    }

    private fun parseInputParams(
        inputMap: Map<String, Any>,
        variables: Map<String, String>,
        acrossInfo: BuildTemplateAcrossInfo?,
        dialect: IPipelineDialect
    ): Map<String, String> {
        val atomParams = mutableMapOf<String, String>()
        try {
            if (dialect.supportUseExpression()) {
                val customReplacement = EnvReplacementParser.getCustomExecutionContextByMap(
                    variables = variables,
                    extendNamedValueMap = listOf(
                        CredentialUtils.CredentialRuntimeNamedValue(targetProjectId = acrossInfo?.targetProjectId),
                        CIKeywordsService.CIKeywordsRuntimeNamedValue()
                    )
                )
                inputMap.forEach { (name, value) ->
                    logger.info("parseInputParams|name=$name|value=$value")
                    atomParams[name] = EnvReplacementParser.parse(
                        value = JsonUtil.toJson(value),
                        contextMap = variables,
                        dialect = dialect,
                        contextPair = customReplacement,
                        functions = SpecialFunctions.functions,
                        output = SpecialFunctions.output
                    ).parseCredentialValue(null, acrossInfo?.targetProjectId)
                }
            } else {
                inputMap.forEach { (name, value) ->
                    // 修复插件input环境变量替换问题 #5682
                    atomParams[name] = JsonUtil.toJson(
                        ObjectReplaceEnvVarUtil.replaceEnvVar(value, variables)
                    ).parseCredentialValue(null, acrossInfo?.targetProjectId)
                }
            }
        } catch (e: Throwable) {
            logger.error("plugin input illegal! ", e)
            LoggerService.addErrorLine("plugin input illegal! ${e.message}")
            throw TaskExecuteException(
                errorMsg = "plugin input illegal",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }
        return atomParams
    }

    private fun getAtomSensitiveConfMap(atomCode: String): Map<String, MutableMap<String, String>> {
        // 查询插件的敏感信息
        val atomSensitiveConfResult = atomApi.getAtomSensitiveConf(atomCode)
        val atomSensitiveConfList = atomSensitiveConfResult.data
        val atomSensitiveConfMap = mutableMapOf<String, String>()
        atomSensitiveConfList?.forEach {
            atomSensitiveConfMap[it.fieldName] = it.fieldValue
        }
        return mapOf("bkSensitiveConfInfo" to atomSensitiveConfMap)
    }

    private fun printInput(
        atomData: AtomEnv,
        atomParams: Map<String, String>,
        inputTemplate: Map<String, Map<String, Any>>
    ) {
        LoggerService.addFoldStartLine("[Plugin info]")
        LoggerService.addNormalLine("=====================================================================")
        LoggerService.addNormalLine("Task           : ${atomData.atomName}")
        if (!atomData.summary.isNullOrBlank()) {
            LoggerService.addNormalLine("Description    : ${atomData.summary}")
        }
        LoggerService.addNormalLine("Version        : ${atomData.version}")
        LoggerService.addNormalLine("Author         : ${atomData.creator}")
        if (!atomData.docsLink.isNullOrBlank()) {
            LoggerService.addNormalLine(
                "Help           : <a target=\"_blank\" href=\"${atomData.docsLink}\">More Information</a>"
            )
        }
        LoggerService.addNormalLine("=====================================================================")

        val atomStatus = AtomStatusEnum.getAtomStatus(atomData.atomStatus)
        if (atomStatus == AtomStatusEnum.UNDERCARRIAGED) {
            LoggerService.addWarnLine(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_ATOM_HAS_BEEN_REMOVED,
                    language = AgentEnv.getLocaleLanguage()
                )
            )
        } else if (atomStatus == AtomStatusEnum.UNDERCARRIAGING) {
            LoggerService.addWarnLine(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_ATOM_IS_IN_THE_TRANSITION_PERIOD_OF_DELISTING,
                    language = AgentEnv.getLocaleLanguage()
                )
            )
        }
        LoggerService.addFoldEndLine("-----")
        LoggerService.addNormalLine("")
        LoggerService.addFoldStartLine("[Input]")
        atomParams.forEach { (key, value) ->
            if (inputTemplate[key] != null) {
                val def = inputTemplate[key] as Map<String, Any>
                val sensitiveFlag = def["isSensitive"]
                if (sensitiveFlag != null && sensitiveFlag.toString() == "true") {
                    LoggerService.addWarnLine("input(sensitive): (${def["label"]})$key=******")
                } else {
                    LoggerService.addNormalLine("input(normal): (${def["label"]})$key=$value")
                }
            } else {
                LoggerService.addWarnLine("input(except): $key=$value")
            }
        }
        LoggerService.addFoldEndLine("-----")
        LoggerService.addNormalLine("")
    }

    private fun writeSdkEnv(workspace: File, buildTask: BuildTask, buildVariables: BuildVariables) {
        val inputFileFile = File(workspace, sdkFile)
        val sdkEnv: SdkEnv = when (BuildEnv.getBuildType()) {
            BuildType.AGENT, BuildType.DOCKER, BuildType.MACOS, BuildType.MACOS_NEW -> {
                SdkEnv(
                    buildType = BuildEnv.getBuildType(),
                    projectId = buildVariables.projectId,
                    agentId = AgentEnv.getAgentId(),
                    secretKey = AgentEnv.getAgentSecretKey(),
                    buildId = buildTask.buildId,
                    vmSeqId = buildTask.vmSeqId,
                    gateway = AgentEnv.getGateway(),
                    fileGateway = getFileGateway(buildVariables.containerType),
                    taskId = buildTask.taskId ?: "",
                    executeCount = buildTask.executeCount ?: 1
                )
            }
            BuildType.WORKER -> {
                SdkEnv(
                    buildType = BuildEnv.getBuildType(),
                    projectId = buildVariables.projectId,
                    agentId = "",
                    secretKey = "",
                    buildId = buildTask.buildId,
                    vmSeqId = buildTask.vmSeqId,
                    gateway = AgentEnv.getGateway(),
                    fileGateway = getFileGateway(buildVariables.containerType),
                    taskId = buildTask.taskId ?: "",
                    executeCount = buildTask.executeCount ?: 1
                )
            }
        }
        logger.info("sdkEnv is:${JsonUtil.skipLogFields(sdkEnv)}")
        inputFileFile.writeText(JsonUtil.toJson(sdkEnv))
    }

    private fun getFileGateway(containerType: String?): String {
        if (!AgentEnv.getFileGateway().isNullOrBlank()) {
            return AgentEnv.getFileGateway()!!
        }
        val vmBuildEnvFlag = TaskUtil.isVmBuildEnv(containerType)
        var fileDevnetGateway = CommonEnv.fileDevnetGateway
        var fileIdcGateway = CommonEnv.fileIdcGateway
        if (fileDevnetGateway == null || fileIdcGateway == null) {
            val fileGatewayInfo = ArtifactoryBuildResourceApi().getFileGatewayInfo()
            logger.info("fileGatewayInfo: $fileGatewayInfo")
            fileDevnetGateway = fileGatewayInfo?.fileDevnetGateway
            CommonEnv.fileDevnetGateway = fileDevnetGateway
            fileIdcGateway = fileGatewayInfo?.fileIdcGateway
            CommonEnv.fileIdcGateway = fileIdcGateway
        }
        logger.info("fileGateway: ${CommonEnv.fileDevnetGateway}, ${CommonEnv.fileIdcGateway}")
        return (if (vmBuildEnvFlag) CommonEnv.fileDevnetGateway else CommonEnv.fileIdcGateway) ?: ""
    }

    private fun writeParamEnv(
        atomCode: String,
        atomTmpSpace: File,
        workspace: File,
        buildTask: BuildTask,
        buildVariables: BuildVariables
    ) {
        if (atomCode in PIPELINE_SCRIPT_ATOM_CODE) {
            try {
                val param = mapOf(
                    "workspace" to jacksonObjectMapper().writeValueAsString(workspace),
                    "buildTask" to jacksonObjectMapper().writeValueAsString(buildTask),
                    "buildVariables" to jacksonObjectMapper().writeValueAsString(buildVariables)
                )
                val paramStr = jacksonObjectMapper().writeValueAsString(param)
                val inputFileFile = File(atomTmpSpace, paramFile)

                logger.info("paramFile is:$paramFile")
                inputFileFile.writeText(paramStr)
            } catch (e: Throwable) {
                logger.error("Write param exception", e)
            }
        }
    }

    data class SdkEnv(
        val buildType: BuildType,
        val projectId: String,
        val agentId: String,
        @SkipLogField
        val secretKey: String,
        val gateway: String,
        val buildId: String,
        val vmSeqId: String,
        val fileGateway: String,
        val taskId: String,
        val executeCount: Int
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
        atomTmpSpace: File,
        bkWorkspace: File,
        buildVariables: BuildVariables,
        outputTemplate: Map<String, Map<String, Any>>,
        namespace: String?,
        atomCode: String
    ) {
        val atomResult = readOutputFile(atomTmpSpace)
        logger.info("the atomResult from Market is :\n$atomResult")
        deletePluginFile(atomTmpSpace)
        // 添加插件监控数据
        val monitorData = atomResult?.monitorData
        if (monitorData != null) {
            addMonitorData(monitorData)
        }
        // 校验插件对接平台错误码信息失败
        val platformCode = atomResult?.platformCode
        if (!platformCode.isNullOrBlank()) {
            var isPlatformCodeRegistered = false
            try {
                isPlatformCodeRegistered = storeApi.isPlatformCodeRegistered(platformCode).data ?: false
            } catch (e: RemoteServiceException) {
                logger.warn(
                    "Failed to verify the error code information of the atom " +
                        "docking platformm $platformCode | ${e.errorMessage}"
                )
            }
            if (isPlatformCodeRegistered) {
                addPlatformCode(platformCode)
                atomApi.addAtomDockingPlatforms(atomCode, setOf(platformCode))
                val platformErrorCode = atomResult.platformErrorCode
                if (platformErrorCode != null) {
                    addPlatformErrorCode(platformErrorCode)
                }
            } else {
                logger.warn(
                    "PlatformCode:$platformCode has not been registered and failed to enter " +
                        "the library. Please contact Devops-helper to register first"
                )
            }
        }
        val success: Boolean
        if (atomResult == null) {
            LoggerService.addWarnLine("No output")
        } else {
            success = atomResult.status == "success"

            val outputData = atomResult.data
            val env = mutableMapOf<String, String>()
            LoggerService.addNormalLine("")
            LoggerService.addFoldStartLine("[Output]")
            outputData?.forEach { (varKey, output) ->
                val type = output[TYPE]
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
                        "enableEmail": true, # 是否开启发送邮件
                        "emailReceivers": [], # 邮件接收人
                        "emailTitle": "" # 邮件标题
                    }
                }
                 */
                TaskUtil.setTaskId(buildTask.taskId ?: "")
                when (type) {
                    STRING -> env[key] = output[VALUE] as String
                    REPORT -> env[key] = archiveReport(
                        buildTask = buildTask,
                        varKey = varKey,
                        output = output,
                        buildVariables = buildVariables,
                        atomWorkspace = bkWorkspace
                    )
                    ARTIFACT -> env[key] = archiveArtifact(
                        buildTask = buildTask,
                        varKey = varKey,
                        output = output,
                        atomWorkspace = bkWorkspace,
                        buildVariables = buildVariables
                    )
                }

                // #4518 如果定义了插件上下文标识ID，进行上下文outputs输出
                // 即使没有jobId也以containerId前缀输出
                val value = env[key] ?: ""
                if (!buildTask.stepId.isNullOrBlank() &&
                    !buildVariables.jobId.isNullOrBlank() &&
                    !key.startsWith("variables.")
                ) {
                    val contextKey = "jobs.${buildVariables.jobId}.steps.${buildTask.stepId}.outputs.$key"
                    env[contextKey] = value
                    // 原变量名输出只在未开启 pipeline as code 的逻辑中保留
                    if (
                        // TODO 暂时只对stream进行拦截原key
                        buildVariables.variables[BK_CI_RUN] == "true" &&
                        buildVariables.pipelineAsCodeSettings?.enable == true
                    ) env.remove(key)
                }

                TaskUtil.removeTaskId()
                if (outputTemplate.containsKey(varKey)) {
                    val outPutDefine = outputTemplate[varKey]
                    val sensitiveFlag = outPutDefine!!["isSensitive"] as Boolean? ?: false
                    if (sensitiveFlag) {
                        LoggerService.addNormalLine("output(sensitive): $key=******")
                    } else {
                        LoggerService.addNormalLine("output(normal): $key=$value")
                    }
                } else {
                    LoggerService.addWarnLine("output(except): $key=$value")
                }
            }

            LoggerService.addFoldEndLine("-----")

            buildVariables.jobId?.let {
                env["jobs.${buildVariables.jobId}.os"] = AgentEnv.getOS().name
            }

            if (atomResult.type == "default") {
                if (env.isNotEmpty()) {
                    addEnv(env)
                }
            }

            if (atomResult.type == "quality") {
                if (env.isNotEmpty()) {
                    addEnv(env)
                }

                // 处理质量红线数据
                val qualityMap = atomResult.qualityData?.map {
                    val value = it.value["value"]?.toString() ?: ""
                    it.key to value
                }?.toMap()
                if (qualityMap != null) {
                    qualityGatewayResourceApi.saveScriptHisMetadata(
                        atomCode,
                        buildTask.taskId ?: "",
                        buildTask.elementName ?: "",
                        qualityMap
                    )
                }
            } else {
                if (!atomResult.qualityData.isNullOrEmpty()) {
                    logger.warn("qualityData is not empty, but type is ${atomResult.type}, expected 'quality' !")
                }
            }

            // 若插件执行失败返回错误信息
            if (!success) {
                throw TaskExecuteException(
                    errorMsg = "[Finish task] status: ${atomResult.status}, errorType: ${atomResult.errorType}, " +
                        "errorCode: ${atomResult.errorCode}, message: ${atomResult.message}",
                    errorType = when (atomResult.errorType) {
                        // 插件上报的错误类型，若非用户业务错误或插件内的第三方服务调用错误，统一设为插件逻辑错误
                        1 -> ErrorType.USER
                        2 -> ErrorType.THIRD_PARTY
                        else -> ErrorType.PLUGIN
                    },
                    errorCode = atomResult.errorCode ?: ErrorCode.PLUGIN_DEFAULT_ERROR
                )
            }
        }
    }

    private fun deletePluginFile(atomWorkspace: File) {
        FileUtils.deleteRecursivelyOnExit(atomWorkspace)
    }

    /**
     * 上传归档
     */
    @Suppress("UNCHECKED_CAST")
    private fun archiveArtifact(
        buildTask: BuildTask,
        varKey: String,
        output: Map<String, Any>,
        atomWorkspace: File,
        buildVariables: BuildVariables
    ): String {
        var oneArtifact = ""
        val artifactoryType = (output[ARTIFACTORY_TYPE] as? String) ?: ArtifactoryType.PIPELINE.name
        val customFlag = artifactoryType == ArtifactoryType.CUSTOM_DIR.name
        val token = archiveApi.getRepoToken(
            userId = buildVariables.variables[PIPELINE_START_USER_ID] ?: "",
            projectId = buildVariables.projectId,
            repoName = if (customFlag) "custom" else "pipeline",
            path = if (customFlag) "/" else "/${buildVariables.pipelineId}/${buildVariables.buildId}",
            type = TokenType.UPLOAD,
            expireSeconds = TaskUtil.getTimeOut(buildTask).times(60)
        )
        try {
            val artifacts = output[VALUE] as List<String>
            artifacts.forEach { artifact ->
                oneArtifact = artifact
                if (artifactoryType == ArtifactoryType.PIPELINE.name) {
                    ArchiveUtils.archivePipelineFiles(
                        filePath = artifact,
                        workspace = atomWorkspace,
                        buildVariables = buildVariables,
                        token = token
                    )
                } else if (artifactoryType == ArtifactoryType.CUSTOM_DIR.name) {
                    output[PATH] ?: throw TaskExecuteException(
                        errorMsg = "$varKey.$PATH cannot be empty",
                        errorType = ErrorType.USER,
                        errorCode = ErrorCode.USER_INPUT_INVAILD
                    )
                    val destPath = output[PATH] as String
                    ArchiveUtils.archiveCustomFiles(
                        filePath = artifact,
                        destPath = destPath,
                        workspace = atomWorkspace,
                        buildVariables = buildVariables,
                        token = token
                    )
                }
            }
        } catch (e: Exception) {
            LoggerService.addErrorLine(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_GET_OUTPUT_ARTIFACTVALUE_ERROR,
                    language = AgentEnv.getLocaleLanguage()
                ) + "：${e.message}"
            )
            logger.error("Get output artifact [artifact] value error", e)
        }
        return oneArtifact
    }

    /**
     * 上传报告
     */
    private fun archiveReport(
        buildTask: BuildTask,
        varKey: String,
        output: Map<String, Any>,
        buildVariables: BuildVariables,
        atomWorkspace: File
    ): String {
        output[LABEL] ?: throw TaskExecuteException(
            errorMsg = "$varKey.$LABEL cannot be empty",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_INPUT_INVAILD
        )
        val params = mutableMapOf<String, String>()
        if (buildTask.params != null) {
            params.putAll(buildTask.params!!)
        }
        val resultData: String
        val reportType = output[REPORT_TYPE] ?: ReportTypeEnum.INTERNAL.name // 报告类型，如果用户不传则默认为平台内置类型
        params[REPORT_TYPE] = reportType.toString()
        if (reportType == ReportTypeEnum.INTERNAL.name) {
            output[PATH] ?: throw TaskExecuteException(
                errorMsg = "$varKey.$PATH cannot be empty",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD
            )
            output[KEY_TARGET] ?: throw TaskExecuteException(
                errorMsg = "$varKey.$KEY_TARGET cannot be empty",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD
            )
            params["fileDir"] = output[PATH] as String
            val target = output[KEY_TARGET] as String
            params["indexFile"] = target
            resultData = target
        } else {
            output[URL] ?: throw TaskExecuteException(
                errorMsg = "$varKey.$URL cannot be empty",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD
            )
            val url = output[URL] as String
            params["reportUrl"] = url
            resultData = url
        }
        params["reportName"] = output[LABEL] as String
        val emailReceivers = output["emailReceivers"] as? String
        val emailTitle = output["emailTitle"] as? String
        if (emailReceivers != null && emailTitle != null) {
            params["enableEmail"] = output["enableEmail"].toString()
            params["emailReceivers"] = JsonUtil.toJson(emailReceivers)
            params["emailTitle"] = emailTitle
        }
        logger.info("${buildTask.buildId}|reportArchTask|atomWorkspacePath=${atomWorkspace.absolutePath}")

        TaskFactory.create(ReportArchiveElement.classType).run(
            buildTask = buildTask.copy(params = params),
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

    private fun checkSha1(file: File, sha1: String): Boolean {
        val fileSha1 = file.inputStream().use { ShaUtils.sha1InputStream(it) }
        if (fileSha1 != sha1) {
            throw TaskExecuteException(
                errorMsg = "Plugin File Sha1 is wrong! wrong sha1: $fileSha1",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }
        return true
    }

    private fun downloadAtomExecuteFile(
        projectId: String,
        atomFilePath: String,
        atomExecuteFile: File,
        authFlag: Boolean,
        queryCacheFlag: Boolean
    ): File {
        try {
            atomApi.downloadAtom(
                projectId = projectId,
                atomFilePath = atomFilePath,
                file = atomExecuteFile,
                authFlag = authFlag,
                queryCacheFlag = queryCacheFlag
            )
            return atomExecuteFile
        } catch (t: Throwable) {
            logger.error("download plugin execute file fail:", t)
            LoggerService.addErrorLine("download plugin execute file fail: $t")
            throw TaskExecuteExceptionDecorator.decorate(t)
        }
    }

    private fun getContainerVariables(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspacePath: String
    ): Map<String, String> {
        val context = mutableMapOf<String, String>()
        // 只将本插件的状态改为RUNNING，其他插件上下文保持引擎传入
        buildTask.stepId?.let { stepId ->
            context["step.status"] = BuildStatus.RUNNING.name
            context["steps.$stepId.status"] = BuildStatus.RUNNING.name
        }
        // 将token加入上下文
        buildVariables.variables[CI_TOKEN_CONTEXT]?.let { context[CI_TOKEN_CONTEXT] = it }

        // 如果为有编译环境则追加WORKSPACE，无编译环境不添加
        if (buildTask.containerType == VMBuildContainer.classType) {
            // 只有构建环境下运行的插件才有workspace变量
            context.putAll(
                mapOf(
                    WORKSPACE_ENV to workspacePath,
                    WORKSPACE_CONTEXT to workspacePath,
                    JOB_OS_CONTEXT to AgentEnv.getOS().name
                )
            )
        }
        return context
    }

    companion object {
        private const val DIR_ENV = "bk_data_dir"
        private const val INPUT_ENV = "bk_data_input"
        private const val OUTPUT_ENV = "bk_data_output"
        private const val DEFAULT_PUBLIC_HOST_MAX_ATOM_FILE_CACHE_SIZE = 209715200L
        private const val DEFAULT_THIRD_HOST_MAX_ATOM_FILE_CACHE_SIZE = 2147483648L
        private val logger = LoggerFactory.getLogger(MarketAtomTask::class.java)
    }
}
