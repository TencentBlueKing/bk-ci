/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.constant.NAME
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineModelTaskRecord
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BUILD_TASK_RETRY_NOTICE
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.pojo.UpdateTaskInfo
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.pojo.PipelineProjectRel
import com.tencent.devops.process.pojo.task.PipelineBuildTaskInfo
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.process.utils.BK_CI_BUILD_FAIL_TASKNAMES
import com.tencent.devops.process.utils.BK_CI_BUILD_FAIL_TASKS
import com.tencent.devops.process.utils.JOB_RETRY_TASK_ID
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PROJECT_ID
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress(
    "TooManyFunctions",
    "LongParameterList",
    "LongMethod",
    "ComplexMethod",
    "NestedBlockDepth",
    "ReturnCount",
    "LargeClass"
)
@Service
class PipelineTaskService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val pipelineInfoDao: PipelineInfoDao,
    private val taskBuildRecordService: TaskBuildRecordService,
    private val pipelineModelTaskDao: PipelineModelTaskDao,
    private val pipelineBuildTaskDao: PipelineBuildTaskDao,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineVariableService: BuildVariableService,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val pipelineBuildRecordService: PipelineBuildRecordService,
    private val pipelinePauseExtService: PipelinePauseExtService
) {

    fun list(projectId: String, pipelineIds: Collection<String>): Map<String, List<PipelineModelTask>> {
        return pipelineModelTaskDao.listByPipelineIds(dslContext, projectId, pipelineIds)?.map {
            PipelineModelTask(
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                stageId = it.stageId,
                containerId = it.containerId,
                taskId = it.taskId,
                taskSeq = it.taskSeq,
                taskName = it.taskName,
                atomCode = it.atomCode,
                atomVersion = it.atomVersion,
                classType = it.classType,
                taskAtom = it.taskAtom,
                taskParams = objectMapper.readValue(it.taskParams),
                additionalOptions = if (it.additionalOptions.isNullOrBlank()) {
                    null
                } else {
                    objectMapper.readValue(it.additionalOptions, ElementAdditionalOptions::class.java)
                },
                os = it.os
            )
        }?.groupBy { it.pipelineId } ?: mapOf()
    }

    fun getAllBuildTaskInfo(projectId: String, buildId: String): List<PipelineBuildTaskInfo> {
        val list = pipelineBuildTaskDao.getByBuildId(dslContext, projectId, buildId)
        return list.map {
            with(it) {
                PipelineBuildTaskInfo(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stageId,
                    containerId = containerId,
                    containerHashId = containerHashId,
                    containerType = containerType,
                    taskSeq = taskSeq,
                    taskId = taskId,
                    taskName = taskName,
                    taskType = taskType,
                    taskAtom = taskAtom,
                    status = status,
                    taskParams = JsonUtil.toMutableMap(taskParams),
                    additionalOptions = additionalOptions,
                    executeCount = executeCount ?: 1,
                    starter = starter,
                    approver = approver,
                    subBuildId = subBuildId,
                    startTime = startTime?.timestampmilli() ?: 0L,
                    endTime = endTime?.timestampmilli() ?: 0L,
                    errorType = errorType,
                    errorCode = errorCode,
                    errorMsg = errorMsg
                )
            }
        }
    }

    fun getAllBuildTask(projectId: String, buildId: String): Collection<PipelineBuildTask> {
        return pipelineBuildTaskDao.getByBuildId(dslContext, projectId, buildId)
    }

    fun getRunningTask(projectId: String, buildId: String): List<Map<String, Any>> {
        val listByStatus = pipelineBuildTaskDao.getTasksInCondition(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            containerId = null,
            statusSet = listOf(BuildStatus.RUNNING, BuildStatus.REVIEWING, BuildStatus.PAUSE)
        )
        val list = mutableListOf<Map<String, Any>>()
        listByStatus.forEach {
            list.add(
                mapOf(
                    "taskId" to it.taskId,
                    "containerId" to it.containerId,
                    "status" to it.status,
                    "executeCount" to (it.executeCount ?: 1),
                    "stepId" to (it.stepId ?: "")
                )
            )
        }
        return list
    }

    fun listByBuildId(projectId: String, buildId: String): Collection<PipelineBuildTask> {
        return pipelineBuildTaskDao.getByBuildId(dslContext, projectId, buildId)
    }

    fun batchSave(transactionContext: DSLContext?, taskList: Collection<PipelineBuildTask>) {
        return pipelineBuildTaskDao.batchSave(transactionContext ?: dslContext, taskList)
    }

    fun batchUpdate(transactionContext: DSLContext?, taskList: List<PipelineBuildTask>) {
        return JooqUtils.retryWhenDeadLock {
            pipelineBuildTaskDao.batchUpdate(transactionContext ?: dslContext, taskList)
        }
    }

    fun deleteTasksByContainerSeqId(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String
    ): Int {
        return pipelineBuildTaskDao.deleteBuildTasksByContainerSeqId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = containerId
        )
    }

    fun getByTaskId(
        transactionContext: DSLContext? = null,
        projectId: String,
        buildId: String,
        taskId: String?,
        stepId: String? = null,
        executeCount: Int? = null
    ): PipelineBuildTask? {
        return pipelineBuildTaskDao.get(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            buildId = buildId,
            taskId = taskId,
            stepId = stepId,
            executeCount = executeCount
        )
    }

    fun getTaskStatus(
        transactionContext: DSLContext? = null,
        projectId: String,
        buildId: String,
        taskId: String
    ): BuildStatus? {
        val statusOrdinal = pipelineBuildTaskDao.getTaskStatus(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            buildId = buildId,
            taskId = taskId
        ) ?: return null

        return BuildStatus.values()[statusOrdinal.value1()]
    }

    fun updateTaskParamWithElement(projectId: String, buildId: String, taskId: String, newElement: Element) {
        JooqUtils.retryWhenDeadLock {
            pipelineBuildTaskDao.updateTaskParam(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                taskId = taskId,
                taskParam = JsonUtil.toJson(newElement, false)
            )
        }
    }

    fun updateTaskParam(
        transactionContext: DSLContext? = null,
        projectId: String,
        buildId: String,
        taskId: String,
        taskParam: String
    ): Int {
        return JooqUtils.retryWhenDeadLock {
            pipelineBuildTaskDao.updateTaskParam(
                dslContext = transactionContext ?: dslContext,
                projectId = projectId,
                buildId = buildId,
                taskId = taskId,
                taskParam = taskParam
            )
        }
    }

    fun listContainerBuildTasks(
        projectId: String,
        buildId: String,
        containerSeqId: String?,
        buildStatusSet: Set<BuildStatus>? = null
    ): List<PipelineBuildTask> {
        return pipelineBuildTaskDao.getTasksInCondition(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            containerId = containerSeqId,
            statusSet = buildStatusSet
        )
    }

    fun getBuildTask(
        projectId: String,
        buildId: String,
        taskId: String?,
        stepId: String? = null,
        executeCount: Int? = null
    ): PipelineBuildTask? {
        return pipelineBuildTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            taskId = taskId,
            stepId = stepId,
            executeCount = executeCount
        )
    }

    fun updateSubBuildId(
        projectId: String,
        buildId: String,
        taskId: String,
        subBuildId: String,
        subProjectId: String
    ): Int {
        return JooqUtils.retryWhenDeadLock {
            pipelineBuildTaskDao.updateSubBuildId(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                taskId = taskId,
                subBuildId = subBuildId,
                subProjectId = subProjectId
            )
        }
    }

    fun updateTaskStatusInfo(userId: String? = null, task: PipelineBuildTask?, updateTaskInfo: UpdateTaskInfo) {
        val taskRecord by lazy {
            task ?: pipelineBuildTaskDao.get(
                dslContext = dslContext,
                projectId = updateTaskInfo.projectId,
                buildId = updateTaskInfo.buildId,
                taskId = updateTaskInfo.taskId,
                stepId = null,
                executeCount = null
            )
        }
        if (updateTaskInfo.taskStatus.isFinish()) {
            val dbStartTime = taskRecord?.startTime
            updateTaskInfo.endTime = LocalDateTime.now()
            updateTaskInfo.totalTime = if (dbStartTime == null) {
                0
            } else {
                Duration.between(dbStartTime, updateTaskInfo.endTime).toMillis()
            }
            if (updateTaskInfo.taskStatus.isReview() && !userId.isNullOrBlank()) {
                updateTaskInfo.approver = userId
            }
        } else if (updateTaskInfo.taskStatus.isRunning()) {
            if (TaskUtils.isRefreshTaskTime(
                    buildId = updateTaskInfo.buildId,
                    taskId = updateTaskInfo.taskId,
                    additionalOptions = taskRecord?.additionalOptions,
                    executeCount = taskRecord?.executeCount
                )
            ) {
                // 如果是自动重试则不重置task的时间
                updateTaskInfo.startTime = LocalDateTime.now()
                if (!userId.isNullOrBlank()) {
                    updateTaskInfo.starter = userId
                }
            }
        }

        JooqUtils.retryWhenDeadLock {
            pipelineBuildTaskDao.updateTaskInfo(dslContext = dslContext, updateTaskInfo = updateTaskInfo)
        }
    }

    /**
     * 根据插件标识，获取使用插件的流水线详情
     */
    fun listPipelinesByAtomCode(
        atomCode: String,
        projectCode: String?,
        page: Int?,
        pageSize: Int?
    ): Page<PipelineProjectRel> {
        val pageNotNull = PageUtil.getValidPage(page)
        val pageSizeNotNull = PageUtil.getValidPageSize(pageSize)

        val count = pipelineModelTaskDao.getPipelineCountByAtomCode(dslContext, atomCode, projectCode).toLong()
        val pipelineTasks =
            pipelineModelTaskDao.listByAtomCode(
                dslContext = dslContext,
                atomCode = atomCode,
                projectId = projectCode,
                page = pageNotNull,
                pageSize = pageSizeNotNull
            )

        val pipelineAtomVersionInfo = mutableMapOf<String, MutableSet<String>>()
        val pipelineIds = pipelineTasks?.map { it[KEY_PIPELINE_ID] as String }?.toSet()
        var pipelineNameMap: MutableMap<String, String>? = null
        if (!pipelineIds.isNullOrEmpty()) {
            pipelineNameMap = mutableMapOf()
            val pipelineAtoms = pipelineModelTaskDao.listByAtomCodeAndPipelineIds(
                dslContext = dslContext,
                atomCode = atomCode,
                pipelineIds = pipelineIds
            )
            pipelineAtoms?.forEach {
                val version = it[KEY_VERSION] as? String ?: return@forEach
                val pipelineId = it[KEY_PIPELINE_ID] as String
                if (pipelineAtomVersionInfo.containsKey(pipelineId)) {
                    pipelineAtomVersionInfo[pipelineId]!!.add(version)
                } else {
                    pipelineAtomVersionInfo[pipelineId] = mutableSetOf(version)
                }
            }
            val pipelineInfoRecords =
                pipelineInfoDao.listInfoByPipelineIds(dslContext = dslContext, pipelineIds = pipelineIds)
            pipelineInfoRecords.forEach {
                pipelineNameMap[it.pipelineId] = it.pipelineName
            }
        }

        val records = pipelineTasks?.map {
            val pipelineId = it[KEY_PIPELINE_ID] as String
            PipelineProjectRel(
                pipelineId = pipelineId,
                pipelineName = pipelineNameMap?.get(pipelineId) ?: "",
                projectCode = it[KEY_PROJECT_ID] as String,
                atomVersion = pipelineAtomVersionInfo[pipelineId]?.joinToString(",") ?: ""
            )
        }
            ?: listOf<PipelineProjectRel>()

        return Page(pageNotNull, pageSizeNotNull, count, records)
    }

    fun listPipelineNumByAtomCodes(projectId: String? = null, atomCodes: List<String>): Map<String, Int> {
        val dataMap = mutableMapOf<String, Int>()
        atomCodes.forEach { atomCode ->
            val count = pipelineModelTaskDao.getPipelineCountByAtomCode(dslContext, atomCode, projectId)
            dataMap[atomCode] = count
        }
        return dataMap
    }

    fun isRetryWhenFail(projectId: String, taskId: String, buildId: String, failedMsg: String?): Boolean {
        val taskRecord = getBuildTask(projectId, buildId, taskId)
            ?: return false
        val retryCount = redisOperation.get(
            TaskUtils.getFailRetryTaskRedisKey(buildId = taskRecord.buildId, taskId = taskRecord.taskId)
        )?.toInt() ?: 0
        val isRry = ControlUtils.retryWhenFailure(taskRecord.additionalOptions, retryCount)
        if (isRry) {
            val nextCount = retryCount + 1
            redisOperation.set(
                key = TaskUtils.getFailRetryTaskRedisKey(buildId = taskRecord.buildId, taskId = taskRecord.taskId),
                value = nextCount.toString()
            )
            buildLogPrinter.addYellowLine(
                buildId = buildId,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_BUILD_TASK_RETRY_NOTICE,
                    params = arrayOf(taskRecord.taskName, nextCount.toString(), failedMsg ?: "run failed"),
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                tag = taskRecord.taskId,
                containerHashId = taskRecord.containerId,
                executeCount = 1,
                jobId = null,
                stepId = taskRecord.stepId
            )
        }
        return isRry
    }

    fun taskRetryRecordSet(
        projectId: String,
        taskId: String,
        buildId: String,
        pipelineId: String,
        containerId: String,
        executeCount: Int
    ) {
        val lastContainerRecord = containerBuildRecordService.getRecord(
            transactionContext = null,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = containerId,
            executeCount = executeCount.coerceAtLeast(1) // 至少取第一次执行结果
        )
        if (lastContainerRecord != null) {
            lastContainerRecord.containerVar[JOB_RETRY_TASK_ID] = taskId
            pipelineBuildRecordService.batchSave(
                transactionContext = null, model = null, stageList = null,
                containerList = listOf(lastContainerRecord), taskList = null
            )
        }
    }

    fun isNeedPause(taskId: String, buildId: String, taskRecord: PipelineBuildTask): Boolean {
        val alreadyPause = taskBuildRecordService.taskAlreadyPause(
            projectId = taskRecord.projectId,
            pipelineId = taskRecord.pipelineId,
            buildId = buildId,
            taskId = taskId,
            executeCount = taskRecord.executeCount ?: 1
        )
        return ControlUtils.pauseBeforeExec(taskRecord.additionalOptions, alreadyPause)
    }

    fun executePause(taskId: String, buildId: String, taskRecord: PipelineBuildTask) {
        buildLogPrinter.addYellowLine(
            buildId = buildId,
            message = "[${taskRecord.taskName}] pause, waiting ...",
            tag = taskRecord.taskId,
            containerHashId = taskRecord.containerId,
            executeCount = taskRecord.executeCount ?: 1,
            jobId = null,
            stepId = taskRecord.stepId
        )

        pauseBuild(task = taskRecord)

        pipelinePauseExtService.sendPauseNotify(buildId, taskRecord)
    }

    fun removeRetryCache(buildId: String, taskId: String) {
        // 清除该原子内的重试记录
        redisOperation.delete(TaskUtils.getFailRetryTaskRedisKey(buildId = buildId, taskId = taskId))
    }

    fun createFailTaskVar(buildId: String, projectId: String, pipelineId: String, taskId: String) {
        val taskRecord = getBuildTask(projectId, buildId, taskId)
            ?: return
        val buildRecordContainer = containerBuildRecordService.getRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = taskRecord.containerId,
            executeCount = taskRecord.executeCount
        ) ?: return
        val containerName = buildRecordContainer.containerVar[NAME]?.toString() ?: taskRecord.containerId
        val failTask = pipelineVariableService.getVariable(
            projectId, pipelineId, buildId, BK_CI_BUILD_FAIL_TASKS
        )
        val failTaskNames = pipelineVariableService.getVariable(
            projectId, pipelineId, buildId, BK_CI_BUILD_FAIL_TASKNAMES
        )
        try {
            val errorElement = findElementMsg(containerName, taskRecord)

            // 存在的不重复添加 fix：流水线设置的变量重试一次就会叠加一次变量值 #6058
            if (inFailTasks(failTasks = failTask, failTask = errorElement.first)) {
                logger.info("$projectId|$buildId|$taskId| skip_createFailTaskVar: ${errorElement.first}")
                return
            }

            val errorElements = if (failTask.isNullOrBlank()) {
                errorElement.first
            } else {
                failTask + errorElement.first
            }

            val errorElementsName = if (failTaskNames.isNullOrBlank()) {
                errorElement.second
            } else {
                "$failTaskNames,${errorElement.second}"
            }
            val valueMap = mutableMapOf<String, Any>()
            valueMap[BK_CI_BUILD_FAIL_TASKS] = errorElements
            valueMap[BK_CI_BUILD_FAIL_TASKNAMES] = errorElementsName
            pipelineVariableService.batchUpdateVariable(
                buildId = buildId,
                projectId = projectId,
                pipelineId = pipelineId,
                variables = valueMap
            )
        } catch (ignored: Exception) {
            logger.warn("$buildId| $taskId| createFailElementVar error, msg: $ignored")
        }
    }

    fun removeFailTaskVar(buildId: String, projectId: String, pipelineId: String, taskId: String) {
        val failTaskRecord = redisOperation.get(failTaskRedisKey(buildId = buildId, taskId = taskId))
        if (failTaskRecord.isNullOrBlank()) {
            return
        }
        val failTaskNameRecord = redisOperation.get(failTaskNameRedisKey(buildId = buildId, taskId = taskId))
        if (failTaskNameRecord.isNullOrBlank()) {
            return
        }
        try {
            val failTask = pipelineVariableService.getVariable(
                projectId, pipelineId, buildId, BK_CI_BUILD_FAIL_TASKS
            ) ?: return
            val newFailTask = delTaskString(strings = failTask, string = failTaskRecord, " \n")

            val failTaskNames = pipelineVariableService.getVariable(
                projectId, pipelineId, buildId, BK_CI_BUILD_FAIL_TASKNAMES
            ) ?: return
            val newFailTaskNames = delTaskString(strings = failTaskNames, string = failTaskNameRecord, ",")
            if (newFailTask != failTask || newFailTaskNames != failTaskNames) {
                val valueMap = mutableMapOf<String, Any>()
                valueMap[BK_CI_BUILD_FAIL_TASKS] = newFailTask
                valueMap[BK_CI_BUILD_FAIL_TASKNAMES] = newFailTaskNames
                pipelineVariableService.batchUpdateVariable(
                    buildId = buildId, projectId = projectId, pipelineId = pipelineId, variables = valueMap
                )
            }
            redisOperation.delete(failTaskRedisKey(buildId = buildId, taskId = taskId))
            redisOperation.delete(failTaskNameRedisKey(buildId = buildId, taskId = taskId))
        } catch (ignored: Exception) {
            logger.warn("$buildId|$taskId|removeFailVarWhenSuccess error, msg: $ignored")
        }
    }

    private fun inFailTasks(failTasks: String?, failTask: String) =
        failTasks?.split(" \n")?.contains(failTask.replace(" \n", "")) ?: false

    private fun delTaskString(strings: String, string: String, delimiter: String) =
        strings.split(delimiter).toMutableList().let {
            it.remove(string.replace(delimiter, ""))
            it.joinToString(separator = delimiter)
        }

    private fun failTaskRedisKey(buildId: String, taskId: String): String {
        return "devops:failTask:redis:key:$buildId:$taskId"
    }

    private fun failTaskNameRedisKey(buildId: String, taskId: String): String {
        return "devops:failTaskName:redis:key:$buildId:$taskId"
    }

    private fun findElementMsg(
        containerName: String,
        taskRecord: PipelineBuildTask
    ): Pair<String, String> {
        val failTask = "[${taskRecord.stageId}][$containerName]${taskRecord.taskName} \n"
        val failTaskName = taskRecord.taskName

        redisOperation.set(
            key = failTaskRedisKey(buildId = taskRecord.buildId, taskId = taskRecord.taskId),
            value = failTask,
            expiredInSecond = expiredInSecond,
            expired = true
        )
        redisOperation.set(
            key = failTaskNameRedisKey(buildId = taskRecord.buildId, taskId = taskRecord.taskId),
            value = failTaskName,
            expiredInSecond = expiredInSecond,
            expired = true
        )
        return Pair(failTask, failTaskName)
    }

    fun pauseBuild(task: PipelineBuildTask) {
        logger.info("ENGINE|${task.buildId}|PAUSE_BUILD|${task.stageId}|j(${task.containerId})|task=${task.taskId}")
        // 修改任务状态位暂停
        updateTaskStatus(task = task, userId = task.starter, buildStatus = BuildStatus.PAUSE)

        taskBuildRecordService.taskPause(
            projectId = task.projectId,
            pipelineId = task.pipelineId,
            buildId = task.buildId,
            stageId = task.stageId,
            containerId = task.containerId,
            taskId = task.taskId,
            executeCount = task.executeCount ?: 1
        )
    }

    fun updateTaskStatus(
        task: PipelineBuildTask,
        userId: String,
        buildStatus: BuildStatus,
        errorType: ErrorType? = null,
        errorCode: Int? = null,
        errorMsg: String? = null,
        platformCode: String? = null,
        platformErrorCode: Int? = null
    ) {
        val taskStatus = BuildStatusSwitcher.taskStatusMaker.switchByErrorCode(buildStatus, errorCode)
        val projectId = task.projectId
        val buildId = task.buildId
        val taskId = task.taskId
        val taskName = task.taskName
        val executeCount = task.executeCount ?: 1
        logger.info(
            "${task.buildId}|UPDATE_TASK_STATUS|$taskName|$taskStatus|$userId|$errorCode" +
                "|opt_change=${task.additionalOptions?.change}"
        )
        updateTaskStatusInfo(
            userId = userId,
            task = task,
            updateTaskInfo = UpdateTaskInfo(
                projectId = projectId,
                buildId = buildId,
                taskId = taskId,
                executeCount = executeCount,
                taskStatus = taskStatus,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg,
                platformCode = platformCode,
                platformErrorCode = platformErrorCode,
                taskParams = task.taskParams.takeIf { task.additionalOptions?.change == true }, // opt有变需要一起变
                additionalOptions = task.additionalOptions?.takeIf { task.additionalOptions!!.change }
            )
        )
        // 卡片界面上 已经不再展示当前正在执行的插件任务名称,因此不需要更新,并减少热点流水线的该表锁竞争.
//        // #5109 非事务强相关，减少影响。仅做摘要展示，无需要时时更新
//        if (buildStatus.isRunning()) {
//            pipelineBuildSummaryDao.updateCurrentBuildTask(
//                dslContext = dslContext,
//                projectId = projectId,
//                pipelineId = pipelineId,
//                buildId = buildId,
//                currentTaskId = taskId,
//                currentTaskName = taskName
//            )
//        }
    }

    /**
     * 更新ModelTask表插件版本
     */
    fun asyncUpdateTaskAtomVersion(): Boolean {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin asyncUpdateTaskAtomVersion!!")
            var offset = 0
            do {
                // 查询流水线记录
                val pipelineInfoRecords = pipelineInfoDao.listPipelineInfoByProject(
                    dslContext = dslContext,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
                // 更新流水线任务表的插件版本
                updatePipelineTaskAtomVersion(pipelineInfoRecords)
                offset += DEFAULT_PAGE_SIZE
            } while (pipelineInfoRecords?.size == DEFAULT_PAGE_SIZE)
            logger.info("end asyncUpdateTaskAtomVersion!!")
        }
        return true
    }

    private fun updatePipelineTaskAtomVersion(pipelineInfoRecords: Result<TPipelineInfoRecord>?) {
        if (pipelineInfoRecords?.isNotEmpty == true) {
            pipelineInfoRecords.forEach { pipelineInfoRecord ->
                val modelTasks = pipelineModelTaskDao.getModelTasks(
                    dslContext = dslContext,
                    projectId = pipelineInfoRecord.projectId,
                    pipelineId = pipelineInfoRecord.pipelineId,
                    isAtomVersionNull = true
                )
                modelTasks?.forEach { modelTask ->
                    updateModelTaskVersion(modelTask, pipelineInfoRecord)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateModelTaskVersion(
        modelTask: TPipelineModelTaskRecord,
        pipelineInfoRecord: TPipelineInfoRecord
    ) {
        val pipelineId = modelTask.pipelineId
        val taskParamsStr = modelTask.taskParams
        val taskParams = if (!taskParamsStr.isNullOrBlank()) JsonUtil.getObjectMapper()
            .readValue(taskParamsStr, Map::class.java) as Map<String, Any?> else mapOf()
        val atomVersion = taskParams[KEY_VERSION]?.toString()
        try {
            pipelineModelTaskDao.updateTaskAtomVersion(
                dslContext = dslContext,
                atomVersion = atomVersion ?: "",
                createTime = pipelineInfoRecord.createTime,
                updateTime = pipelineInfoRecord.updateTime,
                projectId = modelTask.projectId,
                pipelineId = pipelineId,
                stageId = modelTask.stageId,
                containerId = modelTask.containerId,
                taskId = modelTask.taskId
            )
        } catch (ignored: Exception) {
            val taskName = modelTask.taskName
            logger.warn("update pipelineId:$pipelineId,taskName:$taskName version fail:", ignored)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTaskService::class.java)
        private val expiredInSecond = TimeUnit.DAYS.toMinutes(7L)
        private const val DEFAULT_PAGE_SIZE = 50
    }
}
