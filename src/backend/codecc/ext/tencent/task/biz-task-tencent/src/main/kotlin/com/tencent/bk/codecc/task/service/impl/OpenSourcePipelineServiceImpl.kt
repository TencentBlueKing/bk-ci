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
import com.tencent.bk.codecc.task.constant.TaskMessageCode
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.pojo.ActiveProjParseModel
import com.tencent.bk.codecc.task.pojo.GongfengPublicProjModel
import com.tencent.bk.codecc.task.service.MetaService
import com.tencent.bk.codecc.task.service.OpenSourcePipelineService
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
import com.tencent.devops.common.api.pojo.CodeCCResult
import com.tencent.devops.common.api.util.EncodeUtils
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
import com.tencent.devops.project.api.service.service.ServicePublicScanResource
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.store.api.container.ServiceContainerAppResource
import net.sf.json.JSONArray
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.concurrent.TimeUnit
import kotlin.collections.set

/**
 * 与蓝盾交互工具类
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service
open class OpenSourcePipelineServiceImpl @Autowired constructor(
    private val client: Client,
    private val baseDataRepository: BaseDataRepository
) : OpenSourcePipelineService {


    @Value("\${devops.dispatch.imageName:tlinux_ci}")
    private val imageName: String = ""

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

    override fun createGongfengDevopsProject(newProject: GongfengPublicProjModel): String {
        val projectCreateInfo = ProjectCreateInfo(
            projectName = "CODE_${newProject.id}",
            englishName = "CODE_${newProject.id}",
            projectType = 5,
            description = "public scan of gongfeng project/description: ${EncodeUtils.decodeX(
                newProject.description
                    ?: ""
            )}",
            bgId = 0L,
            bgName = "",
            deptId = 0L,
            deptName = "",
            centerId = 0L,
            centerName = "",
            secrecy = false,
            kind = 0
        )
        logger.info("start to create public scan project")
        val result = client.getDevopsService(ServicePublicScanResource::class.java).createCodeCCScanProject(
            newProject.owner!!.userName, projectCreateInfo
        )
        if (result.isNotOk() || null == result.data) {
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return result.data!!.projectId
    }

    override fun createActiveProjDevopsProject(activeProjParseModel: ActiveProjParseModel): String {
        val projectCreateInfo = ProjectCreateInfo(
            projectName = "CODE_${activeProjParseModel.id}",
            englishName = "CODE_${activeProjParseModel.id}",
            projectType = 5,
            description = "public scan of active project: ${activeProjParseModel.id}",
            bgId = 0L,
            bgName = "",
            deptId = 0L,
            deptName = "",
            centerId = 0L,
            centerName = "",
            secrecy = false,
            kind = 0
        )
        logger.info("start to create public scan project")
        val result = client.getDevopsService(ServicePublicScanResource::class.java).createCodeCCScanProject(
            activeProjParseModel.creator, projectCreateInfo
        )
        if (result.isNotOk() || null == result.data) {
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return result.data!!.projectId
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
        dispatchRoute: ComConstants.CodeCCDispatchRoute
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
                    "gongfengProjectId" to gongfengPublicProjEntity.id.toString()
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
                        "refName" to (gongfengPublicProjEntity.defaultBranch ?: "master"),
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
            customCondition = ""
        )

        val codeccElement: Element =
            MarketBuildAtomElement(
                name = "CodeCC代码检查(New)",
                atomCode = "CodeccCheckAtomDebug",
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
            customCondition = ""
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
                customCondition = ""
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
                codeccTaskId = dispatchRoute.flag()
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

    private fun createGongfengCommonPipeline(
        httpRepoUrl: String?, id: Int,
        pipelineDesc: String?, owner: String?, projectId: String,
        type: String, branchName: String = "master",
        dispatchRoute: ComConstants.CodeCCDispatchRoute
    ): String {
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
                    "gongfengProjectId" to id.toString()
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
                    if (type == "normal")
                        mapOf(
                            "username" to codeccPublicAccount,
                            "password" to codeccPublicPassword,
                            "refName" to branchName,
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
                            "repositoryUrl" to httpRepoUrl,
                            "strategy" to "FRESH_CHECKOUT",
                            "tagName" to ""
                        )
                    else
                        mapOf(
                            "accessToken" to client.getDevopsService(ServiceGitResource::class.java).getToken(id.toLong()).data?.accessToken,
                            "branchName" to "master",
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
                            "repositoryUrl" to httpRepoUrl,
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
            customCondition = ""
        )

        val codeccElement: Element =
            MarketBuildAtomElement(
                name = "CodeCC代码检查(New)",
                atomCode = "CodeccCheckAtomDebug",
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
                    value = branchName
                ),
                NameAndValue(
                    key = "BK_CI_CODECC_SCAN_FILTER_PASS",
                    value = "true"
                )
            ),
            customCondition = ""
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
                timeout = 600,
                runCondition = RunCondition.PRE_TASK_FAILED_ONLY,
                otherTask = "",
                customVariables = listOf(),
                customCondition = ""
            )
        }

        val containerSecond = VMBuildContainer(
            id = null,
            name = "demo",
            elements = listOf(filterElement, gitElement, codeccElement, notifyElement),
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
                codeccTaskId = dispatchRoute.flag()
            ),
            canRetry = null,
            enableExternal = null,
            containerId = null,
            jobControlOption = JobControlOption(
                enable = true,
                timeout = 900,
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
                "project id: $id"
        )
        /**
         * 总流水线拼装
         */
        val pipelineModel = Model(
            name = "CODEPIPELINE_$id",
            desc = pipelineDesc,
            stages = arrayListOf(stageFirst, stageSecond),
            labels = emptyList(),
            instanceFromTemplate = null,
            pipelineCreator = null,
            srcTemplateId = null
        )

        val pipelineCreateResult = client.getDevopsService(ServicePipelineResource::class.java).create(
            owner
                ?: "codecc-admin", projectId, pipelineModel, ChannelCode.GONGFENGSCAN
        )
        return if (pipelineCreateResult.isOk()) pipelineCreateResult.data?.id
            ?: "" else throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
    }

    /**
     * 获取开源扫描插件版本号
     */
    private fun getOpenSourceVersion(versionType : String) : String?{
        val baseDataList = baseDataRepository.findAllByParamTypeAndParamCode(ComConstants.KEY_OPENSOURCE_VERSION, versionType)
        return if(baseDataList.isNullOrEmpty()) null else baseDataList[0].paramValue
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OpenSourcePipelineServiceImpl::class.java)
    }
}

