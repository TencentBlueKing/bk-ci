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

package com.tencent.devops.prebuild.v2.component

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.image.BuildType
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.MacOS
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.image.PoolType
import com.tencent.devops.common.ci.task.CodeCCScanInContainerTask
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.prebuild.PreBuildCode
import com.tencent.devops.prebuild.PreBuildCode.BK_CHECK_YML_CONFIGURATION
import com.tencent.devops.prebuild.PreBuildCode.BK_PIPELINE_MUST_AT_LEAST_ONE
import com.tencent.devops.prebuild.PreBuildCode.BK_PIPELINE_NAME_CREATOR_CANNOT_EMPTY
import com.tencent.devops.prebuild.PreBuildCode.BK_PUBLIC_BUILD_RESOURCE_POOL_NOT_EXIST
import com.tencent.devops.prebuild.PreBuildCode.BK_SYNCHRONIZE_LOCAL_CODE
import com.tencent.devops.prebuild.pojo.CreateStagesRequest
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.process.yaml.v2.models.IfType
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.job.Container2
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import com.tencent.devops.process.yaml.v2.models.step.Step
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.ws.rs.core.Response
import com.tencent.devops.process.yaml.v2.models.job.Container as V2Container
import com.tencent.devops.process.yaml.v2.models.stage.Stage as V2Stage

/**
 * 流水线编排
 * 1.流水线由多个Stages组成
 * 2.Stage由多个Jobs组成
 * 3.Job由多个(Steps/Tasks)组成
 */
class PipelineLayout private constructor(
    private val projectId: String,
    private val userId: String,
    private val variables: Map<String, Variable>?,
    // v2版本Stages
    private val v2Stages: List<V2Stage>?,
    private val finallyJobs: List<Job>?,
    private val agentId: String,
    private val agentOS: String,
    // 用户本地项目相关信息
    private val userLocalProjectInfo: StartUpReq,
    private val channelCode: ChannelCode
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineLayout::class.java)
        private const val VARIABLE_PREFIX = "variables."
        private const val REMOTE_SYNC_CODE_PLUGIN_ATOM_CODE = "syncCodeToRemote"
        private const val LOCAL_SYNC_CODE_PLUGIN_ATOM_CODE = "syncLocalCode"
    }

    /**
     * 生成整体Stages的门面入口
     */
    private fun generateStages(): List<Stage> {
        val stageList = mutableListOf<Stage>()
        // 第一个stage，触发类
        val manualTriggerElement = ManualTriggerElement(
            MessageUtil.getMessageByLocale(
                messageCode = PreBuildCode.BK_MANUAL_TRIGGER,
                language = I18nUtil.getLanguage(userId)
            ), "T-1-1-1")
        val triggerContainer = TriggerContainer(
            id = "0",
            name = MessageUtil.getMessageByLocale(
                messageCode = PreBuildCode.BK_BUILD_TRIGGER,
                language = I18nUtil.getLanguage(userId)
            ),
            elements = listOf(manualTriggerElement),
            params = makeBuildProperties()
        )
        val firstStage = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(firstStage)

        // 其他的stage
        v2Stages!!.forEachIndexed { stageIndex, stage ->
            stageList.add(
                makeStage(v2Stage = stage, stageIndex = stageIndex + 2)
            )
        }

        // 添加finally
        if (!finallyJobs.isNullOrEmpty()) {
            val v2Stage = V2Stage(
                name = "Finally",
                label = emptyList(),
                ifField = null,
                fastKill = false,
                jobs = finallyJobs,
                checkIn = null,
                checkOut = null
            )

            stageList.add(
                makeStage(v2Stage = v2Stage, finalStage = true)
            )
        }

        return stageList
    }

    /**
     * 生成构建参数
     */
    private fun makeBuildProperties(): List<BuildFormProperty> {
        if (variables.isNullOrEmpty()) {
            return emptyList()
        }

        val retList = mutableListOf<BuildFormProperty>()
        val startParams = mutableMapOf<String, String>()
        variables.forEach { (key, variable) ->
            startParams[VARIABLE_PREFIX + key] =
                variable.copy(value = formatVariablesValue(variable.value, startParams)).value ?: ""
        }

        startParams.forEach {
            val property = BuildFormProperty(
                id = it.key,
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = it.value,
                options = null,
                desc = null,
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            )
            retList.add(property)
        }

        return retList
    }

    /**
     * 生成作业步骤信息
     */
    private fun makeStepElementList(job: Job): MutableList<Element> {
        val elementList = mutableListOf<Element>()
        job.steps?.forEach { step ->
            if (step.run != null && JobRunsOnType.AGENT_LESS.type == job.runsOn.poolName) {
                throw CustomException(
                    Response.Status.NOT_FOUND, MessageUtil.getMessageByLocale(
                        messageCode = BK_CHECK_YML_CONFIGURATION,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }

            // service, checkOut 无需处理
            val additionalOptions = ElementAdditionalOptions(
                continueWhenFailed = step.continueOnError ?: false,
                timeout = step.timeoutMinutes?.toLong(),
                retryWhenFailed = step.retryTimes != null,
                retryCount = step.retryTimes ?: 0,
                enableCustomEnv = step.env != null,
                customEnv = emptyList(),
                runCondition = if (step.ifFiled.isNullOrBlank()) {
                    RunCondition.PRE_TASK_SUCCESS
                } else {
                    RunCondition.CUSTOM_CONDITION_MATCH
                },
                customCondition = step.ifFiled
            )

            // bash
            val element: Element? = when {
                step.run != null -> makeScriptElement(job, step, additionalOptions)
                else -> makeNormalElement(job, step, additionalOptions)
            }

            if (element != null) {
                elementList.add(element)
            }

            if (element is MarketBuildAtomElement) {
                logger.info("install market atom: ${element.getAtomCode()}")
                installMarketAtom(element.getAtomCode())
            }
        }

        return elementList
    }

    /**
     * 生成Stage
     *
     * @param v2Stage V2版本的Stage
     * @param stageIndex 阶段索引
     * @param finalStage 是否最后阶段
     * @return 流水线核心Stage
     */
    private fun makeStage(v2Stage: V2Stage, stageIndex: Int = 0, finalStage: Boolean = false): Stage {
        val containerList = mutableListOf<Container>()
        v2Stage.jobs.forEachIndexed { jobIndex, job ->
            val elementList = makeStepElementList(job)

            if (JobRunsOnType.AGENT_LESS.type == job.runsOn.poolName) {
                // 无编译环境
                val normalContainer = NormalContainer(
                    containerId = null,
                    id = job.id,
                    name = MessageUtil.getMessageByLocale(
                        messageCode = PreBuildCode.BK_NO_COMPILATION_ENVIRONMENT,
                        language = I18nUtil.getLanguage(userId)
                    ),
                    elements = elementList,
                    status = null,
                    startEpoch = null,
                    systemElapsed = null,
                    elementElapsed = null,
                    enableSkip = false,
                    conditions = null,
                    canRetry = false,
                    jobControlOption = makeJobControlOption(job, finalStage),
                    mutexGroup = null
                )
                containerList.add(normalContainer)
            } else {
                // 处理nfs挂载
                val buildEnv = when (job.runsOn.poolName) {
                    JobRunsOnType.DOCKER.type -> job.runsOn.needs
                    JobRunsOnType.DEV_CLOUD.type -> job.runsOn.needs
                    else -> null
                }

                // 构建机信息
                val vmContainer = VMBuildContainer(
                    jobId = job.id,
                    name = "Job_${jobIndex + 1} " + (job.name ?: ""),
                    elements = elementList,
                    status = null,
                    startEpoch = null,
                    systemElapsed = null,
                    elementElapsed = null,
                    baseOS = getBaseOs(job),
                    vmNames = setOf(),
                    maxQueueMinutes = 60,
                    maxRunningMinutes = job.timeoutMinutes ?: 900,
                    buildEnv = buildEnv,
                    customBuildEnv = job.env,
                    thirdPartyAgentId = null,
                    thirdPartyAgentEnvId = null,
                    thirdPartyWorkspace = null,
                    dockerBuildVersion = null,
                    tstackAgentId = null,
                    jobControlOption = makeJobControlOption(job, finalStage),
                    dispatchType = makeDispatchType(job)
                )

                containerList.add(vmContainer)
            }
        }

        // 根据if设置stageController
        var stageControlOption = StageControlOption()
        if (!finalStage && !v2Stage.ifField.isNullOrBlank()) {
            stageControlOption = StageControlOption(
                runCondition = StageRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = v2Stage.ifField.toString()
            )
        }

        return Stage(
            id = null,
            name = v2Stage.name ?: when (finalStage) {
                true -> "Final"
                false -> "Stage-$stageIndex"
            },
            tag = v2Stage.label,
            fastKill = v2Stage.fastKill,
            stageControlOption = stageControlOption,
            containers = containerList,
            finally = finalStage
        )
    }

    /**
     * 脚本类step
     */
    private fun makeScriptElement(job: Job, step: Step, additionalOptions: ElementAdditionalOptions): Element {
        val linuxScriptElement = LinuxScriptElement(
            name = step.name ?: "run",
            id = step.id,
            scriptType = BuildScriptType.SHELL,
            script = step.run!!,
            continueNoneZero = false,
            additionalOptions = additionalOptions
        )

        return if (job.runsOn.agentSelector.isNullOrEmpty()) {
            linuxScriptElement
        } else {
            when (job.runsOn.agentSelector!!.first()) {
                "linux" -> linuxScriptElement
                "macos" -> linuxScriptElement
                "windows" -> WindowsScriptElement(
                    name = step.name ?: "run",
                    id = step.id,
                    scriptType = BuildScriptType.BAT,
                    script = step.run!!
                )
                else -> linuxScriptElement
            }
        }
    }

    /**
     * 非脚本类step
     */
    private fun makeNormalElement(job: Job, step: Step, additionalOptions: ElementAdditionalOptions): Element? {
        val data = mutableMapOf<String, Any>()
        val atomCode = step.uses!!.split('@')[0]

        // 若是"代码同步"插件标识
        if (atomCode.equals(LOCAL_SYNC_CODE_PLUGIN_ATOM_CODE, ignoreCase = true)) {
            // 若是本地构建机，则无需进行"代码同步"插件
            if (job.runsOn.poolName == JobRunsOnType.LOCAL.type) {
                return null
            }

            // 安装"代码同步"插件
            installMarketAtom(REMOTE_SYNC_CODE_PLUGIN_ATOM_CODE)
            val input = step.with?.toMutableMap() ?: mutableMapOf()
            input["agentId"] = input["agentId"] ?: agentId
            input["workspace"] = input["workspace"] ?: userLocalProjectInfo.workspace
            input["useDelete"] = input["useDelete"] ?: true
            input["syncGitRepository"] = input["syncGitRepository"] ?: false
            data["input"] = input

            return MarketBuildAtomElement(
                name = step.name ?: MessageUtil.getMessageByLocale(
                    messageCode = BK_SYNCHRONIZE_LOCAL_CODE,
                    language = I18nUtil.getLanguage(userId)
                ),
                id = null,
                atomCode = "syncAgentCode",
                version = "3.*",
                data = data,
                additionalOptions = additionalOptions
            )
        } else {
            data["input"] = step.with ?: Any()
            setWhitePath(atomCode, data, job)

            return MarketBuildAtomElement(
                name = step.name ?: step.uses!!.split('@')[0],
                id = step.id,
                atomCode = step.uses!!.split('@')[0],
                version = step.uses!!.split('@')[1],
                data = data,
                additionalOptions = additionalOptions
            )
        }
    }

    /**
     * 设置白名单
     */
    private fun setWhitePath(atomCode: String, data: MutableMap<String, Any>, job: Job) {
        if (atomCode == CodeCCScanInContainerTask.atomCode &&
            userLocalProjectInfo.extraParam != null
        ) {
            val input = (data["input"] as Map<*, *>).toMutableMap()
            val isRunOnDocker =
                JobRunsOnType.DEV_CLOUD.type == job.runsOn.poolName || JobRunsOnType.DOCKER.type == job.runsOn.poolName
            input["path"] = getWhitePathList(isRunOnDocker)
        }
    }

    /**
     * 生成Job相关控制设置
     */
    private fun makeJobControlOption(job: Job, finalStage: Boolean = false): JobControlOption {
        return if (!job.ifField.isNullOrBlank()) {
            if (finalStage) {
                JobControlOption(
                    timeout = job.timeoutMinutes,
                    runCondition = when (job.ifField) {
                        IfType.SUCCESS.name -> JobRunCondition.PREVIOUS_STAGE_SUCCESS
                        IfType.FAILURE.name -> JobRunCondition.PREVIOUS_STAGE_FAILED
                        IfType.CANCELLED.name -> JobRunCondition.PREVIOUS_STAGE_CANCEL
                        else -> JobRunCondition.STAGE_RUNNING
                    },
                    dependOnType = DependOnType.ID,
                    dependOnId = job.dependOn,
                    continueWhenFailed = job.continueOnError
                )
            } else {
                JobControlOption(
                    timeout = job.timeoutMinutes,
                    runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                    customCondition = job.ifField.toString(),
                    dependOnType = DependOnType.ID,
                    dependOnId = job.dependOn,
                    continueWhenFailed = job.continueOnError
                )
            }
        } else {
            JobControlOption(
                timeout = job.timeoutMinutes,
                dependOnType = DependOnType.ID,
                dependOnId = job.dependOn,
                continueWhenFailed = job.continueOnError
            )
        }
    }

    /**
     * 生成构建资源池
     */
    private fun makeContainerPool(job: Job): Pool {
        val buildType = if (job.runsOn.poolName == JobRunsOnType.DOCKER.type) {
            BuildType.DOCKER_VM
        } else {
            BuildType.DEVCLOUD
        }

        return if (job.runsOn.container == null) {
            Pool(
                container = "http://mirrors.tencent.com/ci/tlinux3_ci:0.1.1.0",
                credential = Credential(
                    user = "",
                    password = ""
                ),
                macOS = null,
                third = null,
                env = job.env,
                buildType = buildType
            )
        } else {
            try {
                val container = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(job.runsOn.container!!),
                    V2Container::class.java
                )

                Pool(
                    container = container.image,
                    credential = Credential(
                        user = container.credentials?.username ?: "",
                        password = container.credentials?.password ?: ""
                    ),
                    macOS = null,
                    third = null,
                    env = job.env,
                    buildType = buildType
                )
            } catch (e: Exception) {
                // 凭据credential可能是String
                val container = YamlUtil.getObjectMapper().readValue(
                    JsonUtil.toJson(job.runsOn.container!!),
                    Container2::class.java
                )

                Pool(
                    container = container.image,
                    credential = Credential(
                        user = null,
                        password = null,
                        credentialId = container.credentials ?: ""
                    ),
                    macOS = null,
                    third = null,
                    env = job.env,
                    buildType = buildType
                )
            }
        }
    }

    /**
     * 生成调度信息
     */
    private fun makeDispatchType(job: Job): DispatchType {
        if (job.runsOn.poolName == JobRunsOnType.LOCAL.type) {
            return ThirdPartyAgentIDDispatchType(
                displayName = agentId,
                workspace = userLocalProjectInfo.workspace,
                agentType = AgentType.ID,
                dockerInfo = null
            )
        }

        // 第三方构建机
        if (job.runsOn.selfHosted == true) {
            return ThirdPartyAgentEnvDispatchType(
                envName = job.runsOn.poolName,
                envProjectId = null,
                workspace = job.runsOn.workspace,
                agentType = AgentType.NAME,
                dockerInfo = null
            )
        }

        val containerPool = makeContainerPool(job)

        if (job.runsOn.poolName.startsWith("macos", ignoreCase = true)) {
            return PoolType.Macos.toDispatchType(
                containerPool.copy(macOS = MacOS("Catalina10.15.4", "12.2"))
            )
        }

        logger.info("prebuild v2 $projectId, runsOn: ${JsonUtil.toJson(job.runsOn)}")

        return when (job.runsOn.poolName) {
            JobRunsOnType.DEV_CLOUD.type -> {
                PoolType.DockerOnDevCloud.toDispatchType(containerPool)
            }
            JobRunsOnType.DOCKER.type -> {
                PoolType.DockerOnVm.toDispatchType(containerPool)
            }
            else -> {
                throw CustomException(Response.Status.NOT_FOUND, MessageUtil.getMessageByLocale(
                    messageCode = BK_PUBLIC_BUILD_RESOURCE_POOL_NOT_EXIST,
                    language = I18nUtil.getLanguage(userId)
                ))
            }
        }
    }

    /**
     * 获取系统类型
     */
    private fun getBaseOs(job: Job): VMBaseOS {
        if (job.runsOn.poolName == JobRunsOnType.LOCAL.type) {
            return VMBaseOS.valueOf(agentOS)
        }

        if (job.runsOn.poolName.startsWith("macos", ignoreCase = true)) {
            return VMBaseOS.MACOS
        }

        if (job.runsOn.agentSelector.isNullOrEmpty()) {
            return VMBaseOS.LINUX
        }

        return when (job.runsOn.agentSelector!![0]) {
            "linux" -> VMBaseOS.LINUX
            "macos" -> VMBaseOS.MACOS
            "windows" -> VMBaseOS.WINDOWS
            else -> VMBaseOS.LINUX
        }
    }

    /**
     * 格式化构建参数
     */
    private fun formatVariablesValue(varValue: String?, startParams: MutableMap<String, String>): String? {
        if (varValue.isNullOrEmpty()) {
            return ""
        }

        val settingMap = mutableMapOf<String, String>().apply {
            putAll(startParams)
        }

        return ScriptYmlUtils.parseVariableValue(varValue, settingMap)
    }

    /**
     * 安装插件
     */
    private fun installMarketAtom(atomCode: String) {
        val projectCodes = ArrayList<String>().apply {
            add(projectId)
        }

        try {
            val request = InstallAtomReq(projectCodes, atomCode)
            val client = SpringContextUtil.getBean(Client::class.java)
            client.get(ServiceMarketAtomResource::class).installAtom(userId, channelCode, request)
        } catch (e: Throwable) {
            // 可能之前安装过，继续执行不中断
            logger.error("install atom($atomCode) failed, exception:", e)
        }
    }

    /**
     * 获取白名单列表
     */
    private fun getWhitePathList(isRunOnDocker: Boolean = false): List<String> {
        val whitePathList = mutableListOf<String>()

        // idea右键扫描
        if (!(userLocalProjectInfo.extraParam!!.codeccScanPath.isNullOrBlank())) {
            whitePathList.add(userLocalProjectInfo.extraParam!!.codeccScanPath!!)
        }

        // push/commit前扫描的文件路径
        if (userLocalProjectInfo.extraParam!!.incrementFileList != null &&
            userLocalProjectInfo.extraParam!!.incrementFileList!!.isNotEmpty()
        ) {
            whitePathList.addAll(userLocalProjectInfo.extraParam!!.incrementFileList!!)
        }

        // 若不是容器中执行的，则无法进行本地路径替换
        if (!isRunOnDocker) {
            return whitePathList
        }

        // 容器文件路径处理
        whitePathList.forEachIndexed { index, path ->
            val filePath = path.removePrefix(userLocalProjectInfo.workspace)
            // 路径开头不匹配则不替换
            if (filePath != path) {
                // 兼容workspace可能带'/'的情况
                if (userLocalProjectInfo.workspace.last() == '/') {
                    whitePathList[index] = "/data/landun/workspace/$filePath"
                } else {
                    whitePathList[index] = "/data/landun/workspace$filePath"
                }
            }
        }

        return whitePathList
    }

    /**
     * builder辅助类
     */
    @Component
    class Builder {
        private var pipelineName: String = ""
        private var description: String = ""
        private var creator: String = ""
        private var labels: List<String> = emptyList()
        private var stages: List<Stage> = emptyList()

        /**
         * 流水线名称
         */
        fun pipelineName(name: String): Builder {
            this.pipelineName = name
            return this
        }

        /**
         * 流水线描述
         */
        fun description(desc: String): Builder {
            this.description = desc
            return this
        }

        /**
         * 流水线创建者
         */
        fun creator(userId: String): Builder {
            this.creator = userId
            return this
        }

        /**
         * 流水线标签
         */
        fun labels(labels: List<String>): Builder {
            this.labels = labels
            return this
        }

        /**
         * 流水线阶段
         */
        fun stages(stages: List<Stage>): Builder {
            this.stages = stages
            return this
        }

        /**
         * 流水线阶段
         */
        fun stages(createStagesRequest: CreateStagesRequest): Builder {
            createStagesRequest.let {
                val projectId = "_${it.userId}"
                this.stages = PipelineLayout(
                    projectId = projectId,
                    userId = it.userId,
                    variables = it.scriptBuildYaml.variables,
                    v2Stages = it.scriptBuildYaml.stages,
                    finallyJobs = it.scriptBuildYaml.finally,
                    agentId = it.agentInfo.agentId,
                    agentOS = it.agentInfo.os,
                    userLocalProjectInfo = it.startUpReq,
                    channelCode = it.channelCode
                ).generateStages()
            }

            return this
        }

        /**
         * 空流水线阶段，仅保留触发器
         */
        fun stagesEmpty(): Builder {
            val stageList = mutableListOf<Stage>()
            val triggerContainer = TriggerContainer(
                id = "0",
                name = MessageUtil.getMessageByLocale(
                    messageCode = PreBuildCode.BK_BUILD_TRIGGER,
                    language = I18nUtil.getLanguage()
                ),
                elements = listOf(ManualTriggerElement(
                    MessageUtil.getMessageByLocale(
                        messageCode = PreBuildCode.BK_MANUAL_TRIGGER,
                        language = I18nUtil.getLanguage()
                ), "T-1-1-1")),
                params = emptyList()
            )
            stageList.add(Stage(listOf(triggerContainer), "stage-1"))
            this.stages = stageList

            return this
        }

        /**
         * 生成流水线编排模型
         */
        fun build(): Model {
            if (pipelineName.isBlank() || creator.isBlank()) {
                throw CustomException(Response.Status.BAD_REQUEST, MessageUtil.getMessageByLocale(
                    messageCode = BK_PIPELINE_NAME_CREATOR_CANNOT_EMPTY,
                    language = I18nUtil.getLanguage()
                ))
            }

            if (stages.isEmpty()) {
                throw CustomException(Response.Status.BAD_REQUEST,
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_PIPELINE_MUST_AT_LEAST_ONE,
                        language = I18nUtil.getLanguage()
                    ))
            }

            return Model(
                name = pipelineName,
                desc = description,
                stages = stages,
                labels = labels,
                pipelineCreator = creator,
                instanceFromTemplate = false
            )
        }
    }
}
