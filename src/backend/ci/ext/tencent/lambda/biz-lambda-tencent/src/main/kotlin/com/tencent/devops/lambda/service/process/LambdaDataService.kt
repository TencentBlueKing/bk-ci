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
package com.tencent.devops.lambda.service.process

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildCommitFinishEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildTaskFinishBroadCastEvent
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.TimerTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.lambda.LambdaCode.BK_STARTUP_CONFIGURATION_MISSING
import com.tencent.devops.lambda.LambdaMessageCode.ERROR_LAMBDA_PROJECT_NOT_EXIST
import com.tencent.devops.lambda.config.LambdaKafkaTopicConfig
import com.tencent.devops.lambda.dao.process.LambdaBuildCommitDao
import com.tencent.devops.lambda.dao.process.LambdaBuildContainerDao
import com.tencent.devops.lambda.dao.process.LambdaBuildTaskDao
import com.tencent.devops.lambda.dao.process.LambdaPipelineBuildDao
import com.tencent.devops.lambda.dao.process.LambdaPipelineLabelDao
import com.tencent.devops.lambda.dao.process.LambdaPipelineModelDao
import com.tencent.devops.lambda.dao.process.LambdaPipelineTemplateDao
import com.tencent.devops.lambda.pojo.DataPlatBuildCommits
import com.tencent.devops.lambda.pojo.DataPlatBuildDetail
import com.tencent.devops.lambda.pojo.DataPlatBuildHistory
import com.tencent.devops.lambda.pojo.DataPlatJobDetail
import com.tencent.devops.lambda.pojo.DataPlatTaskDetail
import com.tencent.devops.lambda.pojo.MakeUpBuildVO
import com.tencent.devops.lambda.pojo.ProjectOrganize
import com.tencent.devops.lambda.service.store.LambdaStoreService
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildHistoryRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.scm.utils.code.git.GitUtils
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import org.jooq.DSLContext
import org.json.simple.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class LambdaDataService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val lambdaPipelineBuildDao: LambdaPipelineBuildDao,
    private val lambdaPipelineModelDao: LambdaPipelineModelDao,
    private val lambdaPipelineTemplateDao: LambdaPipelineTemplateDao,
    private val lambdaBuildTaskDao: LambdaBuildTaskDao,
    private val lambdaBuildContainerDao: LambdaBuildContainerDao,
    private val lambdaPipelineLabelDao: LambdaPipelineLabelDao,
    private val kafkaClient: KafkaClient,
    private val lambdaKafkaTopicConfig: LambdaKafkaTopicConfig,
    private val lambdaBuildCommitDao: LambdaBuildCommitDao,
    private val lambdaStoreService: LambdaStoreService
) {

    fun onBuildFinish(event: PipelineBuildFinishBroadCastEvent) {
        Thread.sleep(1000)
        val history = lambdaPipelineBuildDao.getBuildHistory(
            dslContext = dslContext,
            projectId = event.projectId,
            buildId = event.buildId
        )
        if (history == null) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] The build history is not exist")
            return
        }
        val model = lambdaPipelineModelDao.getBuildDetailModel(dslContext, event.projectId, event.buildId)
        if (model == null) {
            logger.warn(
                "[${event.projectId}|${event.pipelineId}|${event.buildId}] Fail to get the pipeline detail model"
            )
            return
        }

        val projectInfo = projectCache.get(history.projectId)
        pushBuildHistory(projectInfo, history)
        pushBuildDetail(projectInfo, event.pipelineId, model)
        val variablesWithType = lambdaPipelineBuildDao.getVarsWithType(
            dslContext = dslContext,
            projectId = event.projectId,
            buildId = event.buildId
        )
        pushInvalidVariables(projectInfo, history, variablesWithType)
    }

    fun onBuildTaskFinish(event: PipelineBuildTaskFinishBroadCastEvent) {
        val task = lambdaBuildTaskDao.getTask(
            dslContext = dslContext,
            projectId = event.projectId,
            buildId = event.buildId,
            taskId = event.taskId
        )
        if (task == null) {
            logger.warn(
                "[${event.projectId}|${event.pipelineId}|${event.buildId}|${event.taskId}] Fail to get the build task"
            )
            return
        }
        val projectInfo = projectCache.get(event.projectId)
        pushTaskDetail(projectInfo, task)
    }

    fun makeUpBuildHistory(userId: String, makeUpBuildVOs: List<MakeUpBuildVO>): Boolean {
        makeUpBuildVOs.forEach {
            with(it) {
                val history = lambdaPipelineBuildDao.getBuildHistory(dslContext, projectId, buildId)
                if (history == null) {
                    logger.warn("[$projectId|$buildId] The build history is not exist")
                    return false
                }
                val model = lambdaPipelineModelDao.getBuildDetailModel(dslContext, projectId, buildId)
                if (model == null) {
                    logger.warn("[$projectId|$buildId] Fail to get the pipeline detail model")
                    return false
                }
                val projectInfo = projectCache.get(history.projectId)
                pushBuildHistory(projectInfo, history)
                pushBuildDetail(projectInfo, history.pipelineId, model)
            }
        }

        return true
    }

    fun makeUpBuildTasks(userId: String, makeUpBuildVOs: List<MakeUpBuildVO>): Boolean {
        makeUpBuildVOs.forEach {
            with(it) {
                val taskList = lambdaBuildTaskDao.getTaskByBuildId(dslContext, projectId, buildId)
                taskList.forEach { it1 ->
                    val projectInfo = projectCache.get(it1.projectId)
                    pushTaskDetail(projectInfo, it1)
                }
            }
        }

        return true
    }

    fun onBuildCommitFinish(event: PipelineBuildCommitFinishEvent) {
        val records = lambdaBuildCommitDao.getCommits(
            dslContext = dslContext,
            projectId = event.projectId,
            buildId = event.buildId
        )
        if (records.isEmpty()) {
            logger.warn("[${event.projectId}|${event.pipelineId}|${event.buildId}] The build commits is empty")
            return
        }
        try {
            records.map { record ->
                val buildCommits = with(record) {
                    DataPlatBuildCommits(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        commitId = commitId,
                        authorName = authorName,
                        message = message,
                        repoType = repositoryType,
                        commitTime = commitTime.format(dateTimeFormatter),
                        createTime = createTime.format(dateTimeFormatter),
                        mrId = mergeRequestId,
                        url = url,
                        eventType = eventType,
                        channel = channel,
                        action = action
                    )
                }
                checkParamBlank(lambdaKafkaTopicConfig.buildCommitsTopic, "buildCommitsTopic")
                kafkaClient.send(lambdaKafkaTopicConfig.buildCommitsTopic!!, JsonUtil.toJson(buildCommits))
            }
        } catch (ignore: Throwable) {
            logger.warn("Push build commits to kafka error, buildId: ${event.buildId}", ignore)
        }
    }

    private fun pushTaskDetail(projectInfo: ProjectOrganize, task: TPipelineBuildTaskRecord) {
        try {
            val isSecrecy = projectInfo.secrecy
            val startTime = task.startTime?.timestampmilli() ?: 0
            val endTime = task.endTime?.timestampmilli() ?: 0
            val taskAtom = task.taskAtom
            val platformCode = task.platformCode
            val platformErrorCode = task.platformErrorCode
            val taskParamMap = JsonUtil.toMap(task.taskParams)
            val platformName = lambdaStoreService.getPlatName(platformCode)

            if (task.taskType == "VM" || task.taskType == "NORMAL") {
                if (taskAtom == "dispatchVMShutdownTaskAtom") {
                    Thread.sleep(3000)
                    val buildContainer = lambdaBuildContainerDao.getContainer(
                        dslContext = dslContext,
                        projectId = task.projectId,
                        buildId = task.buildId,
                        stageId = task.stageId,
                        containerId = task.containerId
                    )
                    if (buildContainer != null) {
                        @Suppress("UNCHECKED_CAST")
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
                            errorType = task.errorType,
                            errorCode = task.errorCode,
                            errorMsg = task.errorMsg,
                            baseOS = taskParamMap["baseOS"] as String,
                            washTime = LocalDateTime.now().format(dateTimeFormatter)
                        )
                        val jobDetailTopic = checkParamBlank(lambdaKafkaTopicConfig.jobDetailTopic, "jobDetailTopic")
                        logger.info("pushJobDetail buildId:${dataPlatJobDetail.buildId}")
                        kafkaClient.send(jobDetailTopic, JsonUtil.toJson(dataPlatJobDetail))
//                        kafkaClient.send(KafkaTopic.LANDUN_JOB_DETAIL_TOPIC, JsonUtil.toJson(dataPlatJobDetail))
                    }
                }
            } else {
                val taskParams = if (
                    // @type 为buildType
                    taskParamMap["@type"] != "marketBuild" &&
                    taskParamMap["@type"] != "marketBuildLess"
                ) {
                    val inputMap = mutableMapOf<String, String>()
                    when {
                        taskParamMap["@type"] == "linuxScript" -> {
                            inputMap["name"] = taskParamMap["name"] as String
                            inputMap["scriptType"] = taskParamMap["scriptType"] as String
                            inputMap["script"] = taskParamMap["script"] as String
                            inputMap["continueNoneZero"] =
                                (taskParamMap["continueNoneZero"] as Boolean?)?.toString() ?: ""
                            inputMap["enableArchiveFile"] =
                                (taskParamMap["enableArchiveFile"] as Boolean?)?.toString() ?: ""
                            if (taskParamMap["archiveFile"] != null) {
                                inputMap["archiveFile"] = taskParamMap["archiveFile"] as String
                            }
                        }
                        taskParamMap["@type"] == "windowsScript" -> {
                            inputMap["name"] = taskParamMap["name"] as String
                            inputMap["scriptType"] = taskParamMap["scriptType"] as String
                            inputMap["script"] = taskParamMap["script"] as String
                        }
                        taskParamMap["@type"] == "manualReviewUserTask" -> {
                            inputMap["name"] = taskParamMap["name"] as String
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
                    taskParamMap.toMutableMap()["data"] = dataMap
                    JSONObject(taskParamMap)
                } else {
                    JSONObject(JsonUtil.toMap(task.taskParams))
                }
                val dataPlatTaskDetail = DataPlatTaskDetail(
                    pipelineId = task.pipelineId,
                    buildId = task.buildId,
                    projectEnglishName = task.projectId,
                    vmSeqId = task.containerId,
                    type = "task",
                    itemId = task.taskId,
                    atomCode = task.atomCode,
                    taskParams = taskParams,
                    isSecrecy = isSecrecy,
                    status = BuildStatus.values()[task.status].statusName,
                    errorType = task.errorType,
                    errorCode = task.errorCode,
                    errorMsg = task.errorMsg,
                    startTime = task.startTime?.format(dateTimeFormatter),
                    endTime = task.endTime?.format(dateTimeFormatter),
                    costTime = if ((endTime - startTime) < 0) 0 else (endTime - startTime),
                    starter = task.starter,
                    washTime = LocalDateTime.now().format(dateTimeFormatter),
                    bgId = projectInfo.bgId,
                    deptId = projectInfo.deptId,
                    centerId = projectInfo.centerId,
                    bgName = projectInfo.bgName,
                    deptName = projectInfo.deptName,
                    centerName = projectInfo.centerName,
                    platformCode = platformCode,
                    platformErrorCode = platformErrorCode,
                    platformName = platformName
                )
                logger.info("pushTaskDetail buildId: ${dataPlatTaskDetail.buildId}| taskId: ${dataPlatTaskDetail.itemId}")
                val taskDetailTopic = checkParamBlank(lambdaKafkaTopicConfig.taskDetailTopic, "taskDetailTopic")
                kafkaClient.send(taskDetailTopic, JsonUtil.toJson(dataPlatTaskDetail))
//                kafkaClient.send(KafkaTopic.LANDUN_TASK_DETAIL_TOPIC, JsonUtil.toJson(dataPlatTaskDetail))
            }
        } catch (e: Exception) {
            logger.warn("Push task detail to kafka error, buildId: ${task.buildId}, taskId: ${task.taskId}", e)
        }
    }

    private fun pushInvalidVariables(
        projectInfo: ProjectOrganize,
        historyRecord: TPipelineBuildHistoryRecord,
        variablesWithType: List<BuildParameters>
    ) {
        try {
            val variables = variablesWithType.filter {
                !it.key.startsWith("BK_") && !it.key.startsWith("X-DEVOPS")
            }.associate { it.key to it.value.toString() }
            val invalidKeyList = mutableSetOf<String>()
            variables.forEach { (key, _) ->
                if (!pattern.matcher(key).find()) invalidKeyList.add(key)
            }
            if (invalidKeyList.isEmpty()) {
                variables.forEach { (key, _) ->
                    variables.forEach { (another, _) ->
                        if (
                            key != another && !(invalidKeyList.contains(key) && invalidKeyList.contains(another)) &&
                            (
                                key.startsWith(another) && key.removePrefix(another).startsWith('.') ||
                                    another.startsWith(key) && another.removePrefix(key).startsWith('.')
                                )
                        ) {
                            invalidKeyList.add(key)
                            invalidKeyList.add(another)
                        }
                    }
                }
            }
            if (invalidKeyList.isEmpty()) return
            logger.info(
                "invalidVarBuild buildId=${historyRecord.buildId}" +
                    "|${historyRecord.startUser}|${historyRecord.buildNum}|$invalidKeyList"
            )
            val history = genBuildHistory(
                projectInfo = projectInfo,
                tPipelineBuildHistoryRecord = historyRecord,
                buildStatus = BuildStatus.values(),
                currentTimestamp = System.currentTimeMillis(),
                invalidKeyList = invalidKeyList.toList()
            )
            val topic = checkParamBlank(
                lambdaKafkaTopicConfig.invalidVarBuildHistoryTopic,
                "invalidVarBuildHistoryTopic"
            )
            kafkaClient.send(topic, JsonUtil.toJson(history))
        } catch (e: Exception) {
            logger.warn(
                "Push build history with invalid variable key to kafka error, buildId: ${historyRecord.buildId}",
                e
            )
        }
    }

    private fun pushBuildHistory(projectInfo: ProjectOrganize, historyRecord: TPipelineBuildHistoryRecord) {
        try {
            logger.info(
                "pushBuildHistory buildId=${historyRecord.buildId}" +
                        "|${historyRecord.executeTime}|${historyRecord.buildNum}"
            )
            val history = genBuildHistory(projectInfo, historyRecord, BuildStatus.values(), System.currentTimeMillis())
            val buildHistoryTopic = checkParamBlank(lambdaKafkaTopicConfig.buildHistoryTopic, "buildHistoryTopic")
            kafkaClient.send(buildHistoryTopic, JsonUtil.toJson(history))
//            kafkaClient.send(KafkaTopic.LANDUN_BUILD_HISTORY_TOPIC, JsonUtil.toJson(history))
        } catch (e: Exception) {
            logger.warn("Push build history to kafka error, buildId: ${historyRecord.buildId}", e)
        }
    }

    private fun pushBuildDetail(projectInfo: ProjectOrganize, pipelineId: String, model: TPipelineBuildDetailRecord) {
        try {
            logger.info("pushBuildDetail buildId: ${model.buildId}|${model.buildNum}")
            val buildDetail = genBuildDetail(projectInfo, pipelineId, model)
            val buildDetailTopic = checkParamBlank(lambdaKafkaTopicConfig.buildDetailTopic, "buildDetailTopic")
            kafkaClient.send(buildDetailTopic, JsonUtil.toJson(buildDetail))
//            kafkaClient.send(KafkaTopic.LANDUN_BUILD_DETAIL_TOPIC, JsonUtil.toJson(buildDetail))
        } catch (e: Exception) {
            logger.warn("Push build detail to kafka error, buildId: ${model.buildId}", e)
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
                    sendGitTask2Kafka(atomCode as String, task, gitUrl)
                }
                "gitCodeRepoCommon" -> {
                    val dataMap = JsonUtil.toMap(taskParamsMap["data"] ?: error(""))
                    val inputMap = JsonUtil.toMap(dataMap["input"] ?: error(""))
                    gitUrl = inputMap["repositoryUrl"].toString()
                    sendGitTask2Kafka(atomCode as String, task, gitUrl)
                }
                "PullFromGithub", "GitLab" -> {
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
                    sendGitTask2Kafka(atomCode as String, task, gitUrl)
                }
                "gitCodeRepo" -> {
                    val dataMap = JsonUtil.toMap(taskParamsMap["data"] ?: error(""))
                    val inputMap = JsonUtil.toMap(dataMap["input"] ?: error(""))
                    val repositoryType = inputMap["repositoryType"].toString()
                    val repositoryHashId = inputMap["repositoryHashId"] as String?
                    val repositoryName = inputMap["repositoryName"] as String?
                    val repositoryConfig = RepositoryConfig(
                        repositoryHashId = repositoryHashId,
                        repositoryName = repositoryName,
                        repositoryType = RepositoryType.parseType(repositoryType)
                    )

                    val gitRepository = client.get(ServiceRepositoryResource::class)
                        .get(
                            projectId = event.projectId, repositoryId = repositoryConfig.getRepositoryId(),
                            repositoryType = RepositoryType.parseType(repositoryType)
                        )
                    gitUrl = gitRepository.data!!.url
                    if (gitUrl.isNotBlank()) {
                        sendGitTask2Kafka(atomCode as String, task, gitUrl)
                    }
                }
                "checkout" -> {
                    // post action阶段不需要统计
                    if (task.taskName == "POST：checkout") {
                        return
                    }
                    val dataMap = JsonUtil.toMap(taskParamsMap["data"] ?: error(""))
                    val inputMap = JsonUtil.toMap(dataMap["input"] ?: error(""))
                    val repositoryType = inputMap["repositoryType"].toString()
                    gitUrl = when (repositoryType) {
                        "URL" -> inputMap["repositoryUrl"].toString()
                        "ID", "NAME" -> {
                            val repositoryHashId = inputMap["repositoryHashId"] as String?
                            val repositoryName = inputMap["repositoryName"] as String?
                            val repositoryConfig = RepositoryConfig(
                                repositoryHashId = repositoryHashId,
                                repositoryName = repositoryName,
                                repositoryType = RepositoryType.parseType(repositoryType)
                            )

                            val gitRepository = client.get(ServiceRepositoryResource::class)
                                .get(
                                    projectId = event.projectId, repositoryId = repositoryConfig.getRepositoryId(),
                                    repositoryType = RepositoryType.parseType(repositoryType)
                                )
                            gitRepository.data!!.url
                        }
                        else -> ""
                    }
                    if (gitUrl.isNotBlank()) {
                        sendGitTask2Kafka(atomCode as String, task, gitUrl)
                    }
                }
            }
        } catch (ignore: Exception) {
            logger.warn("Push git task to kafka error, buildId: ${event.buildId}, taskId: ${event.taskId}", ignore)
        }
    }

    private fun sendGitTask2Kafka(atomCode: String, task: TPipelineBuildTaskRecord, gitUrl: String) {
        val taskMap = task.intoMap()
        taskMap["GIT_URL"] = gitUrl
        taskMap["GIT_PROJECT_NAME"] = GitUtils.getProjectName(gitUrl)
        taskMap["WASH_TIME"] = LocalDateTime.now().format(dateTimeFormatter)
        taskMap["ATOM_CODE"] = atomCode
        taskMap.remove("TASK_PARAMS")
        val gitTaskTopic = checkParamBlank(lambdaKafkaTopicConfig.gitTaskTopic, "buildDetailTopic")
        kafkaClient.send(gitTaskTopic, JsonUtil.toJson(taskMap))
//        kafkaClient.send(KafkaTopic.LANDUN_GIT_TASK_TOPIC, JsonUtil.toJson(taskMap))
    }

    private val projectCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build(
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
                        secrecy = projectInfo.secrecy,
                        bgName = projectInfo.bgName ?: "",
                        deptName = projectInfo.deptName ?: "",
                        centerName = projectInfo.centerName ?: "",
                        bgId = projectInfo.bgId ?: "",
                        deptId = projectInfo.deptId ?: "",
                        centerId = projectInfo.centerId ?: ""
                    )
                }
            }
        )

    private val templateCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build(
            object : CacheLoader<String, String>() {
                override fun load(cacheKey: String): String {
                    val arrs = cacheKey.split("::")
                    val projectId = arrs[0]
                    val pipelineId = arrs[1]
                    return lambdaPipelineTemplateDao.getTemplate(dslContext, projectId, pipelineId)?.templateId ?: ""
                }
            }
        )

    private fun genBuildDetail(
        projectInfo: ProjectOrganize,
        pipelineId: String,
        buildDetailRecord: TPipelineBuildDetailRecord
    ): DataPlatBuildDetail {
        return with(buildDetailRecord) {
            val projectId = projectInfo.projectId
            val isSecrecy = projectInfo.secrecy
            DataPlatBuildDetail(
                washTime = LocalDateTime.now().format(dateTimeFormatter),
                buildId = buildId,
                templateId = templateCache.get("$projectId::$pipelineId"),
                bgName = projectInfo.bgName,
                deptName = projectInfo.deptName,
                centerName = projectInfo.centerName,
                projectId = projectId,
                pipelineId = pipelineId,
                buildNum = buildNum,
                isSecrecy = isSecrecy,
                model = model,
                trigger = trigger,
                startUser = startUser,
                startTime = startTime?.format(dateTimeFormatter) ?: "",
                endTime = endTime?.format(dateTimeFormatter) ?: "",
                status = status,
                bgId = projectInfo.bgId,
                deptId = projectInfo.deptId,
                centerId = projectInfo.centerId
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
                triggerUser = t.triggerUser,
                queueTime = t.queueTime?.timestampmilli() ?: 0L,
                startTime = t.startTime?.timestampmilli() ?: 0L,
                endTime = t.endTime?.timestampmilli() ?: 0L,
                taskCount = t.taskCount,
                firstTaskId = t.firstTaskId,
                parentBuildId = t.parentBuildId,
                parentTaskId = t.parentTaskId,
                channelCode = ChannelCode.valueOf(t.channel),
                errorInfoList = null,
                executeTime = t.executeTime ?: 0,
                buildParameters = t.buildParameters?.let { self ->
                    JsonUtil.getObjectMapper().readValue(self) as List<BuildParameters>
                }
            )
        }
    }

    private fun genBuildHistory(
        projectInfo: ProjectOrganize,
        tPipelineBuildHistoryRecord: TPipelineBuildHistoryRecord,
        buildStatus: Array<BuildStatus>,
        currentTimestamp: Long,
        invalidKeyList: List<String>? = null
    ): DataPlatBuildHistory {
        return with(tPipelineBuildHistoryRecord) {
            val totalTime = if (startTime == null || endTime == null) {
                0
            } else {
                Duration.between(startTime, endTime).toMillis()
            }

            val labelList = mutableListOf<String>()
            val projectId = projectInfo.projectId
            val isSecrecy = projectInfo.secrecy
            lambdaPipelineLabelDao.getLables(dslContext, projectId, pipelineId)?.forEach { label ->
                labelList.add(label["name"] as String)
            }

            DataPlatBuildHistory(
                washTime = LocalDateTime.now().format(dateTimeFormatter),
                templateId = templateCache.get("$projectId::$pipelineId"),
                bgName = projectInfo.bgName,
                deptName = projectInfo.deptName,
                centerName = projectInfo.centerName,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = triggerUser ?: startUser,
                trigger = StartType.toReadableString(trigger, ChannelCode.valueOf(channel)),
                buildNum = buildNum,
                pipelineVersion = version,
                startTime = startTime?.format(dateTimeFormatter) ?: "",
                endTime = endTime?.format(dateTimeFormatter) ?: "",
                status = buildStatus[status].name,
                statusOrdinal = buildStatus[status].ordinal,
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
                errorInfoList = errorInfo,
                startUser = startUser,
                channel = channel,
                labels = labelList,
                isSecrecy = isSecrecy,
                buildMsg = buildMsg,
                bgId = projectInfo.bgId,
                deptId = projectInfo.deptId,
                centerId = projectInfo.centerId,
                invalidKeyList = invalidKeyList
            )
        }
    }

    private fun checkParamBlank(param: String?, message: String): String {
        if (param.isNullOrBlank()) {
            throw ParamBlankException(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_STARTUP_CONFIGURATION_MISSING,
                    language = I18nUtil.getLanguage(),
                    params = arrayOf(message)
                )
            )
        }
        return param
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
        private val pattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9\\.\\-_]*[a-zA-Z0-9\\-_]\$")
    }
}
