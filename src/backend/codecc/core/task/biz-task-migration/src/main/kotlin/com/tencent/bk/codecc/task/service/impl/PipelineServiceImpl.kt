/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.task.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.bk.codecc.defect.api.ServiceReportTaskLogRestResource
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.ActiveProjParseModel
import com.tencent.bk.codecc.task.pojo.GongfengPublicProjModel
import com.tencent.bk.codecc.task.service.MetaService
import com.tencent.bk.codecc.task.service.PipelineService
import com.tencent.bk.codecc.task.utils.PipelineUtils
import com.tencent.bk.codecc.task.vo.AnalyzeConfigInfoVO
import com.tencent.bk.codecc.task.vo.BatchRegisterVO
import com.tencent.bk.codecc.task.vo.BuildEnvVO
import com.tencent.bk.codecc.task.vo.RepoInfoVO
import com.tencent.bk.codecc.task.vo.checkerset.ToolCheckerSetVO
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.StreamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.type.codecc.CodeCCDispatchType
import com.tencent.devops.common.service.ToolMetaCacheService
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.common.util.ObjectDynamicCreator
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccBuildInfo
import com.tencent.devops.plugin.codecc.pojo.coverity.ProjectLanguage
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import lombok.extern.slf4j.Slf4j
import net.sf.json.JSONArray
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import com.tencent.devops.common.util.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set

/**
 * 与蓝盾交互工具类
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service
@Slf4j
open class PipelineServiceImpl @Autowired constructor(
    private val client: Client,
    private val baseDataRepository: BaseDataRepository,
    private val toolMetaCacheService: ToolMetaCacheService,
    private val taskRepository: TaskRepository,
    private val objectMapper: ObjectMapper,
    private val metaService: MetaService,
    private val pipelineUtils: PipelineUtils
) : PipelineService {

    @Value("\${devops.retry.attempt:#{null}}")
    private val retryAttempt: Int = 0

    @Value("\${devops.retry.interval:#{null}}")
    private val retryInterval: Long = 0

    @Value("\${devops.dispatch.imageName:tlinux_ci}")
    private val imageName: String = ""

    @Value("\${devops.dispatch.buildType:DOCKER}")
    private val buildType: String = ""

    @Value("\${devops.dispatch.imageVersion:3.*}")
    private val imageVersion: String = ""

    @Value("\${codecc.public.account:#{null}}")
    private val codeccPublicAccount: String? = null

    @Value("\${codecc.public.password:#{null}}")
    private val codeccPublicPassword: String? = null

    @Value("\${git.path:#{null}}")
    private val gitCodePath: String? = null

    private val pluginVersionCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, String?>(
            object : CacheLoader<String, String?>() {
                override fun load(versionType: String): String? {
                    return try {
                        logger.info("begin to get plugin version: $versionType")
                        getOpenSourceVersion(versionType)
                    } catch (t: Throwable) {
                        logger.info("get plugin version fail! version type: $versionType, message: ${t.message}")
                        null
                    }
                }
            }
        )

    /**
     * 获取流水线ID
     */
    override fun assembleCreatePipeline(
        registerVO: BatchRegisterVO, taskInfoEntity: TaskInfoEntity,
        defaultExecuteTime: String, defaultExecuteDate: List<String>, userName: String, relPath: String
    ): String {
        val modelParam = pipelineUtils.createPipeline(
            registerVO = registerVO,
            taskInfoEntity = taskInfoEntity,
            relPath = relPath,
            imageName = imageName,
            dispatchType = pipelineUtils.getDispatchType(buildType, imageName, imageVersion)
        )
        val result = client.getDevopsService(ServicePipelineResource::class.java)
            .create(userName, taskInfoEntity.projectId, modelParam, ChannelCode.CODECC_EE)
        if (result.isNotOk() || null == result.data) {
            logger.error("create pipeline fail! err msg: {}", result.message)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return result.data!!.id
    }

    override fun updateCodeLibrary(userName: String, registerVO: BatchRegisterVO, taskEntity: TaskInfoEntity): Boolean {
        if (taskEntity.createFrom == ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() ||
            taskEntity.createFrom == ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value()) {
            logger.info("no codecc task channel, do not update...")
            return false
        }

        val model = with(taskEntity) {
            getPipelineModel(userName, projectId, pipelineId, createFrom, nameEn)
        }

        val stageList = mutableListOf<Stage>()
        model.stages.forEach { stage ->
            val containerList = mutableListOf<Container>()
            stage.containers.forEach { container ->
                val elementList = mutableListOf<Element>()
                container.elements.forEach { element ->
                    // 旧插件替换成新的
                    val newElement = when {
                        pipelineUtils.isNewCodeElement(element) || pipelineUtils.isOldCodeElement(element) -> {
                            pipelineUtils.getNewCodeElement(
                                PipelineUtils.Companion.CodeElementData(
                                    scmType = registerVO.scmType,
                                    repoHashId = registerVO.repoHashId,
                                    branch= registerVO.branch,
                                    relPath = ""
                                )
                            )
                        }
                        pipelineUtils.isOldCodeCCElement(element) -> {
                            pipelineUtils.transferOldCodeCCElementToNew()
                        }
                        else -> {
                            element
                        }
                    }
                    elementList.add(newElement)

                }
                container.elements = elementList

                // 改buildEnv
                val newContainer = if (container is VMBuildContainer) {
                    VMBuildContainer(
                        id = container.id,
                        name = container.name,
                        elements = elementList,
                        status = container.status,
                        startEpoch = container.startEpoch,
                        systemElapsed = container.systemElapsed,
                        elementElapsed = container.elementElapsed,
                        baseOS = container.baseOS,
                        vmNames = container.vmNames,
                        maxQueueMinutes = container.maxQueueMinutes,
                        maxRunningMinutes = container.maxRunningMinutes,
                        buildEnv = registerVO.buildEnv,
                        customBuildEnv = container.customBuildEnv,
                        thirdPartyAgentId = container.thirdPartyAgentId,
                        thirdPartyAgentEnvId = container.thirdPartyAgentEnvId,
                        thirdPartyWorkspace = container.thirdPartyWorkspace,
                        dockerBuildVersion = container.dockerBuildVersion,
                        dispatchType = container.dispatchType,
                        canRetry = container.canRetry,
                        enableExternal = container.enableExternal,
                        containerId = container.containerId,
                        jobControlOption = container.jobControlOption,
                        mutexGroup = container.mutexGroup,
                        tstackAgentId = container.tstackAgentId
                    )
                } else {
                    container
                }

                containerList.add(newContainer)
            }
            val newStage = Stage(
                containers = containerList,
                id = null
            )
            stageList.add(newStage)
        }

        val newModel = with(model) {
            Model(name, desc, stageList, labels, instanceFromTemplate, pipelineCreator)
        }

        // 更新流水线model
        with(taskEntity) {
            val channelCode = pipelineUtils.getDevopsChannelCode(createFrom, taskEntity.nameEn)
            val edit = client.getDevopsService(ServicePipelineResource::class.java).edit(userName, projectId, pipelineId, newModel, channelCode)
            if (Objects.nonNull(edit) && edit.data != true) {
                throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
            }
            logger.info("update codecc task schedule successfully! project id: $projectId, pipeline id: $pipelineId")
        }

        return true
    }

    override fun createGongfengDevopsProject(newProject: GongfengPublicProjModel): String {
        // TODO("not implemented")
        return ""
    }

    override fun installAtom(userName: String, projectIds: ArrayList<String>, atomCode: String): Boolean {
        val result = client.getDevopsService(ServiceMarketAtomResource::class.java).installAtom(
                userName,
                ChannelCode.GONGFENGSCAN,
                InstallAtomReq(projectIds, atomCode)
        )

        if (result.isNotOk() || result.data == null) {
            logger.info("install atom for open scan project fail, userName: $userName | projects: $projectIds, atomCode: $atomCode")
            return false
        }

        return result.data!!
    }

    override fun createActiveProjDevopsProject(activeProjParseModel: ActiveProjParseModel): String {
        // TODO("not implemented")
        return ""
    }

    override fun createGongfengDevopsPipeline(
        gongfengPublicProjModel: GongfengPublicProjModel,
        projectId: String
    ): String {
        return with(gongfengPublicProjModel) {
            createGongfengCommonPipeline(
                    httpRepoUrl = httpUrlToRepo,
                    id = id,
                    pipelineDesc = description,
                    owner = owner?.userName,
                    projectId = projectId,
                    type = "normal",
                    branchName = (gongfengPublicProjModel.defaultBranch ?: "master"),
                    dispatchRoute = ComConstants.CodeCCDispatchRoute.OPENSOURCE
            )
        }
    }

    override fun createGongfengActivePipeline(
            activeProjParseModel: ActiveProjParseModel,
            projectId: String, taskName: String, taskId: Long
    ): String {
        return with(activeProjParseModel) {
            createGongfengCommonPipeline(
                    httpRepoUrl = "$gitCodePath/$gitPath",
                    id = id,
                    pipelineDesc = "工蜂活跃项目$id",
                    owner = activeProjParseModel.creator,
                    projectId = projectId,
                    type = "active",
                    dispatchRoute = ComConstants.CodeCCDispatchRoute.OPENSOURCE
            )
        }
    }
    
    override fun updateExistsCommonPipeline(
            gongfengPublicProjEntity: GongfengPublicProjEntity,
            projectId: String, taskId: Long, pipelineId: String, owner: String,
            dispatchRoute: ComConstants.CodeCCDispatchRoute, commitId: String?
    ): Boolean {
        /**
         * 第一个stage的内容
         */
        val elementFirst = ManualTriggerElement(
                name = "手动触发",
                id = null,
                status = null,
                canElementSkip = false,
                useLatestParameters = false
        )

        val paramList = listOf(
            BuildFormProperty(
                id = "scheduledTriggerPipeline",
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = 0,
                options = null,
                desc = "是否是手动触发定时开源扫描流水线",
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            )
        )

        val containerFirst = TriggerContainer(
            id = null,
            name = "demo",
            elements = arrayListOf(elementFirst),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            params = paramList,
            templateParams = null,
            buildNo = null,
            canRetry = null,
            containerId = null,
            jobId = null
        )

        val stageFirst = Stage(
            containers = arrayListOf(containerFirst),
            id = null
        )

        /**
         * 第二个stage
         */
        //用于判断是否需要过滤的插件
        val filterElement: Element = MarketBuildAtomElement(
            name = "动态判断是否需要扫描",
            atomCode = "CodeCCFilter",
            data = mapOf(
                "input" to mapOf(
                    "gongfengProjectId" to gongfengPublicProjEntity.id.toString()
                ),
                "output" to mapOf()
            )
        )

        val gitElement: Element = if (StringUtils.isBlank(commitId)) {
            MarketBuildAtomElement(
                    name = "拉取代码",
                    atomCode = "gitCodeRepoCommon",
                    version = (pluginVersionCache.get("GIT") ?: "2.*"),
                    data = mapOf(
                            "input" to
                                    mapOf(
                                            "username" to codeccPublicAccount,
                                            "password" to codeccPublicPassword,
                                            "refName" to (gongfengPublicProjEntity.defaultBranch
                                                    ?: "master"),
                                            "commitId" to "",
                                            "enableAutoCrlf" to false,
                                            "enableGitClean" to true,
                                            "enableSubmodule" to false,
                                            "enableSubmoduleRemote" to false,
                                            "enableVirtualMergeBranch" to false,
                                            "excludePath" to "",
                                            "fetchDepth" to "",
                                            "includePath" to "",
                                            "localPath" to "",
                                            "paramMode" to "SIMPLE",
                                            "pullType" to "BRANCH",
                                            "repositoryUrl" to gongfengPublicProjEntity.httpUrlToRepo,
                                            "strategy" to "FRESH_CHECKOUT",
                                            "tagName" to ""
                                    ),
                            "output" to mapOf()
                    )
            )
        } else {
            MarketBuildAtomElement(
                    name = "拉取代码",
                    atomCode = "gitCodeRepoCommon",
                    version = (pluginVersionCache.get("GIT") ?: "2.*"),
                    data = mapOf(
                            "input" to
                                    mapOf(
                                            "username" to codeccPublicAccount,
                                            "password" to codeccPublicPassword,
                                            "refName" to "",
                                            "commitId" to commitId,
                                            "enableAutoCrlf" to false,
                                            "enableGitClean" to true,
                                            "enableSubmodule" to false,
                                            "enableSubmoduleRemote" to false,
                                            "enableVirtualMergeBranch" to false,
                                            "excludePath" to "",
                                            "fetchDepth" to "",
                                            "includePath" to "",
                                            "localPath" to "",
                                            "paramMode" to "SIMPLE",
                                            "pullType" to "BRANCH",
                                            "repositoryUrl" to gongfengPublicProjEntity.httpUrlToRepo,
                                            "strategy" to "FRESH_CHECKOUT",
                                            "tagName" to ""
                                    ),
                            "output" to mapOf()
                    )
            )
        }


        gitElement.additionalOptions = ElementAdditionalOptions(
            enable = true,
            continueWhenFailed = true,
            retryWhenFailed = false,
            retryCount = 1,
            timeout = 900L,
            runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
            otherTask = "",
            customVariables = listOf(
                NameAndValue(
                    key = "BK_CI_CODECC_SCAN_FILTER_PASS",
                    value = "true"
                )
            ),
            customCondition = "",
            pauseBeforeExec = false,
            subscriptionPauseUser = ""
        )

        val codeccElement: Element =
            MarketBuildAtomElement(
                name = "CodeCC代码检查(New)",
                atomCode = pipelineUtils.CODECC_ATOM_CODE,
                version = (pluginVersionCache.get("CODECC") ?: "4.*"),
                data = mapOf(
                    "input" to mapOf(
                        "languages" to listOf<String>(),
                        "tools" to listOf("CLOC"),
                        "asynchronous" to "false",
                        "openScanPrj" to true
                    ),
                    "output" to mapOf()
                )
            )

        codeccElement.additionalOptions = ElementAdditionalOptions(
            enable = true,
            continueWhenFailed = true,
            retryWhenFailed = false,
            retryCount = 1,
            timeout = 900L,
            runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
            otherTask = "",
            customVariables = listOf(
                NameAndValue(
                    key = "BK_CI_GIT_REPO_BRANCH",
                    value = (gongfengPublicProjEntity.defaultBranch ?: "master")
                ),
                NameAndValue(
                    key = "BK_CI_CODECC_SCAN_FILTER_PASS",
                    value = "true"
                )
            ),
            customCondition = "",
            pauseBeforeExec = false,
            subscriptionPauseUser = ""
        )

        val judgeElement = LinuxScriptElement(
            name = "判断是否推送",
            scriptType = BuildScriptType.SHELL,
            script = "# 您可以通过setEnv函数设置插件间传递的参数\n# setEnv \"FILENAME\" \"package.zip\"\n# 然后在后续的插件的表单中使用\${FILENAME}引用这个变量\n\n# 您可以在质量红线中创建自定义指标，然后通过setGateValue函数设置指标值\n# setGateValue \"CodeCoverage\" \$myValue\n# 然后在质量红线选择相应指标和阈值。若不满足，流水线在执行时将会被卡住\n\n# cd \${WORKSPACE} 可进入当前工作空间目录\necho \$BK_CI_GIT_REPO_BRANCH\necho \$BK_CI_CODECC_TASK_STATUS\nif [ \$BK_CI_GIT_REPO_BRANCH ] && [ \$BK_CI_GIT_REPO_BRANCH = 'master' ]\nthen\n    if [ ! \$BK_CI_CODECC_TASK_STATUS ]\n    then\n        setEnv BK_CI_CODECC_MESSAGE_PUSH 'true'\n    fi\nfi\necho \$BK_CI_CODECC_MESSAGE_PUSH\n",
            continueNoneZero = false,
            enableArchiveFile = false,
            archiveFile = ""
        )

        val notifyElement = MarketBuildLessAtomElement(
            name = "企业微信机器人推送",
            atomCode = "WechatWorkRobot",
            data = mapOf(
                "input" to mapOf(
                    "inputWebhook" to "e01e5f25-4362-4c6f-b163-18b4c529163b",
                    "inputChatid" to "",
                    "inputMsgtype" to "text",
                    "inputTextContent" to "【开源扫描构建失败提醒】\n蓝盾项目id:\${BK_CI_PROJECT_NAME}\n流水线id: \${BK_CI_PIPELINE_ID}\n构建id: \${BK_CI_BUILD_ID}\n构建机日志路径: /data/landun/logs/\${BK_CI_BUILD_ID}/1",
                    "inputTextMentionedList" to "",
                    "inputMarkdownContent" to "",
                    "inputImageBase64" to "",
                    "inputImageMd5" to "",
                    "inputNewsTitle" to "",
                    "inputNewsDescription" to "",
                    "inputNewsUrl" to "",
                    "inputNewsPicurl" to "",
                    "inputRetry" to 1

                ),
                "output" to mapOf()
            )
        )

        with(notifyElement) {
            executeCount = 1
            canRetry = false
            additionalOptions = ElementAdditionalOptions(
                enable = true,
                continueWhenFailed = false,
                retryWhenFailed = false,
                retryCount = 1,
                timeout = 900,
                runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                otherTask = "",
                customVariables = listOf(
                    NameAndValue(
                        key = "BK_CI_CODECC_MESSAGE_PUSH",
                        value = "true"
                    )
                ),
                customCondition = "",
                pauseBeforeExec = false,
                subscriptionPauseUser = ""
            )
        }

        val containerSecond = VMBuildContainer(
            id = null,
            name = "demo",
            elements = listOf(filterElement, gitElement, codeccElement, judgeElement, notifyElement),
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = VMBaseOS.valueOf("LINUX"),
            vmNames = emptySet(),
            maxQueueMinutes = null,
            maxRunningMinutes = 80,
            buildEnv = null,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = imageName,
            dispatchType = CodeCCDispatchType(
                codeccTaskId = dispatchRoute.flag(),
                extraInfo = mapOf()
            ),
            canRetry = null,
            enableExternal = null,
            containerId = null,
            jobControlOption = JobControlOption(
                enable = true,
                timeout = 600,
                runCondition = JobRunCondition.STAGE_RUNNING,
                customVariables = null,
                customCondition = null
            ),
            mutexGroup = null,
            tstackAgentId = null
        )
        val stageSecond = Stage(
            containers = arrayListOf(containerSecond),
            id = null
        )

        logger.info(
            "assemble pipeline parameter successfully! gongfeng " +
                "project id: ${gongfengPublicProjEntity.id}"
        )
        /**
         * 总流水线拼装
         */
        val pipelineModel = Model(
            name = "CODEPIPELINE_${gongfengPublicProjEntity.id}",
            desc = gongfengPublicProjEntity.description,
            stages = arrayListOf(stageFirst, stageSecond),
            labels = emptyList(),
            instanceFromTemplate = null,
            pipelineCreator = null,
            srcTemplateId = null
        )

        val pipelineCreateResult = client.getDevopsService(ServicePipelineResource::class.java).edit(
                userId = owner.split(",")[0],
                projectId = projectId,
                pipelineId = pipelineId,
                pipeline = pipelineModel,
                channelCode = ChannelCode.GONGFENGSCAN
        )
        return if (pipelineCreateResult.isOk()) pipelineCreateResult.data
                ?: throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR) else throw CodeCCException(
                CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR
        )
    }

    override fun updateExistsCommonPipeline(
            gongfengId: Int, url: String,
            projectId: String, taskId: Long, pipelineId: String, owner: String,
            dispatchRoute: ComConstants.CodeCCDispatchRoute, commitId: String?
    ): Boolean {
        /**
         * 第一个stage的内容
         */
        val elementFirst = ManualTriggerElement(
                name = "手动触发",
                id = null,
                status = null,
                canElementSkip = false,
                useLatestParameters = false
        )

        val paramList = listOf(
                BuildFormProperty(
                        id = "scheduledTriggerPipeline",
                        required = false,
                        type = BuildFormPropertyType.STRING,
                        defaultValue = 0,
                        options = null,
                        desc = "是否是手动触发定时开源扫描流水线",
                        repoHashId = null,
                        relativePath = null,
                        scmType = null,
                        containerType = null,
                        glob = null,
                        properties = null
                )
        )

        val containerFirst = TriggerContainer(
                id = null,
                name = "demo",
                elements = arrayListOf(elementFirst),
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                params = paramList,
                templateParams = null,
                buildNo = null,
                canRetry = null,
                containerId = null
        )

        val stageFirst = Stage(
                containers = arrayListOf(containerFirst),
                id = null
        )

        /**
         * 第二个stage
         */
        //用于判断是否需要过滤的插件
        val filterElement: Element = MarketBuildAtomElement(
                name = "动态判断是否需要扫描",
                atomCode = "CodeCCFilter",
                data = mapOf(
                        "input" to mapOf(
                                "gongfengProjectId" to gongfengId.toString()
                        ),
                        "output" to mapOf()
                )
        )

        val gitElement: Element = MarketBuildAtomElement(
                name = "拉取代码",
                atomCode = "gitCodeRepoCommon",
                version = (pluginVersionCache.get("GIT") ?: "2.*"),
                data = mapOf(
                        "input" to
                                mapOf(
                                        "username" to codeccPublicAccount,
                                        "password" to codeccPublicPassword,
                                        "refName" to if (StringUtils.isBlank(commitId)) {
                                            ("master")
                                        } else {
                                            ""
                                        },
                                        "commitId" to commitId,
                                        "enableAutoCrlf" to false,
                                        "enableGitClean" to true,
                                        "enableSubmodule" to false,
                                        "enableSubmoduleRemote" to false,
                                        "enableVirtualMergeBranch" to false,
                                        "excludePath" to "",
                                        "fetchDepth" to "",
                                        "includePath" to "",
                                        "localPath" to "",
                                        "paramMode" to "SIMPLE",
                                        "pullType" to if (StringUtils.isBlank(commitId)) {
                                            "BRANCH"
                                        } else {
                                            "COMMIT_ID"
                                        },
                                        "repositoryUrl" to url,
                                        "strategy" to "FRESH_CHECKOUT",
                                        "tagName" to ""
                                ),
                        "output" to mapOf()
                )
        )


        gitElement.additionalOptions = ElementAdditionalOptions(
                enable = true,
                continueWhenFailed = true,
                retryWhenFailed = false,
                retryCount = 1,
                timeout = 900L,
                runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                otherTask = "",
                customVariables = listOf(
                        NameAndValue(
                                key = "BK_CI_CODECC_SCAN_FILTER_PASS",
                                value = "true"
                        )
                ),
                customCondition = "",
            pauseBeforeExec = false,
            subscriptionPauseUser = ""
        )

        val codeccElement: Element =
                MarketBuildAtomElement(
                        name = "CodeCC代码检查(New)",
                        atomCode = pipelineUtils.CODECC_ATOM_CODE,
                        version = (pluginVersionCache.get("CODECC") ?: "4.*"),
                        data = mapOf(
                                "input" to mapOf(
                                        "languages" to listOf<String>(),
                                        "tools" to listOf("CLOC"),
                                        "asynchronous" to "false",
                                        "openScanPrj" to true
                                ),
                                "output" to mapOf()
                        )
                )

        codeccElement.additionalOptions = ElementAdditionalOptions(
                enable = true,
                continueWhenFailed = true,
                retryWhenFailed = false,
                retryCount = 1,
                timeout = 900L,
                runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                otherTask = "",
                customVariables = listOf(
                        NameAndValue(
                                key = "BK_CI_GIT_REPO_BRANCH",
                                value = ("master")
                        ),
                        NameAndValue(
                                key = "BK_CI_CODECC_SCAN_FILTER_PASS",
                                value = "true"
                        )
                ),
                customCondition = "",
                pauseBeforeExec = false,
                subscriptionPauseUser = ""
        )

        val judgeElement = LinuxScriptElement(
                name = "判断是否推送",
                scriptType = BuildScriptType.SHELL,
                script = "# 您可以通过setEnv函数设置插件间传递的参数\n# setEnv \"FILENAME\" \"package.zip\"\n# 然后在后续的插件的表单中使用\${FILENAME}引用这个变量\n\n# 您可以在质量红线中创建自定义指标，然后通过setGateValue函数设置指标值\n# setGateValue \"CodeCoverage\" \$myValue\n# 然后在质量红线选择相应指标和阈值。若不满足，流水线在执行时将会被卡住\n\n# cd \${WORKSPACE} 可进入当前工作空间目录\necho \$BK_CI_GIT_REPO_BRANCH\necho \$BK_CI_CODECC_TASK_STATUS\nif [ \$BK_CI_GIT_REPO_BRANCH ] && [ \$BK_CI_GIT_REPO_BRANCH = 'master' ]\nthen\n    if [ ! \$BK_CI_CODECC_TASK_STATUS ]\n    then\n        setEnv BK_CI_CODECC_MESSAGE_PUSH 'true'\n    fi\nfi\necho \$BK_CI_CODECC_MESSAGE_PUSH\n",
                continueNoneZero = false,
                enableArchiveFile = false,
                archiveFile = ""
        )

        val notifyElement = MarketBuildLessAtomElement(
                name = "企业微信机器人推送",
                atomCode = "WechatWorkRobot",
                data = mapOf(
                        "input" to mapOf(
                                "inputWebhook" to "e01e5f25-4362-4c6f-b163-18b4c529163b",
                                "inputChatid" to "",
                                "inputMsgtype" to "text",
                                "inputTextContent" to "【开源扫描构建失败提醒】\n蓝盾项目id:\${BK_CI_PROJECT_NAME}\n流水线id: \${BK_CI_PIPELINE_ID}\n构建id: \${BK_CI_BUILD_ID}\n构建机日志路径: /data/landun/logs/\${BK_CI_BUILD_ID}/1",
                                "inputTextMentionedList" to "",
                                "inputMarkdownContent" to "",
                                "inputImageBase64" to "",
                                "inputImageMd5" to "",
                                "inputNewsTitle" to "",
                                "inputNewsDescription" to "",
                                "inputNewsUrl" to "",
                                "inputNewsPicurl" to "",
                                "inputRetry" to 1

                        ),
                        "output" to mapOf()
                )
        )

        with(notifyElement) {
            executeCount = 1
            canRetry = false
            additionalOptions = ElementAdditionalOptions(
                    enable = true,
                    continueWhenFailed = false,
                    retryWhenFailed = false,
                    retryCount = 1,
                    timeout = 900,
                    runCondition = RunCondition.CUSTOM_VARIABLE_MATCH,
                    otherTask = "",
                    customVariables = listOf(
                            NameAndValue(
                                    key = "BK_CI_CODECC_MESSAGE_PUSH",
                                    value = "true"
                            )
                    ),
                    customCondition = "",
                    pauseBeforeExec = false,
                    subscriptionPauseUser = ""
            )
        }

        val containerSecond = VMBuildContainer(
                id = null,
                name = "demo",
                elements = listOf(filterElement, gitElement, codeccElement, judgeElement, notifyElement),
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                baseOS = VMBaseOS.valueOf("LINUX"),
                vmNames = emptySet(),
                maxQueueMinutes = null,
                maxRunningMinutes = 80,
                buildEnv = null,
                customBuildEnv = null,
                thirdPartyAgentId = null,
                thirdPartyAgentEnvId = null,
                thirdPartyWorkspace = null,
                dockerBuildVersion = imageName,
                dispatchType = CodeCCDispatchType(
                        codeccTaskId = dispatchRoute.flag(),
                        extraInfo = mapOf()
                ),
                canRetry = null,
                enableExternal = null,
                containerId = null,
                jobControlOption = JobControlOption(
                        enable = true,
                        timeout = 600,
                        runCondition = JobRunCondition.STAGE_RUNNING,
                        customVariables = null,
                        customCondition = null
                ),
                mutexGroup = null,
                tstackAgentId = null
        )
        val stageSecond = Stage(
                containers = arrayListOf(containerSecond),
                id = null
        )

        logger.info(
                "assemble pipeline parameter successfully! gongfeng " +
                        "project id: $gongfengId"
        )
        /**
         * 总流水线拼装
         */
        val pipelineModel = Model(
                name = "CODEPIPELINE_$gongfengId",
                desc = "",
                stages = arrayListOf(stageFirst, stageSecond),
                labels = emptyList(),
                instanceFromTemplate = null,
                pipelineCreator = null,
                srcTemplateId = null
        )

        val pipelineCreateResult = client.getDevopsService(ServicePipelineResource::class.java).edit(
                userId = owner.split(",")[0],
                projectId = projectId,
                pipelineId = pipelineId,
                pipeline = pipelineModel,
                channelCode = ChannelCode.GONGFENGSCAN
        )
        return if (pipelineCreateResult.isOk()) pipelineCreateResult.data
                ?: throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR) else throw CodeCCException(
                CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR
        )
    }

    private fun createGongfengCommonPipeline(
            httpRepoUrl: String?, id: Int,
            pipelineDesc: String?, owner: String?, projectId: String,
            type: String, branchName: String = "master",
            dispatchRoute: ComConstants.CodeCCDispatchRoute
    ): String {
        // TODO("not implemented")
        return ""
    }

    /**
     * 获取开源扫描插件版本号
     */
    private fun getOpenSourceVersion(versionType : String) : String?{
        val baseDataList = baseDataRepository.findAllByParamTypeAndParamCode(ComConstants.KEY_OPENSOURCE_VERSION, versionType)
        return if(baseDataList.isNullOrEmpty()) null else baseDataList[0].paramValue
    }

    override fun updatePipelineTools(
        userName: String, taskId: Long, toolList: List<String>, taskInfoEntity: TaskInfoEntity?,
        updateType: ComConstants.PipelineToolUpdateType, registerVO: BatchRegisterVO?, relPath: String?
    ): Set<String> {
        var currentToolSet = mutableSetOf<String>()
        if (null == taskInfoEntity) {
            return currentToolSet
        }

        // 获取CodeCC原子
        val codeElement = pipelineUtils.getOldCodeElement(registerVO, relPath)

        val scriptType =
            BuildScriptType.valueOf(if (registerVO?.projectBuildType.isNullOrBlank()) BuildScriptType.SHELL.name else registerVO!!.projectBuildType)
        val script = if (registerVO?.projectBuildCommand.isNullOrBlank()) "echo" else registerVO?.projectBuildCommand
        val osType =
            if (registerVO != null && !registerVO.osType.isNullOrBlank()) VMBaseOS.valueOf(registerVO.osType) else null
        val buildEnv = if (registerVO?.buildEnv != null) registerVO.buildEnv else null
        updateCodeCCModel(
            userName,
            taskInfoEntity,
            toolList,
            updateType,
            null,
            scriptType,
            script,
            codeElement,
            osType,
            buildEnv
        )
        return currentToolSet
    }

    /**
     * 初始化老的服务型流水线的Params元素，以配合流水线支持触发单个工具
     *
     * @date 2019/6/27
     * @version V4.0
     */
    private fun initParamsElementForStageOne(stage: Stage): Stage {
        if ("stage-1" == stage.id) {
            val containerList = mutableListOf<Container>()
            stage.containers.forEachIndexed { index, container ->
                val newContainer = if (index == 0) {
                    val params = (container as TriggerContainer).params.toMutableList()
                    val containParams = checkIsPipelineContainParams(params)
                    //params肯定不会为空
                    if (!containParams) {
                        val param = BuildFormProperty(
                            id = "_CODECC_FILTER_TOOLS",
                            required = false,
                            type = BuildFormPropertyType.STRING,
                            defaultValue = "",
                            options = null,
                            desc = "",
                            repoHashId = null,
                            relativePath = null,
                            scmType = null,
                            containerType = null,
                            glob = null,
                            properties = mapOf()
                        )
                        params.add(param)
                    }
                    TriggerContainer(
                        id = container.id,
                        name = container.name,
                        elements = container.elements,
                        status = container.status,
                        startEpoch = container.startEpoch,
                        systemElapsed = container.systemElapsed,
                        elementElapsed = container.elementElapsed,
                        params = params,
                        templateParams = container.templateParams,
                        buildNo = container.buildNo,
                        canRetry = container.canRetry,
                        containerId = container.containerId,
                        jobId = null
                    )
                } else {
                    container
                }
                containerList.add(newContainer)
            }
            return Stage(containerList, stage.id)
        }
        return stage
    }

    /**
     * 检查pipeline里面的params是否已经有值,有值返回true，否则返回false
     *
     * @param paramList
     * @return
     */
    private fun checkIsPipelineContainParams(paramList: List<BuildFormProperty>): Boolean {
        return if (paramList.isNullOrEmpty()) {
            false
        } else
            paramList.any { it.id.isBlank() && "_CODECC_FILTER_TOOLS".equals(it.id, ignoreCase = true) }
    }

    /**
     * 启动流水线
     *
     * @param taskInfoEntity
     * @param toolName
     * @param userName
     * @return
     */
    override fun startPipeline(
        pipelineId: String,
        projectId: String,
        nameEn: String,
        createFrom: String?,
        toolName: List<String>,
        userName: String
    ): String {
        val buildId: String
        val valueMap = mapOf(
            "_CODECC_FILTER_TOOLS" to toolName.joinToString(","),
            "scheduledTriggerPipeline" to "false",
            "manualTriggerPipeline" to "true",
            "openSourceCheckerSetType" to ""
        )
        var channelCode = ChannelCode.CODECC_EE
        if (nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
            channelCode = ChannelCode.CODECC
        } else if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() == createFrom) {
            channelCode = ChannelCode.GONGFENGSCAN
        }
        val buildIdResult = client.getDevopsService(ServiceBuildResource::class.java).manualStartup(
            userName, projectId, pipelineId, valueMap, channelCode
        )
        if (buildIdResult.isNotOk() || null == buildIdResult.data) {
            logger.error(
                "start pipeline fail! project id: {}, pipeline id: {}, msg is: {}", projectId,
                pipelineId, buildIdResult.message
            )
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        buildId = buildIdResult.data?.id ?: ""
        logger.info("return build id: {}", buildId)

        //根据构件号获取构建number, 设置7s超时
        var buildNo: String? = null

        var buildInfoResult: Result<Map<String, CodeccBuildInfo>>
        var realRetryAttempt = retryAttempt
        while (realRetryAttempt > 0 && null == buildNo) {
            try {
                Thread.sleep(retryInterval)
            } catch (e: InterruptedException) {
                logger.error("current thread interrupted!")
                e.printStackTrace()
            }

            realRetryAttempt--
            buildInfoResult =
                client.getDevopsService(ServiceCodeccResource::class.java).getCodeccBuildInfo(setOf(buildId))
            if (buildInfoResult.isOk() &&
                null != buildInfoResult.data &&
                buildInfoResult.data!!.containsKey(buildId)
            ) {
                buildNo = buildInfoResult.data!![buildId]?.buildNo
            }
        }

        if (buildId.isBlank() || buildNo.isNullOrBlank()) {
            logger.error(
                "start devops pipeline failed! project id: {}, pipeline id: {}, build id: {}",
                projectId, pipelineId, buildId
            )
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return buildId
    }

    @Async("asyncTaskExecutor")
    override fun updateTaskInitStep(
        isFirstTrigger: String?, taskInfoEntity: TaskInfoEntity,
        pipelineBuildId: String, toolName: String, userName: String
    ) {
        val uploadTaskLogStepVO = UploadTaskLogStepVO()
        uploadTaskLogStepVO.stepNum = 1
        uploadTaskLogStepVO.startTime = System.currentTimeMillis()
        uploadTaskLogStepVO.endTime = 0L

        uploadTaskLogStepVO.msg = if (isFirstTrigger?.toBoolean() == true) {
            "添加${toolMetaCacheService.getToolDisplayName(toolName)}后自动触发第一次分析"
        } else {
            "手动触发($userName)"
        }

        uploadTaskLogStepVO.flag = TaskConstants.TASK_FLAG_PROCESSING
        uploadTaskLogStepVO.toolName = toolName
        uploadTaskLogStepVO.streamName = taskInfoEntity.nameEn
        uploadTaskLogStepVO.pipelineBuildId = pipelineBuildId
        uploadTaskLogStepVO.triggerFrom = userName

        val uploadResult = client.get(ServiceReportTaskLogRestResource::class.java).uploadTaskLog(uploadTaskLogStepVO)
        if (uploadResult.isNotOk()) {
            logger.error(
                "upload task analysis log fail! stream name: {}, tool name: {}",
                taskInfoEntity.nameEn, toolName
            )
            throw CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL)
        }
        logger.info("update task log step status success! task id: ${taskInfoEntity.taskId}, tool name: $toolName")
    }

    override fun getRepositoryList(projCode: String): List<RepoInfoVO> {
        val repoResult = client.getDevopsService(ServiceRepositoryResource::class.java).list(projCode, null)
        if (repoResult.isNotOk() || null == repoResult.data) {
            logger.error("get repo list fail!")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val supportRepoType = listOf<String>(ScmType.CODE_GIT.name,
            ScmType.CODE_GITLAB.name,
            ScmType.CODE_SVN.name,
            ScmType.GITHUB.name)
        return repoResult.data!!.filter { repo ->
            supportRepoType.contains(repo.type.name)
        }.map { (repositoryHashId, aliasName, url, type, _, _, _, authType) ->
            val repoInfoVO = RepoInfoVO()
            repoInfoVO.repoHashId = repositoryHashId
            repoInfoVO.url = url
            repoInfoVO.authType = authType
            repoInfoVO.type = type.name
            repoInfoVO.aliasName = aliasName
            repoInfoVO
        }
    }

    /**
     * 获取代码库分支
     *
     * @param projCode
     * @param url
     * @param scmType
     * @return
     */
    override fun getRepositoryBranches(projCode: String, url: String, scmType: String): List<String>? {

        val repoResult = client.getDevopsService(ServiceScmResource::class.java)
            .listBranches(projCode, url, ScmType.valueOf(scmType), null, null, null, null, null)
        if (repoResult.isNotOk() || null == repoResult.data) {
            logger.error("get repo list fail!")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return repoResult.data
    }

    /**
     * 将codecc平台的项目语言转换为蓝盾平台的codecc原子语言
     */
    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun localConvertDevopsCodeLang(langCode: Long): List<String> {
        val metadataList = metaService.queryMetadatas(ComConstants.METADATA_TYPE)[ComConstants.METADATA_TYPE]
        val languageList = metadataList?.filter { metadataVO ->
            (metadataVO.key.toLong() and langCode) != 0L
        }
            ?.map { metadataVO ->
                JSONArray.fromObject(metadataVO.aliasNames)[0].toString()
            }
        return if (languageList.isNullOrEmpty()) listOf(ComConstants.CodeLang.OTHERS.langName()) else languageList
    }

    @Throws(StreamException::class)
    override fun convertDevopsCodeLangToCodeCC(codeLang: String): Long? {
        if (codeLang.isBlank()) {
            return 0L
        }
        val metaLangList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG)
        val reqLangList = objectMapper.readValue<List<String>>(codeLang, object : TypeReference<List<String>>() {

        })
        return metaLangList.filter { metaLang ->
            val langArray = JSONArray.fromObject(metaLang.paramExtend2)
            langArray.any { reqLangList.contains(it as String) }
        }.map { metaLang ->
            metaLang.paramCode.toLong()
        }.ifEmpty { return 0L }.reduce { acc, l -> acc or l }
    }


    @Throws(StreamException::class)
    override fun convertDevopsCodeLangToCodeCCWithOthers(codeLang: String): Long? {
        if (codeLang.isBlank()) {
            return 0L
        }
        val metaLangList = baseDataRepository.findAllByParamType(ComConstants.KEY_CODE_LANG)
        val reqLangList = objectMapper.readValue<List<String>>(codeLang, object : TypeReference<List<String>>() {

        })
        var filteredReqLangList = reqLangList

        val filteredMetaLangList = metaLangList.filter { metaLang ->
            val langArray = JSONArray.fromObject(metaLang.paramExtend2)
            filteredReqLangList = reqLangList.filterNot { langArray.toList().contains(it) }
            langArray.any { reqLangList.contains(it as String) }
        }
        val otherBaseData = metaLangList.find { metaLang ->
            val langArray = JSONArray.fromObject(metaLang.paramExtend2)
            langArray.toList().contains("OTHERS")
        }
        return if(!filteredReqLangList.isNullOrEmpty() && null != otherBaseData){
            filteredMetaLangList.map { metaLang ->
                metaLang.paramCode.toLong()
            }.ifEmpty { return 0L }.reduce { acc, l -> acc or l } or otherBaseData!!.paramCode.toLong()
        } else {
            filteredMetaLangList.map { metaLang ->
                metaLang.paramCode.toLong()
            }.ifEmpty { return 0L }.reduce { acc, l -> acc or l }
        }
    }

    override fun convertCodeCCLangToString(codeLang: Long): Set<String> {
        val metadataList = metaService.queryMetadatas(ComConstants.METADATA_TYPE)[ComConstants.METADATA_TYPE]
        return metadataList?.filter { (it.key.toLong() and codeLang) != 0L }?.map { it.langFullKey }?.toSet()?: setOf()
    }

    override fun modifyCodeCCTiming(
        taskInfoEntity: TaskInfoEntity,
        executeDate: List<String>,
        executeTime: String,
        userName: String
    ) {
        val projectId = taskInfoEntity.projectId
        val pipelineId = taskInfoEntity.pipelineId
        val channelCode = if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == taskInfoEntity.createFrom) {
            ChannelCode.BS
        } else if (taskInfoEntity.nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
            ChannelCode.CODECC
        } else {
            ChannelCode.CODECC_EE
        }
        val modelResult = client.getDevopsService(ServicePipelineResource::class.java)
            .get(userName, projectId, pipelineId, channelCode)
        if (modelResult.isNotOk() || null == modelResult.data) {
            logger.error("get pipeline info fail! bs project id: {}, bs pipeline id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        val model = modelResult.data
        val newModel = with(model!!)
        {
            val stageList = kotlin.collections.mutableListOf<Stage>()
            stages.forEachIndexed { stageIndex, stage ->
                val newStage =
                    if (stageIndex == 0) {
                        val containerList = kotlin.collections.mutableListOf<Container>()
                        stage.containers.forEachIndexed { containerIndex, container ->
                            val newContainer: Container = if (container is TriggerContainer &&
                                containerIndex == 0
                            ) {
                                val newElements = container.elements.toMutableList()
                                if (newElements.size > 1) {
                                    newElements.removeAt(1)
                                }
                                //定时时间或者日期为空则删除定时任务编排
                                if (!executeTime.isBlank() && !executeDate.isNullOrEmpty()) {
                                    val cronTabStr = pipelineUtils.getCrontabTimingStr(executeTime, executeDate)
                                    //无论原来是否有定时任务原子，都新建更新
                                    val timerTriggerElement =
                                        TimerTriggerElement("定时触发", null, null,
                                            cronTabStr,
                                            null, listOf(cronTabStr), null
                                        )
                                    newElements.add(timerTriggerElement)
                                }
                                TriggerContainer(
                                    id = container.id,
                                    name = container.name,
                                    elements = newElements,
                                    status = container.status,
                                    startEpoch = container.startEpoch,
                                    systemElapsed = container.systemElapsed,
                                    elementElapsed = container.elementElapsed,
                                    params = container.params,
                                    templateParams = container.templateParams,
                                    buildNo = container.buildNo,
                                    canRetry = container.canRetry,
                                    containerId = container.containerId,
                                    jobId = null
                                )
                            } else {
                                container
                            }
                            containerList.add(newContainer)
                        }
                        Stage(containerList, stage.id)
                    } else {
                        stage
                    }
                stageList.add(newStage)
            }
            Model(
                name,
                desc,
                stageList,
                labels,
                instanceFromTemplate,
                pipelineCreator
            )
        }

        val modifyResult = client.getDevopsService(ServicePipelineResource::class.java)
            .edit(userName, projectId, pipelineId, newModel, channelCode)
        if (modifyResult.isNotOk() || modifyResult.data != true) {
            logger.error("mongotemplate bs pipeline info fail! bs project id: {}", projectId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        logger.info("modify codecc task schedule successfully! project id: $projectId, pipeline id: $pipelineId")

        taskInfoEntity.executeDate = executeDate
        taskInfoEntity.executeTime = executeTime
    }

    override fun deleteCodeCCTiming(userName: String, taskEntity: TaskInfoEntity) {
        var createFrom = taskEntity.getCreateFrom()
        var pipelineId = taskEntity.getPipelineId()
        var projectId = taskEntity.getProjectId()
        val channelCode = if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == createFrom)
            ChannelCode.BS
        else if (taskEntity.nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
            ChannelCode.CODECC
        } else
            ChannelCode.CODECC_EE

        val modelResult = client.getDevopsService(ServicePipelineResource::class.java)
            .get(userName, projectId, pipelineId, channelCode)
        if (modelResult.isNotOk() || null == modelResult.data) {
            logger.error("get pipeline info fail! bs project id: {}, bs pipeline id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        val model = modelResult.data

        val newModel = with(model!!)
        {
            val stageList = kotlin.collections.mutableListOf<Stage>()
            stages.forEachIndexed { stageIndex, stage ->
                val newStage =
                    if (stageIndex == 0) {
                        val containerList = kotlin.collections.mutableListOf<Container>()
                        stage.containers.forEachIndexed { containerIndex, container ->
                            val newContainer: Container = if (container is TriggerContainer &&
                                containerIndex == 0
                            ) {
                                val newElements = container.elements.toMutableList()
                                //todo 要把定时trigger去掉
                                if (newElements.size >= 2 && newElements[1] is TimerTriggerElement) {
                                    newElements.removeAt(1)
                                }
                                com.tencent.devops.common.pipeline.container.TriggerContainer(
                                    id = container.id,
                                    name = container.name,
                                    elements = newElements,
                                    status = container.status,
                                    startEpoch = container.startEpoch,
                                    systemElapsed = container.systemElapsed,
                                    elementElapsed = container.elementElapsed,
                                    params = container.params,
                                    templateParams = container.templateParams,
                                    buildNo = container.buildNo,
                                    canRetry = container.canRetry,
                                    containerId = container.containerId,
                                    jobId = null
                                )
                            } else {
                                container
                            }
                            containerList.add(newContainer)
                        }
                        com.tencent.devops.common.pipeline.container.Stage(containerList, stage.id)
                    } else {
                        stage
                    }
                stageList.add(newStage)
            }
            com.tencent.devops.common.pipeline.Model(
                name,
                desc,
                stageList,
                labels,
                instanceFromTemplate,
                pipelineCreator
            )
        }
        val modifyResult = client.getDevopsService(ServicePipelineResource::class.java)
            .edit(userName, projectId, pipelineId, newModel, channelCode)
        if (modifyResult.isNotOk()) {
            logger.error("mongotemplate bs pipeline info fail! bs project id: {}", projectId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        logger.info("delete codecc task schedule successfully! project id: $projectId, pipeline id: $pipelineId")
    }

    override fun getRepoDetail(
        taskInfoEntity: TaskInfoEntity,
        analyzeConfigInfoVO: AnalyzeConfigInfoVO
    ): AnalyzeConfigInfoVO {
        val projectId = taskInfoEntity.projectId
        val repoHashId = taskInfoEntity.repoHashId
        val repoResult =
            client.getDevopsService(ServiceRepositoryResource::class.java).get(projectId, repoHashId, RepositoryType.ID)
        if (repoResult.isNotOk()) {
            logger.error("get repo detail fail! project id: $projectId, repo hash id: $repoHashId")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val repository = repoResult.data
        //设置代码库信息
        if (repository is CodeSvnRepository) {
            analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.SVN.name
        } else if (repository is CodeGitRepository) {
            analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.GIT.name
            analyzeConfigInfoVO.gitBranch = taskInfoEntity.branch
        } else if (repository is CodeGitlabRepository) {
            //确认gitlab也是git
            analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.GIT.name
            analyzeConfigInfoVO.gitBranch = taskInfoEntity.branch
        } else if (repository is GithubRepository) {
            analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.GITHUB.name
            analyzeConfigInfoVO.gitBranch = taskInfoEntity.branch
        } else {
            if (null != repository) {
                analyzeConfigInfoVO.scmType = ComConstants.CodeHostingType.GITHUB.name
                analyzeConfigInfoVO.gitBranch = taskInfoEntity.branch
            }
        }
        analyzeConfigInfoVO.url = repository!!.url
        return analyzeConfigInfoVO
    }

    override fun getBuildEnv(
        os: String
    ): List<BuildEnvVO> {
        val repoResult = client.getDevopsService(ServiceContainerAppResource::class.java).listApp(os)
        if (repoResult.isNotOk()) {
            logger.error("get build env list fail! os: $os")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val buildEnvVOList = ArrayList<BuildEnvVO>()
        repoResult.data?.forEach {
            val buildEnvVO = BuildEnvVO()
            buildEnvVO.name = it.name
            buildEnvVO.versions = it.versions
            buildEnvVOList.add(buildEnvVO)
        }
        return buildEnvVOList
    }

    /**
     * 根据蓝盾项目ID列表获取代码库信息Map url，vo
     */
    override fun getRepoUrlByBkProjects(projectIds: Set<String>): Map<String, RepoInfoVO> {
        logger.info("query bk project id count: {}", projectIds.size)
        val result = client.getDevopsService(ServiceRepositoryResource::class.java).listByProjects(
            projectIds, 1,
            20000
        )
        if (result.isNotOk() || result.data == null) {
            logger.error("get repo url info fail! bs project ids: {}", projectIds)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        val repoUrlMap = HashMap<String, RepoInfoVO>()
        val resultData = result.data!!
        logger.info("fetch repo url count: {}", resultData.count)

        resultData.records.forEach {
            val repo = RepoInfoVO()
            BeanUtils.copyProperties(it, repo)
            repoUrlMap[it.url] = repo
        }

        return repoUrlMap
    }

    override fun updateCheckerSets(
        userName: String,
        projectId: String,
        pipelineId: String,
        taskId: Long,
        checkerSets: List<ToolCheckerSetVO>
    ): Boolean {
        var taskEntity = taskRepository.findFirstByTaskId(taskId)
        updateCodeCCModel(userName, taskEntity, null, null, checkerSets, null, null, null, null, null)
        return true
    }

    private fun updateCodeCCModel(
        userName: String,
        taskInfoEntity: TaskInfoEntity,
        toolList: List<String>?,
        toolUpdateType: ComConstants.PipelineToolUpdateType?,
        checkerSets: List<ToolCheckerSetVO>?,
        scriptType: BuildScriptType?,
        script: String?,
        codeElement: Element?,
        osType: VMBaseOS?,
        buildEnv: Map<String, String>?
    ) {
        val projectId = taskInfoEntity.projectId
        val pipelineId = taskInfoEntity.pipelineId
        val createFrom = taskInfoEntity.createFrom

        var currentToolSet = mutableSetOf<String>()

        //如果是从流水线创建的
        val model = getPipelineModel(userName, projectId, pipelineId, createFrom, taskInfoEntity.nameEn)

        val newModel = with(model)
        {
            val stageList = mutableListOf<Stage>()
            stages.forEach { stage ->
                val containerList = mutableListOf<Container>()
                var newStage = stage
                if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == createFrom) {
                    newStage = initParamsElementForStageOne(stage)
                }
                newStage.containers.forEach { container ->

                    val newContainer: Container =
                        if (container is VMBuildContainer &&
                            container.elements.any { it is LinuxCodeCCScriptElement || it is LinuxPaasCodeCCScriptElement }
                        ) {
                            val elementList = mutableListOf<Element>()
                            container.elements.forEach { element ->
                                val newElement: Element =
                                    if (element is LinuxCodeCCScriptElement && element.getClassType() == LinuxCodeCCScriptElement.classType) {
                                        val tools = element.tools
                                        if (!tools.isNullOrEmpty()) {
                                            currentToolSet.addAll(tools)
                                        }
                                        if (toolUpdateType != null && toolList != null) {
                                            when (toolUpdateType) {
                                                ComConstants.PipelineToolUpdateType.ADD ->
                                                    currentToolSet.addAll(toolList)
                                                ComConstants.PipelineToolUpdateType.REMOVE ->
                                                    currentToolSet.removeAll(toolList)
                                                ComConstants.PipelineToolUpdateType.REPLACE ->
                                                    currentToolSet = toolList.toMutableSet()
                                                ComConstants.PipelineToolUpdateType.GET ->
                                                    currentToolSet
                                            }
                                        }
                                        LinuxCodeCCScriptElement(
                                            name = element.name,
                                            id = element.id,
                                            status = element.status,
                                            scriptType = scriptType ?: element.scriptType,
                                            script = script ?: element.script,
                                            codeCCTaskName = element.codeCCTaskName,
                                            codeCCTaskCnName = element.codeCCTaskCnName,
                                            languages = localConvertDevopsCodeLang(taskInfoEntity.codeLang).map { ProjectLanguage.valueOf(it) },
                                            asynchronous = element.asynchronous,
                                            scanType = element.scanType,
                                            path = element.path,
                                            compilePlat = element.compilePlat,
                                            tools = if (currentToolSet.isNullOrEmpty()) element.tools else currentToolSet.toList(),
                                            pyVersion = element.pyVersion,
                                            eslintRc = element.eslintRc,
                                            phpcsStandard = element.phpcsStandard,
                                            goPath = element.goPath
                                        )
                                    } else if (element is LinuxPaasCodeCCScriptElement && element.getClassType() == LinuxPaasCodeCCScriptElement.classType) {
                                        // 加入规则集
                                        logger.info("update pipeline checkerSets: {}", JsonUtil.toJson(checkerSets))
                                        if (!checkerSets.isNullOrEmpty()) {
                                            var fieldMap = mutableMapOf<String, String>()
                                            for (checkerSet in checkerSets) {
                                                var fieldKey = checkerSet.toolName.toLowerCase() + "ToolSetId"
                                                if (ComConstants.Tool.CHECKSTYLE.name == checkerSet.toolName) {
                                                    fieldKey = "checkStyleToolSetId"
                                                } else if (ComConstants.Tool.STYLECOP.name == checkerSet.toolName) {
                                                    fieldKey = "styleCopToolSetId"
                                                } else if (ComConstants.Tool.GOML.name == checkerSet.toolName) {
                                                    fieldKey = "gometalinterToolSetId"
                                                } else if (ComConstants.Tool.WOODPECKER_SENSITIVE.name == checkerSet.toolName) {
                                                    fieldKey = "woodpeckerToolSetId"
                                                }
                                                val fieldValue = checkerSet.checkerSetId
                                                fieldMap[fieldKey] = fieldValue
                                            }
                                            var elementWithCheckerSet: LinuxCodeCCScriptElement =
                                                ObjectDynamicCreator.setFieldValue(
                                                    fieldMap,
                                                    LinuxCodeCCScriptElement::class.java
                                                )
                                            ObjectDynamicCreator.copyNonNullProperties(
                                                elementWithCheckerSet, element,
                                                LinuxCodeCCScriptElement::class.java, fieldMap.keys
                                            )
                                            logger.info("updated element: {}", JsonUtil.toJson(element))
                                        }
                                        element
                                    } else if (null != codeElement && ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() != createFrom
                                        && (pipelineUtils.isOldCodeElement(element))
                                    ) {
                                        codeElement
                                    } else {
                                        element
                                    }
                                elementList.add(newElement)
                            }
                            VMBuildContainer(
                                containerId = container.containerId,
                                id = container.id,
                                name = container.name,
                                elements = elementList,
                                status = container.status,
                                startEpoch = container.startEpoch,
                                systemElapsed = container.systemElapsed,
                                elementElapsed = container.elementElapsed,
                                baseOS = osType ?: container.baseOS,
                                vmNames = container.vmNames,
                                maxQueueMinutes = container.maxQueueMinutes,
                                maxRunningMinutes = container.maxRunningMinutes,
                                buildEnv = buildEnv ?: container.buildEnv,
                                customBuildEnv = container.customBuildEnv,
                                thirdPartyAgentId = container.thirdPartyAgentId,
                                thirdPartyAgentEnvId = container.thirdPartyAgentEnvId,
                                thirdPartyWorkspace = container.thirdPartyWorkspace,
                                dockerBuildVersion = container.dockerBuildVersion,
                                tstackAgentId = null,
                                canRetry = container.canRetry,
                                enableExternal = container.enableExternal,
                                jobControlOption = container.jobControlOption,
                                mutexGroup = container.mutexGroup,
                                dispatchType = container.dispatchType
                            )
                        } else {
                            container
                        }
                    containerList.add(newContainer)
                }
                stageList.add(Stage(containerList, newStage.id))
            }
            Model(name, desc, stageList, labels, instanceFromTemplate, pipelineCreator)
        }

        val modifyResult = when (createFrom) {
            ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() ->
                client.getDevopsService(ServicePipelineResource::class.java)
                    .edit(userName, projectId, pipelineId, newModel, ChannelCode.BS)
            ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() ->
                client.getDevopsService(ServicePipelineResource::class.java)
                    .edit(userName, projectId, pipelineId, newModel, ChannelCode.GONGFENGSCAN)
            else -> {
                var channelCode = ChannelCode.CODECC_EE
                if (taskInfoEntity.nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
                    channelCode = ChannelCode.CODECC
                }
                client.getDevopsService(ServicePipelineResource::class.java)
                    .edit(userName, projectId, pipelineId, newModel, channelCode)
            }
        }

        if (modifyResult.isNotOk() || modifyResult.data != true) {
            logger.error("mongotemplate bs pipeline info fail! bs project id: $projectId")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        logger.info("update pipeline info successfully! project id: $projectId, pipeline id: $pipelineId")
    }

    private fun getPipelineModel(
        userName: String,
        projectId: String,
        pipelineId: String,
        createFrom: String,
        nameEn: String
    ): Model {
        val channelCode = pipelineUtils.getDevopsChannelCode(createFrom, nameEn)

        val result = client.getDevopsService(ServicePipelineResource::class.java)
            .get(userName, projectId, pipelineId, channelCode)
        if (result.isNotOk() || null == result.data) {
            logger.error("get incremental info fail! bs project id: {}, bs incremental id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return result.data as Model
    }


    /**
     * 更新流水线插件版本
     */
    override fun updatePluginVersion(userId: String, projectId : String, pipelineId : String){
        val pipelineModelResult = client.getDevopsService(ServicePipelineResource::class.java).get(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = ChannelCode.GONGFENGSCAN
        )
        if(pipelineModelResult.isNotOk() || null == pipelineModelResult.data){
            logger.info("get pipeline model fail! pipeline id: $pipelineId")
            return
        }
        val pipelineModel = pipelineModelResult.data
        pipelineModel!!.stages.forEach { stage ->
            val containerList = mutableListOf<Container>()
            stage.containers.forEach { container ->
                val elementList = mutableListOf<Element>()
                container.elements.forEach { element ->
                    if(element is MarketBuildAtomElement){
                        //拉取代码原子更新版本
                        if(element.getAtomCode() == pipelineUtils.GIT_COMMON_ATOM_CODE){
                            element.version = (pluginVersionCache.get("GIT") ?: pipelineUtils.GIT_COMMON_ATOM_CODE_VERSION)
                        }
                        //代码扫描原子更新版本
                        if(element.getAtomCode() == pipelineUtils.CODECC_ATOM_CODE){
                            element.version = (pluginVersionCache.get("CODECC") ?: pipelineUtils.CODECC_ATOM_VERSION)
                        }
                    }
                }
            }
        }
        client.getDevopsService(ServicePipelineResource::class.java).edit(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            pipeline = pipelineModel,
            channelCode = ChannelCode.GONGFENGSCAN
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}
