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

import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.extend.ModelCheckPlugin
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.BK_MANUALLY_SKIPPED
import com.tencent.devops.process.constant.ProcessMessageCode.BK_START_USER
import com.tencent.devops.process.constant.ProcessMessageCode.BK_TRIGGER_USER
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.context.MatrixBuildContext
import com.tencent.devops.process.engine.control.VmOperateTaskGenerator
import com.tencent.devops.process.engine.control.lock.PipelineBuildNoLock
import com.tencent.devops.process.engine.dao.PipelineBuildContainerDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.app.StartBuildContext
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.PIPELINE_NAME
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线Container相关的服务
 * @version 1.0
 */
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
class PipelineContainerService @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val dslContext: DSLContext,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val pipelineTaskService: PipelineTaskService,
    private val vmOperatorTaskGenerator: VmOperateTaskGenerator,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val pipelineBuildContainerDao: PipelineBuildContainerDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineContainerService::class.java)
        private const val ELEMENT_NAME_MAX_LENGTH = 128
    }

    fun getContainer(
        projectId: String,
        buildId: String,
        stageId: String?,
        containerId: String
    ): PipelineBuildContainer? {
        // #4518 防止出错暂时保留两个字段的兼容查询
        return pipelineBuildContainerDao.getByContainerId(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            containerId = containerId
        )
    }

    fun listContainers(
        projectId: String,
        buildId: String,
        stageId: String? = null,
        containsMatrix: Boolean? = true,
        statusSet: Set<BuildStatus>? = null
    ): List<PipelineBuildContainer> {
        return pipelineBuildContainerDao.listByBuildId(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            containsMatrix = containsMatrix,
            statusSet = statusSet
        )
    }

    fun countStageContainers(
        transactionContext: DSLContext?,
        projectId: String,
        buildId: String,
        stageId: String,
        onlyMatrixGroup: Boolean
    ): Int {
        return pipelineBuildContainerDao.countStageContainers(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            onlyMatrixGroup = onlyMatrixGroup
        )
    }

    fun listGroupContainers(
        projectId: String,
        buildId: String,
        matrixGroupId: String
    ): List<PipelineBuildContainer> {
        return pipelineBuildContainerDao.listByMatrixGroupId(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            matrixGroupId = matrixGroupId
        )
    }

    fun listByBuildId(projectId: String, buildId: String, stageId: String? = null): List<PipelineBuildContainer> {
        return pipelineBuildContainerDao.listByBuildId(dslContext, projectId, buildId, stageId)
    }

    fun batchSave(transactionContext: DSLContext?, containerList: Collection<PipelineBuildContainer>) {
        return pipelineBuildContainerDao.batchSave(transactionContext ?: dslContext, containerList)
    }

    fun batchUpdate(transactionContext: DSLContext?, containerList: List<PipelineBuildContainer>) {
        JooqUtils.retryWhenDeadLock {
            pipelineBuildContainerDao.batchUpdate(transactionContext ?: dslContext, containerList)
        }
    }

    fun updateContainerStatus(
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        startTime: LocalDateTime? = null,
        controlOption: PipelineBuildContainerControlOption? = null,
        endTime: LocalDateTime? = null,
        buildStatus: BuildStatus
    ) {
        logger.info("[$buildId]|updateContainerStatus|status=$buildStatus|containerSeqId=$containerId|s($stageId)")
        JooqUtils.retryWhenDeadLock {
            pipelineBuildContainerDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                stageId = stageId,
                containerId = containerId,
                buildStatus = buildStatus,
                startTime = startTime,
                controlOption = controlOption,
                endTime = endTime
            )
        }
    }

    fun updateMatrixGroupStatus(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        matrixGroupId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        modelContainer: Container?,
        controlOption: PipelineBuildContainerControlOption
    ) {
        logger.info("[$buildId]|updateMatrixGroupStatus|option=$controlOption|matrixGroupId=$matrixGroupId|s($stageId)")
        JooqUtils.retryWhenDeadLock {
            pipelineBuildContainerDao.updateControlOption(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                stageId = stageId,
                containerId = matrixGroupId,
                controlOption = controlOption
            )
        }
        containerBuildRecordService.updateMatrixGroupContainer(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stageId,
            matrixGroupId = matrixGroupId,
            executeCount = executeCount,
            buildStatus = buildStatus,
            matrixOption = controlOption.matrixControlOption!!,
            modelContainer = modelContainer
        )
    }

    fun cleanContainersInMatrixGroup(
        transactionContext: DSLContext?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        matrixGroupId: String
    ) {
        val groupContainers = pipelineBuildContainerDao.listBuildContainerIdsInMatrixGroup(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            matrixGroupId = matrixGroupId
        )
        logger.info("[$buildId]|cleanContainersInMatrixGroup|groupContainers=$groupContainers")
        var taskCount = 0
        groupContainers.forEach { containerId ->
            taskCount += pipelineTaskService.deleteTasksByContainerSeqId(
                dslContext = transactionContext ?: dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                containerId = containerId
            )
        }
        val containerCount = pipelineBuildContainerDao.deleteBuildContainerInMatrixGroup(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            matrixGroupId = matrixGroupId
        )
        logger.info("[$buildId]|cleanContainersInMatrixGroup|deleteT=$taskCount|deleteC=$containerCount")
    }

    fun prepareMatrixBuildContainer(
        projectId: String,
        pipelineId: String,
        buildId: String,
        stage: Stage,
        container: Container,
        context: MatrixBuildContext,
        buildTaskList: MutableList<PipelineBuildTask>,
        recordTaskList: MutableList<BuildRecordTask>,
        resourceVersion: Int?,
        matrixGroupId: String,
        jobControlOption: JobControlOption,
        postParentIdMap: Map<String, String>,
        mutexGroup: MutexGroup?
    ): PipelineBuildContainer {
        var startVMTaskSeq = -1 // 启动构建机位置，解决如果在执行人工审核插件时，无编译环境不需要提前无意义的启动
        var taskSeq = 0
        val parentElements = container.elements

        parentElements.forEach nextElement@{ atomElement ->
            taskSeq++ // 跳过的也要+1，Seq不需要连续性
            // 计算启动构建机的插件任务的序号
            if (startVMTaskSeq < 0) {
                startVMTaskSeq = calculateStartVMTaskSeq(taskSeq, container, atomElement)
                if (startVMTaskSeq > 0) {
                    taskSeq++ // 当前插件任务的执行序号往后移动一位，留给构建机启动插件任务
                }
            }
            context.taskCount++
            val buildTask = genBuildTaskToList(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = context.userId,
                stage = stage,
                container = container,
                taskSeq = taskSeq,
                atomElement = atomElement,
                status = BuildStatus.QUEUE,
                executeCount = context.executeCount,
                postParentIdMap = postParentIdMap
            )
            buildTaskList.add(buildTask)
            resourceVersion?.let {
                recordTaskList.add(
                    BuildRecordTask(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        stageId = stage.id!!,
                        containerId = container.id!!,
                        taskSeq = taskSeq,
                        taskId = atomElement.id!!,
                        classType = MatrixStatusElement.classType,
                        atomCode = atomElement.getAtomCode(),
                        executeCount = context.executeCount,
                        originClassType = atomElement.getClassType(),
                        resourceVersion = resourceVersion,
                        timestamps = mapOf(),
                        elementPostInfo = buildTask.additionalOptions?.elementPostInfo,
                        // 对矩阵产生的插件特殊表示类型
                        taskVar = mutableMapOf(
                            "@type" to MatrixStatusElement.classType,
                            MatrixStatusElement::originClassType.name to atomElement.getClassType(),
                            MatrixStatusElement::originAtomCode.name to atomElement.getAtomCode(),
                            MatrixStatusElement::originTaskAtom.name to atomElement.getTaskAtom()
                        )
                    )
                )
            }
        }
        // 填入: 构建机或无编译环境的环境处理，需要启动和结束构建机/环境的插件任务
        supplyVMTask(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            userId = context.userId,
            stage = stage,
            container = container,
            containerSeq = context.containerSeq,
            startVMTaskSeq = startVMTaskSeq,
            lastTimeBuildTasks = listOf(),
            updateExistsTask = mutableListOf(),
            buildTaskList = buildTaskList,
            executeCount = context.executeCount
        )

        return PipelineBuildContainer(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stageId = stage.id!!,
            containerId = container.id!!,
            containerHashId = container.containerHashId ?: "",
            containerType = container.getClassType(),
            seq = context.containerSeq,
            status = BuildStatus.QUEUE,
            executeCount = context.executeCount,
            controlOption = PipelineBuildContainerControlOption(
                jobControlOption = jobControlOption,
                inFinallyStage = stage.finally,
                mutexGroup = mutexGroup,
                containPostTaskFlag = container.containPostTaskFlag
            ),
            matrixGroupFlag = false,
            matrixGroupId = matrixGroupId
        )
    }

    fun prepareBuildContainerTasks(
        stage: Stage,
        container: Container,
        context: StartBuildContext,
        buildTaskList: MutableList<PipelineBuildTask>,
        buildContainers: MutableList<Pair<PipelineBuildContainer, Container>>,
        updateExistsTask: MutableList<PipelineBuildTask>,
        updateExistsContainer: MutableList<Pair<PipelineBuildContainer, Container>>,
        containerBuildRecords: MutableList<BuildRecordContainer>,
        taskBuildRecords: MutableList<BuildRecordTask>,
        lastTimeBuildContainers: Collection<PipelineBuildContainer>,
        lastTimeBuildTasks: Collection<PipelineBuildTask>
    ) {
        var startVMTaskSeq = -1 // 启动构建机位置，解决如果在执行人工审核插件时，无编译环境不需要提前无意义的启动
        var needStartVM = false // 是否需要启动构建
        var needUpdateContainer = false
        var taskSeq = 0
        val containerElements = container.elements
        val retryFlag = lastTimeBuildTasks.isEmpty()

        containerElements.forEach nextElement@{ atomElement ->
            modelCheckPlugin.checkElementTimeoutVar(container, atomElement, contextMap = context.variables)
            taskSeq++ // 跳过的也要+1，Seq不需要连续性
            // 计算启动构建机的插件任务的序号
            if (startVMTaskSeq < 0) {
                startVMTaskSeq = calculateStartVMTaskSeq(taskSeq, container, atomElement)
                if (startVMTaskSeq > 0) {
                    taskSeq++ // 当前插件任务的执行序号往后移动一位，留给构建机启动插件任务
                }
            }

            // #4245 直接将启动时跳过的插件置为不可用，减少存储变量
            atomElement.disableBySkipVar(variables = context.variables)

            val status = atomElement.initStatus(
                rerun = context.needRerunTask(stage = stage, container = container)
            )
            if (status.isFinish()) {
                logger.info("[${context.buildId}|${atomElement.id}] status=$status")
                atomElement.status = status.name
                if (retryFlag) taskBuildRecords.add(
                    BuildRecordTask(
                        projectId = context.projectId, pipelineId = context.pipelineId,
                        buildId = context.buildId, stageId = stage.id!!, containerId = container.containerId!!,
                        taskId = atomElement.id!!, classType = atomElement.getClassType(),
                        atomCode = atomElement.getTaskAtom(), executeCount = context.executeCount,
                        resourceVersion = context.resourceVersion, taskSeq = taskSeq, status = status.name,
                        taskVar = mutableMapOf(), timestamps = mapOf(),
                        elementPostInfo = atomElement.additionalOptions?.elementPostInfo?.takeIf { info ->
                            info.parentElementId != atomElement.id
                        }
                    )
                )
                return@nextElement
            }

            // 全新构建，其中构建矩阵不需要添加待执行插件
            if (retryFlag) {
                if (container.matrixGroupFlag != true) {
                    context.taskCount++
                    val buildTask = genBuildTaskToList(
                        projectId = context.projectId,
                        pipelineId = context.pipelineId,
                        buildId = context.buildId,
                        userId = context.userId,
                        stage = stage,
                        container = container,
                        taskSeq = taskSeq,
                        atomElement = atomElement,
                        status = status,
                        executeCount = context.executeCount,
                        postParentIdMap = emptyMap()
                    )
                    buildTaskList.add(buildTask)
                }
                needUpdateContainer = true
            } else {
                // 如果是失败的插件重试，并且当前插件不是要重试或跳过的插件，则检查其之前的状态，如果已经执行过，则跳过
                if (context.needSkipTaskWhenRetry(stage, container, atomElement.id)) {
                    val target = findLastTimeBuildTask(
                        lastTimeBuildTasks = lastTimeBuildTasks,
                        container = container,
                        retryStartTaskId = atomElement.id!!
                    )
                    // 插件任务在历史中找不到，则跳过当前插件
                    // 如果插件任务之前已经是完成状态，则跳过当前插件
                    try {
                        if (target == null || target.status.isFinish()) {
                            return@nextElement
                        }
                    } catch (ignored: Exception) { // 如果存在异常的ordinal
                        logger.error("[${context.buildId}]|BAD_B_STATUS|${target?.taskId}|${target?.status}|$ignored")
                        return@nextElement
                    }
                }

                // Rebuild/Stage-Retry/Fail-Task-Retry  重跑/Stage重试/失败的插件重试
                val skipWhenFailed = context.inSkipStage(stage, atomElement)
                val buildStatus = if (skipWhenFailed) BuildStatus.SKIP else null
                val recordStatus = if (skipWhenFailed) BuildStatus.FAILED.name else null
                val taskRecord = retryDetailModelStatus(
                    lastTimeBuildTasks = lastTimeBuildTasks,
                    container = container,
                    retryStartTaskId = atomElement.id!!,
                    executeCount = context.executeCount,
                    atomElement = atomElement, // #4245 将失败跳过的插件置为跳过
                    initialStatus = buildStatus
                )

                if (taskRecord != null) {
                    if (updateExistsTask.find { it.taskId == taskRecord.taskId } == null) {
                        updateExistsTask.add(taskRecord)
                    }
                    // 新插件重试需要判断其是否有post操作,如果有那么post操作也需要重试
                    if (atomElement is MarketBuildAtomElement || atomElement is MarketBuildLessAtomElement) {
                        val pair = findPostTask(lastTimeBuildTasks, atomElement, containerElements)
                        // 整体重试时，如果update列表已经包含post插件则不反复插入
                        if (pair != null && updateExistsTask.find { it.taskId == pair.first.taskId } == null) {
                            setRetryBuildTask(
                                target = pair.first,
                                executeCount = context.executeCount,
                                atomElement = pair.second
                            )
                            updateExistsTask.add(pair.first)
                        }
                    }
                    // #8955 针对被跳过或重试插件单独保留原状态
                    taskBuildRecords.add(
                        BuildRecordTask(
                            projectId = context.projectId, pipelineId = context.pipelineId,
                            buildId = context.buildId, stageId = taskRecord.stageId,
                            containerId = taskRecord.containerId, taskSeq = taskRecord.taskSeq,
                            taskId = taskRecord.taskId, classType = taskRecord.taskType,
                            atomCode = taskRecord.atomCode ?: taskRecord.taskAtom, timestamps = mapOf(),
                            executeCount = taskRecord.executeCount ?: 1, taskVar = mutableMapOf(),
                            status = recordStatus, resourceVersion = context.resourceVersion,
                            elementPostInfo = taskRecord.additionalOptions?.elementPostInfo?.takeIf { info ->
                                info.parentElementId != taskRecord.taskId
                            }
                        )
                    )
                    needUpdateContainer = true
                } else if (container.matrixGroupFlag == true && BuildStatus.parse(container.status).isFinish()) {
                    // 构建矩阵没有对应的重试插件，单独进行重试判断
                    needUpdateContainer = true
                }
            }

            // 确认是否要启动构建机/无编译环境
            if (!needStartVM && startVMTaskSeq > 0 && container.matrixGroupFlag != true) {
                needStartVM = true
            }
        }
        // 填入: 构建机或无编译环境的环境处理，需要启动和结束构建机/环境的插件任务
        if (needStartVM) {
            supplyVMTask(
                projectId = context.projectId,
                pipelineId = context.pipelineId,
                buildId = context.buildId,
                userId = context.userId,
                stage = stage,
                container = container,
                containerSeq = context.containerSeq,
                startVMTaskSeq = startVMTaskSeq,
                lastTimeBuildTasks = lastTimeBuildTasks,
                updateExistsTask = updateExistsTask,
                buildTaskList = buildTaskList,
                executeCount = context.executeCount
            )
        }
        if (needUpdateContainer) {
            container.resetBuildOption(context.executeCount)
            if (lastTimeBuildContainers.isNotEmpty()) {
                run findHistoryContainer@{
                    lastTimeBuildContainers.forEach { dbRecord ->
                        if (dbRecord.containerId == container.id) { // #958 在Element.initStatus 位置确认重试插件
                            dbRecord.run {
                                status = BuildStatus.QUEUE
                                startTime = null
                                endTime = null
                                executeCount = context.executeCount
                                updateExistsContainer.add(Pair(this, container))
                            }
                            return@findHistoryContainer
                        }
                    }
                }
            } else {
                ModelUtils.initContainerOldData(container)
                val controlOption = when (container) {
                    is NormalContainer -> PipelineBuildContainerControlOption(
                        jobControlOption = container.jobControlOption!!,
                        matrixControlOption = container.matrixControlOption,
                        inFinallyStage = stage.finally,
                        mutexGroup = container.mutexGroup?.also { s ->
                            s.linkTip = "${context.pipelineId}_Pipeline" +
                                "[${context.variables[PIPELINE_NAME]}]Job[${container.name}]"
                            s.runtimeMutexGroup = null
                        },
                        containPostTaskFlag = container.containPostTaskFlag
                    )

                    is VMBuildContainer -> PipelineBuildContainerControlOption(
                        jobControlOption = container.jobControlOption!!,
                        matrixControlOption = container.matrixControlOption,
                        inFinallyStage = stage.finally,
                        mutexGroup = container.mutexGroup?.also { s ->
                            s.linkTip = "${context.pipelineId}_Pipeline" +
                                "[${context.variables[PIPELINE_NAME]}]Job[${container.name}]"
                            s.runtimeMutexGroup = null
                        },
                        containPostTaskFlag = container.containPostTaskFlag
                    )

                    else -> PipelineBuildContainerControlOption(jobControlOption = JobControlOption())
                }
                buildContainers.add(
                    Pair(
                        PipelineBuildContainer(
                            projectId = context.projectId,
                            pipelineId = context.pipelineId,
                            buildId = context.buildId,
                            stageId = stage.id!!,
                            containerId = container.id!!,
                            containerHashId = container.containerHashId ?: "",
                            containerType = container.getClassType(),
                            seq = context.containerSeq,
                            status = BuildStatus.QUEUE,
                            controlOption = controlOption,
                            matrixGroupFlag = container.matrixGroupFlag,
                            matrixGroupId = null
                        ),
                        container
                    )
                )
            }
            context.needUpdateStage = true
        } else if (lastTimeBuildContainers.isEmpty()) {
            // 新的构建需要为跳过的container增加SKIP的状态记录
            containerBuildRecords.add(
                BuildRecordContainer(
                    projectId = context.projectId, pipelineId = context.pipelineId, buildId = context.buildId,
                    resourceVersion = context.resourceVersion, stageId = stage.id!!,
                    containerId = container.containerId!!, containerType = container.getClassType(),
                    executeCount = context.executeCount, matrixGroupFlag = container.matrixGroupFlag,
                    matrixGroupId = null, status = BuildStatus.SKIP.name, containerVar = mutableMapOf(),
                    startTime = null, endTime = null, timestamps = mapOf()
                )
            )
        }
    }

    fun findLastTimeBuildTask(
        lastTimeBuildTasks: Collection<PipelineBuildTask>,
        container: Container,
        retryStartTaskId: String?
    ): PipelineBuildTask? {
        var target: PipelineBuildTask? = null
        run findOutRetryTask@{
            lastTimeBuildTasks.forEach {
                if (it.containerId == container.id && retryStartTaskId == it.taskId) {
                    target = it
                    logger.info("[${it.buildId}|found|j(${container.id})|${container.name}|retryId=$retryStartTaskId")
                    return@findOutRetryTask
                }
            }
        }
        return target
    }

    private fun supplyVMTask(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        stage: Stage,
        container: Container,
        containerSeq: Int,
        startVMTaskSeq: Int,
        lastTimeBuildTasks: Collection<PipelineBuildTask>,
        updateExistsTask: MutableList<PipelineBuildTask>,
        buildTaskList: MutableList<PipelineBuildTask>,
        executeCount: Int
    ) {
        if (startVMTaskSeq <= 0) {
            return
        }

        if (lastTimeBuildTasks.isEmpty()) {
            buildTaskList.add(
                vmOperatorTaskGenerator.makeStartVMContainerTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stage.id!!,
                    container = container,
                    taskSeq = startVMTaskSeq,
                    userId = userId,
                    executeCount = executeCount
                )
            )
            buildTaskList.addAll(
                vmOperatorTaskGenerator.makeShutdownVMContainerTasks(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stage.id!!,
                    container = container,
                    containerSeq = containerSeq,
                    taskSeq = startVMTaskSeq,
                    userId = userId,
                    executeCount = executeCount
                )
            )
        } else {
            val startTaskVMId = VMUtils.genStartVMTaskId(container.id!!)
            var taskRecord = retryDetailModelStatus(
                lastTimeBuildTasks = lastTimeBuildTasks,
                container = container,
                executeCount = executeCount,
                retryStartTaskId = startTaskVMId
            )
            if (taskRecord != null) {
                updateExistsTask.add(taskRecord)
            } else {
                logger.info("[$buildId]|RETRY| not found $startTaskVMId(${container.name})")
            }

            val endPointTaskId = VMUtils.genEndPointTaskId(
                VMUtils.genVMTaskSeq(containerSeq, taskSeq = startVMTaskSeq - 1)
            )
            taskRecord = retryDetailModelStatus(
                lastTimeBuildTasks = lastTimeBuildTasks,
                container = container,
                executeCount = executeCount,
                retryStartTaskId = endPointTaskId
            )
            if (taskRecord != null) {
                updateExistsTask.add(taskRecord)
                val stopVmTaskId = VMUtils.genStopVMTaskId(VMUtils.genVMTaskSeq(containerSeq, taskSeq = startVMTaskSeq))
                taskRecord = retryDetailModelStatus(
                    lastTimeBuildTasks = lastTimeBuildTasks,
                    container = container,
                    executeCount = executeCount,
                    retryStartTaskId = stopVmTaskId
                )
                if (taskRecord != null) {
                    updateExistsTask.add(taskRecord)
                } else {
                    logger.warn("[$buildId]|RETRY| not found $stopVmTaskId(${container.name})")
                }
            } else {
                logger.info("[$buildId]|RETRY| not found $endPointTaskId(${container.name})")
            }
        }
    }

    /**
     * 刷新要重试的任务，如果任务是在当前容器，需要将当前容器的状态一并刷新
     * @param lastTimeBuildTasks 之前重试任务记录列表
     * @param container 当前任务所在构建容器
     * @param retryStartTaskId 要重试的任务i
     * @param atomElement 需要重置状态的任务原子Element，可以为空。
     * @param initialStatus 插件在重试时的初始状态，默认是QUEUE，也可以指定
     */
    private fun retryDetailModelStatus(
        lastTimeBuildTasks: Collection<PipelineBuildTask>,
        container: Container,
        retryStartTaskId: String,
        executeCount: Int,
        atomElement: Element? = null,
        initialStatus: BuildStatus? = null
    ): PipelineBuildTask? {
        val target: PipelineBuildTask? = findLastTimeBuildTask(
            lastTimeBuildTasks = lastTimeBuildTasks,
            container = container,
            retryStartTaskId = retryStartTaskId
        )

        if (target != null) {
            setRetryBuildTask(
                target = target,
                executeCount = executeCount,
                atomElement = atomElement,
                initialStatus = initialStatus
            )
        }
        return target
    }

    private fun setRetryBuildTask(
        target: PipelineBuildTask,
        executeCount: Int,
        atomElement: Element?,
        initialStatus: BuildStatus? = null
    ) {
        target.startTime = null
        target.endTime = null
        target.executeCount = executeCount
        target.status = initialStatus ?: BuildStatus.QUEUE // 如未指定状态，则默认进入排队状态
        if (target.status != BuildStatus.SKIP) { // 排队要准备执行，要清除掉上次失败状态
            target.errorMsg = null
            target.errorCode = null
            target.errorType = null
        } else { // 跳过的需要保留下跳过的信息
            target.errorMsg = I18nUtil.getCodeLanMessage(BK_MANUALLY_SKIPPED)
        }
        if (atomElement != null) { // 将原子状态重置
            if (initialStatus == null) { // 未指定状态的，将重新运行
                atomElement.status = null
            } else { // 指定了状态了，表示不会再运行，需要将重试与跳过关闭，因为已经跳过
                atomElement.additionalOptions =
                    atomElement.additionalOptions?.copy(manualSkip = false, manualRetry = false)
            }
            atomElement.executeCount = executeCount
            atomElement.elapsed = null
            atomElement.startEpoch = null
            atomElement.canRetry = false
            val originVersion = JsonUtil.toMutableMap(target.taskParams)["version"] as String
            if (originVersion.contains("*")) {
                atomElement.version = originVersion
            }
            target.taskParams = atomElement.genTaskParams() // 更新参数
        }
    }

    private fun findPostTask(
        lastTimeBuildTaskRecords: Collection<PipelineBuildTask>,
        atomElement: Element,
        containerElements: List<Element>
    ): Pair<PipelineBuildTask, Element>? {
        lastTimeBuildTaskRecords.forEach { buildTaskRecord ->
            val additionalOptions = buildTaskRecord.additionalOptions
            if (additionalOptions != null) {
                val elementPostInfo = additionalOptions.elementPostInfo
                if (elementPostInfo != null && elementPostInfo.parentElementId == atomElement.id) {
                    containerElements.forEach { element ->
                        if (element.id == buildTaskRecord.taskId) {
                            return buildTaskRecord to element
                        }
                    }
                }
            }
        }
        return null
    }

    private fun calculateStartVMTaskSeq(taskSeq: Int, container: Container, atomElement: Element): Int {
        // 在当前位置插入启动构建机
        if (container is VMBuildContainer) {
            return taskSeq
        }
        // 如果是无编译环境，检查是否存在研发商店上架类插件，需要启动无编译环境机器
        if (container is NormalContainer) {
            if (atomElement is MarketBuildAtomElement || atomElement is MarketBuildLessAtomElement) {
                return taskSeq
            }
        }
        return -1
    }

    private fun genBuildTaskToList(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        stage: Stage,
        container: Container,
        taskSeq: Int,
        atomElement: Element,
        status: BuildStatus,
        executeCount: Int,
        postParentIdMap: Map<String, String>
    ) = PipelineBuildTask(
        projectId = projectId,
        pipelineId = pipelineId,
        buildId = buildId,
        stageId = stage.id!!,
        containerId = container.id!!,
        containerHashId = container.containerHashId ?: "",
        containerType = container.getClassType(),
        taskSeq = taskSeq,
        taskId = atomElement.id!!,
        taskName = atomElement.name.coerceAtMaxLength(ELEMENT_NAME_MAX_LENGTH),
        taskType = atomElement.getClassType(),
        taskAtom = atomElement.getTaskAtom(),
        status = status,
        taskParams = atomElement.genTaskParams(),
        // 由于遍历时对内部属性有修改，需要复制一个新对象赋值
        additionalOptions = postParentIdMap[atomElement.id]?.let { self ->
            atomElement.additionalOptions?.copy(
                elementPostInfo = atomElement.additionalOptions?.elementPostInfo?.copy(
                    parentElementId = self
                )
            )
        } ?: atomElement.additionalOptions,
        executeCount = executeCount,
        starter = userId,
        approver = null,
        subProjectId = null,
        subBuildId = null,
        atomCode = atomElement.getAtomCode(),
        stepId = atomElement.stepId
    )

    fun setUpTriggerContainer(
        stage: Stage,
        container: TriggerContainer,
        context: StartBuildContext,
        stageBuildRecords: MutableList<BuildRecordStage>,
        containerBuildRecords: MutableList<BuildRecordContainer>,
        taskBuildRecords: MutableList<BuildRecordTask>
    ) {
        // #4518 Model中的container.containerId转移至container.containerHashId，进行新字段值补充
        container.containerHashId = container.containerHashId ?: container.containerId
        container.containerId = container.id

        val buildNoObj = container.buildNo
        if (buildNoObj != null && context.actionType == ActionType.START) {
            context.buildNoType = buildNoObj.buildNoType
            var needUpdateBuildNoRecord = false
            var needAddCurrentBuildNo = false
            if (context.buildNoType == BuildNoType.CONSISTENT) {
                // 只有用户勾选中"锁定构建号"这种类型才允许指定构建号
                needUpdateBuildNoRecord = context.currentBuildNo != null
            } else if (context.buildNoType == BuildNoType.EVERY_BUILD_INCREMENT) {
                needAddCurrentBuildNo = true
                needUpdateBuildNoRecord = true
            }

            if (needAddCurrentBuildNo || needUpdateBuildNoRecord || context.currentBuildNo == null) {
                PipelineBuildNoLock(redisOperation = redisOperation, pipelineId = context.pipelineId).use { lock ->
                    context.watcher.start("${stage.id}.currentBuildNo_lock")
                    lock.lock()
                    context.watcher.stop()
                    if (context.currentBuildNo == null) { // 兼容buildNo为空的情况

                        context.currentBuildNo = pipelineBuildSummaryDao.getBuildNo(
                            dslContext = dslContext, projectId = context.projectId, pipelineId = context.pipelineId
                        )
                            ?: let {
                                if (needAddCurrentBuildNo) {
                                    0
                                } else {
                                    buildNoObj.buildNo
                                }
                            }
                    }

                    if (needAddCurrentBuildNo) { // buildNo根据数据库的记录值每次新增1
                        context.currentBuildNo = context.currentBuildNo!! + 1
                    }

                    if (needUpdateBuildNoRecord) {
                        pipelineBuildSummaryDao.updateBuildNo(
                            dslContext = dslContext,
                            projectId = context.projectId,
                            pipelineId = context.pipelineId,
                            buildNo = context.currentBuildNo!!
                        )
                    }
                }

                if (context.buildNoType != BuildNoType.SUCCESS_BUILD_INCREMENT) { // 成功才+1的构建只在真正启动时赋值,配合排队
                    val buildParameters = BuildParameters(BUILD_NO, value = context.currentBuildNo!!, readOnly = true)
                    context.pipelineParamMap[BUILD_NO] = buildParameters
                    context.buildParameters.add(buildParameters)
                }
            }
        }

        container.name = ContainerUtils.getQueuingWaitName(container.name, context.startBuildStatus)
        container.status = BuildStatus.RUNNING.name
        container.executeCount = context.executeCount

        // TODO #7983 后续全换到record时保持同步刷新
//        stageBuildRecords.add(
//            BuildRecordStage(
//                buildId = context.buildId, projectId = context.projectId, pipelineId = context.pipelineId,
//                resourceVersion = resourceVersion, stageId = stage.id!!, status = BuildStatus.RUNNING.name,
//                executeCount = context.executeCount, stageSeq = 0, stageVar = mutableMapOf(), timestamps = mapOf()
//            )
//        )
//        containerBuildRecords.add(
//            BuildRecordContainer(
//                buildId = context.buildId, projectId = context.projectId, pipelineId = context.pipelineId,
//                resourceVersion = resourceVersion, stageId = stage.id!!, containerId = container.containerId!!,
//                executeCount = context.executeCount, matrixGroupFlag = null, matrixGroupId = null,
//                containerType = container.getClassType(), status = BuildStatus.RUNNING.name, timestamps = mapOf(),
//                containerVar = mutableMapOf(
//                    // 名字刷新成非队列中
//                    Container::name.name to container.name,
//                    Container::startEpoch.name to System.currentTimeMillis()
//                )
//            )
//        )
        container.elements.forEachIndexed { index, atomElement ->
            if (context.firstTaskId.isBlank() && atomElement.isElementEnable()) {
                context.firstTaskId = atomElement.findFirstTaskIdByStartType(context.startType)
            }

            if (context.firstTaskId.isNotBlank() && context.firstTaskId == atomElement.id) {
                atomElement.status = BuildStatus.SUCCEED.name
                atomElement.executeCount = context.executeCount
//                taskBuildRecords.add(
//                    BuildRecordTask(
//                        buildId = context.buildId, projectId = context.projectId,
//                        pipelineId = context.pipelineId, resourceVersion = resourceVersion,
//                        stageId = stage.id!!, containerId = container.containerId!!,
//                        taskId = atomElement.id!!, taskSeq = index + 1,
//                        executeCount = context.executeCount, taskVar = mutableMapOf(),
//                        classType = atomElement.getClassType(), atomCode = atomElement.getAtomCode(),
//                        status = BuildStatus.SUCCEED.name, timestamps = mapOf(), originClassType = null
//                    )
//                )
                buildLogPrinter.addLine(
                    buildId = context.buildId,
                    message = "${I18nUtil.getCodeLanMessage(BK_TRIGGER_USER)}: ${context.triggerUser}," +
                            " ${I18nUtil.getCodeLanMessage(BK_START_USER)}: ${context.userId}",
                    tag = context.firstTaskId,
                    jobId = container.id,
                    executeCount = context.executeCount
                )
                return
            }
        }
    }
}
