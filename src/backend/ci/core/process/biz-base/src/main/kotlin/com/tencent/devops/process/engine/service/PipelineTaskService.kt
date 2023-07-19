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

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.model.process.tables.records.TPipelineInfoRecord
import com.tencent.devops.model.process.tables.records.TPipelineModelTaskRecord
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineBuildTaskDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.pojo.UpdateTaskInfo
import com.tencent.devops.process.engine.service.detail.TaskBuildDetailService
import com.tencent.devops.process.engine.service.record.TaskBuildRecordService
import com.tencent.devops.process.engine.utils.PauseRedisUtils
import com.tencent.devops.process.pojo.PipelineProjectRel
import com.tencent.devops.process.pojo.task.PipelineBuildTaskInfo
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.process.utils.BK_CI_BUILD_FAIL_TASKNAMES
import com.tencent.devops.process.utils.BK_CI_BUILD_FAIL_TASKS
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PROJECT_ID
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    private val taskBuildDetailService: TaskBuildDetailService,
    private val taskBuildRecordService: TaskBuildRecordService,
    private val pipelineModelTaskDao: PipelineModelTaskDao,
    private val pipelineBuildTaskDao: PipelineBuildTaskDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val buildLogPrinter: BuildLogPrinter,
    private val pipelineVariableService: BuildVariableService,
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
                    "executeCount" to (it.executeCount ?: 1)
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

    fun deletePipelineBuildTasks(transactionContext: DSLContext?, projectId: String, pipelineId: String) {
        pipelineBuildTaskDao.deletePipelineBuildTasks(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
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
        taskId: String
    ): PipelineBuildTask? {
        return pipelineBuildTaskDao.get(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            buildId = buildId,
            taskId = taskId
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

    fun getBuildTask(projectId: String, buildId: String, taskId: String): PipelineBuildTask? {
        return pipelineBuildTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            taskId = taskId
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

    fun setTaskErrorInfo(
        transactionContext: DSLContext?,
        projectId: String,
        buildId: String,
        taskId: String,
        errorType: ErrorType,
        errorCode: Int,
        errorMsg: String
    ) {
        JooqUtils.retryWhenDeadLock {
            pipelineBuildTaskDao.setTaskErrorInfo(
                dslContext = transactionContext ?: dslContext,
                projectId = projectId,
                buildId = buildId,
                taskId = taskId,
                errorType = errorType,
                errorCode = errorCode,
                errorMsg = errorMsg
            )
        }
    }

    fun updateTaskStatusInfo(userId: String? = null, task: PipelineBuildTask?, updateTaskInfo: UpdateTaskInfo) {
        val taskRecord by lazy {
            task ?: pipelineBuildTaskDao.get(
                dslContext = dslContext,
                projectId = updateTaskInfo.projectId,
                buildId = updateTaskInfo.buildId,
                taskId = updateTaskInfo.taskId
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
                )) {
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
    @Suppress("UNCHECKED_CAST")
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

    fun isRetryWhenFail(projectId: String, taskId: String, buildId: String): Boolean {
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
                message = "[${taskRecord.taskName}] failed, and retry $nextCount",
                tag = taskRecord.taskId,
                jobId = taskRecord.containerId,
                executeCount = 1
            )
        }
        return isRry
    }

    fun isNeedPause(taskId: String, buildId: String, taskRecord: PipelineBuildTask): Boolean {
        val alreadyPause = redisOperation.get(PauseRedisUtils.getPauseRedisKey(buildId = buildId, taskId = taskId))
        return ControlUtils.pauseBeforeExec(taskRecord.additionalOptions, alreadyPause)
    }

    fun executePause(taskId: String, buildId: String, taskRecord: PipelineBuildTask) {
        buildLogPrinter.addYellowLine(
            buildId = buildId,
            message = "[${taskRecord.taskName}] pause, waiting ...",
            tag = taskRecord.taskId,
            jobId = taskRecord.containerId,
            executeCount = taskRecord.executeCount ?: 1
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
        val model = taskBuildDetailService.getBuildModel(projectId, buildId)
        val failTask = pipelineVariableService.getVariable(
            projectId, pipelineId, buildId, BK_CI_BUILD_FAIL_TASKS
        )
        val failTaskNames = pipelineVariableService.getVariable(
            projectId, pipelineId, buildId, BK_CI_BUILD_FAIL_TASKNAMES
        )
        try {
            val errorElement = findElementMsg(model, taskRecord)
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
        val failTaskNameRecord = redisOperation.get(failTaskNameRedisKey(buildId = buildId, taskId = taskId))
        if (failTaskRecord.isNullOrBlank() || failTaskNameRecord.isNullOrBlank()) {
            return
        }
        try {
            val failTask = pipelineVariableService.getVariable(
                projectId, pipelineId, buildId, BK_CI_BUILD_FAIL_TASKS
            )
            val failTaskNames = pipelineVariableService.getVariable(
                projectId, pipelineId, buildId, BK_CI_BUILD_FAIL_TASKNAMES
            )
            val newFailTask = failTask!!.replace(failTaskRecord, "")
            val newFailTaskNames = failTaskNames!!.replace(failTaskNameRecord, "")
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

    private fun failTaskRedisKey(buildId: String, taskId: String): String {
        return "devops:failTask:redis:key:$buildId:$taskId"
    }

    private fun failTaskNameRedisKey(buildId: String, taskId: String): String {
        return "devops:failTaskName:redis:key:$buildId:$taskId"
    }

    private fun findElementMsg(
        model: Model?,
        taskRecord: PipelineBuildTask
    ): Pair<String, String> {
        val containerName = model?.getContainer(taskRecord.containerId)?.name ?: ""
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

        redisOperation.set(
            key = PauseRedisUtils.getPauseRedisKey(buildId = task.buildId, taskId = task.taskId),
            value = "true",
            expiredInSecond = Timeout.CONTAINER_MAX_MILLS / 1000
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
        val pipelineId = task.pipelineId
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
        // #5109 非事务强相关，减少影响。仅做摘要展示，无需要时时更新
        if (buildStatus.isRunning()) {
            pipelineBuildSummaryDao.updateCurrentBuildTask(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                currentTaskId = taskId,
                currentTaskName = taskName
            )
        }
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
