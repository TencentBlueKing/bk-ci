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

package com.tencent.devops.stream.v1.components

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.EmojiUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.NORMAL_JOB
import com.tencent.devops.common.ci.VM_JOB
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.MacOS
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.task.DockerRunDevCloudTask
import com.tencent.devops.common.ci.task.GitCiCodeRepoInput
import com.tencent.devops.common.ci.task.GitCiCodeRepoTask
import com.tencent.devops.common.ci.task.ServiceJobDevCloudTask
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.ci.yaml.Job
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.type.gitci.GitCIDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import com.tencent.devops.common.webhook.pojo.code.BK_CI_REF
import com.tencent.devops.common.webhook.pojo.code.BK_CI_REPOSITORY
import com.tencent.devops.common.webhook.pojo.code.BK_CI_REPO_OWNER
import com.tencent.devops.common.webhook.pojo.code.BK_CI_RUN
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_COMMIT_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_COMMIT_ID_SHORT
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_COMMIT_MESSAGE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_EVENT_TYPE
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_ID
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_SOURCE_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TARGET_BRANCH
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_TARGET_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_GIT_WEBHOOK_MR_URL
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.BK_REPO_WEBHOOK_REPO_URL
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.stream.constant.StreamCode.BK_BUILD_TRIGGER
import com.tencent.devops.stream.constant.StreamCode.BK_CREATE_SERVICE
import com.tencent.devops.stream.constant.StreamCode.BK_GIT_CI_NO_RECOR
import com.tencent.devops.stream.constant.StreamCode.BK_MANUAL_TRIGGER
import com.tencent.devops.stream.constant.StreamCode.BK_MIRROR_VERSION_NOT_AVAILABLE
import com.tencent.devops.stream.constant.StreamCode.BK_PULL_CODE
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.v1.client.V1ScmClient
import com.tencent.devops.stream.v1.config.V1BuildConfig
import com.tencent.devops.stream.v1.dao.V1GitCIServicesConfDao
import com.tencent.devops.stream.v1.dao.V1GitCISettingDao
import com.tencent.devops.stream.v1.dao.V1GitPipelineResourceDao
import com.tencent.devops.stream.v1.dao.V1GitRequestEventBuildDao
import com.tencent.devops.stream.v1.pojo.V1GitRepositoryConf
import com.tencent.devops.stream.v1.pojo.V1GitRequestEvent
import com.tencent.devops.stream.v1.service.V1GitCIEventService
import com.tencent.devops.stream.v1.service.V1StreamPipelineBranchService
import com.tencent.devops.stream.v1.utils.V1GitCIParameterUtils
import com.tencent.devops.stream.v1.utils.V1GitCIPipelineUtils
import com.tencent.devops.stream.v1.utils.V1GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class V1YamlBuild @Autowired constructor(
    private val client: Client,
    private val scmClient: V1ScmClient,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation,
    private val gitPipelineResourceDao: V1GitPipelineResourceDao,
    private val gitCISettingDao: V1GitCISettingDao,
    private val gitRequestEventBuildDao: V1GitRequestEventBuildDao,
    private val gitServicesConfDao: V1GitCIServicesConfDao,
    private val buildConfig: V1BuildConfig,
    private val gitCIParameterUtils: V1GitCIParameterUtils,
    private val gitCIEventSaveService: V1GitCIEventService,
    private val streamPipelineBranchService: V1StreamPipelineBranchService
) : V1YamlBaseBuild<CIBuildYaml>(
    client,
    scmClient,
    dslContext,
    redisOperation,
    gitPipelineResourceDao,
    gitRequestEventBuildDao,
    gitCIEventSaveService,
    streamPipelineBranchService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(V1YamlBuild::class.java)
        const val BK_REPO_GIT_WEBHOOK_MR_IID = "BK_CI_REPO_GIT_WEBHOOK_MR_IID"
        const val BK_REPO_GIT_EVENT_CONTENT = "BK_CI_EVENT_CONTENT"
    }

    @Value("\${gitci.v2GitUrl:#{null}}")
    private val v2GitUrl: String? = null

    @Value("\${devopsGateway.host:#{null}}")
    private val gateway: String? = null

    private val channelCode = ChannelCode.GIT

    override fun gitStartBuild(
        pipeline: StreamTriggerPipeline,
        event: V1GitRequestEvent,
        yaml: CIBuildYaml,
        gitBuildId: Long
    ): BuildId? {
        logger.info("Git request gitBuildId:$gitBuildId, pipeline:$pipeline, event: $event, yaml: $yaml")

        // create or refresh pipeline
        val gitProjectConf = gitCISettingDao.getSetting(dslContext, event.gitProjectId)
            ?: throw OperationException("git ci projectCode not exist")

        val model = createPipelineModel(event, gitProjectConf, yaml)

        return startBuild(pipeline, event, gitProjectConf, model, gitBuildId)
    }

    private fun createPipelineModel(
        event: V1GitRequestEvent,
        gitProjectConf: V1GitRepositoryConf,
        yaml: CIBuildYaml
    ): Model {
        // 先安装插件市场的插件
        installMarketAtom(gitProjectConf, event.userId, GitCiCodeRepoTask.atomCode)
        installMarketAtom(gitProjectConf, event.userId, DockerRunDevCloudTask.atomCode)
        installMarketAtom(gitProjectConf, event.userId, ServiceJobDevCloudTask.atomCode)

        val stageList = mutableListOf<Stage>()

        // 第一个stage，触发类
        val manualTriggerElement = ManualTriggerElement(
            MessageUtil.getMessageByLocale(
                messageCode = BK_MANUAL_TRIGGER,
                language = I18nUtil.getLanguage()
            ), "T-1-1-1")
        val params = createPipelineParams(gitProjectConf, yaml, event)
        val triggerContainer =
            TriggerContainer("0", MessageUtil.getMessageByLocale(
                messageCode = BK_BUILD_TRIGGER,
                language = I18nUtil.getLanguage()
            ), listOf(manualTriggerElement), null, null, null, null, params)
        val stage1 = Stage(listOf(triggerContainer), VMUtils.genStageId(1))
        stageList.add(stage1)

        // 第二个stage，services初始化
        addServicesStage(yaml, stageList)

        // 其他的stage
        yaml.stages!!.forEachIndexed { stageIndex, stage ->
            val containerList = mutableListOf<Container>()
            stage.stage.forEachIndexed { jobIndex, job ->
                val elementList = mutableListOf<Element>()
                // 根据job类型创建构建容器或者无构建环境容器，默认vmBuild
                if (job.job.type == null || job.job.type == VM_JOB) {
                    // 构建环境容器每个job的第一个插件都是拉代码
                    elementList.add(createGitCodeElement(event, gitProjectConf))
                    makeElementList(job, elementList, gitProjectConf, event.userId)
                    addVmBuildContainer(job, elementList, containerList, jobIndex)
                } else if (job.job.type == NORMAL_JOB) {
                    makeElementList(job, elementList, gitProjectConf, event.userId)
                    addNormalContainer(job, elementList, containerList, jobIndex)
                }
            }

            stageList.add(Stage(containerList, VMUtils.genStageId(stageIndex)))
        }
        return Model(
            name = V1GitCIPipelineUtils.genBKPipelineName(gitProjectConf.gitProjectId),
            desc = "",
            stages = stageList,
            labels = emptyList(),
            instanceFromTemplate = false,
            pipelineCreator = event.userId
        )
    }

    private fun addNormalContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int
    ) {
        val displayName = if (!job.job.displayName.isNullOrBlank()) {
            job.job.displayName!!
        } else if (!job.job.name.isNullOrBlank()) {
            job.job.name!!
        } else {
            ""
        }
        containerList.add(
            NormalContainer(
                containerId = null,
                id = null,
                name = "Job_${jobIndex + 1} $displayName",
                elements = elementList,
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                enableSkip = false,
                conditions = null,
                canRetry = false,
                jobControlOption = null,
                mutexGroup = null
            )
        )
    }

    private fun addVmBuildContainer(
        job: Job,
        elementList: List<Element>,
        containerList: MutableList<Container>,
        jobIndex: Int
    ) {
        var osType = VMBaseOS.LINUX
        val containerPool =
            when {
                // 有container配置时优先使用
                job.job.pool?.container != null -> {
                    Pool(
                        container = job.job.pool!!.container,
                        credential = Credential(
                            user = job.job.pool!!.credential?.user ?: "",
                            password = job.job.pool!!.credential?.password ?: ""
                        ),
                        macOS = null,
                        third = null
                    )
                }

                // 没有container配置时，优先使用macOS配置
                job.job.pool?.macOS != null -> {
                    osType = VMBaseOS.MACOS
                    Pool(
                        container = null,
                        credential = null,
                        macOS = MacOS(
                            systemVersion = job.job.pool!!.macOS?.systemVersion ?: "",
                            xcodeVersion = job.job.pool!!.macOS?.xcodeVersion ?: ""
                        ),
                        third = null
                    )
                }

                // 假设都没有配置，使用默认镜像
                else -> {
                    Pool(buildConfig.registryImage, Credential("", ""), null, null)
                }
            }

        val displayName = if (!job.job.displayName.isNullOrBlank()) {
            job.job.displayName!!
        } else if (!job.job.name.isNullOrBlank()) {
            job.job.name!!
        } else {
            ""
        }

        val vmContainer = VMBuildContainer(
            id = null,
            name = "Job_${jobIndex + 1} $displayName",
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = osType,
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = 900,
            buildEnv = null,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            dispatchType = if (containerPool.macOS != null) {
                MacOSDispatchType(
                    macOSEvn = containerPool.macOS!!.systemVersion!! + ":" + containerPool.macOS!!.xcodeVersion!!,
                    systemVersion = containerPool.macOS!!.systemVersion!!,
                    xcodeVersion = containerPool.macOS!!.xcodeVersion!!
                )
            } else {
                GitCIDispatchType(objectMapper.writeValueAsString(containerPool))
            }
        )
        containerList.add(vmContainer)
    }

    private fun makeElementList(
        job: Job,
        elementList: MutableList<Element>,
        gitProjectConf: V1GitRepositoryConf,
        userId: String
    ) {
        job.job.steps.forEach {
            val element = it.covertToElement(getCiBuildConf(buildConfig))
            elementList.add(element)
            if (element is MarketBuildAtomElement || element is MarketBuildLessAtomElement) {
                logger.info("install market atom: ${element.getAtomCode()}")
                installMarketAtom(gitProjectConf, userId, element.getAtomCode())
            }
        }
    }

    private fun createGitCodeElement(event: V1GitRequestEvent, gitProjectConf: V1GitRepositoryConf): Element {
        val gitToken = client.getScm(ServiceGitResource::class).getToken(gitProjectConf.gitProjectId).data!!
        logger.info("get token from scm success, gitToken: $gitToken")
        val gitCiCodeRepoInput = when (event.objectKind) {
            StreamGitObjectKind.PUSH.value -> {
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
            StreamGitObjectKind.TAG_PUSH.value -> {
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
            StreamGitObjectKind.MERGE_REQUEST.value -> {
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
                    hookSourceUrl = if (event.sourceGitProjectId != null &&
                        event.sourceGitProjectId != event.gitProjectId
                    ) {
                        gitEvent.object_attributes.source.http_url
                    } else {
                        gitProjectConf.gitHttpUrl
                    },
                    hookTargetUrl = gitProjectConf.gitHttpUrl
                )
            }
            StreamGitObjectKind.MANUAL.value -> {
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
                logger.warn("event.objectKind invalid")
                null
            }
        }

        return MarketBuildAtomElement(
            name = MessageUtil.getMessageByLocale(
                messageCode = BK_PULL_CODE,
                language = I18nUtil.getLanguage()
            ),
            id = null,
            status = null,
            atomCode = GitCiCodeRepoTask.atomCode,
            version = "1.*",
            data = mapOf("input" to gitCiCodeRepoInput!!)
        )
    }

    private fun addServicesStage(yaml: CIBuildYaml, stageList: MutableList<Stage>) {
        if (yaml.services == null || yaml.services!!.isEmpty()) {
            return
        }
        yaml.services!!.forEachIndexed { index, it ->
            // 判断镜像格式是否合法
            val (imageName, imageTag) = it.parseImage()
            val record = gitServicesConfDao.get(dslContext, imageName, imageTag)
                ?: throw RuntimeException(MessageUtil.getMessageByLocale(
                    messageCode = BK_GIT_CI_NO_RECOR,
                    language = I18nUtil.getLanguage()
                ) + ". ${it.image}")
            if (!record.enable) {
                throw RuntimeException(
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_MIRROR_VERSION_NOT_AVAILABLE,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            val serviceJobDevCloudInput =
                it.getServiceInput(record.repoUrl, record.repoUsername, record.repoPwd, record.env)

            val servicesElement = MarketBuildAtomElement(
                name = MessageUtil.getMessageByLocale(
                    messageCode = BK_CREATE_SERVICE,
                    language = I18nUtil.getLanguage(),
                    params = arrayOf(it.getType())
                ),
                id = null,
                status = null,
                atomCode = ServiceJobDevCloudTask.atomCode,
                version = "1.*",
                data = mapOf("input" to serviceJobDevCloudInput, "namespace" to it.getServiceParamNameSpace())
            )

            val servicesContainer = NormalContainer(
                containerId = null,
                id = null,
                name = MessageUtil.getMessageByLocale(
                    messageCode = BK_CREATE_SERVICE,
                    language = I18nUtil.getLanguage(),
                    params = arrayOf(it.getType())
                ),
                elements = listOf(servicesElement),
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                enableSkip = false,
                conditions = null,
                canRetry = true,
                jobControlOption = null,
                mutexGroup = null
            )
            val stage = Stage(listOf(servicesContainer), "stage-service-job-$index")
            stageList.add(stage)
        }
    }

    private fun createPipelineParams(
        gitProjectConf: V1GitRepositoryConf,
        yaml: CIBuildYaml,
        event: V1GitRequestEvent
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
        val parsedCommitMsg = EmojiUtil.removeAllEmoji(event.commitMsg ?: "")

        // 通用参数
        startParams[BK_CI_RUN] = "true"
        startParams[BK_CI_REPO_OWNER] = V1GitCommonUtils.getRepoOwner(gitProjectConf.gitHttpUrl)
        startParams[BK_CI_REPOSITORY] =
            V1GitCommonUtils.getRepoOwner(gitProjectConf.gitHttpUrl) + "/" + gitProjectConf.name
        startParams[BK_REPO_GIT_WEBHOOK_EVENT_TYPE] = event.objectKind
        startParams[BK_REPO_GIT_WEBHOOK_FINAL_INCLUDE_BRANCH] = event.branch
        startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID] = event.commitId
        startParams[BK_REPO_WEBHOOK_REPO_NAME] = gitProjectConf.name
        startParams[BK_REPO_WEBHOOK_REPO_URL] = gitProjectConf.url
        startParams[BK_REPO_GIT_WEBHOOK_COMMIT_MESSAGE] = parsedCommitMsg
        if (event.commitId.isNotBlank() && event.commitId.length >= 8) {
            startParams[BK_REPO_GIT_WEBHOOK_COMMIT_ID_SHORT] = event.commitId.substring(0, 8)
        }

        // 替换BuildMessage为了展示commit信息
        startParams[PIPELINE_BUILD_MSG] = parsedCommitMsg

        // 写入WEBHOOK触发环境变量
        val originEvent = try {
            objectMapper.readValue<GitEvent>(event.event)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${e.message}")
        }

        when (originEvent) {
            is GitPushEvent -> {
                startParams[BK_CI_REF] = originEvent.ref
                startParams[BK_REPO_GIT_EVENT_CONTENT] = JsonUtil.toJson(
                    bean = originEvent.copy(commits = null),
                    formatted = false
                )
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_BEFORE_COMMIT] = originEvent.before
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_AFTER_COMMIT] = originEvent.after
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_TOTAL_COMMIT] = originEvent.total_commits_count.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_PUSH_OPERATION_KIND] = originEvent.operation_kind
            }
            is GitTagPushEvent -> {
                startParams[BK_CI_REF] = originEvent.ref
                startParams[BK_REPO_GIT_EVENT_CONTENT] = JsonUtil.toJson(
                    bean = originEvent.copy(commits = null),
                    formatted = false
                )
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
                startParams[BK_REPO_GIT_WEBHOOK_MR_URL] = originEvent.object_attributes.url ?: ""
//                startParams[BK_REPO_GIT_WEBHOOK_MR_NUMBER] = originEvent.object_attributes.id.toString()
//                startParams[BK_REPO_GIT_WEBHOOK_MR_DESCRIPTION] = originEvent.object_attributes.description
//                startParams[BK_REPO_GIT_WEBHOOK_MR_ASSIGNEE] = originEvent.object_attributes.assignee_id.toString()
                startParams[BK_REPO_GIT_WEBHOOK_MR_IID] = originEvent.object_attributes.iid.toString()
                startParams[BK_REPO_GIT_EVENT_CONTENT] = event.event
            }
        }

        val vars = yaml.variables?.map { (key, value) ->
            key to EnvUtils.parseEnv(value, startParams)
        }?.toMap()

        // 用户自定义变量
        startParams.putAll(vars ?: mapOf())

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

    private fun getCiBuildConf(buildConf: V1BuildConfig): CiBuildConfig {
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
