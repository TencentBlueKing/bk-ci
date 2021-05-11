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

package com.tencent.devops.gitci.v2.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.common.ci.OBJECT_KIND_PUSH
import com.tencent.devops.common.ci.OBJECT_KIND_TAG_PUSH
import com.tencent.devops.common.ci.image.BuildType
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.task.DockerRunDevCloudTask
import com.tencent.devops.common.ci.task.GitCiCodeRepoInput
import com.tencent.devops.common.ci.task.GitCiCodeRepoTask
import com.tencent.devops.common.ci.task.ServiceJobDevCloudInput
import com.tencent.devops.common.ci.task.ServiceJobDevCloudTask
import com.tencent.devops.common.ci.v2.Job
import com.tencent.devops.common.ci.v2.JobRunsOnType
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
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.gitci.GitCIDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.gitci.dao.GitCIServicesConfDao
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventNotBuildDao
import com.tencent.devops.gitci.pojo.BuildConfig
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.gitci.pojo.GitRequestEvent
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.git.GitPushEvent
import com.tencent.devops.gitci.pojo.git.GitTagPushEvent
import com.tencent.devops.gitci.service.BaseBuildService
import com.tencent.devops.gitci.utils.GitCIParameterUtils
import com.tencent.devops.gitci.utils.GitCIPipelineUtils
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.setting.Subscription
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.scm.pojo.BK_CI_REF
import com.tencent.devops.scm.pojo.BK_CI_REPOSITORY
import com.tencent.devops.scm.pojo.BK_CI_REPO_OWNER
import com.tencent.devops.scm.pojo.BK_CI_RUN
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_ID_SHORT
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_TARGET_URL
import com.tencent.devops.scm.pojo.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_NAME
import com.tencent.devops.scm.pojo.BK_REPO_WEBHOOK_REPO_URL
import com.tencent.devops.scm.utils.code.git.GitUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TriggerBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val buildConfig: BuildConfig,
    private val objectMapper: ObjectMapper,
    private val gitCISettingDao: GitCISettingDao,
    private val gitCIParameterUtils: GitCIParameterUtils,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val scmClient: ScmClient,
    redisOperation: RedisOperation,
    gitPipelineResourceDao: GitPipelineResourceDao,
    gitRequestEventBuildDao: GitRequestEventBuildDao,
    gitRequestEventNotBuildDao: GitRequestEventNotBuildDao
) : BaseBuildService<ScriptBuildYaml>(client, scmClient, dslContext, redisOperation, gitPipelineResourceDao, gitRequestEventBuildDao, gitRequestEventNotBuildDao) {
    private val channelCode = ChannelCode.GIT

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerBuildService::class.java)

        const val BK_REPO_GIT_WEBHOOK_MR_IID = "BK_CI_REPO_GIT_WEBHOOK_MR_IID"
        const val VARIABLE_PREFIX = "variables."
    }

    override fun gitStartBuild(
        pipeline: GitProjectPipeline,
        event: GitRequestEvent,
        yaml: ScriptBuildYaml,
        gitBuildId: Long
    ): BuildId? {
        logger.info("Git request gitBuildId:$gitBuildId, pipeline:$pipeline, event: $event, yaml: $yaml")

        // create or refresh pipeline
        val gitProjectConf = gitCISettingDao.getSetting(dslContext, event.gitProjectId) ?: throw OperationException("git ci projectCode not exist")

        val model = createPipelineModel(event, gitProjectConf, yaml)
        logger.info("Git request gitBuildId:$gitBuildId, pipeline:$pipeline, model: $model")

        return startBuild(pipeline, event, gitProjectConf, model, gitBuildId)
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
        gitProjectConf: GitRepositoryConf,
        yaml: ScriptBuildYaml
    ): Model {
        // 预安装插件市场的插件
        installMarketAtom(gitProjectConf, event.userId, GitCiCodeRepoTask.atomCode)
        installMarketAtom(gitProjectConf, event.userId, DockerRunDevCloudTask.atomCode)
        installMarketAtom(gitProjectConf, event.userId, ServiceJobDevCloudTask.atomCode)

        val stageList = mutableListOf<Stage>()

        // 第一个stage，触发类
        val manualTriggerElement = ManualTriggerElement("手动触发", "T-1-1-1")
        val params = createPipelineParams(yaml, gitProjectConf, event)
        val triggerContainer =
            TriggerContainer("0", "构建触发", listOf(manualTriggerElement), null, null, null, null, params)
        val stage1 = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(stage1)

        // 其他的stage
        yaml.stages.forEachIndexed { stageIndex, stage ->
            stageList.add(createStage(stage, event, gitProjectConf))
        }

        yaml.finally?.forEach {
            stageList.add(createStage(it, event, gitProjectConf, true))
        }

        return Model(
            name = GitCIPipelineUtils.genBKPipelineName(gitProjectConf.gitProjectId),
            desc = "",
            stages = stageList,
            labels = emptyList(),
            instanceFromTemplate = false,
            pipelineCreator = event.userId
        )
    }

    private fun createStage(
        stage: com.tencent.devops.common.ci.v2.Stage,
        event: GitRequestEvent,
        gitProjectConf: GitRepositoryConf,
        finalStage: Boolean = false
    ): Stage {
        val containerList = mutableListOf<Container>()
        stage.jobs.forEachIndexed { jobIndex, job ->
            val elementList = makeElementList(job, gitProjectConf, event.userId)
            if (job.runsOn.poolName == JobRunsOnType.AGENT_LESS.type) {
                addNormalContainer(job, elementList, containerList, jobIndex)
            } else {
                addVmBuildContainer(job, elementList, containerList, jobIndex)
            }
        }

        // 根据if设置stageController
        var stageControlOption = StageControlOption()
        if (stage.ifField != null) {
            stageControlOption = StageControlOption(
                runCondition = StageRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = stage.ifField.toString()
            )
        }

        return Stage(
            id = stage.id,
            tag = listOf(stage.label),
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
        jobIndex: Int
    ) {

/*        val listPreAgentResult =
            client.get(ServicePreBuildAgentResource::class).listPreBuildAgent(userId, getUserProjectId(userId), os)
        if (listPreAgentResult.isNotOk()) {
            logger.error("list prebuild agent failed")
            throw OperationException("list prebuild agent failed")
        }
        val preAgents = listPreAgentResult.data!!

        val dispatchType = getDispatchType(job, startUpReq, agentInfo)

        val vmBaseOS = if (vmType == ResourceType.REMOTE) {
            when (dispatchType) {
                is ThirdPartyAgentIDDispatchType -> {
                    job.job.pool?.os ?: VMBaseOS.LINUX
                }
                is ThirdPartyAgentEnvDispatchType -> {
                    job.job.pool?.os ?: VMBaseOS.LINUX
                }
                is MacOSDispatchType -> VMBaseOS.MACOS
                else -> VMBaseOS.LINUX
            }
        } else VMBaseOS.valueOf(agentInfo.os)*/

        val vmContainer = VMBuildContainer(
            id = job.id,
            name = "Job_${jobIndex + 1} ${job.name ?: ""}",
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = VMBaseOS.LINUX,
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
            jobControlOption = getJobControlOption(job),
            dispatchType = getDispatchType(job)
        )
        containerList.add(vmContainer)
    }

    fun getDispatchType(job: Job): DispatchType {
        // macos构建机
        if (job.runsOn.poolName.startsWith("macos")) {
            return MacOSDispatchType(
                macOSEvn = "",
                systemVersion = "",
                xcodeVersion = ""
            )
        }

        // 第三方构建机
        if (job.runsOn.selfHosted) {
            return ThirdPartyAgentEnvDispatchType(
                envName = job.runsOn.poolName,
                workspace = "",
                agentType = AgentType.ID
            )
        }

        // 公共docker构建机
        val containerPool = Pool(
            container = job.runsOn.container.image,
            credential = Credential(
                user = job.runsOn.container.credentials?.username ?: "",
                password = job.runsOn.container.credentials?.password ?: ""
            ),
            macOS = null,
            third = null,
            env = job.env,
            buildType = BuildType.DOCKER_VM
        )

        return GitCIDispatchType(objectMapper.writeValueAsString(containerPool))
    }

    private fun addNormalContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int
    ) {

        containerList.add(
            NormalContainer(
                containerId = null,
                id = job.id,
                name = "Job_${jobIndex + 1} ${job.name ?: ""}",
                elements = elementList,
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                enableSkip = false,
                conditions = null,
                canRetry = false,
                jobControlOption = getJobControlOption(job),
                mutexGroup = null
            )
        )
    }

    private fun getJobControlOption(job: Job): JobControlOption {
        return if (job.ifField != null) {
            JobControlOption(
                timeout = job.timeoutMinutes,
                runCondition = JobRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = job.ifField.toString(),
                dependOnType = DependOnType.ID,
                dependOnId = job.dependOn,
                continueWhenFailed = job.continueOnError
            )
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
        gitProjectConf: GitRepositoryConf,
        userId: String
    ): MutableList<Element> {
        // 解析service
        val elementList = makeServiceElementList(job)

        // 解析job steps
        job.steps!!.forEach { step ->
            // bash
            val additionalOptions = ElementAdditionalOptions(
                continueWhenFailed = step.continueOnError ?: false,
                timeout = step.timeoutMinutes?.toLong(),
                retryWhenFailed = step.retryTimes != null,
                retryCount = step.retryTimes?.toInt() ?: 0,
                enableCustomEnv = step.env != null,
                customEnv = emptyList(),
                runCondition = RunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = step.ifFiled
            )

            val element: Element = when {
                step.run != null -> {
                    LinuxScriptElement(
                        name = step.name ?: "执行Linux脚本",
                        id = step.id,
                        // todo: 如何判断类型
                        scriptType = BuildScriptType.SHELL,
                        script = step.run!!,
                        continueNoneZero = false,
                        additionalOptions = additionalOptions
                    )
                }
                step.checkout != null -> {
                    // checkout插件装配
                    val inputMap = mutableMapOf<String, Any>()
                    inputMap.putAll(step.with!!)
                    // 拉取本地工程代码
                    if (step.checkout == "self") {
                        inputMap["accessToken"] = scmClient.getAccessToken(gitProjectConf.gitProjectId)
                        inputMap["repositoryUrl"] = gitProjectConf.gitHttpUrl
                    } else {
                        inputMap["repositoryUrl"] = step.checkout!!
                    }

                    val data = mutableMapOf<String, Any>()
                    data["input"] = inputMap

                    MarketBuildAtomElement(
                        name = step.name ?: "拉代码插件",
                        id = step.id,
                        atomCode = "checkout",
                        version = "1.latest",
                        data = data,
                        additionalOptions = additionalOptions
                    )
                }
                else -> {
                    val data = mutableMapOf<String, Any>()
                    data["input"] = step.with!!
                    MarketBuildAtomElement(
                        name = step.name ?: "插件市场第三方构建环境类插件",
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
                installMarketAtom(gitProjectConf, userId, element.getAtomCode())
            }
        }

        return elementList
    }

    private fun makeServiceElementList(job: Job): MutableList<Element> {
        val elementList = mutableListOf<Element>()

        // 解析services
        if (job.services != null) {
            job.services!!.forEach {
                val (imageName, imageTag) = ScriptYmlUtils.parseServiceImage(it.image)

                val record = gitServicesConfDao.get(dslContext, imageName, imageTag) ?: throw RuntimeException("Git CI没有此镜像版本记录. ${it.image}")
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

    private fun createGitCodeElement(event: GitRequestEvent, gitProjectConf: GitRepositoryConf): Element {
        val gitToken = client.getScm(ServiceGitResource::class).getToken(gitProjectConf.gitProjectId).data!!
        logger.info("get token from scm success, gitToken: $gitToken")
        val gitCiCodeRepoInput = when (event.objectKind) {
            OBJECT_KIND_PUSH -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.COMMIT_ID,
                    refName = event.commitId
                )
            }
            OBJECT_KIND_TAG_PUSH -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.TAG,
                    refName = event.branch.removePrefix("refs/tags/")
                )
            }
            OBJECT_KIND_MERGE_REQUEST -> {
                // MR时fork库的源仓库URL会不同，需要单独拿出来处理
                val gitEvent = objectMapper.readValue<GitEvent>(event.event) as GitMergeRequestEvent
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.BRANCH,
                    refName = "",
                    pipelineStartType = StartType.WEB_HOOK,
                    hookEventType = CodeEventType.MERGE_REQUEST.name,
                    hookSourceBranch = event.branch,
                    hookTargetBranch = event.targetBranch,
                    hookSourceUrl = if (event.sourceGitProjectId != null && event.sourceGitProjectId != event.gitProjectId) {
                        gitEvent.object_attributes.source.http_url
                    } else {
                        gitProjectConf.gitHttpUrl
                    },
                    hookTargetUrl = gitProjectConf.gitHttpUrl
                )
            }
            OBJECT_KIND_MANUAL -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.BRANCH,
                    refName = event.branch.removePrefix("refs/heads/")
                )
            }
            else -> {
                logger.error("event.objectKind invalid")
                null
            }
        }

        return MarketBuildAtomElement(
            name = "拉代码",
            id = null,
            status = null,
            atomCode = GitCiCodeRepoTask.atomCode,
            version = "1.*",
            data = mapOf("input" to gitCiCodeRepoInput!!)
        )
    }

    private fun createPipelineParams(
        yaml: ScriptBuildYaml,
        gitProjectConf: GitRepositoryConf,
        event: GitRequestEvent
    ): MutableList<BuildFormProperty> {
        val result = mutableListOf<BuildFormProperty>()
        gitProjectConf.env?.forEach {
            val value = gitCIParameterUtils.encrypt(it.value)
            result.add(
                BuildFormProperty(
                    id = it.name,
                    required = false,
                    type = BuildFormPropertyType.PASSWORD,
                    defaultValue = value,
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

        val startParams = mutableMapOf<String, String>()

        // 通用参数
        startParams[BK_CI_RUN] = "true"
        startParams[BK_CI_REPO_OWNER] = GitCommonUtils.getRepoOwner(gitProjectConf.gitHttpUrl)
        startParams[BK_CI_REPOSITORY] =
            GitCommonUtils.getRepoOwner(gitProjectConf.gitHttpUrl) + "/" + gitProjectConf.name
        startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = event.objectKind
        startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH] = event.branch
        startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = event.commitId
        startParams[BK_REPO_WEBHOOK_REPO_NAME] = gitProjectConf.name
        startParams[BK_REPO_WEBHOOK_REPO_URL] = gitProjectConf.url
        startParams[BK_REPO_GIT_WEBHOOK_COMMIT_MESSAGE] = event.commitMsg.toString()
        if (!event.commitId.isBlank() && event.commitId.length >= 8)
            startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID_SHORT] = event.commitId.substring(0, 8)

        // 写入WEBHOOK触发环境变量
        val originEvent = try {
            startParams["BK_CI_EVENT_CONTENT"] = event.event
            objectMapper.readValue<GitEvent>(event.event)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${e.message}")
        }

        when (originEvent) {
            is GitPushEvent -> {
                startParams[BK_CI_REF] = originEvent.ref
                addContext(yaml, originEvent, startParams, event)

//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT] = originEvent.before
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT] = originEvent.after
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = originEvent.total_commits_count.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND] = originEvent.operation_kind
            }
            is GitTagPushEvent -> {
                startParams[BK_CI_REF] = originEvent.ref
                addContext(yaml, originEvent, startParams, event)

//                startParams[BK_REPO_GIT_WEBHOOK_TAG_NAME] = event.branch
//                startParams[BK_REPO_GIT_WEBHOOK_TAG_OPERATION] = originEvent.operation_kind ?: ""
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = originEvent.total_commits_count.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_TAG_USERNAME] = event.userId
//                startParams[BK_REPO_GIT_WEBHOOK_TAG_CREATE_FROM] = originEvent.create_from.toString()
            }
            is GitMergeRequestEvent -> {
//                startParams[BK_REPO_GIT_WEBHOOK_MR_ACTION] = originEvent.object_attributes.action
//                startParams[BK_REPO_GIT_WEBHOOK_MR_AUTHOR] = originEvent.user.username
                startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH] = originEvent.object_attributes.target_branch
                startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH] = originEvent.object_attributes.source_branch
                startParams[BK_REPO_GIT_WEBHOOK_MR_TARGET_URL] = originEvent.object_attributes.target.http_url
                startParams[BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL] = originEvent.object_attributes.source.http_url
//                startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIME] = originEvent.object_attributes.created_at
//                startParams[BK_REPO_GIT_WEBHOOK_MR_CREATE_TIMESTAMP] =
//                    DateTimeUtil.zoneDateToTimestamp(originEvent.object_attributes.created_at).toString()
//                startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIME] = originEvent.object_attributes.updated_at
//                startParams[BK_REPO_GIT_WEBHOOK_MR_UPDATE_TIMESTAMP] =
//                    DateTimeUtil.zoneDateToTimestamp(originEvent.object_attributes.updated_at).toString()
                startParams[BK_REPO_GIT_WEBHOOK_MR_ID] = originEvent.object_attributes.id.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_MR_TITLE] = originEvent.object_attributes.title
                startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = originEvent.object_attributes.url
//                startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = originEvent.object_attributes.id.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = originEvent.object_attributes.description
//                startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = originEvent.object_attributes.assignee_id.toString()
                startParams[BK_REPO_GIT_WEBHOOK_MR_IID] = originEvent.object_attributes.iid.toString()

                addContext(yaml, originEvent, startParams, event)
            }
        }

        // 用户自定义变量
        // startParams.putAll(yaml.variables ?: mapOf())
        putVariables2StartParams(yaml, gitProjectConf, startParams)

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

        return result
    }

    private fun addContext(
        yaml: ScriptBuildYaml,
        originEvent: GitEvent,
        startParams: MutableMap<String, String>,
        event: GitRequestEvent
    ) {
        // 上下文
        startParams["ci.pipeline_name"] = yaml.name ?: ""
        startParams["ci.actor"] = event.userId
        startParams["ci.build_url"] = "https://git-ci.woa.com/" // FIXME

        val gitProjectName = when (originEvent){
            is GitPushEvent -> {
                startParams["ci.ref"] = originEvent.ref
                GitUtils.getProjectName(originEvent.repository.git_http_url)
            }
            is GitTagPushEvent -> {
                startParams["ci.ref"] = originEvent.ref
                GitUtils.getProjectName(originEvent.repository.git_http_url)
            }
            is GitMergeRequestEvent -> {
                startParams["ci.head_ref"] = originEvent.object_attributes.target_branch
                startParams["ci.base_ref"] = originEvent.object_attributes.source_branch
                GitUtils.getProjectName(originEvent.object_attributes.source.http_url)
            }
            else -> {
                return
            }
        }

        startParams["ci.repo"] = gitProjectName
        val repoName = gitProjectName.split("/")
        startParams["ci.repo_name"] = if (repoName.size >= 2) {
            gitProjectName.removePrefix(repoName[0] + "/")
        } else {
            gitProjectName
        }
        startParams["ci.repo_group"] = repoName[0]
        startParams["ci.event"] = GitPushEvent.classType
        startParams["ci.event_content"] = event.event

        startParams["ci.sha"] = event.commitId
        startParams["ci.sha_short"] = event.commitId.substring(0, 8)
        startParams["ci.commit_message"] = event.commitMsg.toString()

    }

    private fun putVariables2StartParams(
        yaml: ScriptBuildYaml,
        gitProjectConf: GitRepositoryConf,
        startParams: MutableMap<String, String>
    ) {
        if (yaml.variables == null) {
            return
        }

        yaml.variables!!.forEach { (key, variable) ->
            startParams[VARIABLE_PREFIX + key] =
                variable.copy(value = formatVariablesValue(variable.value, gitProjectConf)).toString()
        }
    }

    private fun formatVariablesValue(value: String?, gitProjectConf: GitRepositoryConf): String? {
        if (value == null || value.isEmpty()) {
            return ""
        }

        val settingMap = mutableMapOf<String, String>()
        gitProjectConf.env?.forEach {
            settingMap[it.name] = it.value
        }

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
}
