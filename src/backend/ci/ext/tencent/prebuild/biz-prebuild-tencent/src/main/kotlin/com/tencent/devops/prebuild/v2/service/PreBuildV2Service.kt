package com.tencent.devops.prebuild.v2.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.image.BuildType
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.image.PoolType
import com.tencent.devops.common.ci.task.CodeCCScanInContainerTask
import com.tencent.devops.common.ci.v2.Container2
import com.tencent.devops.common.ci.v2.IfType
import com.tencent.devops.common.ci.v2.Job
import com.tencent.devops.common.ci.v2.JobRunsOnType
import com.tencent.devops.common.ci.v2.PreJob
import com.tencent.devops.common.ci.v2.PreScriptBuildYaml
import com.tencent.devops.common.ci.v2.RunsOn
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildScriptType
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
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.dao.PrebuildProjectDao
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.prebuild.service.CommonPreBuildService
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildId
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response
import com.tencent.devops.common.ci.v2.Stage as V2Stage

@Service
class PreBuildV2Service @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val prebuildProjectDao: PrebuildProjectDao
) : CommonPreBuildService(client, dslContext, prebuildProjectDao) {
    companion object {
        private val logger = LoggerFactory.getLogger(PreBuildV2Service::class.java)
        const val VARIABLE_PREFIX = "variables."
    }

    fun checkYamlSchema(originYaml: String): Result<String> {
        return try {
            checkYamlSchemaCore(originYaml)
            Result("OK")
        } catch (e: Exception) {
            logger.error("Check yaml schema failed.", e)
            Result(1, "Invalid yaml: ${e.message}")
        }
    }

    fun checkYamlSchemaCore(originYaml: String): PreScriptBuildYaml {
        val formatYamlStr = ScriptYmlUtils.formatYaml(originYaml)
        val yamlJsonStr = ScriptYmlUtils.convertYamlToJson(formatYamlStr)
        val yamlSchema = getYamlSchema()
        val (schemaPassed, errorMessage) = ScriptYmlUtils.validate(
            schema = yamlSchema,
            yamlJson = yamlJsonStr
        )
        // 整体schema校验
        if (!schemaPassed) {
            logger.error("Check yaml schema failed. $errorMessage")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, errorMessage)
        }

        val preScriptBuildYaml = YamlUtil
            .getObjectMapper().readValue(formatYamlStr, PreScriptBuildYaml::class.java)
        checkYamlBusiness(preScriptBuildYaml, originYaml)

        return preScriptBuildYaml
    }

    fun startBuild(
        userId: String,
        preProjectId: String,
        startUpReq: StartUpReq,
        agentId: ThirdPartyAgentStaticInfo
    ): BuildId {
        val preYamlObject = try {
            checkYamlSchemaCore(startUpReq.yaml)
        } catch (e: Exception) {
            logger.error("Invalid yml preci 2.0", e)
            throw CustomException(Response.Status.BAD_REQUEST, "YAML非法: ${e.message}")
        }

        val scriptBuildYaml = ScriptYmlUtils.normalizePreCiYaml(preYamlObject)
        val model = getPipelineModel(userId, preProjectId, startUpReq, scriptBuildYaml, agentId)
        val pipelineId = createOrUpdatePipeline(userId, preProjectId, startUpReq, model)
        val projectId = getUserProjectId(userId)

        val buildId = client.get(ServiceBuildResource::class)
            .manualStartup(userId, projectId, pipelineId, mapOf(), channelCode).data!!.id

        return BuildId(buildId)
    }

    private fun getPipelineModel(
        userId: String,
        preProjectId: String,
        startUpReq: StartUpReq,
        yamlObject: ScriptBuildYaml,
        agentInfo: ThirdPartyAgentStaticInfo
    ): Model {
        val buildFormProperties = mutableListOf<BuildFormProperty>()
        if (yamlObject.variables != null && yamlObject.variables!!.isNotEmpty()) {
            val startParams = mutableMapOf<String, String>()
            yamlObject.variables!!.forEach { (key, variable) ->
                startParams[VARIABLE_PREFIX + key] =
                    variable.copy(value = formatVariablesValue(variable.value, startParams)).value
                        ?: ""
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
                buildFormProperties.add(property)
            }
        }

        val stageList = mutableListOf<Stage>()
        // 第一个stage，触发类
        val manualTriggerElement = ManualTriggerElement("手动触发", "T-1-1-1")
        val triggerContainer = TriggerContainer(
            id = "0",
            name = "构建触发",
            elements = listOf(manualTriggerElement),
            params = buildFormProperties
        )
        val stage1 = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(stage1)

        // 其他的stage
        yamlObject.stages!!.forEachIndexed { stageIndex, stage ->
            stageList.add(
                createStage(
                    stage = stage,
                    startUpReq = startUpReq,
                    agentInfo = agentInfo,
                    userId = userId,
                    stageIndex = stageIndex + 2,
                    finalStage = false
                )
            )
        }

        // 添加finally
        if (!yamlObject.finally.isNullOrEmpty()) {
            stageList.add(
                createStage(
                    stage = V2Stage(
                        name = "Finally",
                        id = null,
                        label = emptyList(),
                        ifField = null,
                        fastKill = false,
                        jobs = yamlObject.finally!!
                    ),
                    startUpReq = startUpReq,
                    agentInfo = agentInfo,
                    userId = userId,
                    finalStage = true
                )
            )
        }

        return Model(
            name = preProjectId,
            desc = "",
            stages = stageList,
            labels = emptyList(),
            instanceFromTemplate = false,
            pipelineCreator = userId
        )
    }

    private fun createStage(
        stage: V2Stage,
        startUpReq: StartUpReq,
        agentInfo: ThirdPartyAgentStaticInfo,
        userId: String,
        stageIndex: Int = 0,
        finalStage: Boolean = false
    ): Stage {
        val containerList = mutableListOf<Container>()
        stage.jobs.forEachIndexed { jobIndex, job ->
            val elementList = makeElementList(job, startUpReq, agentInfo, userId)

            if (JobRunsOnType.AGENT_LESS.type == job.runsOn.poolName) {
                // 无编译环境
                val normalContainer = NormalContainer(
                    containerId = null,
                    id = job.id,
                    name = "无编译环境",
                    elements = elementList,
                    status = null,
                    startEpoch = null,
                    systemElapsed = null,
                    elementElapsed = null,
                    enableSkip = false,
                    conditions = null,
                    canRetry = false,
                    jobControlOption = getJobControlOption(job, finalStage),
                    mutexGroup = null
                )
                containerList.add(normalContainer)
            } else {
                // nfs挂载
                val buildEnv = if (JobRunsOnType.DOCKER.type == job.runsOn.poolName
                    || JobRunsOnType.DEV_CLOUD.type == job.runsOn.poolName
                ) {
                    job.runsOn.nfsMount
                } else {
                    null
                }

                val vmContainer = VMBuildContainer(
                    jobId = job.id,
                    name = "Job_${jobIndex + 1} " + (job.name ?: ""),
                    elements = elementList,
                    status = null,
                    startEpoch = null,
                    systemElapsed = null,
                    elementElapsed = null,
                    baseOS = getBaseOs(job, agentInfo),
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
                    jobControlOption = getJobControlOption(job, finalStage),
                    dispatchType = getDispatchType(job, startUpReq, agentInfo)
                )
                containerList.add(vmContainer)
            }
        }

        // 根据if设置stageController
        var stageControlOption = StageControlOption()
        if (!finalStage && !stage.ifField.isNullOrBlank()) {
            stageControlOption = StageControlOption(
                runCondition = StageRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = stage.ifField.toString()
            )
        }

        return Stage(
            id = stage.id,
            name = stage.name ?: if (finalStage) {
                "Final"
            } else {
                "Stage-$stageIndex"
            },
            tag = stage.label,
            fastKill = stage.fastKill,
            stageControlOption = stageControlOption,
            containers = containerList,
            finally = finalStage
        )
    }

    private fun makeElementList(
        job: Job,
        startUpReq: StartUpReq,
        agentInfo: ThirdPartyAgentStaticInfo,
        userId: String
    ): MutableList<Element> {
        val elementList = mutableListOf<Element>()
        job.steps!!.forEach { step ->
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
            val element: Element = when {
                step.run != null -> {
                    val linux = LinuxScriptElement(
                        name = step.name ?: "run",
                        id = step.id,
                        scriptType = BuildScriptType.SHELL,
                        script = step.run!!,
                        continueNoneZero = false,
                        additionalOptions = additionalOptions
                    )
                    if (job.runsOn.agentSelector.isNullOrEmpty()) {
                        linux
                    } else {
                        when (job.runsOn.agentSelector!!.first()) {
                            "linux" -> linux
                            "macos" -> linux
                            "windows" -> WindowsScriptElement(
                                name = step.name ?: "run",
                                id = step.id,
                                scriptType = BuildScriptType.BAT,
                                script = step.run!!
                            )
                            else -> linux
                        }
                    }
                }
                else -> {
                    val data = mutableMapOf<String, Any>()
                    val atomCode = step.uses!!.split('@')[0]

                    // 代码同步
                    if (atomCode == "syncCodeToRemote") {
                        // 确保同步代码插件安装
                        installMarketAtom(userId, "syncCodeToRemote")
                        val input = step.with?.toMutableMap() ?: mutableMapOf()
                        input["agentId"] = input["agentId"] ?: agentInfo.agentId
                        input["workspace"] = input["workspace"] ?: startUpReq.workspace
                        input["useDelete"] = input["useDelete"] ?: true
                        input["syncGitRepository"] = input["syncGitRepository"] ?: false
                        data["input"] = input

                        MarketBuildAtomElement(
                            name = step.name ?: "同步本地代码",
                            id = null,
                            atomCode = "syncAgentCode",
                            version = "3.*",
                            data = data,
                            additionalOptions = additionalOptions
                        )
                    } else {
                        data["input"] = step.with ?: Any()

                        // codecc插件路径转换
                        if (startUpReq.extraParam != null
                            && atomCode == CodeCCScanInContainerTask.atomCode
                        ) {
                            val input = (data["input"] as Map<*, *>).toMutableMap()
                            val isRunOnDocker = JobRunsOnType.DEV_CLOUD.type == job.runsOn.poolName
                                    || JobRunsOnType.DOCKER.type == job.runsOn.poolName
                            input["path"] = getWhitePath(startUpReq, isRunOnDocker)
                        }

                        MarketBuildAtomElement(
                            name = step.name ?: step.uses!!.split('@')[0],
                            id = step.id,
                            atomCode = step.uses!!.split('@')[0],
                            version = step.uses!!.split('@')[1],
                            data = data,
                            additionalOptions = additionalOptions
                        )
                    }
                }
            }

            elementList.add(element)

            if (element is MarketBuildAtomElement) {
                logger.info("install market atom: ${element.getAtomCode()}")
                installMarketAtom(userId, element.getAtomCode())
            }
        }

        return elementList
    }

    private fun getJobControlOption(
        job: Job,
        finalStage: Boolean = false
    ): JobControlOption {
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

    private fun getContainerPool(job: Job): Pool {
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
                    com.tencent.devops.common.ci.v2.Container::class.java
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

    private fun getDispatchType(
        job: Job,
        startUpReq: StartUpReq,
        agentInfo: ThirdPartyAgentStaticInfo
    ): DispatchType {
        if (job.runsOn.poolName == JobRunsOnType.LOCAL.type) {
            return ThirdPartyAgentIDDispatchType(
                displayName = agentInfo.agentId,
                workspace = startUpReq.workspace,
                agentType = AgentType.ID
            )
        }

        // 第三方构建机
        if (job.runsOn.selfHosted == true) {
            return ThirdPartyAgentEnvDispatchType(
                envName = job.runsOn.poolName,
                workspace = "",
                agentType = AgentType.NAME
            )
        }

        val containerPool = getContainerPool(job)
        return when (job.runsOn.poolName) {
            JobRunsOnType.DEV_CLOUD.type -> {
                PoolType.DockerOnDevCloud.toDispatchType(containerPool)
            }
            JobRunsOnType.DOCKER.type -> {
                PoolType.DockerOnVm.toDispatchType(containerPool)
            }
            else -> {
                throw CustomException(Response.Status.NOT_FOUND, "公共构建资源池不存在，请检查yml配置.")
            }
        }
    }

    private fun getBaseOs(job: Job, agentInfo: ThirdPartyAgentStaticInfo): VMBaseOS {
        if (job.runsOn.poolName == JobRunsOnType.LOCAL.type) {
            return VMBaseOS.valueOf(agentInfo.os)
        }

        if (job.runsOn.poolName.startsWith("macos")) {
            return VMBaseOS.MACOS
        }

        if (job.runsOn.agentSelector.isNullOrEmpty()) {
            return VMBaseOS.ALL
        }

        return when (job.runsOn.agentSelector!![0]) {
            "linux" -> VMBaseOS.LINUX
            "macos" -> VMBaseOS.MACOS
            "windows" -> VMBaseOS.WINDOWS
            else -> VMBaseOS.LINUX
        }
    }

    private fun formatVariablesValue(
        value: String?,
        startParams: MutableMap<String, String>
    ): String? {
        if (value == null || value.isEmpty()) {
            return ""
        }
        val settingMap = mutableMapOf<String, String>()
        settingMap.putAll(startParams)
        return ScriptYmlUtils.parseVariableValue(value, settingMap)
    }

    private fun getYamlSchema(): String {
        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        val schemaGenerator = JsonSchemaGenerator(mapper)
        val schema = schemaGenerator.generateSchema(PreScriptBuildYaml::class.java)
        with(schema) {
            `$schema` = "http://json-schema.org/draft-03/schema#"
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    }

    private fun getRunsOnYamlSchema(): String {
        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
        val schemaGenerator = JsonSchemaGenerator(mapper)
        val schema = schemaGenerator.generateSchema(RunsOn::class.java)
        with(schema) {
            `$schema` = "http://json-schema.org/draft-03/schema#"
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)
    }

    private fun checkYamlBusiness(preScriptBuildYaml: PreScriptBuildYaml, originYaml: String) {
        checkRunsOn(preScriptBuildYaml)
        checkVariable(preScriptBuildYaml)
        checkStage(preScriptBuildYaml)
        checkExtend(originYaml)
    }

    private fun checkRunsOn(preScriptBuildYaml: PreScriptBuildYaml) {
        val preJobList = mutableListOf<PreJob>()
        if (!preScriptBuildYaml.stages.isNullOrEmpty()) {
            preScriptBuildYaml.stages!!.forEach { stage ->
                if (!stage.jobs.isNullOrEmpty()) {
                    preJobList.addAll(stage.jobs!!.values)
                }
            }
        }
        if (!preScriptBuildYaml.jobs.isNullOrEmpty()) {
            preJobList.addAll(preScriptBuildYaml.jobs!!.values)
        }

        if (preJobList.isNullOrEmpty()) {
            return
        }

        val runsOnYamlSchema = getRunsOnYamlSchema()
        val objectMapper = ObjectMapper()

        preJobList.forEach { preJob ->
            if (preJob.runsOn == null || preJob.runsOn!! is String || preJob.runsOn!! is List<*>) {
                return@forEach
            }

            val (passed, errMsg) = ScriptYmlUtils.validate(
                schema = runsOnYamlSchema,
                yamlJson = objectMapper.writeValueAsString(preJob.runsOn)
            )

            if (!passed) {
                logger.error("Check yaml schema failed [runs-on]. $errMsg")
                throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, errMsg)
            }
        }
    }

    private fun checkStage(preScriptBuildYaml: PreScriptBuildYaml) {
        if ((preScriptBuildYaml.stages != null && preScriptBuildYaml.jobs != null) ||
            (preScriptBuildYaml.stages != null && preScriptBuildYaml.steps != null) ||
            (preScriptBuildYaml.jobs != null && preScriptBuildYaml.steps != null)
        ) {
            throw CustomException(
                Response.Status.BAD_REQUEST, "stages, jobs, steps不能并列存在，只能存在其一"
            )
        }
    }

    private fun checkVariable(preScriptBuildYaml: PreScriptBuildYaml) {
        if (preScriptBuildYaml.variables == null) {
            return
        }
        preScriptBuildYaml.variables!!.forEach {
            val keyRegex = Regex("^[0-9a-zA-Z_]+$")
            if (!keyRegex.matches(it.key)) {
                throw CustomException(
                    Response.Status.BAD_REQUEST, "变量名称必须是英文字母、数字或下划线(_)"
                )
            }
        }
    }

    private fun checkExtend(yaml: String) {
        val yamlMap = YamlUtil.getObjectMapper()
            .readValue(yaml, object : TypeReference<Map<String, Any?>>() {})
        if (yamlMap["extends"] == null) {
            return
        }
        yamlMap.forEach { (t, _) ->
            if (t != "triggerOn" && t != "extends" && t != "version"
                && t != "resources" && t != "name" && t != "on"
            ) {
                throw CustomException(
                    status = Response.Status.BAD_REQUEST,
                    message = "使用 extends 时顶级关键字只能有触发器 on 与 resources"
                )
            }
        }
    }
}
