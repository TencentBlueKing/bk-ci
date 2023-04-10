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

package com.tencent.devops.process.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_PIPELINE_NAME
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_PROJECT_ID
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.ci.NORMAL_JOB
import com.tencent.devops.common.ci.VM_JOB
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.MacOS
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.image.PoolType
import com.tencent.devops.common.ci.task.BashTask
import com.tencent.devops.common.ci.task.LinuxScriptInput
import com.tencent.devops.common.ci.task.MarketBuildInput
import com.tencent.devops.common.ci.task.MarketBuildLessTask
import com.tencent.devops.common.ci.task.MarketBuildTask
import com.tencent.devops.common.ci.task.WindowsScriptInput
import com.tencent.devops.common.ci.task.WindowsScriptTask
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.ci.yaml.Job
import com.tencent.devops.common.ci.yaml.JobDetail
import com.tencent.devops.common.ci.yaml.ResourceType
import com.tencent.devops.common.ci.yaml.Stage
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.pipeline.type.pcg.PCGDispatchType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.process.api.quality.pojo.PipelineListRequest
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BEE_CI_NOT_SUPPORT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CHECK_INTEGRITY_YAML
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CHECK_OPERATING_SYSTEM_CORRECT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CHECK_POOL_FIELD
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CONSTRUCTION_MACHINE_NOT_SUPPORTED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_CONTACT_PLUG_DEVELOPER
import com.tencent.devops.process.constant.ProcessMessageCode.BK_EXPORT
import com.tencent.devops.process.constant.ProcessMessageCode.BK_EXPORT_SYSTEM_CREDENTIALS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_EXPORT_TIME
import com.tencent.devops.process.constant.ProcessMessageCode.BK_MODIFICATION_GUIDELINES
import com.tencent.devops.process.constant.ProcessMessageCode.BK_NODE_NOT_EXIST_UNDER_NEW_BUSINESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_NOT_CONFIRMED_CAN_EXECUTED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_NOT_EXIST_UNDER_NEW_BUSINESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_NOT_SUPPORT_CURRENT_CONSTRUCTION_MACHINE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_NO_RIGHT_EXPORT_PIPELINE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_ONLY_VISIBLE_PCG_BUSINESS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PIPELINED_ID
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PLEASE_MANUALLY_MODIFY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_PLUG_NOT_SUPPORTED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_SEARCH_STORE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_SENSITIVE_INFORMATION_IN_PARAMETERS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_WORKER_BEE_CI_NOT_SUPPORT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.CodeCCExportYamlData
import com.tencent.devops.process.pojo.JobData
import com.tencent.devops.process.pojo.OldVersionTask
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineExportYamlData
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.PoolData
import com.tencent.devops.process.pojo.TaskData
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.quality.QualityPipeline
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.op.GitCiMarketAtomService
import com.tencent.devops.process.utils.PIPELINE_VIEW_ALL_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_FAVORITE_PIPELINES
import com.tencent.devops.process.utils.PIPELINE_VIEW_MY_PIPELINES
import com.tencent.devops.store.api.image.service.ServiceStoreImageResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.io.BufferedReader
import java.io.StringReader
import java.net.URLEncoder
import java.time.LocalDateTime
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

/**
 *
 * @author irwinsun
 * @version 1.0
 */
@Suppress("ALL")
@Service("newPipelineService")
class TXPipelineService @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineListFacadeService: PipelineListFacadeService,
    private val processJmxApi: ProcessJmxApi,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val gitCiMarketAtomService: GitCiMarketAtomService,
    private val client: Client,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TXPipelineService::class.java)
    }

    fun listQualityViewPipelines(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        sortType: PipelineSortType,
        channelCode: ChannelCode,
        viewId: String,
        checkPermission: Boolean = true,
        filterByPipelineName: String? = null,
        filterByCreator: String? = null,
        filterByLabels: String? = null,
        callByApp: Boolean? = false,
        authPipelineIds: List<String> = emptyList(),
        skipPipelineIds: List<String> = emptyList()
    ): PipelineViewPipelinePage<QualityPipeline> {

        val watch = StopWatch()
        watch.start("perm_r_perm")
        val authPipelines = if (authPipelineIds.isEmpty()) {
            pipelinePermissionService.getResourceByPermission(
                userId, projectId, AuthPermission.LIST
            )
        } else {
            authPipelineIds
        }
        watch.stop()

        watch.start("s_r_summary")
        val buildPipelineRecords = pipelineRuntimeService.getBuildPipelineRecords(projectId, channelCode)
        watch.stop()

        watch.start("s_r_fav")
        val skipPipelineIdsNew = mutableListOf<String>()
        if (buildPipelineRecords.isNotEmpty) {
            buildPipelineRecords.forEach {
                skipPipelineIdsNew.add(it.pipelineId)
            }
        }
        if (skipPipelineIds.isNotEmpty()) {
            skipPipelineIdsNew.addAll(skipPipelineIds)
        }

        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        var slqLimit: SQLLimit? = null
        if (pageSizeNotNull != -1) slqLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)

        val offset = slqLimit?.offset ?: 0
        val limit = slqLimit?.limit ?: -1
        var count = 0L
        try {

            val list = if (buildPipelineRecords.isNotEmpty) {

                val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
                val pipelines = pipelineListFacadeService.buildPipelines(
                    pipelineInfoRecords = buildPipelineRecords,
                    favorPipelines = favorPipelines,
                    authPipelines = authPipelines,
                    projectId = projectId
                )
                val allFilterPipelines = pipelineListFacadeService.filterViewPipelines(
                    projectId = projectId,
                    pipelines = pipelines,
                    filterByName = filterByPipelineName,
                    filterByCreator = filterByCreator,
                    filterByLabels = filterByLabels
                )

                val hasPipelines = allFilterPipelines.isNotEmpty()

                if (!hasPipelines) {
                    return PipelineViewPipelinePage(pageNotNull, pageSizeNotNull, 0, emptyList())
                }

                val filterPipelines = when (viewId) {
                    PIPELINE_VIEW_FAVORITE_PIPELINES -> {
                        logger.info("User($userId) favorite pipeline ids($favorPipelines)")
                        allFilterPipelines.filter { favorPipelines.contains(it.pipelineId) }
                    }
                    PIPELINE_VIEW_MY_PIPELINES -> {
                        logger.info("User($userId) my pipelines")
                        allFilterPipelines.filter {
                            authPipelines.contains(it.pipelineId)
                        }
                    }
                    PIPELINE_VIEW_ALL_PIPELINES -> {
                        logger.info("User($userId) all pipelines")
                        allFilterPipelines
                    }
                    else -> {
                        logger.info("User($userId) filter view($viewId)")
                        pipelineListFacadeService.filterViewPipelines(userId, projectId, allFilterPipelines, viewId)
                    }
                }

                val permissionList = filterPipelines.filter { it.hasPermission }.toMutableList()
                pipelineListFacadeService.sortPipelines(permissionList, sortType)
                count = permissionList.size.toLong()

                val toIndex =
                    if (limit == -1 || permissionList.size <= (offset + limit)) permissionList.size else offset + limit

                if (offset >= permissionList.size) mutableListOf() else permissionList.subList(offset, toIndex)
            } else {
                mutableListOf()
            }
            watch.stop()

            val records = list.map {
                QualityPipeline(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    pipelineName = it.pipelineName,
                    pipelineDesc = it.pipelineDesc,
                    taskCount = it.taskCount,
                    buildCount = it.buildCount,
                    latestBuildStartTime = it.latestBuildStartTime,
                    latestBuildEndTime = it.latestBuildEndTime
                )
            }
            return PipelineViewPipelinePage(pageNotNull, pageSizeNotNull, count, records)
        } finally {
            logger.info("listViewPipelines|[$projectId]|$userId|watch=$watch")
            processJmxApi.execute(ProcessJmxApi.LIST_NEW_PIPELINES, watch.totalTimeMillis)
        }
    }

    fun listPipelineInfo(userId: String, projectId: String, request: PipelineListRequest?): List<Pipeline> {
        return pipelineListFacadeService.listPipelineInfo(userId, projectId, request?.pipelineId, request?.templateId)
    }

    fun exportYaml(userId: String, projectId: String, pipelineId: String, isGitCI: Boolean = false): Response {
        val (model, yamlSb) = checkPermissionAndGetHead(userId, projectId, pipelineId, isGitCI)
        // 在stages对象的生成中会添加顶部注释，所以放在分隔注释上面
        val stages = getStageFromModel(userId, projectId, pipelineId, model, yamlSb, isGitCI)
        yamlSb.append("################################################################" +
            "#####################################################\n\n")
        val yamlObj = CIBuildYaml(
            name = null,
            trigger = null,
            mr = null,
            variables = getVariableFromModel(model),
            services = null,
            stages = stages.map { it.stage },
            steps = null
        )
        var yamlStr = YamlUtil.toYaml(yamlObj)
        // 无法使用的替换为带注释的,目前仅工蜂CI需要
        if (isGitCI) {
            val replaceList = mutableListOf<List<Pair<PoolData, List<TaskData>>>>()
            stages.forEach { stage ->
                replaceList.add(stage.jobDataList.map { job -> Pair(job.poolData, job.taskDataList) })
            }
            if (replaceList.isNotEmpty()) {
                replaceList.forEach { stage ->
                    stage.forEach {
                        yamlStr = replaceJobYamlStrLineToComment(
                            yamlStr = yamlStr,
                            tip = it.first.tip,
                            replaceYamlStr = it.first.replaceYamlStr
                        )
                        if (it.second.isNotEmpty()) {
                            yamlStr = replaceTaskYamlStrLineToComment(
                                yamlStr = yamlStr,
                                replaceList = it.second.map { task -> Pair(task.tip, task.replaceYamlStr) }
                            )
                        }
                    }
                }
            }
        }
        yamlSb.append(replaceTaskType(yamlStr))
        return exportToFile(yamlSb.toString(), model.name)
    }

    private fun replaceTaskType(yamlStr: String): String {
        val sb = StringBuilder()
        val taskTypeRegex = Regex("\\- \\!\\<.*\\>")
        val br = BufferedReader(StringReader(yamlStr))
        var line: String? = br.readLine()
        while (line != null) {
            val taskTypeMatches = taskTypeRegex.find(line)
            if (null != taskTypeMatches) {
                line = line.replace("- !<", "- taskType: ").replace(">", "")
            }
            sb.append(line).append("\n")
            line = br.readLine()
        }
        return sb.toString()
    }

    private fun getStageFromModel(
        userId: String,
        projectId: String,
        pipelineId: String,
        model: Model,
        comment: StringBuilder,
        isGitCI: Boolean = false
    ): List<PipelineExportYamlData> {
        val stages = mutableListOf<PipelineExportYamlData>()
        model.stages.drop(1).forEach {
            val jobs = getJobsFromStage(userId, projectId, pipelineId, it, comment, isGitCI)
            val jobList = jobs.map { job -> job.job }
            if (jobList.isNotEmpty()) {
                stages.add(
                    PipelineExportYamlData(
                        Stage(jobList),
                        jobs
                    )
                )
            }
        }
        return stages
    }

    private fun getJobsFromStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        stage: com.tencent.devops.common.pipeline.container.Stage,
        comment: StringBuilder,
        isGitCI: Boolean = false
    ): List<JobData> {
        val jobs = mutableListOf<JobData>()
        stage.containers.forEach {
            val pool = getPoolFromModelContainer(userId, projectId, pipelineId, it, comment, isGitCI)
            // 目前仅工蜂CI，当注释项不为空时，说明当前pool不支持，直接跳过steps
            if (isGitCI && pool.tip != null && pool.replaceYamlStr != null) {
                val jobDetail = JobDetail(
                    name = null, // 推荐用displayName
                    displayName = it.name,
                    type = when (it.getClassType()) {
                        VMBuildContainer.classType -> VM_JOB
                        NormalContainer.classType -> NORMAL_JOB
                        else -> {
                            logger.error("get jobs from stage failed, unknown classType:(${it.getClassType()})")
                            VM_JOB
                        }
                    },
                    pool = pool.pool,
                    steps = emptyList(),
                    condition = null,
                    resourceType = if (pool.pool != null) {
                        ResourceType.REMOTE
                    } else {
                        ResourceType.LOCAL
                    }
                )
                // pool不能用时注释掉的是整个job
                jobs.add(
                    JobData(
                        Job(jobDetail),
                        PoolData(pool.pool, pool.tip, toYamlStr(Job(jobDetail))),
                        emptyList()
                    )
                )
                return@forEach
            }

            val steps = getStepsFromModelContainer(it, comment, isGitCI)
            val stepsList = steps.map { step -> step.task }
            if (steps.isNotEmpty()) {
                val jobDetail = JobDetail(
                    name = null, // 推荐用displayName
                    displayName = it.name,
                    type = when (it.getClassType()) {
                        VMBuildContainer.classType -> VM_JOB
                        NormalContainer.classType -> NORMAL_JOB
                        else -> {
                            logger.error("get jobs from stage failed, unknown classType:(${it.getClassType()})")
                            VM_JOB
                        }
                    },
                    pool = pool.pool,
                    steps = stepsList,
                    condition = null,
                    resourceType = if (pool.pool != null) {
                        ResourceType.REMOTE
                    } else {
                        ResourceType.LOCAL
                    }
                )
                jobs.add(
                    JobData(
                        Job(jobDetail),
                        pool,
                        steps
                    )
                )
            }
        }
        return jobs
    }

    private fun getStepsFromModelContainer(
        modelContainer: Container,
        comment: StringBuilder,
        isGitCI: Boolean = false
    ): List<TaskData> {
        val taskList = mutableListOf<TaskData>()
        modelContainer.elements.forEach {
            val gitCINotSupportTip =
                "# ======== " + MessageUtil.getMessageByLocale(
                    messageCode = BK_NOT_CONFIRMED_CAN_EXECUTED,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                    params = arrayOf(it.name)
                ) + MessageUtil.getMessageByLocale(
                            messageCode = BK_CONTACT_PLUG_DEVELOPER,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        ) + "（https://iwiki.woa.com/x/CqARHg） ======== "
            when (it.getClassType()) {
                LinuxScriptElement.classType -> {
                    val element = it as LinuxScriptElement
                    taskList.add(
                        TaskData(
                            BashTask(
                                displayName = element.name,
                                inputs = LinuxScriptInput(
                                    scriptType = element.scriptType,
                                    content = element.script,
                                    continueOnError = element.continueNoneZero
                                ),
                                condition = null
                            ),
                            null,
                            null
                        )
                    )
                }
                WindowsScriptElement.classType -> {
                    val element = it as WindowsScriptElement
                    val task = WindowsScriptTask(
                        displayName = element.name,
                        inputs = WindowsScriptInput(
                            content = element.script,
                            scriptType = element.scriptType,
                            charsetType = element.charsetType
                        ),
                        condition = null
                    )
                    val tip = if (isGitCI) {
                        gitCINotSupportTip
                    } else {
                        null
                    }
                    val replaceYamlStr = if (isGitCI) {
                        toYamlStr(task)
                    } else {
                        null
                    }
                    taskList.add(
                        TaskData(task, tip, replaceYamlStr)
                    )
                }
                MarketBuildAtomElement.classType -> {
                    val element = it as MarketBuildAtomElement
                    var elementData = mutableMapOf<String, Any>()
                    // 针对CodeCC的插件导出参数做处理
                    if (element.getAtomCode() == "CodeccCheckAtomDebug") {
                        element.data.forEach dataLoop@{ (key, value) ->
                            if (key == "input") {
                                elementData[key] = objectMapper.convertValue<CodeCCExportYamlData>(
                                    value,
                                    object : TypeReference<CodeCCExportYamlData>() {}
                                )
                            } else if (key == "output" || key == "namespace") {
                                return@dataLoop
                            } else {
                                elementData[key] = value
                            }
                        }
                    } else {
                        elementData = element.data.toMutableMap()
                    }
                    val task = MarketBuildTask(
                        displayName = element.name,
                        inputs = MarketBuildInput(
                            atomCode = element.getAtomCode(),
                            name = element.name,
                            version = element.version,
                            data = elementData
                        ),
                        condition = null
                    )
                    // 工蜂CI仅支持部分商店插件导出
                    if (isGitCI) {
                        val codeList = gitCiMarketAtomService.list(
                            atomCode = element.getAtomCode(),
                            page = null,
                            pageSize = null
                        ).records.map { atom -> atom.atomCode }
                        if (element.getAtomCode() !in codeList) {
                            taskList.add(
                                TaskData(
                                    task,
                                    gitCINotSupportTip,
                                    toYamlStr(task)
                                )
                            )
                            return@forEach
                        }
                    }
                    taskList.add(
                        TaskData(task, null, null)
                    )
                }
                MarketBuildLessAtomElement.classType -> {
                    val element = it as MarketBuildLessAtomElement
                    val task = MarketBuildLessTask(
                        displayName = element.name,
                        inputs = MarketBuildInput(
                            atomCode = element.getAtomCode(),
                            name = element.name,
                            version = element.version,
                            data = element.data
                        ),
                        condition = null
                    )
                    // 工蜂CI仅支持部分商店插件导出
                    if (isGitCI) {
                        val codeList = gitCiMarketAtomService.list(
                            atomCode = element.getAtomCode(),
                            page = null,
                            pageSize = null
                        ).records.map { atom -> atom.atomCode }
                        if (element.getAtomCode() !in codeList) {
                            taskList.add(
                                TaskData(
                                    task,
                                    gitCINotSupportTip,
                                    toYamlStr(task)
                                )
                            )
                            return@forEach
                        }
                    }
                    taskList.add(TaskData(task, null, null))
                }
                else -> {
                    logger.info("Not support plugin:${it.getClassType()}, skip...")
                    comment.append(
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_PLUG_NOT_SUPPORTED,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                            params = arrayOf(it.name, it.getClassType())
                        ) + "！" + MessageUtil.getMessageByLocale(
                            messageCode = BK_CHECK_INTEGRITY_YAML,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        )+ "。\n"
                    )
                    if (isGitCI) {
                        val task = OldVersionTask(
                            displayName = it.name,
                            inputs = null,
                            condition = null
                        )
                        taskList.add(
                            TaskData(
                                task,
                                "# ======== " + MessageUtil.getMessageByLocale(
                                    messageCode = BK_BEE_CI_NOT_SUPPORT,
                                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                                ) + " ${it.name} ，" +
                                        MessageUtil.getMessageByLocale(
                                            messageCode = BK_SEARCH_STORE,
                                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                                        ) + " ======== \n ${it.getClassType()}@latest",
                                toYamlStr(task)
                            )
                        )
                    }
                }
            }
        }
        return taskList
    }

    private fun getPoolFromModelContainer(
        userId: String,
        projectId: String,
        pipelineId: String,
        modelContainer: Container,
        comment: StringBuilder,
        isGitCI: Boolean = false
    ): PoolData {
        when (modelContainer) {
            is VMBuildContainer -> {
                val dispatchType = modelContainer.dispatchType ?: return PoolData(null, null, null)
                // 工蜂CI仅支持docker，devCloud，macos
                val tip =
                    MessageUtil.getMessageByLocale(
                        messageCode = BK_NOT_SUPPORT_CURRENT_CONSTRUCTION_MACHINE,
                        language = I18nUtil.getLanguage(userId)
                    ) +
                            "【${dispatchType.buildType().value}(${dispatchType.buildType().name})】 " +
                            MessageUtil.getMessageByLocale(
                                messageCode = BK_EXPORT,
                                language = I18nUtil.getLanguage(userId)
                    ) + MessageUtil.getMessageByLocale(
                                messageCode = BK_CHECK_POOL_FIELD,
                                language = I18nUtil.getLanguage(userId),
                                params = arrayOf(modelContainer.name)
                            )
                when (dispatchType.buildType()) {
                    BuildType.DOCKER, BuildType.PUBLIC_DEVCLOUD -> {
                        return PoolData(
                            getPublicDockerPool(
                                dispatchType = dispatchType,
                                userId = userId,
                                projectId = projectId,
                                pipelineId = pipelineId,
                                modelContainer = modelContainer,
                                isGitCI = isGitCI
                            ), null, null
                        )
                    }
                    BuildType.MACOS -> {
                        return PoolData(getMacOSPool(dispatchType), null, null)
                    }
                    BuildType.THIRD_PARTY_PCG -> {
                        val pool = getPcgPool(dispatchType, comment)
                        return if (isGitCI) {
                            PoolData(
                                pool,
                                tip,
                                toYamlStr(pool)
                            )
                        } else {
                            PoolData(pool, null, null)
                        }
                    }
                    BuildType.THIRD_PARTY_AGENT_ID -> {
                        val pool = getThirdPartyAgentPool(dispatchType, projectId, comment)
                        return if (isGitCI) {
                            PoolData(
                                pool,
                                tip,
                                toYamlStr(pool)
                            )
                        } else {
                            PoolData(pool, null, null)
                        }
                    }
                    BuildType.THIRD_PARTY_AGENT_ENV -> {
                        val pool = getThirdPartyEnvPool(dispatchType, projectId, comment)
                        return if (isGitCI) {
                            PoolData(
                                pool,
                                tip,
                                toYamlStr(pool)
                            )
                        } else {
                            PoolData(pool, null, null)
                        }
                    }
                    else -> {
                        comment.append(
                            MessageUtil.getMessageByLocale(
                                messageCode = BK_CONSTRUCTION_MACHINE_NOT_SUPPORTED,
                                language = I18nUtil.getLanguage(userId)
                            ) +
                                    "【${dispatchType.buildType().value}(${dispatchType.buildType().name})】" +
                                    MessageUtil.getMessageByLocale(
                                        messageCode = BK_EXPORT,
                                        language = I18nUtil.getLanguage(userId)
                                    ) +
                                    MessageUtil.getMessageByLocale(
                                        messageCode = BK_CHECK_POOL_FIELD,
                                        language = I18nUtil.getLanguage(userId),
                                        params = arrayOf(modelContainer.name)
                                    ) + " \n"
                        )
                        return PoolData(null, null, null)
                    }
                }
            }
            else -> {
                return PoolData(null, null, null)
            }
        }
    }

    private fun getThirdPartyEnvPool(dispatchType: DispatchType, projectId: String, comment: StringBuilder): Pool? {
        comment.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_NOT_EXIST_UNDER_NEW_BUSINESS,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(BuildType.THIRD_PARTY_AGENT_ENV.value, dispatchType.value)
            ) + MessageUtil.getMessageByLocale(
                        messageCode = BK_CHECK_OPERATING_SYSTEM_CORRECT,
                        language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                    ) + "! \n"
        )
        return if (dispatchType is ThirdPartyAgentEnvDispatchType) {
            val agentsResult = if (dispatchType.agentType == AgentType.ID) {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentsByEnvId(projectId, dispatchType.value)
            } else {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentsByEnvName(
                        projectId,
                        dispatchType.envProjectId?.let { "$it@${dispatchType.value}" } ?: dispatchType.envName)
            }
            val os = if (agentsResult.isNotOk() || null == agentsResult.data || agentsResult.data!!.isEmpty()) {
                logger.error(
                    "getPoolFromModelContainer , ThirdPartyAgentIDDispatchType , " +
                            "not found agent:${dispatchType.envName}"
                )
                VMBaseOS.LINUX
            } else {
                when (agentsResult.data!![0].os) {
                    "MACOS" -> VMBaseOS.MACOS
                    "WINDOWS" -> VMBaseOS.WINDOWS
                    else -> VMBaseOS.LINUX
                }
            }

            Pool(
                container = null,
                credential = null,
                macOS = null,
                third = null,
                performanceConfigId = null,
                env = null,
                type = PoolType.SelfHosted,
                agentName = null,
                agentId = null,
                envName = if (dispatchType.agentType == AgentType.NAME) {
                    dispatchType.value
                } else {
                    null
                },
                envProjectId = dispatchType.envProjectId,
                envId = if (dispatchType.agentType == AgentType.ID) {
                    dispatchType.value
                } else {
                    null
                },
                os = os,
                workspace = dispatchType.workspace
            )
        } else {
            logger.error("Unknown dispatchType: ${dispatchType.buildType()}")
            null
        }
    }

    private fun getThirdPartyAgentPool(dispatchType: DispatchType, projectId: String, comment: StringBuilder): Pool? {
        comment.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_NODE_NOT_EXIST_UNDER_NEW_BUSINESS,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(BuildType.THIRD_PARTY_AGENT_ID.value, dispatchType.value)
            ) + MessageUtil.getMessageByLocale(
                messageCode = BK_PLEASE_MANUALLY_MODIFY,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ) + "！ \n"
        )
        return if (dispatchType is ThirdPartyAgentIDDispatchType) {
            val agentResult = if (dispatchType.agentType == AgentType.ID) {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentById(projectId, dispatchType.value)
            } else {
                client.get(ServiceThirdPartyAgentResource::class)
                    .getAgentByDisplayName(projectId, dispatchType.value)
            }
            val os = if (agentResult.isNotOk() || null == agentResult.data) {
                logger.error(
                    "getPoolFromModelContainer , ThirdPartyAgentIDDispatchType , " +
                            "not found agent:${dispatchType.displayName}"
                )
                VMBaseOS.LINUX
            } else {
                when (agentResult.data!!.os) {
                    "MACOS" -> VMBaseOS.MACOS
                    "WINDOWS" -> VMBaseOS.WINDOWS
                    else -> VMBaseOS.LINUX
                }
            }

            Pool(
                container = null,
                credential = null,
                macOS = null,
                third = null,
                performanceConfigId = null,
                env = null,
                type = PoolType.SelfHosted,
                agentName = if (dispatchType.agentType == AgentType.NAME) {
                    dispatchType.value
                } else {
                    null
                },
                agentId = if (dispatchType.agentType == AgentType.ID) {
                    dispatchType.value
                } else {
                    null
                },
                envName = null,
                envId = null,
                os = os,
                workspace = dispatchType.workspace
            )
        } else {
            logger.error("Unknown dispatchType: ${dispatchType.buildType()}")
            null
        }
    }

    private fun getPcgPool(dispatchType: DispatchType, comment: StringBuilder): Pool? {
        comment.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_ONLY_VISIBLE_PCG_BUSINESS,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                params = arrayOf(BuildType.THIRD_PARTY_PCG.value)
            ))
        return if (dispatchType is PCGDispatchType) {
            Pool(
                container = dispatchType.value,
                credential = null,
                macOS = null,
                third = null,
                performanceConfigId = null,
                env = null,
                type = PoolType.DockerOnPcg,
                agentName = null,
                agentId = null,
                envName = null,
                envId = null,
                workspace = null
            )
        } else {
            logger.error("Unknown dispatchType: ${dispatchType.buildType()}")
            null
        }
    }

    private fun getMacOSPool(dispatchType: DispatchType): Pool? {
        if (dispatchType !is MacOSDispatchType) {
            logger.error("Unknown dispatchType: ${dispatchType.buildType()}")
            return null
        } else {
            return Pool(
                container = null,
                credential = null,
                macOS = MacOS(
                    systemVersion = dispatchType.systemVersion,
                    xcodeVersion = dispatchType.xcodeVersion
                ),
                third = null,
                performanceConfigId = null,
                env = null,
                type = PoolType.Macos,
                agentName = null,
                agentId = null,
                envName = null,
                envId = null,
                workspace = null
            )
        }
    }

    private fun getPublicDockerPool(
        dispatchType: DispatchType,
        userId: String,
        projectId: String,
        pipelineId: String,
        modelContainer: VMBuildContainer,
        isGitCI: Boolean = false
    ): Pool? {
        // 工蜂CI中的pool仅有container，和credential两个字段
        val poolType = if (isGitCI) {
            null
        } else {
            if (dispatchType.buildType().name == BuildType.DOCKER.name) {
                PoolType.DockerOnVm
            } else {
                PoolType.DockerOnDevCloud
            }
        }

        val env = if (isGitCI) {
            null
        } else {
            modelContainer.buildEnv
        }

        if (dispatchType is StoreDispatchType) {
            when (dispatchType.imageType) {
                ImageType.BKSTORE -> {
                    // 调商店接口获取镜像信息
                    val imageRepoInfo = client.get(ServiceStoreImageResource::class)
                        .getImageRepoInfoByCodeAndVersion(
                            userId = userId,
                            projectCode = projectId,
                            imageCode = dispatchType.imageCode!!,
                            imageVersion = dispatchType.imageVersion,
                            pipelineId = pipelineId,
                            buildId = null
                        ).data!!
                    val completeImageName = if (imageRepoInfo.repoUrl.isBlank()) {
                        imageRepoInfo.repoName
                    } else {
                        "${imageRepoInfo.repoUrl}/${imageRepoInfo.repoName}"
                    } + ":" + imageRepoInfo.repoTag
                    return Pool(
                        container = completeImageName,
                        credential = Credential(null, null, imageRepoInfo.ticketId),
                        macOS = null,
                        third = null,
                        performanceConfigId = null,
                        env = env,
                        type = poolType,
                        agentName = null,
                        agentId = null,
                        envName = null,
                        envId = null,
                        workspace = null
                    )
                }
                ImageType.BKDEVOPS -> {
                    // 在商店发布的蓝盾源镜像，无需凭证
                    return Pool(
                        container = if (dispatchType is DockerDispatchType) {
                            dispatchType.value.removePrefix("paas/")
                        } else {
                            dispatchType.value.removePrefix("/")
                        },
                        credential = null,
                        macOS = null,
                        third = null,
                        performanceConfigId = null,
                        env = env,
                        type = poolType,
                        agentName = null,
                        agentId = null,
                        envName = null,
                        envId = null,
                        workspace = null
                    )
                }
                else -> {
                    return Pool(
                        container = dispatchType.value,
                        credential = if (dispatchType is PublicDevCloudDispathcType) {
                            Credential(null, null, dispatchType.credentialId)
                        } else {
                            null
                        },
                        macOS = null,
                        third = null,
                        performanceConfigId = null,
                        env = env,
                        type = poolType,
                        agentName = null,
                        agentId = null,
                        envName = null,
                        envId = null,
                        workspace = null
                    )
                }
            }
        }
        logger.error("Unknown dispatchType: ${dispatchType.buildType()}")
        return null
    }

    private fun getVariableFromModel(model: Model): Map<String, String>? {
        val params = (model.stages[0].containers[0] as TriggerContainer).params
        val result = mutableMapOf<String, String>()
        params.forEach {
            result[it.id] = it.defaultValue.toString()
        }
        return if (result.isEmpty()) {
            null
        } else {
            result
        }
    }

    // 方便为可能为空值的对象转为yaml
    private fun toYamlStr(bean: Any?): String {
        return bean?.let { YamlUtil.toYaml(it) } ?: ""
    }

    // 对Job对象的Yaml字符串按行进行带注释的替换
    private fun replaceJobYamlStrLineToComment(yamlStr: String?, tip: String?, replaceYamlStr: String?): String {
        if (yamlStr == null || yamlStr.isBlank()) {
            return ""
        }
        if (replaceYamlStr == null || replaceYamlStr.isBlank()) {
            return yamlStr
        }
        if (tip == null || tip.isBlank()) {
            return yamlStr
        }
        val yamlList = yamlStr.split("\n").toMutableList()
        val replaceYamlList = replaceYamlStr.split("\n")

        /**
         *  目前生成的replaceJob对象 [---, job:, displayName:xxx, ...]
         *  源串的Job对象 [ - job:, displayName:xxx, ...]
         *  前两个根据生成对象的方式不同，所以为了方便对比取第三个
         */
        val tipIndex = yamlList.indexOf(yamlList.find { it.trim() == replaceYamlList[2].trim() }) - 1
        if (tipIndex < 0) {
            return yamlStr
        }
        val startIndex = tipIndex + 1
        // replaceJob 比 源对象多了 "---" 和 "- job" 所以 -3
        val endIndex = startIndex + replaceYamlList.size - 3
        yamlList.add(tipIndex, tip)
        for (index in startIndex..endIndex) {
            if (yamlList[index].isBlank()) {
                continue
            }
            yamlList[index] = "# ${yamlList[index]}"
        }
        val sb = StringBuilder()
        yamlList.forEach {
            if (it.isBlank()) {
                return@forEach
            }
            sb.append("${it}\n")
        }
        return sb.toString()
    }

    // 对Task对象的Yaml字符串按行进行带注释的替换, 针对task的列表形式的重载
    private fun replaceTaskYamlStrLineToComment(yamlStr: String?, replaceList: List<Pair<String?, String?>>): String {
        if (yamlStr == null || yamlStr.isBlank()) {
            return ""
        }
        if (replaceList.isEmpty()) {
            return yamlStr
        }
        val yamlList = yamlStr.split("\n").toMutableList()
        val sb = StringBuilder()
        replaceList.forEach {
            val tip = it.first
            val replaceYamlStr = it.second
            if (replaceYamlStr == null || replaceYamlStr.isBlank()) {
                return@forEach
            }
            if (tip == null || tip.isBlank()) {
                return@forEach
            }

            val replaceYamlList = replaceYamlStr.split("\n")

            /**
             *  目前生成的replaceTask对象 [---, - taskType:, displayName:xxx, ...]
             *  源串的replaceTask对象 [ - taskType:, displayName:xxx, ...]
             *  前两个根据生成对象的方式不同，所以为了方便对比取第二个
             */
            val tipIndex = yamlList.indexOf(yamlList.find { line -> line.trim() == replaceYamlList[1].trim() }) - 1
            if (tipIndex < 0) {
                return@forEach
            }
            val startIndex = tipIndex + 1
            // replaceTask 比 源对象多了 "---" 所以 -2
            val endIndex = startIndex + replaceYamlList.size - 2
            // 针对老版本无法生成市场插件classType的插件单独替换
            if (tip.contains("\n")) {
                val tipAndType = tip.split("\n")
                yamlList.add(tipIndex, tipAndType[0])
                for (index in startIndex..endIndex) {
                    if (yamlList[index].isBlank()) {
                        continue
                    }
                    // 替换classType
                    if (index == startIndex) {
                        yamlList[index] = "# " + yamlList[index].replace("OldVersionTask", tipAndType[1])
                    } else {
                        yamlList[index] = "# ${yamlList[index]}"
                    }
                }
            } else {
                yamlList.add(tipIndex, tip)
                for (index in startIndex..endIndex) {
                    if (yamlList[index].isBlank()) {
                        continue
                    }
                    yamlList[index] = "# ${yamlList[index]}"
                }
            }
        }
        yamlList.forEach { line ->
            if (line.isNotBlank()) {
                sb.append("${line}\n")
            }
        }
        return sb.toString()
    }

    private fun checkPermissionAndGetHead(
        userId: String,
        projectId: String,
        pipelineId: String,
        isGitCI: Boolean
    ): Pair<Model, StringBuilder> {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                messageCode = BK_NO_RIGHT_EXPORT_PIPELINE,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(userId, projectId)
            )
        )
        val model = pipelineRepositoryService.getModel(projectId, pipelineId) ?: throw CustomException(
            Response.Status.BAD_REQUEST,
            MessageUtil.getMessageByLocale(
                messageCode = ERROR_PIPELINE_NOT_EXISTS,
                language = I18nUtil.getLanguage(userId)
            ) + "！"
        )
        val yamlSb = StringBuilder()
        yamlSb.append("############################################################################" +
            "#########################################\n")
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_PROJECT_ID,
                language = I18nUtil.getLanguage(userId)
            ) + " $projectId \n")
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_PIPELINED_ID,
                language = I18nUtil.getLanguage(userId)
            ) + " $pipelineId \n")
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_PIPELINE_NAME,
                language = I18nUtil.getLanguage(userId)
            ) + " ${model.name} \n")
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_EXPORT_TIME,
                language = I18nUtil.getLanguage(userId)
            ) + " ${DateTimeUtil.toDateTime(LocalDateTime.now())} \n")
        yamlSb.append("# \n")
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_EXPORT_SYSTEM_CREDENTIALS,
                language = I18nUtil.getLanguage(userId)
            )
        )
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_SENSITIVE_INFORMATION_IN_PARAMETERS,
                language = I18nUtil.getLanguage(userId)
            )
        )
        if (isGitCI) {
            yamlSb.append(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_WORKER_BEE_CI_NOT_SUPPORT,
                    language = I18nUtil.getLanguage(userId)
                ) +
                        MessageUtil.getMessageByLocale(
                            messageCode = BK_MODIFICATION_GUIDELINES,
                            language = I18nUtil.getLanguage(userId)
                        ))
            yamlSb.append(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_BEE_CI_NOT_SUPPORT,
                    language = I18nUtil.getLanguage(userId)
                ) + "," + MessageUtil.getMessageByLocale(
                    messageCode = BK_SEARCH_STORE,
                    language = I18nUtil.getLanguage(userId)
                ) + " \n")
        }
        return Pair(model, yamlSb)
    }

    private fun exportToFile(yaml: String, pipelineName: String): Response {
        // 流式下载
        val fileStream = StreamingOutput { output ->
            val sb = StringBuilder()
            sb.append(yaml)
            output.write(sb.toString().toByteArray())
            output.flush()
        }
        val fileName = URLEncoder.encode("$pipelineName.yml", "UTF-8")
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $fileName")
            .header("Cache-Control", "no-cache")
            .build()
    }
}
