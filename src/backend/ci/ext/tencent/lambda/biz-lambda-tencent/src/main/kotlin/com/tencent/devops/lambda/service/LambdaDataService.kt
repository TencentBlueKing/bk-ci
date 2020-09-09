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
package com.tencent.devops.lambda.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildTaskFinishBroadCastEvent
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.kafka.KafkaTopic
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.lambda.LambdaMessageCode.ERROR_LAMBDA_PROJECT_NOT_EXIST
import com.tencent.devops.lambda.dao.LambdaBuildContainerDao
import com.tencent.devops.lambda.dao.LambdaBuildTaskDao
import com.tencent.devops.lambda.dao.LambdaPipelineBuildDao
import com.tencent.devops.lambda.dao.LambdaPipelineModelDao
import com.tencent.devops.lambda.dao.LambdaPipelineTemplateDao
import com.tencent.devops.lambda.pojo.BuildData
import com.tencent.devops.lambda.pojo.DataPlatBuildDetail
import com.tencent.devops.lambda.pojo.DataPlatBuildHistory
import com.tencent.devops.lambda.pojo.DataPlatJobDetail
import com.tencent.devops.lambda.pojo.DataPlatTaskDetail
import com.tencent.devops.lambda.pojo.ElementData
import com.tencent.devops.lambda.pojo.ProjectOrganize
import com.tencent.devops.lambda.storage.ESService
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.jooq.DSLContext
import org.json.simple.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Service
class LambdaDataService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val lambdaPipelineBuildDao: LambdaPipelineBuildDao,
    private val lambdaPipelineModelDao: LambdaPipelineModelDao,
    private val lambdaPipelineTemplateDao: LambdaPipelineTemplateDao,
    private val lambdaBuildTaskDao: LambdaBuildTaskDao,
    private val lambdaBuildContainerDao: LambdaBuildContainerDao,
    private val esService: ESService,
    private val kafkaClient: KafkaClient
) {

    fun onBuildFinish(event: PipelineBuildFinishBroadCastEvent) {
        val history = lambdaPipelineBuildDao.getBuildHistory(dslContext, event.buildId)
        if (history == null) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] The build history is not exist")
            return
        }
        val model = lambdaPipelineModelDao.getBuildDetailModel(dslContext, event.buildId)
        if (model == null) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] Fail to get the pipeline detail model")
            return
        }
        val projectInfo = projectCache.get(history.projectId)
        pushBuildHistory(genBuildHistory(projectInfo, history, BuildStatus.values(), System.currentTimeMillis()))
        pushBuildDetail(genBuildDetail(projectInfo, event.pipelineId, model))

        val info = getBuildInfo(event.buildId)
        if (info == null) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] The build info is not exist")
            return
        }

        val data = BuildData(
            projectId = info.projectId,
            pipelineId = info.pipelineId,
            buildId = info.buildId,
            userId = info.startUser,
            status = info.status.name,
            trigger = info.trigger,
            beginTime = info.startTime ?: 0,
            endTime = info.endTime ?: 0,
            buildNum = info.buildNum,
            templateId = templateCache.get(info.pipelineId),
            bgName = projectInfo.bgName,
            deptName = projectInfo.deptName,
            centerName = projectInfo.centerName,
            model = model.model,
            errorInfoList = event.errorInfoList
        )
        esService.build(data)
    }

    fun onBuildTaskFinish(event: PipelineBuildTaskFinishBroadCastEvent) {
        val task = lambdaBuildTaskDao.getTask(dslContext, event.buildId, event.taskId)
        if (task == null) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.taskId}] Fail to get the build task")
            return
        }
        pushElementData2Es(event, task)
        pushGitTaskInfo(event, task)
        pushTaskDetail(event, task)
    }

    private fun getAtomCodeFromTask(task: TPipelineBuildTaskRecord): String {
        return if (!task.taskAtom.isNullOrBlank()) {
            task.taskAtom
        } else {
            val taskParams = JsonUtil.toMutableMapSkipEmpty(task.taskParams ?: "{}")
            if (taskParams.keys.contains("atomCode")) {
                taskParams["atomCode"] as String
            } else {
                logger.warn("unexpected taskParams with no atomCode:${task.taskParams}")
                ""
            }
        }
    }

    private fun pushElementData2Es(event: PipelineBuildTaskFinishBroadCastEvent, task: TPipelineBuildTaskRecord) {
        val data = ElementData(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            elementId = event.taskId,
            elementName = task.taskName ?: "",
            status = BuildStatus.values()[task.status ?: 0].name,
            beginTime = task.startTime?.timestampmilli() ?: 0,
            endTime = task.endTime?.timestampmilli() ?: 0,
            type = task.taskType ?: "",
            atomCode = getAtomCodeFromTask(task),
            errorType = event.errorType,
            errorCode = event.errorCode,
            errorMsg = event.errorMsg
        )
        try {
            esService.buildElement(data)
        } catch (e: Exception) {
            logger.error("Push elementData to es error, buildId: ${event.buildId}, taskId: ${event.taskId}", e)
        }
    }

    private fun pushTaskDetail(event: PipelineBuildTaskFinishBroadCastEvent, task: TPipelineBuildTaskRecord) {
        try {
            val startTime = task.startTime?.timestampmilli() ?: 0
            val endTime = task.endTime?.timestampmilli() ?: 0
            val taskAtom = task.taskAtom
            val taskParamMap = JsonUtil.toMap(task.taskParams)

            if (task.taskType == "VM" || task.taskType == "NORMAL") {
                if (taskAtom == "dispatchVMShutdownTaskAtom") {
                    Thread.sleep(3000)
                    val buildContainer = lambdaBuildContainerDao.getContainer(
                        dslContext = dslContext,
                        buildId = task.buildId,
                        stageId = task.stageId,
                        containerId = task.containerId
                    )
                    if (buildContainer != null) {
                        val dispatchType = taskParamMap["dispatchType"] as Map<String, Any>
                        val dataPlatJobDetail = DataPlatJobDetail(
                            pipelineId = task.pipelineId,
                            buildId = task.buildId,
                            containerType = dispatchType["buildType"].toString(),
                            projectEnglishName = task.projectId,
                            stageId = task.stageId,
                            containerId = task.containerId,
                            jobParams = JSONObject(JsonUtil.toMap(task.taskParams)),
                            status = buildContainer.status.toString(),
                            seq = buildContainer.seq.toString(),
                            startTime = buildContainer.startTime.format(dateTimeFormatter),
                            endTime = buildContainer.endTime.format(dateTimeFormatter),
                            costTime = buildContainer.cost.toLong(),
                            executeCount = buildContainer.executeCount,
                            conditions = JSONObject(JsonUtil.toMap(buildContainer.conditions)),
                            washTime = LocalDateTime.now().format(dateTimeFormatter)
                        )

                        logger.info("pushJobDetail: ${JsonUtil.toJson(dataPlatJobDetail)}")
                        kafkaClient.send(KafkaTopic.LANDUN_JOB_DETAIL_TOPIC, JsonUtil.toJson(dataPlatJobDetail))
                    }
                }
            } else {
                val taskParams = if (taskParamMap["@type"] != "marketBuild" && taskParamMap["@type"] != "marketBuildLess") {
                    val inputMap = mutableMapOf<String, String>()
                    when {
                        taskParamMap["@type"] == "linuxScript" -> {
                            inputMap["scriptType"] = taskParamMap["scriptType"] as String
                            inputMap["script"] = taskParamMap["script"] as String
                            inputMap["continueNoneZero"] = (taskParamMap["continueNoneZero"] as Boolean).toString()
                            inputMap["enableArchiveFile"] = (taskParamMap["enableArchiveFile"] as Boolean).toString()
                            if (taskParamMap["archiveFile"] != null) {
                                inputMap["archiveFile"] = taskParamMap["archiveFile"] as String
                            }
                        }
                        taskParamMap["@type"] == "windowsScript" -> {
                            inputMap["scriptType"] = taskParamMap["scriptType"] as String
                            inputMap["script"] = taskParamMap["script"] as String
                        }
                        taskParamMap["@type"] == "manualReviewUserTask" -> {
                            inputMap["reviewUsers"] = taskParamMap["reviewUsers"] as String
                            if (taskParamMap["params"] != null) {
                                inputMap["desc"] = taskParamMap["params"] as String
                            }
                        }
                        else -> {
                            inputMap["key"] = "value"
                        }
                    }

                    val dataMap = mutableMapOf("input" to inputMap)
                    val taskParamMap1 = mutableMapOf("data" to dataMap)
                    JSONObject(taskParamMap1)
                } else {
                    JSONObject(JsonUtil.toMap(task.taskParams))
                }
                val dataPlatTaskDetail = DataPlatTaskDetail(
                    pipelineId = task.pipelineId,
                    buildId = task.buildId,
                    projectEnglishName = task.projectId,
                    type = "task",
                    itemId = task.taskId,
                    atomCode = task.atomCode,
                    taskParams = taskParams,
                    status = BuildStatus.values()[task.status].statusName,
                    errorCode = task.errorCode,
                    errorMsg = task.errorMsg,
                    startTime = task.startTime?.format(dateTimeFormatter),
                    endTime = task.endTime?.format(dateTimeFormatter),
                    costTime = if ((endTime - startTime) < 0) 0 else (endTime - startTime),
                    starter = task.starter,
                    washTime = LocalDateTime.now().format(dateTimeFormatter)
                )

                logger.info("pushTaskDetail: ${JsonUtil.toJson(dataPlatTaskDetail)}")
                kafkaClient.send(KafkaTopic.LANDUN_TASK_DETAIL_TOPIC, JsonUtil.toJson(dataPlatTaskDetail))
            }
        } catch (e: Exception) {
            logger.error("Push task detail to kafka error, buildId: ${event.buildId}, taskId: ${event.taskId}", e)
        }
    }

    private fun pushBuildHistory(history: DataPlatBuildHistory) {
        try {
            logger.info("pushBuildHistory: ${JsonUtil.toJson(history)}")
            kafkaClient.send(KafkaTopic.LANDUN_BUILD_HISTORY_TOPIC, JsonUtil.toJson(history))
        } catch (e: Exception) {
            logger.error("Push build history to kafka error, buildId: ${history.buildId}", e)
        }
    }

    private fun pushBuildDetail(buildDetail: DataPlatBuildDetail) {
        try {
            logger.info("pushBuildDetail: ${JsonUtil.toJson(buildDetail)}")
            kafkaClient.send(KafkaTopic.LANDUN_BUILD_DETAIL_TOPIC, JsonUtil.toJson(buildDetail))
        } catch (e: Exception) {
            logger.error("Push build detail to kafka error, buildId: ${buildDetail.buildId}", e)
        }
    }

    private fun pushGitTaskInfo(event: PipelineBuildTaskFinishBroadCastEvent, task: TPipelineBuildTaskRecord) {
        try {
            val gitUrl: String
            val taskParamsMap = JsonUtil.toMap(task.taskParams)
            val atomCode = taskParamsMap["atomCode"]
            when (atomCode) {
                "CODE_GIT" -> {
                    val repositoryHashId = taskParamsMap["repositoryHashId"]
                    val gitRepository = client.get(ServiceRepositoryResource::class)
                        .get(event.projectId, repositoryHashId.toString(), RepositoryType.ID)
                    gitUrl = gitRepository.data!!.url
                    sendGitTask2Kafka(task, gitUrl)
                }
                "gitCodeRepoCommon" -> {
                    val dataMap = JsonUtil.toMap(taskParamsMap["data"] ?: error(""))
                    val inputMap = JsonUtil.toMap(dataMap["input"] ?: error(""))
                    gitUrl = inputMap["repositoryUrl"].toString()
                    sendGitTask2Kafka(task, gitUrl)
                }
                "gitCodeRepo", "PullFromGithub", "GitLab" -> {
                    val dataMap = JsonUtil.toMap(taskParamsMap["data"] ?: error(""))
                    val inputMap = JsonUtil.toMap(dataMap["input"] ?: error(""))
                    val repositoryHashId = if (atomCode == "Gitlab") {
                        inputMap["repository"].toString()
                    } else {
                        inputMap["repositoryHashId"].toString()
                    }
                    val gitRepository = client.get(ServiceRepositoryResource::class)
                        .get(event.projectId, repositoryHashId, RepositoryType.ID)
                    gitUrl = gitRepository.data!!.url
                    sendGitTask2Kafka(task, gitUrl)
                }
            }
        } catch (e: Exception) {
            logger.error("Push git task to kafka error, buildId: ${event.buildId}, taskId: ${event.taskId}", e)
        }
    }

    private fun sendGitTask2Kafka(task: TPipelineBuildTaskRecord, gitUrl: String) {
        val taskMap = task.intoMap()
        taskMap["GIT_URL"] = gitUrl
        taskMap["WASH_TIME"] = LocalDateTime.now().format(dateTimeFormatter)
        taskMap.remove("TASK_PARAMS")

        kafkaClient.send(KafkaTopic.LANDUN_GIT_TASK_TOPIC, JsonUtil.toJson(taskMap))
    }

    private val projectCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*Build*/, ProjectOrganize>(
            object : CacheLoader<String, ProjectOrganize>() {
                override fun load(projectId: String): ProjectOrganize {
                    val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data
                    if (projectInfo == null) {
                        logger.warn("[$projectId] Fail to get the project info")
                        throw InvalidParamException(
                            message = "Fail to get the project info, projectId=$projectId",
                            errorCode = ERROR_LAMBDA_PROJECT_NOT_EXIST,
                            params = arrayOf(projectId)
                        )
                    }
                    return ProjectOrganize(
                        projectId = projectId,
                        bgName = projectInfo.bgName ?: "",
                        deptName = projectInfo.deptName ?: "",
                        centerName = projectInfo.centerName ?: ""
                    )
                }
            }
        )

    private val templateCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String/*pipelineId*/, String/*templateId*/>(
            object : CacheLoader<String, String>() {
                override fun load(pipelineId: String): String {
                    return lambdaPipelineTemplateDao.getTemplate(dslContext, pipelineId)?.templateId ?: ""
                }
            }
        )

    private fun getBuildInfo(buildId: String): BuildInfo? {
        return convert(lambdaPipelineBuildDao.getBuildHistory(dslContext, buildId))
    }

    private fun genBuildDetail(
        projectInfo: ProjectOrganize,
        pipelineId: String,
        buildDetailRecord: TPipelineBuildDetailRecord
    ): DataPlatBuildDetail {
        return with(buildDetailRecord) {
            DataPlatBuildDetail(
                washTime = LocalDateTime.now().format(dateTimeFormatter),
                buildId = buildId,
                templateId = templateCache.get(pipelineId),
                bgName = projectInfo.bgName,
                deptName = projectInfo.deptName,
                centerName = projectInfo.centerName,
                projectId = projectInfo.projectId,
                pipelineId = pipelineId,
                buildNum = buildNum,
                model = model,
                trigger = trigger,
                startUser = startUser,
                startTime = startTime.format(dateTimeFormatter),
                endTime = endTime.format(dateTimeFormatter),
                status = status
            )
        }
    }

    private fun convert(t: TPipelineBuildHistoryRecord?): BuildInfo? {
        return if (t == null) {
            null
        } else {
            BuildInfo(
                projectId = t.projectId,
                pipelineId = t.pipelineId,
                buildId = t.buildId,
                version = t.version,
                buildNum = t.buildNum,
                trigger = t.trigger,
                status = BuildStatus.values()[t.status],
                startUser = t.startUser,
                queueTime = t.queueTime?.timestampmilli() ?: 0L,
                startTime = t.startTime?.timestampmilli() ?: 0L,
                endTime = t.endTime?.timestampmilli() ?: 0L,
                taskCount = t.taskCount,
                firstTaskId = t.firstTaskId,
                parentBuildId = t.parentBuildId,
                parentTaskId = t.parentTaskId,
                channelCode = ChannelCode.valueOf(t.channel),
                errorInfoList = null
            )
        }
    }

    private fun genBuildHistory(
        projectInfo: ProjectOrganize,
        tPipelineBuildHistoryRecord: TPipelineBuildHistoryRecord,
        buildStatus: Array<BuildStatus>,
        currentTimestamp: Long
    ): DataPlatBuildHistory {
        return with(tPipelineBuildHistoryRecord) {
            val totalTime = if (startTime == null || endTime == null) {
                0
            } else {
                Duration.between(startTime, endTime).toMillis()
            }
            DataPlatBuildHistory(
                washTime = LocalDateTime.now().format(dateTimeFormatter),
                templateId = templateCache.get(pipelineId),
                bgName = projectInfo.bgName,
                deptName = projectInfo.deptName,
                centerName = projectInfo.centerName,
                projectId = projectInfo.projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = triggerUser ?: startUser,
                trigger = StartType.toReadableString(trigger, ChannelCode.valueOf(channel)),
                buildNum = buildNum,
                pipelineVersion = version,
                startTime = startTime.format(dateTimeFormatter),
                endTime = endTime.format(dateTimeFormatter),
                status = buildStatus[status].name,
                stageStatus = stageStatus,
                deleteReason = "",
                currentTimestamp = currentTimestamp,
                material = material,
                queueTime = queueTime?.timestampmilli(),
                artifactList = artifactInfo,
                remark = remark,
                totalTime = totalTime,
                executeTime = if (executeTime == null || executeTime == 0L) {
                    if (BuildStatus.isFinish(buildStatus[status])) {
                        totalTime
                    } else 0L
                } else {
                    executeTime
                },
                buildParameters = buildParameters,
                webHookType = webhookType,
                webhookInfo = webhookInfo,
                startType = getStartType(trigger, webhookType),
                recommendVersion = recommendVersion,
                retry = isRetry ?: false,
                errorInfoList = errorInfo
            )
        }
    }

    private fun getStartType(trigger: String, webhookType: String?): String {
        return when (trigger) {
            StartType.MANUAL.name -> {
                ManualTriggerElement.classType
            }
            StartType.TIME_TRIGGER.name -> {
                TimerTriggerElement.classType
            }
            StartType.WEB_HOOK.name -> {
                when (webhookType) {
                    CodeType.SVN.name -> {
                        CodeSVNWebHookTriggerElement.classType
                    }
                    CodeType.GIT.name -> {
                        CodeGitWebHookTriggerElement.classType
                    }
                    CodeType.GITLAB.name -> {
                        CodeGitlabWebHookTriggerElement.classType
                    }
                    CodeType.GITHUB.name -> {
                        CodeGithubWebHookTriggerElement.classType
                    }
                    else -> RemoteTriggerElement.classType
                }
            }
            else -> { // StartType.SERVICE.name,  StartType.PIPELINE.name, StartType.REMOTE.name
                RemoteTriggerElement.classType
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LambdaDataService::class.java)
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}