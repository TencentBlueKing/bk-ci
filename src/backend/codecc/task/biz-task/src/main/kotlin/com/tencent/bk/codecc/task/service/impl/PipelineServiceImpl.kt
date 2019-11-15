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

package com.tencent.bk.codecc.task.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.defect.api.ServiceTaskLogRestResource
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import com.tencent.bk.codecc.task.constant.TaskConstants
import com.tencent.bk.codecc.task.constant.TaskMessageCode
import com.tencent.bk.codecc.task.dao.ToolMetaCache
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.service.MetaService
import com.tencent.bk.codecc.task.service.PipelineService
import com.tencent.bk.codecc.task.vo.BatchRegisterVO
import com.tencent.bk.codecc.task.vo.RepoInfoVO
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.StreamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.*
import com.tencent.devops.common.pipeline.enums.*
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.*
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.plugin.codecc.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccBuildInfo
import com.tencent.devops.process.api.ServiceBuildResource
import com.tencent.devops.process.api.ServicePipelineResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.api.scm.ServiceScmResource
import com.tencent.devops.repository.pojo.Repository
import net.sf.json.JSONArray
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.*

/**
 * 与蓝盾交互工具类
 *
 * @version V1.0
 * @date 2019/5/5
 */
@Service
class PipelineServiceImpl @Autowired constructor(
        private val client: Client,
        private val baseDataRepository: BaseDataRepository,
        private val toolMetaCache: ToolMetaCache,
        private val objectMapper: ObjectMapper,
        private val metaService: MetaService
) : PipelineService {

    @Value("\${devops.retry.attempt:#{1}}")
    private val retryAttempt: Int = 0

    @Value("\${devops.retry.interval:#{1}}")
    private val retryInterval: Long = 0

    @Value("\${devops.imageName:#{null}}")
    private val imageName: String? = null

    override fun assembleCreatePipeline(registerVO: BatchRegisterVO, taskInfoEntity: TaskInfoEntity,
                                        defaultExecuteTime: String, defaultExecuteDate: List<String>): Model {
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

        val elementSecond = TimerTriggerElement(
                name = "定时触发",
                id = null,
                status = null,
                expression = getCrontabTimingStr(defaultExecuteTime, defaultExecuteDate),
                newExpression = null,
                advanceExpression = null,
                noScm = null
        )

        val containerFirst = TriggerContainer(
                id = null,
                name = "demo",
                elements = arrayListOf(elementFirst, elementSecond),
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                params = emptyList(),
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
        val elementThird = getCodeElement(registerVO, null)

        val elementFourth: Element =
                LinuxCodeCCScriptElement(
                        name = "执行扫描脚本",
                        id = null,
                        status = null,
                        scriptType = BuildScriptType.valueOf(if (registerVO.buildScriptType.isNullOrBlank()) "SHELL" else registerVO.buildScriptType),
                        script = if (registerVO.scriptContent.isNullOrBlank()) "echo" else registerVO.scriptContent,
                        codeCCTaskName = taskInfoEntity.nameEn,
                        codeCCTaskCnName = null,
                        languages = convertCodeLangToBs(taskInfoEntity.codeLang),
                        asynchronous = true,
                        scanType = null,
                        path = null,
                        compilePlat = null,
                        tools = if (registerVO.tools.isNullOrEmpty()) listOf("OTHERS") else registerVO.tools.map { it.toolName },
                        pyVersion = null,
                        eslintRc = null,
                        phpcsStandard = null,
                        goPath = null)

        val containerSecond = VMBuildContainer(
                id = null,
                name = "demo",
                elements = listOf(elementThird, elementFourth),
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                baseOS = VMBaseOS.valueOf("LINUX"),
                vmNames = emptySet(),
                maxQueueMinutes = null,
                maxRunningMinutes = 80,
                buildEnv = registerVO.buildEnv,
                customBuildEnv = null,
                thirdPartyAgentId = null,
                thirdPartyAgentEnvId = null,
                thirdPartyWorkspace = null,
                dockerBuildVersion = imageName,
                dispatchType = null,
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
                mutexGroup = null
        )
        val stageSecond = Stage(
                containers = arrayListOf(containerSecond),
                id = null
        )

        logger.info("assemble pipeline parameter successfully! task id: ${taskInfoEntity.taskId}")
        /**
         * 总流水线拼装
         */
        return Model(
                name = taskInfoEntity.taskId.toString(),
                desc = taskInfoEntity.projectName,
                stages = arrayListOf(stageFirst, stageSecond),
                labels = emptyList(),
                instanceFromTemplate = null,
                pipelineCreator = null,
                srcTemplateId = null
        )

    }


    override fun getCodeElement(registerVO: BatchRegisterVO, relPath: String?): Element {
        return when (registerVO.scmType) {
            "CODE_GIT" -> CodeGitElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = registerVO.repositoryHashId,
                    branchName = if (registerVO.branchName.isNullOrBlank()) "" else registerVO.branchName,
                    revision = null,
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    path = relPath,
                    enableSubmodule = null,
                    gitPullMode = null,
                    repositoryType = null,
                    repositoryName = null
            )
            "CODE_GITLAB" -> CodeGitlabElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = registerVO.repositoryHashId,
                    branchName = if (registerVO.branchName.isNullOrBlank()) "" else registerVO.branchName,
                    revision = null,
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    path = relPath,
                    enableSubmodule = null,
                    gitPullMode = null,
                    repositoryType = null,
                    repositoryName = null
            )
            "GITHUB" -> GithubElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = registerVO.repositoryHashId,
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    path = relPath,
                    enableSubmodule = null,
                    revision = null,
                    gitPullMode = null,
                    enableVirtualMergeBranch = null,
                    repositoryType = null,
                    repositoryName = null
            )
            else -> CodeSvnElement(
                    name = "下载代码",
                    id = null,
                    status = null,
                    repositoryHashId = registerVO.repositoryHashId,
                    revision = null,
                    strategy = CodePullStrategy.FRESH_CHECKOUT,
                    path = relPath,
                    enableSubmodule = null,
                    specifyRevision = null,
                    svnDepth = null,
                    svnPath = null,
                    svnVersion = null,
                    repositoryType = null,
                    repositoryName = null
            )
        }
    }


    /**
     * 获取定时任务表达式
     *
     * @param executeTime
     * @param executeDateList
     * @return
     */
    private fun getCrontabTimingStr(executeTime: String, executeDateList: List<String>?): String {
        if (executeTime.isBlank() || executeDateList.isNullOrEmpty()) {
            logger.error("execute date and time is empty!")
            throw CodeCCException(
                    errCode = CommonMessageCode.PARAMETER_IS_NULL,
                    msgParam = arrayOf("定时执行时间"),
                    errorCause = null)
        }
        val hour = executeTime.substring(0, executeTime.indexOf(":"))
        val min = executeTime.substring(executeTime.indexOf(":") + 1)

        var weekDayListStr = executeDateList.reduce { acc, s -> "$acc,$s" }
        return String.format("0 %s %s ? * %s", min, hour, weekDayListStr)
    }


    override fun updatePipelineTools(userName: String, taskId: Long, toolList: List<String>, taskInfoEntity: TaskInfoEntity?,
                                     updateType: ComConstants.PipelineToolUpdateType, codeElement: Element?): Set<String> {
        var currentToolSet = mutableSetOf<String>()
        if (null == taskInfoEntity) {
            return currentToolSet
        }
        val projectId = taskInfoEntity.projectId
        val pipelineId = taskInfoEntity.pipelineId

        //如果是从流水线创建的
        val channelCode = if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == taskInfoEntity.createFrom) {
            ChannelCode.BS
        } else {
            ChannelCode.CODECC
        }

        val result = client.get(ServicePipelineResource::class.java).get(userName, projectId, pipelineId, channelCode)
        if (result.isNotOk() || null == result.data) {
            logger.error("get pipeline info fail! bs project id: {}, bs pipeline id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        //stages 一定不为空 服务创建的只有一个codecc原子
        val model = result.data ?: return currentToolSet

        val newModel = with(model)
        {
            val stageList = mutableListOf<Stage>()
            stages.forEach { stage ->
                val containerList = mutableListOf<Container>()
                var newStage = stage
                if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == taskInfoEntity.createFrom) {
                    newStage = initParamsElementForStageOne(stage)
                }
                newStage.containers.forEach { container ->
                    val newContainer: Container =
                            if (container is VMBuildContainer &&
                                    container.elements.any { it is LinuxCodeCCScriptElement }) {
                                val elementList = mutableListOf<Element>()
                                container.elements.forEach { element ->
                                    val newElement: Element =
                                            if (element is LinuxCodeCCScriptElement) {
                                                val tools = element.tools
                                                if (!tools.isNullOrEmpty()) {
                                                    currentToolSet.addAll(tools)
                                                }
                                                when (updateType) {
                                                    ComConstants.PipelineToolUpdateType.ADD ->
                                                        currentToolSet.addAll(toolList)
                                                    ComConstants.PipelineToolUpdateType.REMOVE ->
                                                        currentToolSet.removeAll(toolList)
                                                    ComConstants.PipelineToolUpdateType.REPLACE ->
                                                        currentToolSet = toolList.toMutableSet()
                                                    ComConstants.PipelineToolUpdateType.GET ->
                                                        currentToolSet
                                                }
                                                element.tools = currentToolSet.toList()
                                                element
                                            } else if (null != codeElement &&
                                                    (element is CodeGitElement ||
                                                            element is CodeGitlabElement ||
                                                            element is GithubElement ||
                                                            element is CodeSvnElement)) {
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
                                        baseOS = container.baseOS,
                                        vmNames = container.vmNames,
                                        maxQueueMinutes = container.maxQueueMinutes,
                                        maxRunningMinutes = container.maxRunningMinutes,
                                        buildEnv = container.buildEnv,
                                        customBuildEnv = container.customBuildEnv,
                                        thirdPartyAgentId = container.thirdPartyAgentId,
                                        thirdPartyAgentEnvId = container.thirdPartyAgentEnvId,
                                        thirdPartyWorkspace = container.thirdPartyWorkspace,
                                        dockerBuildVersion = container.dockerBuildVersion,
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

        val modifyResult = if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == taskInfoEntity.createFrom) {
            client.get(ServicePipelineResource::class.java).edit(userName, projectId, pipelineId, newModel, ChannelCode.BS)
        } else {
            client.get(ServicePipelineResource::class.java).edit(userName, projectId, pipelineId, newModel, ChannelCode.CODECC)
        }

        if (modifyResult.isNotOk() || modifyResult.data != true) {
            logger.error("mongotemplate bs pipeline info fail! bs project id: $projectId")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        logger.info("update pipeline info successfully! project id: $projectId, pipeline id: $pipelineId")
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
                                containerType = null
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
                            containerId = container.containerId
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
    override fun startPipeline(taskInfoEntity: TaskInfoEntity, toolName: List<String>, userName: String): String {
        val buildId: String
        val valueMap = mapOf(
                "_CODECC_FILTER_TOOLS" to toolName.joinToString(",")
        )
        val buildIdResult = client.get(ServiceBuildResource::class.java).manualStartup(userName, taskInfoEntity.projectId,
                taskInfoEntity.pipelineId, valueMap, ChannelCode.CODECC)
        if (buildIdResult.isNotOk() || null == buildIdResult.data) {
            logger.error("start pipeline fail! project id: {}, pipeline id: {}, msg is: {}", taskInfoEntity.projectId,
                    taskInfoEntity.pipelineId, buildIdResult.message)
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
            buildInfoResult = client.get(ServiceCodeccResource::class.java).getCodeccBuildInfo(setOf(buildId))
            if (buildInfoResult.isOk() &&
                    null != buildInfoResult.data &&
                    buildInfoResult.data!!.containsKey(buildId)) {
                buildNo = buildInfoResult.data!![buildId]?.buildNo
            }
        }

        if (buildId.isBlank() || buildNo.isNullOrBlank()) {
            logger.error("start devops pipeline failed! project id: {}, codecc task id: {}, pipeline id: {}, build id: {}",
                    taskInfoEntity.projectId, taskInfoEntity.taskId, taskInfoEntity.pipelineId, buildId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return buildId
    }


    @Throws(StreamException::class)
    override fun convertDevopsCodeLangToCodeCC(codeLang: String): Long? {
        if (codeLang.isNullOrBlank()) {
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
        }.reduce { acc, l -> acc or l }
    }


    @Async("asyncTaskExecutor")
    override fun updateTaskInitStep(isFirstTrigger: String?, taskInfoEntity: TaskInfoEntity,
                                    pipelineBuildId: String, toolName: String, userName: String) {
        val uploadTaskLogStepVO = UploadTaskLogStepVO()
        uploadTaskLogStepVO.stepNum = 1
        uploadTaskLogStepVO.startTime = System.currentTimeMillis()
        uploadTaskLogStepVO.endTime = 0L

        uploadTaskLogStepVO.msg = if (isFirstTrigger?.toBoolean() == true) {
            "添加${toolMetaCache.getToolFromCache(toolName).displayName}后自动触发第一次分析"
        } else {
            "手动触发($userName)"
        }

        uploadTaskLogStepVO.flag = TaskConstants.TASK_FLAG_PROCESSING
        uploadTaskLogStepVO.toolName = toolName
        uploadTaskLogStepVO.streamName = taskInfoEntity.nameEn
        uploadTaskLogStepVO.pipelineBuildId = pipelineBuildId
        uploadTaskLogStepVO.triggerFrom = userName

        val uploadResult = client.get(ServiceTaskLogRestResource::class.java).uploadTaskLog(uploadTaskLogStepVO)
        if (uploadResult.isNotOk()) {
            logger.error("upload task analysis log fail! stream name: {}, tool name: {}",
                    taskInfoEntity.nameEn, toolName)
            throw CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL)
        }
        logger.info("update task log step status success! task id: ${taskInfoEntity.taskId}, tool name: $toolName")
    }


    override fun getRepositoryList(projCode: String): List<RepoInfoVO> {
        val repoResult = client.get(ServiceRepositoryResource::class.java).list(projCode, null)
        if (repoResult.isNotOk() || null == repoResult.data) {
            logger.error("get repo list fail!")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return repoResult.data!!.map { (repositoryHashId, aliasName, url, type, _, _, _, authType) ->
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

        val repoResult = client.get(ServiceScmResource::class.java).listBranches(projCode, url, ScmType.valueOf(scmType), null, null, null, null, null)
        if (repoResult.isNotOk() || null == repoResult.data) {
            logger.error("get repo list fail!")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return repoResult.data
    }


    /**
     * 修改codecc原子语言，渠道为CODECC
     *
     * @param taskInfoEntity
     */
    override fun updateBsPipelineLang(taskInfoEntity: TaskInfoEntity, userName: String) {
        if (userName.isBlank()) {
            return
        }

        val codeLang = taskInfoEntity.codeLang
        val projectId = taskInfoEntity.projectId
        val pipelineId = taskInfoEntity.pipelineId
        val data = client.get(ServicePipelineResource::class.java)
                .get(userName, projectId, pipelineId, ChannelCode.CODECC).data

        if (Objects.isNull(data)) {
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        //定位到第二个stage下面的第一个container，下面的第二个element
        val element = data!!.stages[1].containers[0]
                .elements[1] as LinuxCodeCCScriptElement

        // 设置语言
        element.languages = convertCodeLangToBs(codeLang)

        val edit = client.get(ServicePipelineResource::class.java)
                .edit(userName, projectId, pipelineId, data, ChannelCode.CODECC)
        if (Objects.nonNull(edit) && edit.data != true) {
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

    }


    /**
     * 修改codecc原子语言，渠道为BS
     *
     * @param taskInfoEntity
     * @param userName
     */
    override fun updateBsPipelineLangBSChannelCode(taskInfoEntity: TaskInfoEntity, userName: String) {
        if (Objects.isNull(taskInfoEntity) || userName.isBlank()) {
            return
        }

        val codeLang = taskInfoEntity.codeLang
        val projectId = taskInfoEntity.projectId
        val pipelineId = taskInfoEntity.pipelineId

        val modelResult = client.get(ServicePipelineResource::class.java)
                .get(userName, projectId, pipelineId, ChannelCode.BS)

        if (Objects.isNull(modelResult) || Objects.isNull(modelResult.data)) {
            logger.error("get pipeline model info fail! project id: $projectId, pipeline id: $pipelineId")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        //在Model中定位到代码检查原子的element，并修改语言
        val linuxElement = modelResult.data!!.stages
                .flatMap { stage -> stage.containers }
                .flatMap { container -> container.elements }
                .firstOrNull { element -> element is LinuxPaasCodeCCScriptElement } as LinuxPaasCodeCCScriptElement
        if (linuxElement.codeCCTaskName.isNullOrBlank()) {
            logger.error("can not find qualified element! project id: $projectId, pipeline id: $pipelineId")
            throw CodeCCException(TaskMessageCode.ELEMENT_NOT_FIND)
        }

        // 設置語言
        linuxElement.languages = convertCodeLangToBs(codeLang)

        val edit = client.get(ServicePipelineResource::class.java)
                .edit(userName, projectId, pipelineId, modelResult.data!!, ChannelCode.BS)
        if (Objects.nonNull(edit) && edit.data != true) {
            logger.error("update pipeline info fail! project id: $projectId, pipeline id: $pipelineId")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

    }

    /**
     * 将codecc平台的项目语言转换为蓝盾平台的codecc原子语言
     */
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun convertCodeLangToBs(langCode: Long): List<LinuxCodeCCScriptElement.ProjectLanguage> {
        val metadataList = metaService.queryMetadatas(ComConstants.METADATA_TYPE)[ComConstants.METADATA_TYPE]
        val languageList = metadataList?.filter { metadataVO ->
            (metadataVO.key.toLong() and langCode) != 0L
        }
                ?.map { metadataVO ->
                    LinuxCodeCCScriptElement.ProjectLanguage.valueOf(JSONArray.fromObject(metadataVO.aliasNames)[0].toString())
                }
        return if (languageList.isNullOrEmpty()) listOf(LinuxCodeCCScriptElement.ProjectLanguage.OTHERS) else languageList
    }


    override fun modifyCodeCCTiming(taskInfoEntity: TaskInfoEntity, executeDate: List<String>, executeTime: String, userName: String) {
        val projectId = taskInfoEntity.projectId
        val pipelineId = taskInfoEntity.pipelineId
        val channelCode = if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == taskInfoEntity.createFrom) {
            ChannelCode.BS
        } else {
            ChannelCode.CODECC
        }
        val modelResult = client.get(ServicePipelineResource::class.java).get(userName, projectId, pipelineId, channelCode)
        if (modelResult.isNotOk() || null == modelResult.data) {
            logger.error("get pipeline info fail! bs project id: {}, bs pipeline id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        val model = modelResult.data
        val newModel = with(model!!)
        {
            val stageList = mutableListOf<Stage>()
            stages.forEachIndexed { stageIndex, stage ->
                val newStage =
                        if (stageIndex == 0) {
                            val containerList = mutableListOf<Container>()
                            stage.containers.forEachIndexed { containerIndex, container ->
                                val newContainer: Container = if (container is TriggerContainer &&
                                        containerIndex == 0) {
                                    val newElements = container.elements.toMutableList()
                                    if (newElements.size > 1) {
                                        newElements.removeAt(1)
                                    }
                                    //定时时间或者日期为空则删除定时任务编排
                                    if (!executeTime.isBlank() && !executeDate.isNullOrEmpty()) {
                                        //无论原来是否有定时任务原子，都新建更新
                                        val timerTriggerElement = TimerTriggerElement(
                                                "定时触发", null, null,
                                                getCrontabTimingStr(executeTime, executeDate), null, null, null
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
                                            containerId = container.containerId
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
            Model(name, desc, stageList, labels, instanceFromTemplate, pipelineCreator)
        }


        val modifyResult = client.get(ServicePipelineResource::class.java).edit(userName, projectId, pipelineId, newModel, channelCode)
        if (modifyResult.isNotOk() || modifyResult.data != true) {
            logger.error("mongotemplate bs pipeline info fail! bs project id: {}", projectId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        logger.info("modify codecc task schedule successfully! project id: $projectId, pipeline id: $pipelineId")

        taskInfoEntity.executeDate = executeDate
        taskInfoEntity.executeTime = executeTime


    }


    override fun deleteCodeCCTiming(userName: String, createFrom: String, pipelineId: String, projectId: String) {
        val channelCode = if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == createFrom)
            ChannelCode.BS
        else
            ChannelCode.CODECC

        val modelResult = client.get(ServicePipelineResource::class.java).get(userName, projectId, pipelineId, channelCode)
        if (modelResult.isNotOk() || null == modelResult.data) {
            logger.error("get pipeline info fail! bs project id: {}, bs pipeline id: {}", projectId, pipelineId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }

        val model = modelResult.data

        val newModel = with(model!!)
        {
            val stageList = mutableListOf<Stage>()
            stages.forEachIndexed { stageIndex, stage ->
                val newStage =
                        if (stageIndex == 0) {
                            val containerList = mutableListOf<Container>()
                            stage.containers.forEachIndexed { containerIndex, container ->
                                val newContainer: Container = if (container is TriggerContainer &&
                                        containerIndex == 0) {
                                    val newElements = container.elements.toMutableList()
                                    newElements.removeAt(1)
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
                                            containerId = container.containerId
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
            Model(name, desc, stageList, labels, instanceFromTemplate, pipelineCreator)
        }
        val modifyResult = client.get(ServicePipelineResource::class.java).edit(userName, projectId, pipelineId, newModel, channelCode)
        if (modifyResult.isNotOk()) {
            logger.error("mongotemplate bs pipeline info fail! bs project id: {}", projectId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        logger.info("delete codecc task schedule successfully! project id: $projectId, pipeline id: $pipelineId")
    }

    override fun getRepoDetail(projectId: String,
                               repoHashId: String): Repository? {
        val repoResult = client.get(ServiceRepositoryResource::class.java).get(projectId, repoHashId, RepositoryType.ID)
        if (repoResult.isNotOk()) {
            logger.error("get repo detail fail! project id: $projectId, repo hash id: $repoHashId")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return repoResult.data
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }


}
