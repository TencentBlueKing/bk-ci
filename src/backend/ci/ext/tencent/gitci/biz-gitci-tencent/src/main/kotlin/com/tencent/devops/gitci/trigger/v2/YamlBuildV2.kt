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

package com.tencent.devops.gitci.trigger.v2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.EmojiUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.common.ci.OBJECT_KIND_TAG_PUSH
import com.tencent.devops.common.ci.image.BuildType
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.task.DockerRunDevCloudTask
import com.tencent.devops.common.ci.task.GitCiCodeRepoTask
import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput
import com.tencent.devops.common.ci.task.ServiceJobDevCloudTask
import com.tencent.devops.common.ci.v2.Container2
import com.tencent.devops.common.ci.v2.IfType
import com.tencent.devops.common.ci.v2.Job
import com.tencent.devops.common.ci.v2.JobRunsOnType
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.v2.Step
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.NameAndValue
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
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.gitci.GitCIDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_BASE_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_COMMIT_MESSAGE
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_EVENT_CONTENT
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_HEAD_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_MR_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REF
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_GROUP
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_NAME
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_REPO_URL
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA
import com.tencent.devops.common.pipeline.utils.PIPELINE_GIT_SHA_SHORT
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.gitci.dao.GitCIServicesConfDao
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.pojo.BuildConfig
import com.tencent.devops.gitci.pojo.GitCITriggerLock
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.git.GitPushEvent
import com.tencent.devops.gitci.pojo.git.GitTagPushEvent
import com.tencent.devops.gitci.pojo.v2.GitCIBasicSetting
import com.tencent.devops.common.ci.v2.Stage as GitCIV2Stage
import com.tencent.devops.gitci.utils.GitCIPipelineUtils
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.gitci.v2.common.CommonVariables.CI_ACTOR
import com.tencent.devops.gitci.v2.common.CommonVariables.CI_BRANCH
import com.tencent.devops.gitci.v2.common.CommonVariables.CI_BUILD_URL
import com.tencent.devops.gitci.v2.common.CommonVariables.CI_PIPELINE_NAME
import com.tencent.devops.gitci.v2.dao.GitCIBasicSettingDao
import com.tencent.devops.gitci.trigger.GitCIEventService
import com.tencent.devops.gitci.v2.service.GitCIV2WebsocketService
import com.tencent.devops.gitci.v2.service.GitPipelineBranchService
import com.tencent.devops.gitci.v2.service.OauthService
import com.tencent.devops.process.api.user.UserPipelineGroupResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_EVENT_TYPE
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_SOURCE_URL
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.process.utils.PIPELINE_WEBHOOK_TARGET_URL
import com.tencent.devops.scm.pojo.BK_CI_RUN
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.ticket.pojo.enums.CredentialType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Suppress("ALL")
@Service
class YamlBuildV2 @Autowired constructor(
    private val client: Client,
    private val kafkaClient: KafkaClient,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val gitCIBasicSettingDao: GitCIBasicSettingDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val scmClient: ScmClient,
    private val redisOperation: RedisOperation,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val oauthService: OauthService,
    private val gitCIEventSaveService: GitCIEventService,
    private val websocketService: GitCIV2WebsocketService,
    private val gitCISettingDao: GitCISettingDao,
    private val gitPipelineBranchService: GitPipelineBranchService
) : YamlBaseBuildV2<ScriptBuildYaml>(
    client, kafkaClient, scmClient, dslContext, gitPipelineResourceDao,
    gitRequestEventBuildDao, gitCIEventSaveService, websocketService, gitPipelineBranchService
) {

    @Value("\${rtx.v2GitUrl:#{null}}")
    private val v2GitUrl: String? = null

    private val channelCode = ChannelCode.GIT

    companion object {
        private val logger = LoggerFactory.getLogger(YamlBuildV2::class.java)
        private const val ymlVersion = "v2.0"
        const val BK_REPO_GIT_WEBHOOK_MR_IID = "BK_CI_REPO_GIT_WEBHOOK_MR_IID"
        const val VARIABLE_PREFIX = "variables."
    }

    override fun gitStartBuild(
        pipeline: GitProjectPipeline,
        event: GitRequestEvent,
        yaml: ScriptBuildYaml,
        originYaml: String,
        parsedYaml: String?,
        normalizedYaml: String,
        gitBuildId: Long?
    ): BuildId? {
        val triggerLock = GitCITriggerLock(
            redisOperation = redisOperation,
            gitProjectId = event.gitProjectId,
            pipelineId = pipeline.pipelineId
        )
        try {
            triggerLock.lock()
            // 如果事件未传gitBuildId说明是不做触发只做流水线保存
            return if (gitBuildId != null) {
                startBuildPipeline(
                    pipeline = pipeline,
                    event = event,
                    yaml = yaml,
                    gitBuildId = gitBuildId
                )
            } else {
                savePipelineModel(
                    pipeline = pipeline,
                    event = event,
                    yaml = yaml
                )
                null
            }
        } catch (e: Throwable) {
            logger.error("Fail to start the git ci build($event)", e)
            gitCIEventSaveService.saveRunNotBuildEvent(
                userId = event.userId,
                eventId = event.id!!,
                originYaml = originYaml,
                parsedYaml = parsedYaml,
                normalizedYaml = normalizedYaml,
                reason = TriggerReason.PIPELINE_PREPARE_ERROR.name,
                reasonDetail = TriggerReason.PIPELINE_PREPARE_ERROR.detail.format(e.message),
                pipelineId = pipeline.pipelineId,
                pipelineName = pipeline.displayName,
                filePath = pipeline.filePath,
                gitProjectId = event.gitProjectId,
                sendCommitCheck = true,
                commitCheckBlock = (event.objectKind == OBJECT_KIND_MERGE_REQUEST),
                version = ymlVersion
            )
            return null
        } finally {
            triggerLock.unlock()
        }
    }

    fun startBuildPipeline(
        pipeline: GitProjectPipeline,
        event: GitRequestEvent,
        yaml: ScriptBuildYaml,
        gitBuildId: Long
    ): BuildId? {
        logger.info("Git request gitBuildId:$gitBuildId, pipeline:$pipeline, event: $event, yaml: $yaml")

        // create or refresh pipeline
        val gitBasicSetting = gitCIBasicSettingDao.getSetting(dslContext, event.gitProjectId)
            ?: throw OperationException("git ci projectCode not exist")

        val model = createPipelineModel(event, gitBasicSetting, yaml)
        logger.info("Git request gitBuildId:$gitBuildId, pipeline:$pipeline, model: $model")
        savePipeline(pipeline, event, gitBasicSetting, model)
        return startBuild(pipeline, event, gitBasicSetting, model, gitBuildId)
    }

    fun savePipelineModel(
        pipeline: GitProjectPipeline,
        event: GitRequestEvent,
        yaml: ScriptBuildYaml
    ) {
        logger.info("Git request save pipeline, pipeline:$pipeline, event: $event, yaml: $yaml")

        // create or refresh pipeline
        val gitBasicSetting = gitCIBasicSettingDao.getSetting(dslContext, event.gitProjectId)
            ?: throw OperationException("git ci projectCode not exist")

        val model = createPipelineModel(event, gitBasicSetting, yaml)
        logger.info("Git request , pipeline:$pipeline, model: $model")
        savePipeline(pipeline, event, gitBasicSetting, model)
    }

    private fun createPipelineSetting(
        event: GitRequestEvent,
        pipelineId: String,
        landunProjectId: String,
        yaml: ScriptBuildYaml
    ): PipelineSetting {
        yaml.notices
        return PipelineSetting(
            projectId = landunProjectId,
            pipelineId = pipelineId,
            failSubscription = Subscription()
        )
    }

    private fun createPipelineModel(
        event: GitRequestEvent,
        gitBasicSetting: GitCIBasicSetting,
        yaml: ScriptBuildYaml
    ): Model {
        // 流水线插件标签设置
        val labelList = preparePipelineLabels(event, gitBasicSetting, yaml)

        // 预安装插件市场的插件
        installMarketAtom(gitBasicSetting, event.userId, GitCiCodeRepoTask.atomCode)
        installMarketAtom(gitBasicSetting, event.userId, DockerRunDevCloudTask.atomCode)
        installMarketAtom(gitBasicSetting, event.userId, ServiceJobDevCloudTask.atomCode)

        val stageList = mutableListOf<Stage>()

        // 第一个stage，触发类，可能会包含定时触发
        val triggerElementList = mutableListOf<Element>()
        val manualTriggerElement = ManualTriggerElement("手动触发", "T-1-1-1")
        triggerElementList.add(manualTriggerElement)

        if (yaml.triggerOn?.schedules != null &&
            yaml.triggerOn?.schedules!!.cron != null
        ) {
            val timerTrigger = TimerTriggerElement(
                id = "T-1-1-2",
                name = "定时触发",
                advanceExpression = listOf(
                    yaml.triggerOn!!.schedules!!.cron!!
                )
            )
            timerTrigger.additionalOptions = ElementAdditionalOptions(
                runCondition = RunCondition.PRE_TASK_SUCCESS
            )
            triggerElementList.add(timerTrigger)
        }
        val params = createPipelineParams(yaml, gitBasicSetting, event)
        val triggerContainer = TriggerContainer(
            id = "0",
            name = "构建触发",
            elements = triggerElementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = params
        )

        val stage1 = Stage(listOf(triggerContainer), id = "stage-0", name = "Stage-0")
        stageList.add(stage1)

        // 其他的stage
        yaml.stages.forEachIndexed { stageIndex, stage ->
            stageList.add(createStage(
                stage = stage,
                event = event,
                gitBasicSetting = gitBasicSetting,
                stageIndex = stageIndex + 1
            ))
        }
        // 添加finally
        if (!yaml.finally.isNullOrEmpty()) {
            stageList.add(
                createStage(
                    stage = GitCIV2Stage(
                        name = "Finally",
                        id = null,
                        label = emptyList(),
                        ifField = null,
                        fastKill = false,
                        jobs = yaml.finally!!
                    ),
                    event = event,
                    gitBasicSetting = gitBasicSetting,
                    finalStage = true)
            )
        }

        return Model(
            name = GitCIPipelineUtils.genBKPipelineName(gitBasicSetting.gitProjectId),
            desc = "",
            stages = stageList,
            labels = labelList,
            instanceFromTemplate = false,
            pipelineCreator = event.userId
        )
    }

    private fun preparePipelineLabels(
        event: GitRequestEvent,
        gitBasicSetting: GitCIBasicSetting,
        yaml: ScriptBuildYaml
    ): List<String> {
        val gitCIPipelineLabels = mutableListOf<String>()

        try {
            // 获取当前项目下存在的标签组
            val pipelineGroups = client.get(UserPipelineGroupResource::class)
                .getGroups(event.userId, gitBasicSetting.projectCode!!)
                .data

            yaml.label?.forEach {
                // 要设置的标签组不存在，新建标签组和标签（同名）
                if (!checkPipelineLabel(it, pipelineGroups)) {
                    client.get(UserPipelineGroupResource::class).addGroup(
                        event.userId, PipelineGroupCreate(
                        projectId = gitBasicSetting.projectCode!!,
                        name = it
                    )
                    )

                    val pipelineGroup = getPipelineGroup(it, event.userId, gitBasicSetting.projectCode!!)
                    if (pipelineGroup != null) {
                        client.get(UserPipelineGroupResource::class).addLabel(
                            event.userId, PipelineLabelCreate(
                            groupId = pipelineGroup.id,
                            name = it
                        )
                        )
                    }
                }

                // 保证标签已创建成功后，取label加密ID
                val pipelineGroup = getPipelineGroup(it, event.userId, gitBasicSetting.projectCode!!)
                gitCIPipelineLabels.add(pipelineGroup!!.labels[0].id)
            }
        } catch (e: Exception) {
            logger.error("${event.userId}|${gitBasicSetting.projectCode!!} preparePipelineLabels error.", e)
        }

        return gitCIPipelineLabels
    }

    private fun checkPipelineLabel(gitciPipelineLabel: String, pipelineGroups: List<PipelineGroup>?): Boolean {
        pipelineGroups?.forEach { pipelineGroup ->
            pipelineGroup.labels.forEach {
                if (it.name == gitciPipelineLabel) {
                    return true
                }
            }
        }

        return false
    }

    private fun getPipelineGroup(labelGroupName: String, userId: String, projectId: String): PipelineGroup? {
        val pipelineGroups = client.get(UserPipelineGroupResource::class)
            .getGroups(userId, projectId)
            .data
        pipelineGroups?.forEach {
            if (it.name == labelGroupName) {
                return it
            }
        }

        return null
    }

    private fun createStage(
        stage: GitCIV2Stage,
        event: GitRequestEvent,
        gitBasicSetting: GitCIBasicSetting,
        stageIndex: Int = 0,
        finalStage: Boolean = false
    ): Stage {
        val containerList = mutableListOf<Container>()
        stage.jobs.forEachIndexed { jobIndex, job ->
            val elementList = makeElementList(job, gitBasicSetting, event)
            if (job.runsOn.poolName == JobRunsOnType.AGENT_LESS.type) {
                addNormalContainer(job, elementList, containerList, jobIndex, finalStage)
            } else {
                addVmBuildContainer(
                    job = job,
                    elementList = elementList,
                    containerList = containerList,
                    jobIndex = jobIndex,
                    projectCode = gitBasicSetting.projectCode!!,
                    finalStage = finalStage
                )
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

    private fun addVmBuildContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        projectCode: String,
        finalStage: Boolean = false
    ) {
        val vmContainer = VMBuildContainer(
            jobId = job.id,
            name = job.name ?: "Job-${jobIndex + 1}",
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = getBaseOs(job),
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = job.timeoutMinutes ?: 900,
            buildEnv = null,
            customBuildEnv = job.env,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            jobControlOption = getJobControlOption(job, finalStage),
            dispatchType = getDispatchType(job, projectCode)
        )
        containerList.add(vmContainer)
    }

    private fun getBaseOs(job: Job): VMBaseOS {
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

    fun getDispatchType(job: Job, projectCode: String): DispatchType {
        // macos构建机
        if (job.runsOn.poolName.startsWith("macos")) {
            return MacOSDispatchType(
                macOSEvn = "Catalina10.15.4:12.2",
                systemVersion = "Catalina10.15.4",
                xcodeVersion = "12.2"
            )
        }

        // 第三方构建机
        if (job.runsOn.selfHosted == true) {
            return ThirdPartyAgentEnvDispatchType(
                envName = job.runsOn.poolName,
                workspace = job.runsOn.workspace,
                agentType = AgentType.NAME
            )
        }

        // 公共docker构建机
        if (job.runsOn.poolName == "docker") {
            var containerPool = Pool(
                container = "http://mirrors.tencent.com/ci/tlinux3_ci:0.1.1.0",
                credential = Credential(
                    user = "",
                    password = ""
                ),
                macOS = null,
                third = null,
                env = job.env,
                buildType = BuildType.DOCKER_VM
            )

            if (job.runsOn.container != null) {
                try {
                    val container = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(job.runsOn.container!!),
                        com.tencent.devops.common.ci.v2.Container::class.java
                    )

                    containerPool = Pool(
                        container = container.image,
                        credential = Credential(
                            user = container.credentials?.username ?: "",
                            password = container.credentials?.password ?: ""
                        ),
                        macOS = null,
                        third = null,
                        env = job.env,
                        buildType = BuildType.DOCKER_VM
                    )
                } catch (e: Exception) {
                    val container = YamlUtil.getObjectMapper().readValue(
                        JsonUtil.toJson(job.runsOn.container!!),
                        Container2::class.java
                    )

                    var user = ""
                    var password = ""
                    if (!container.credentials.isNullOrEmpty()) {
                        val ticketsMap = GitCommonUtils.getCredential(
                            client = client,
                            projectId = projectCode,
                            credentialId = container.credentials ?: "",
                            type = CredentialType.USERNAME_PASSWORD
                        )
                        user = ticketsMap["v1"] as String
                        password = ticketsMap["v2"] as String
                    }

                    containerPool = Pool(
                        container = container.image,
                        credential = Credential(
                            user = user,
                            password = password
                        ),
                        macOS = null,
                        third = null,
                        env = job.env,
                        buildType = BuildType.DOCKER_VM
                    )
                }
            }

            return GitCIDispatchType(objectMapper.writeValueAsString(containerPool))
        }

        throw CustomException(Response.Status.NOT_FOUND, "公共构建资源池不存在，请检查yml配置.")
    }

    private fun addNormalContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int,
        finalStage: Boolean = false
    ) {

        containerList.add(
            NormalContainer(
                jobId = job.id,
                containerId = null,
                id = job.id,
                name = job.name ?: "Job-${jobIndex + 1}",
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
        )
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

    private fun makeElementList(
        job: Job,
        gitBasicSetting: GitCIBasicSetting,
        event: GitRequestEvent
    ): MutableList<Element> {
        // 解析service
        val elementList = makeServiceElementList(job)

        // 解析job steps
        job.steps!!.forEach { step ->
            val additionalOptions = ElementAdditionalOptions(
                continueWhenFailed = step.continueOnError ?: false,
                timeout = step.timeoutMinutes?.toLong(),
                retryWhenFailed = step.retryTimes != null,
                retryCount = step.retryTimes ?: 0,
                enableCustomEnv = step.env != null,
                customEnv = getElementEnv(step.env),
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
                step.checkout != null -> {
                    makeCheckoutElement(step, gitBasicSetting, event).copy(additionalOptions = additionalOptions)
                }
                else -> {
                    val data = mutableMapOf<String, Any>()
                    data["input"] = step.with ?: Any()
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

            elementList.add(element)

            if (element is MarketBuildAtomElement) {
                logger.info("install market atom: ${element.getAtomCode()}")
                installMarketAtom(gitBasicSetting, event.userId, element.getAtomCode())
            }
        }

        return elementList
    }

    private fun getElementEnv(env: Map<String, Any?>?): List<NameAndValue>? {
        if (env == null) {
            return null
        }

        val nameAndValueList = mutableListOf<NameAndValue>()
        env.forEach {
            nameAndValueList.add(
                NameAndValue(
                key = it.key,
                value = it.value.toString()
            ))
        }

        return nameAndValueList
    }

    private fun makeCheckoutElement(
        step: Step,
        gitBasicSetting: GitCIBasicSetting,
        event: GitRequestEvent
    ): MarketBuildAtomElement {
        // checkout插件装配
        val inputMap = mutableMapOf<String, Any?>()
        if (!step.with.isNullOrEmpty()) {
            inputMap.putAll(step.with!!)
        }
        // 非mr和tag触发下根据commitId拉取本地工程代码
        if (step.checkout == "self") {
            inputMap["authUserId"] = gitBasicSetting.enableUserId
            inputMap["repositoryUrl"] = if (gitBasicSetting.gitHttpUrl.isBlank()) {
                gitCISettingDao.getSetting(dslContext, gitBasicSetting.gitProjectId)?.gitHttpUrl
            } else {
                gitBasicSetting.gitHttpUrl
            }
            inputMap["authType"] = "AUTH_USER_TOKEN"

            when (event.objectKind) {
                OBJECT_KIND_MERGE_REQUEST ->
                    inputMap["pullType"] = "BRANCH"
                OBJECT_KIND_TAG_PUSH -> {
                    inputMap["pullType"] = "TAG"
                    inputMap["refName"] = event.branch
                }
                OBJECT_KIND_MANUAL -> {
                    if (event.commitId.isNotBlank()) {
                        inputMap["pullType"] = "COMMIT_ID"
                        inputMap["refName"] = event.commitId
                    } else {
                        inputMap["pullType"] = "BRANCH"
                        inputMap["refName"] = event.branch
                    }
                }
                else -> {
                    inputMap["pullType"] = "COMMIT_ID"
                    inputMap["refName"] = event.commitId
                }
            }
        } else {
            inputMap["repositoryUrl"] = step.checkout!!
            if (step.with == null || (step.with != null && !step.with!!.containsKey("authType"))) {
                inputMap["authUserId"] = gitBasicSetting.enableUserId
                inputMap["authType"] = "AUTH_USER_TOKEN"
            }
        }

        // 拼装插件固定参数
        inputMap["repositoryType"] = "URL"

        val data = mutableMapOf<String, Any>()
        data["input"] = inputMap

        return MarketBuildAtomElement(
            name = step.name ?: "checkout",
            id = step.id,
            atomCode = "checkout",
            version = "1.*",
            data = data
        )
    }

    private fun makeServiceElementList(job: Job): MutableList<Element> {
        val elementList = mutableListOf<Element>()

        // 解析services
        if (job.services != null) {
            job.services!!.forEach {
                val (imageName, imageTag) = ScriptYmlUtils.parseServiceImage(it.image)

                val record = gitServicesConfDao.get(dslContext, imageName, imageTag)
                    ?: throw RuntimeException("Git CI没有此镜像版本记录. ${it.image}")
                if (!record.enable) {
                    throw RuntimeException("镜像版本不可用")
                }

                val params = if (it.with.password.isNullOrBlank()) {
                    "{\"env\":{\"MYSQL_ALLOW_EMPTY_PASSWORD\":\"yes\"}}"
                } else {
                    "{\"env\":{\"MYSQL_ROOT_PASSWORD\":\"${it.with.password}\"}}"
                }

                val serviceJobDevCloudInput = ServiceJobDevCloudInput(
                    it.image,
                    record.repoUrl,
                    record.repoUsername,
                    record.repoPwd,
                    params,
                    record.env
                )
                val servicesElement = MarketBuildAtomElement(
                    name = "创建${it.image}服务",
                    id = null,
                    status = null,
                    atomCode = ServiceJobDevCloudTask.atomCode,
                    version = "1.*",
                    data = mapOf("input" to serviceJobDevCloudInput, "namespace" to (it.serviceId ?: ""))
                )

                elementList.add(servicesElement)
            }
        }

        return elementList
    }

    private fun createPipelineParams(
        yaml: ScriptBuildYaml,
        gitBasicSetting: GitCIBasicSetting,
        event: GitRequestEvent
    ): MutableList<BuildFormProperty> {
        val result = mutableListOf<BuildFormProperty>()

        val startParams = mutableMapOf<String, String>()
        val parsedCommitMsg = EmojiUtil.removeAllEmoji(event.commitMsg ?: "")

        // 通用参数
        startParams[CI_PIPELINE_NAME] = yaml.name ?: ""
        startParams[CI_BUILD_URL] = v2GitUrl ?: ""
        startParams[BK_CI_RUN] = "true"
        startParams[CI_ACTOR] = event.userId
        startParams[CI_BRANCH] = event.branch
        startParams[PIPELINE_GIT_EVENT_CONTENT] = EmojiUtil.removeAllEmoji(JsonUtil.toJson(event))
        startParams[PIPELINE_GIT_COMMIT_MESSAGE] = parsedCommitMsg
        startParams[PIPELINE_GIT_SHA] = event.commitId
        if (!event.commitId.isBlank() && event.commitId.length >= 8) {
            startParams[PIPELINE_GIT_SHA_SHORT] = event.commitId.substring(0, 8)
        }

        // 替换BuildMessage为了展示commit信息
        startParams[PIPELINE_BUILD_MSG] = parsedCommitMsg

        // 写入WEBHOOK触发环境变量
        val originEvent = try {
            startParams["BK_CI_EVENT_CONTENT"] = event.event
            objectMapper.readValue<GitEvent>(event.event)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${e.message}")
        }

        val gitProjectName = when (originEvent) {
            is GitPushEvent -> {
                startParams[PIPELINE_GIT_REPO_URL] = originEvent.repository.git_http_url
                startParams[PIPELINE_GIT_REF] = originEvent.ref
                startParams[CI_BRANCH] = getBranchName(originEvent.ref)
                startParams[PIPELINE_GIT_EVENT] = GitPushEvent.classType
                GitUtils.getProjectName(originEvent.repository.git_http_url)
            }
            is GitTagPushEvent -> {
                startParams[PIPELINE_GIT_REPO_URL] = originEvent.repository.git_http_url
                startParams[PIPELINE_GIT_REF] = originEvent.ref
                startParams[CI_BRANCH] = getBranchName(originEvent.ref)
                startParams[PIPELINE_GIT_EVENT] = GitTagPushEvent.classType
                GitUtils.getProjectName(originEvent.repository.git_http_url)
            }
            is GitMergeRequestEvent -> {
                startParams[PIPELINE_GIT_REPO_URL] = gitBasicSetting.gitHttpUrl
                startParams[PIPELINE_GIT_BASE_REPO_URL] = originEvent.object_attributes.source.http_url
                startParams[PIPELINE_GIT_HEAD_REPO_URL] = originEvent.object_attributes.target.http_url
                startParams[PIPELINE_GIT_MR_URL] = originEvent.object_attributes.url
                startParams[PIPELINE_GIT_EVENT] = GitMergeRequestEvent.classType
                startParams[PIPELINE_GIT_HEAD_REF] = originEvent.object_attributes.target_branch
                startParams[PIPELINE_GIT_BASE_REF] = originEvent.object_attributes.source_branch
                startParams[PIPELINE_WEBHOOK_EVENT_TYPE] = CodeEventType.MERGE_REQUEST.name
                startParams[PIPELINE_WEBHOOK_SOURCE_BRANCH] = originEvent.object_attributes.source_branch
                startParams[PIPELINE_WEBHOOK_TARGET_BRANCH] = originEvent.object_attributes.target_branch
                startParams[PIPELINE_WEBHOOK_SOURCE_URL] = originEvent.object_attributes.source.http_url
                startParams[PIPELINE_WEBHOOK_TARGET_URL] = originEvent.object_attributes.target.http_url
                GitUtils.getProjectName(originEvent.object_attributes.source.http_url)
            }
            else -> {
                startParams[PIPELINE_GIT_EVENT] = OBJECT_KIND_MANUAL
                startParams[PIPELINE_GIT_REPO_URL] = gitBasicSetting.gitHttpUrl
                GitCommonUtils.getRepoOwner(gitBasicSetting.gitHttpUrl) + "/" + gitBasicSetting.name
            }
        }

        startParams[PIPELINE_GIT_REPO] = gitProjectName
        val repoName = gitProjectName.split("/")
        val repoProjectName = if (repoName.size >= 2) {
            val index = gitProjectName.lastIndexOf("/")
            gitProjectName.substring(index + 1)
        } else {
            gitProjectName
        }
        val repoGroupName = if (repoName.size >= 2) {
            gitProjectName.removeSuffix("/$repoProjectName")
        } else {
            gitProjectName
        }
        startParams[PIPELINE_GIT_REPO_NAME] = repoProjectName
        startParams[PIPELINE_GIT_REPO_GROUP] = repoGroupName

        // 用户自定义变量
        // startParams.putAll(yaml.variables ?: mapOf())
        // putVariables2StartParams(yaml, gitBasicSetting, startParams)
        val buildFormProperties = getBuildFormPropertyFromYmlVariable(yaml, gitBasicSetting, startParams)

        startParams.forEach {
            result.add(
                BuildFormProperty(
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
            )
        }
        result.addAll(buildFormProperties)

        return result
    }

    private fun getBuildFormPropertyFromYmlVariable(
        yaml: ScriptBuildYaml,
        gitBasicSetting: GitCIBasicSetting,
        startParams: MutableMap<String, String>
    ): List<BuildFormProperty> {
        if (yaml.variables == null) {
            return emptyList()
        }
        val buildFormProperties = mutableListOf<BuildFormProperty>()
        yaml.variables!!.forEach { (key, variable) ->
            buildFormProperties.add(
                BuildFormProperty(
                    id = VARIABLE_PREFIX + key,
                    required = false,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = formatVariablesValue(variable.value, gitBasicSetting, startParams) ?: "",
                    options = null,
                    desc = null,
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null,
                    readOnly = variable.readonly
                )
            )
        }
        return buildFormProperties
    }

    private fun putVariables2StartParams(
        yaml: ScriptBuildYaml,
        gitBasicSetting: GitCIBasicSetting,
        startParams: MutableMap<String, String>
    ) {
        if (yaml.variables == null) {
            return
        }
        yaml.variables!!.forEach { (key, variable) ->
            startParams[VARIABLE_PREFIX + key] =
                variable.copy(value = formatVariablesValue(variable.value, gitBasicSetting, startParams)).value ?: ""
        }
    }

    private fun formatVariablesValue(
        value: String?,
        gitBasicSetting: GitCIBasicSetting,
        startParams: MutableMap<String, String>
    ): String? {
        if (value == null || value.isEmpty()) {
            return ""
        }
        val settingMap = mutableMapOf<String, String>()
        settingMap.putAll(startParams)
        return ScriptYmlUtils.parseVariableValue(value, settingMap)
    }

    private fun getCiBuildConf(buildConf: BuildConfig): CiBuildConfig {
        return CiBuildConfig(
            buildConf.codeCCSofwareClientImage,
            buildConf.codeCCSofwarePath,
            buildConf.registryHost,
            buildConf.registryUserName,
            buildConf.registryPassword,
            buildConf.registryImage,
            buildConf.cpu,
            buildConf.memory,
            buildConf.disk,
            buildConf.volume,
            buildConf.activeDeadlineSeconds,
            buildConf.devCloudAppId,
            buildConf.devCloudToken,
            buildConf.devCloudUrl
        )
    }

    private fun getBranchName(ref: String): String {
        return when {
            ref.startsWith("refs/heads/") ->
                ref.removePrefix("refs/heads/")
            ref.startsWith("refs/tags/") ->
                ref.removePrefix("refs/tags/")
            else -> ref
        }
    }
}
