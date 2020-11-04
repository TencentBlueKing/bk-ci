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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.ci.NORMAL_JOB
import com.tencent.devops.common.ci.VM_JOB
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.image.PoolType
import com.tencent.devops.common.ci.task.AbstractTask
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
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.api.quality.pojo.PipelineListRequest
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineService
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.quality.QualityPipeline
import com.tencent.devops.process.service.label.PipelineGroupService
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
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

/**
 *
 * @author irwinsun
 * @version 1.0
 */
@Service("newPipelineService")
class TXPipelineService @Autowired constructor(
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineService: PipelineService,
    private val processJmxApi: ProcessJmxApi,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val client: Client
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
            bkAuthPermissionApi.getUserResourceByPermission(
                userId, bsPipelineAuthServiceCode,
                AuthResourceType.PIPELINE_DEFAULT, projectId, AuthPermission.LIST,
                null
            )
        } else {
            authPipelineIds
        }
        watch.stop()

        watch.start("s_r_summary")
        val pipelineBuildSummary = pipelineRuntimeService.getBuildSummaryRecords(projectId, channelCode)
        watch.stop()

        watch.start("s_r_fav")
        val skipPipelineIdsNew = mutableListOf<String>()
        if (pipelineBuildSummary.isNotEmpty) {
            pipelineBuildSummary.forEach {
                skipPipelineIdsNew.add(it["PIPELINE_ID"] as String)
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

            val list = if (pipelineBuildSummary.isNotEmpty) {

                val favorPipelines = pipelineGroupService.getFavorPipelines(userId, projectId)
                val pipelines = pipelineService.buildPipelines(pipelineBuildSummary, favorPipelines, authPipelines)
                val allFilterPipelines =
                    pipelineService.filterViewPipelines(pipelines, filterByPipelineName, filterByCreator, filterByLabels)

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
                        pipelineService.filterViewPipelines(userId, projectId, allFilterPipelines, viewId)
                    }
                }

                val permissionList = filterPipelines.filter { it.hasPermission }
                pipelineService.sortPipelines(permissionList, sortType)
                count = permissionList.size.toLong()

                val toIndex =
                    if (limit == -1 || permissionList.size <= (offset + limit)) permissionList.size else offset + limit

                if (offset >= permissionList.size) listOf() else permissionList.subList(offset, toIndex)
            } else {
                emptyList()
            }
            watch.stop()

            val records = list.map {
                QualityPipeline(
                    it.projectId,
                    it.pipelineId,
                    it.pipelineName,
                    it.pipelineDesc,
                    it.taskCount,
                    it.buildCount,
                    it.latestBuildStartTime,
                    it.latestBuildEndTime
                )
            }
            return PipelineViewPipelinePage(pageNotNull, pageSizeNotNull, count, records)
        } finally {
            logger.info("listViewPipelines|[$projectId]|$userId|watch=$watch")
            processJmxApi.execute(ProcessJmxApi.LIST_NEW_PIPELINES, watch.totalTimeMillis)
        }
    }

    fun listPipelineInfo(userId: String, projectId: String, request: PipelineListRequest?): List<Pipeline> {
        return pipelineService.listPipelineInfo(userId, projectId, request?.pipelineId, request?.templateId)
    }

    fun exportYaml(userId: String, projectId: String, pipelineId: String, type: String?): Response {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT,
            message = "用户($userId)无权限在工程($projectId)下导出流水线"
        )
        val model = pipelineRepositoryService.getModel(pipelineId) ?: throw OperationException(MessageCodeUtil.getCodeLanMessage(ProcessMessageCode.ILLEGAL_PIPELINE_MODEL_JSON))
        val yamlObj = CIBuildYaml(
            pipelineName = null,
            trigger = null,
            mr = null,
            variables = getVariableFromModel(model),
            services = null,
            stages = getStageFromModel(userId, projectId, pipelineId, model),
            steps = null
        )
        val yamlStr = YamlUtil.toYaml(yamlObj)
        val yaml = replaceTaskType(yamlStr)
        return exportToFile(yaml, model.name)
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

    private fun getStageFromModel(userId: String, projectId: String, pipelineId: String, model: Model): List<Stage>? {
        val stages = mutableListOf<Stage>()
        model.stages.drop(1).forEach {
            stages.add(Stage(getJobsFromStage(userId, projectId, pipelineId, it)))
        }
        return stages
    }

    private fun getJobsFromStage(userId: String, projectId: String, pipelineId: String, stage: com.tencent.devops.common.pipeline.container.Stage): List<Job> {
        val jobs = mutableListOf<Job>()
        stage.containers.forEach {
            val jobDetail = JobDetail(
                name = null, // 推荐用displayName
                displayName = it.name,
                type = when (it.getClassType()) {
                    VMBuildContainer.classType -> VM_JOB
                    NormalContainer.classType -> NORMAL_JOB
                    else -> throw OperationException(MessageCodeUtil.getCodeLanMessage(ProcessMessageCode.ILLEGAL_PIPELINE_MODEL_JSON))
                },
                pool = getPoolFromModelContainer(userId, projectId, pipelineId, it),
                steps = getStepsFromModelContainer(it),
                condition = null,
                resourceType = ResourceType.REMOTE
            )
            jobs.add(Job(jobDetail))
        }
        return jobs
    }

    private fun getStepsFromModelContainer(modelContainer: Container): List<AbstractTask> {
        val taskList = mutableListOf<AbstractTask>()
        modelContainer.elements.forEach {
            when (it.getClassType()) {
                LinuxScriptElement.classType -> {
                    val element = it as LinuxScriptElement
                    taskList.add(BashTask(
                        displayName = element.name,
                        inputs = LinuxScriptInput(
                            scriptType = element.scriptType,
                            content = element.script,
                            continueOnError = element.continueNoneZero
                        ),
                        condition = null
                    ))
                }
                WindowsScriptElement.classType -> {
                    val element = it as WindowsScriptElement
                    taskList.add(WindowsScriptTask(
                        displayName = element.name,
                        inputs = WindowsScriptInput(
                            content = element.script,
                            scriptType = element.scriptType
                        ),
                        condition = null
                    ))
                }
                MarketBuildAtomElement.classType -> {
                    val element = it as MarketBuildAtomElement
                    taskList.add(MarketBuildTask(
                        displayName = element.name,
                        inputs = MarketBuildInput(
                            atomCode = element.getAtomCode(),
                            name = element.name,
                            version = element.version,
                            data = element.data
                        ),
                        condition = null
                    ))
                }
                MarketBuildLessAtomElement.classType -> {
                    val element = it as MarketBuildAtomElement
                    taskList.add(MarketBuildLessTask(
                        displayName = element.name,
                        inputs = MarketBuildInput(
                            atomCode = element.getAtomCode(),
                            name = element.name,
                            version = element.version,
                            data = element.data
                        ),
                        condition = null
                    ))
                }
            }
        }
        return taskList
    }

    private fun getPoolFromModelContainer(userId: String, projectId: String, pipelineId: String, modelContainer: Container): Pool? {
        when (modelContainer) {
            is VMBuildContainer -> {
                val dispatchType = modelContainer.dispatchType ?: return null
                when (dispatchType.buildType()) {
                    BuildType.DOCKER, BuildType.PUBLIC_DEVCLOUD -> {
                        if (dispatchType is StoreDispatchType) {
                            if (dispatchType.imageType == ImageType.BKSTORE) {
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
                                val completeImageName = if (ImageType.BKDEVOPS == imageRepoInfo.sourceType) {
                                    // 蓝盾项目源镜像
                                    imageRepoInfo.repoName
                                } else {
                                    // 第三方源镜像
                                    // dockerhub镜像名称不带斜杠前缀
                                    if (imageRepoInfo.repoUrl.isBlank()) {
                                        imageRepoInfo.repoName
                                    } else {
                                        "${imageRepoInfo.repoUrl}/${imageRepoInfo.repoName}"
                                    }
                                } + ":" + imageRepoInfo.repoTag
                                return Pool(
                                    container = completeImageName,
                                    credential = Credential(null, null, imageRepoInfo.ticketId),
                                    macOS = null,
                                    third = null,
                                    performanceConfigId = null,
                                    env = modelContainer.buildEnv,
                                    type = if (dispatchType.buildType() == BuildType.DOCKER) {
                                        PoolType.DockerOnVm
                                    } else {
                                        PoolType.DockerOnDevCloud
                                    }
                                )
                            } else if (dispatchType.imageType == ImageType.BKDEVOPS) {
                                // 在商店发布的蓝盾源镜像，无需凭证
                                return Pool(
                                    if (dispatchType is DockerDispatchType) {
                                        dispatchType.value.removePrefix("paas/")
                                    } else {
                                        dispatchType.value.removePrefix("/")
                                    },
                                    credential = null,
                                    macOS = null,
                                    third = null,
                                    performanceConfigId = null,
                                    env = modelContainer.buildEnv,
                                    type = PoolType.DockerOnDevCloud
                                )
                            } else {
                                return Pool(
                                    dispatchType.value,
                                    credential = if (dispatchType is PublicDevCloudDispathcType) {
                                        Credential(null, null, dispatchType.credentialId)
                                    } else {
                                        null
                                    },
                                    macOS = null,
                                    third = null,
                                    performanceConfigId = null,
                                    env = modelContainer.buildEnv,
                                    type = PoolType.DockerOnDevCloud
                                )
                            }
                        }
                        return null
                    }
                    else -> {
                        return null
                    }
                }
            }
            else -> {
                return null
            }
        }
    }

    private fun getVariableFromModel(model: Model): Map<String, String>? {
        val params = (model.stages[0].containers[0] as TriggerContainer).params
        val result = mutableMapOf<String, String>()
        params.forEach {
            result[it.id] = it.defaultValue as String
        }
        return if (result.isEmpty()) { null } else { result }
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
