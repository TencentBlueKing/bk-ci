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

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.MutexGroup
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.option.JobControlOption
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.model.process.tables.records.TPipelineBuildContainerRecord
import com.tencent.devops.model.process.tables.records.TPipelineBuildTaskRecord
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.context.MatrixBuildContext
import com.tencent.devops.process.engine.context.StartBuildContext
import com.tencent.devops.process.engine.control.VmOperateTaskGenerator
import com.tencent.devops.process.engine.dao.PipelineBuildContainerDao
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildContainerControlOption
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.detail.ContainerBuildDetailService
import com.tencent.devops.process.utils.PIPELINE_NAME
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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
    private val dslContext: DSLContext,
    private val pipelineTaskService: PipelineTaskService,
    private val vmOperatorTaskGenerator: VmOperateTaskGenerator,
    private val containerBuildDetailService: ContainerBuildDetailService,
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
        val result = pipelineBuildContainerDao.getByContainerId(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            containerId = containerId
        )
        if (result != null) {
            return pipelineBuildContainerDao.convert(result)
        }
        return null
    }

    fun listContainers(
        projectId: String,
        buildId: String,
        stageId: String? = null,
        containsMatrix: Boolean? = true,
        statusSet: Set<BuildStatus>? = null
    ): List<PipelineBuildContainer> {
        val list = pipelineBuildContainerDao.listByBuildId(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            containsMatrix = containsMatrix,
            statusSet = statusSet
        )
        val result = mutableListOf<PipelineBuildContainer>()
        if (list.isNotEmpty()) {
            list.forEach {
                result.add(pipelineBuildContainerDao.convert(it)!!)
            }
        }
        return result
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
        val list = pipelineBuildContainerDao.listByMatrixGroupId(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            matrixGroupId = matrixGroupId
        )
        val result = mutableListOf<PipelineBuildContainer>()
        if (list.isNotEmpty()) {
            list.forEach {
                result.add(pipelineBuildContainerDao.convert(it)!!)
            }
        }
        return result
    }

    fun listByBuildId(
        projectId: String,
        buildId: String,
        stageId: String? = null
    ): Collection<TPipelineBuildContainerRecord> {
        return pipelineBuildContainerDao.listByBuildId(dslContext, projectId, buildId, stageId)
    }

    fun batchSave(transactionContext: DSLContext?, containerList: Collection<PipelineBuildContainer>) {
        return pipelineBuildContainerDao.batchSave(transactionContext ?: dslContext, containerList)
    }

    fun batchUpdate(transactionContext: DSLContext?, containerList: List<TPipelineBuildContainerRecord>) {
        return pipelineBuildContainerDao.batchUpdate(transactionContext ?: dslContext, containerList)
    }

    fun updateContainerStatus(
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        buildStatus: BuildStatus
    ) {
        logger.info("[$buildId]|updateContainerStatus|status=$buildStatus|containerSeqId=$containerId|s($stageId)")
        pipelineBuildContainerDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            containerId = containerId,
            buildStatus = buildStatus,
            startTime = startTime,
            endTime = endTime
        )
    }

    fun updateMatrixGroupStatus(
        projectId: String,
        buildId: String,
        stageId: String,
        matrixGroupId: String,
        buildStatus: BuildStatus,
        modelContainer: Container?,
        controlOption: PipelineBuildContainerControlOption
    ) {
        logger.info("[$buildId]|updateMatrixGroupStatus|option=$controlOption|matrixGroupId=$matrixGroupId|s($stageId)")
        pipelineBuildContainerDao.updateControlOption(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            containerId = matrixGroupId,
            controlOption = controlOption
        )
        containerBuildDetailService.updateMatrixGroupContainer(
            projectId = projectId,
            buildId = buildId,
            stageId = stageId,
            matrixGroupId = matrixGroupId,
            buildStatus = buildStatus,
            matrixOption = controlOption.matrixControlOption!!,
            modelContainer = modelContainer
        )
    }

    fun deletePipelineBuildContainers(transactionContext: DSLContext?, projectId: String, pipelineId: String): Int {
        return pipelineBuildContainerDao.deletePipelineBuildContainers(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun cleanContainersInMatrixGroup(
        transactionContext: DSLContext?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        matrixGroupId: String
    ) {
        val groupContainers = pipelineBuildContainerDao.listBuildContainerInMatrixGroup(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            matrixGroupId = matrixGroupId
        )
        logger.info("[$buildId]|cleanContainersInMatrixGroup|groupContainers=$groupContainers")
        var taskCount = 0
        groupContainers.forEach {
            taskCount += pipelineTaskService.deleteTasksByContainerSeqId(
                dslContext = transactionContext ?: dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                containerId = it.containerId
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
            val status = atomElement.initStatus()
            context.taskCount++
            addBuildTaskToList(
                buildTaskList = buildTaskList,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = context.userId,
                stage = stage,
                container = container,
                taskSeq = taskSeq,
                atomElement = atomElement,
                status = status,
                executeCount = context.executeCount,
                postParentIdMap = postParentIdMap
            )
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
            lastTimeBuildTaskRecords = listOf(),
            updateExistsRecord = mutableListOf(),
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
        projectId: String,
        pipelineId: String,
        buildId: String,
        stage: Stage,
        container: Container,
        startParamMap: Map<String, Any>,
        context: StartBuildContext,
        buildTaskList: MutableList<PipelineBuildTask>,
        buildContainers: MutableList<PipelineBuildContainer>,
        updateTaskExistsRecord: MutableList<TPipelineBuildTaskRecord>,
        updateContainerExistsRecord: MutableList<TPipelineBuildContainerRecord>,
        lastTimeBuildContainerRecords: Collection<TPipelineBuildContainerRecord>,
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>
    ) {
        var startVMTaskSeq = -1 // 启动构建机位置，解决如果在执行人工审核插件时，无编译环境不需要提前无意义的启动
        var needStartVM = false // 是否需要启动构建
        var needUpdateContainer = false
        var taskSeq = 0
        val containerElements = container.elements

        containerElements.forEach nextElement@{ atomElement ->
            taskSeq++ // 跳过的也要+1，Seq不需要连续性
            // 计算启动构建机的插件任务的序号
            if (startVMTaskSeq < 0) {
                startVMTaskSeq = calculateStartVMTaskSeq(taskSeq, container, atomElement)
                if (startVMTaskSeq > 0) {
                    taskSeq++ // 当前插件任务的执行序号往后移动一位，留给构建机启动插件任务
                }
            }

            // #4245 直接将启动时跳过的插件置为不可用，减少存储变量
            atomElement.disableBySkipVar(variables = startParamMap)

            val status = atomElement.initStatus(rerun = context.needRerun(stage))
            if (status.isFinish()) {
                logger.info("[$buildId|${atomElement.id}] status=$status")
                atomElement.status = status.name
                return@nextElement
            }

            // 全新构建，其中构建矩阵不需要添加待执行插件
            if (lastTimeBuildTaskRecords.isEmpty()) {
                if (container.matrixGroupFlag != true) {
                    context.taskCount++
                    addBuildTaskToList(
                        buildTaskList = buildTaskList,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        userId = context.userId,
                        stage = stage,
                        container = container,
                        taskSeq = taskSeq,
                        atomElement = atomElement,
                        status = status,
                        executeCount = context.executeCount,
                        postParentIdMap = emptyMap()
                    )
                }
                needUpdateContainer = true
            } else {
                // 如果是失败的插件重试，并且当前插件不是要重试或跳过的插件，则检查其之前的状态，如果已经执行过，则跳过
                if (context.needSkipTaskWhenRetry(stage, atomElement.id)) {
                    val target = findTaskRecord(
                        lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                        container = container,
                        retryStartTaskId = atomElement.id!!
                    )
                    // 插件任务在历史中找不到，则跳过当前插件
                    // 如果插件任务之前已经是完成状态，则跳过当前插件
                    try {
                        if (target == null || BuildStatus.values()[target.status].isFinish()) {
                            return@nextElement
                        }
                    } catch (ignored: Exception) { // 如果存在异常的ordinal
                        logger.error("[$buildId]|BAD_BUILD_STATUS|${target?.taskId}|${target?.status}|$ignored")
                        return@nextElement
                    }
                }

                // Rebuild/Stage-Retry/Fail-Task-Retry  重跑/Stage重试/失败的插件重试
                val taskRecord = retryDetailModelStatus(
                    lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                    stage = stage,
                    container = container,
                    retryStartTaskId = atomElement.id!!,
                    executeCount = context.executeCount,
                    atomElement = atomElement, // #4245 将失败跳过的插件置为跳过
                    initialStatus = if (context.inSkipStage(stage, atomElement)) BuildStatus.SKIP else null
                )

                if (taskRecord != null) {
                    updateTaskExistsRecord.add(taskRecord)
                    // 新插件重试需要判断其是否有post操作,如果有那么post操作也需要重试
                    if (atomElement is MarketBuildAtomElement || atomElement is MarketBuildLessAtomElement) {
                        val pair = findPostTask(lastTimeBuildTaskRecords, atomElement, containerElements)
                        if (pair != null) {
                            setRetryBuildTask(
                                target = pair.first,
                                executeCount = context.executeCount,
                                stage = stage,
                                container = container,
                                atomElement = pair.second
                            )
                            updateTaskExistsRecord.add(pair.first)
                        }
                    }
                    needUpdateContainer = true
                } else if (container.matrixGroupFlag == true && BuildStatus.parse(container.status).isFinish()) {
                    // 构建矩阵没有对应的重试插件，单独进行重试判断
                    setRetryBuildContainer(container, context.executeCount)
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
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                userId = context.userId,
                stage = stage,
                container = container,
                containerSeq = context.containerSeq,
                startVMTaskSeq = startVMTaskSeq,
                lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                updateExistsRecord = updateTaskExistsRecord,
                buildTaskList = buildTaskList,
                executeCount = context.executeCount
            )
        }
        if (needUpdateContainer) {
            if (lastTimeBuildContainerRecords.isNotEmpty()) {
                run findHistoryContainer@{
                    lastTimeBuildContainerRecords.forEach {
                        if (it.containerId == container.id) { // #958 在Element.initStatus 位置确认重试插件
                            it.status = BuildStatus.QUEUE.ordinal
                            it.startTime = null
                            it.endTime = null
                            it.executeCount = context.executeCount
                            updateContainerExistsRecord.add(it)
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
                            s.linkTip = "${pipelineId}_Pipeline[${startParamMap[PIPELINE_NAME]}]Job[${container.name}]"
                        },
                        containPostTaskFlag = container.containPostTaskFlag
                    )
                    is VMBuildContainer -> PipelineBuildContainerControlOption(
                        jobControlOption = container.jobControlOption!!,
                        matrixControlOption = container.matrixControlOption,
                        inFinallyStage = stage.finally,
                        mutexGroup = container.mutexGroup?.also { s ->
                            s.linkTip = "${pipelineId}_Pipeline[${startParamMap[PIPELINE_NAME]}]Job[${container.name}]"
                        },
                        containPostTaskFlag = container.containPostTaskFlag
                    )
                    else -> null
                }
                buildContainers.add(
                    PipelineBuildContainer(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        stageId = stage.id!!,
                        containerId = container.id!!,
                        containerHashId = container.containerHashId ?: "",
                        containerType = container.getClassType(),
                        seq = context.containerSeq,
                        status = BuildStatus.QUEUE,
                        controlOption = controlOption,
                        matrixGroupFlag = container.matrixGroupFlag,
                        matrixGroupId = null
                    )
                )
            }
            context.needUpdateStage = true
        }
    }

    fun findTaskRecord(
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        container: Container,
        retryStartTaskId: String?
    ): TPipelineBuildTaskRecord? {
        var target: TPipelineBuildTaskRecord? = null
        run findOutRetryTask@{
            lastTimeBuildTaskRecords.forEach {
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
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        updateExistsRecord: MutableList<TPipelineBuildTaskRecord>,
        buildTaskList: MutableList<PipelineBuildTask>,
        executeCount: Int
    ) {
        if (startVMTaskSeq <= 0) {
            return
        }

        if (lastTimeBuildTaskRecords.isEmpty()) {
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
                lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                stage = stage,
                container = container,
                executeCount = executeCount,
                retryStartTaskId = startTaskVMId
            )
            if (taskRecord != null) {
                updateExistsRecord.add(taskRecord)
            } else {
                logger.info("[$buildId]|RETRY| not found $startTaskVMId(${container.name})")
            }

            val endPointTaskId = VMUtils.genEndPointTaskId(
                VMUtils.genVMTaskSeq(containerSeq, taskSeq = startVMTaskSeq - 1)
            )
            taskRecord = retryDetailModelStatus(
                lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                stage = stage,
                container = container,
                executeCount = executeCount,
                retryStartTaskId = endPointTaskId
            )
            if (taskRecord != null) {
                updateExistsRecord.add(taskRecord)
                val stopVmTaskId = VMUtils.genStopVMTaskId(VMUtils.genVMTaskSeq(containerSeq, taskSeq = startVMTaskSeq))
                taskRecord = retryDetailModelStatus(
                    lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
                    stage = stage,
                    container = container,
                    executeCount = executeCount,
                    retryStartTaskId = stopVmTaskId
                )
                if (taskRecord != null) {
                    updateExistsRecord.add(taskRecord)
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
     * @param lastTimeBuildTaskRecords 之前重试任务记录列表
     * @param container 当前任务所在构建容器
     * @param retryStartTaskId 要重试的任务i
     * @param atomElement 需要重置状态的任务原子Element，可以为空。
     * @param initialStatus 插件在重试时的初始状态，默认是QUEUE，也可以指定
     */
    private fun retryDetailModelStatus(
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        stage: Stage,
        container: Container,
        retryStartTaskId: String,
        executeCount: Int,
        atomElement: Element? = null,
        initialStatus: BuildStatus? = null
    ): TPipelineBuildTaskRecord? {
        val target: TPipelineBuildTaskRecord? = findTaskRecord(
            lastTimeBuildTaskRecords = lastTimeBuildTaskRecords,
            container = container,
            retryStartTaskId = retryStartTaskId
        )

        if (target != null) {
            setRetryBuildTask(
                target = target,
                executeCount = executeCount,
                stage = stage,
                container = container,
                atomElement = atomElement,
                initialStatus = initialStatus
            )
        }
        return target
    }

    private fun setRetryBuildTask(
        target: TPipelineBuildTaskRecord,
        executeCount: Int,
        stage: Stage,
        container: Container,
        atomElement: Element?,
        initialStatus: BuildStatus? = null
    ) {
        target.startTime = null
        target.endTime = null
        target.executeCount = executeCount
        target.status = initialStatus?.ordinal ?: BuildStatus.QUEUE.ordinal // 如未指定状态，则默认进入排队状态
        if (target.status != BuildStatus.SKIP.ordinal) { // 排队要准备执行，要清除掉上次失败状态
            target.errorMsg = null
            target.errorCode = null
            target.errorType = null
        } else { // 跳过的需要保留下跳过的信息
            target.errorMsg = "被手动跳过 Manually skipped"
        }
        stage.status = null
        stage.startEpoch = null
        stage.elapsed = null
        setRetryBuildContainer(container, target.executeCount)
        if (atomElement != null) { // 将原子状态重置
            if (initialStatus == null) { // 未指定状态的，将重新运行
                atomElement.status = null
            } else { // 指定了状态了，表示不会再运行，需要将重试与跳过关闭，因为已经跳过
                atomElement.additionalOptions =
                    atomElement.additionalOptions?.copy(manualSkip = false, manualRetry = false)
            }
            atomElement.executeCount = target.executeCount
            atomElement.elapsed = null
            atomElement.startEpoch = null
            atomElement.canRetry = false
            target.taskParams = JsonUtil.toJson(atomElement.genTaskParams(), formatted = false) // 更新参数
        }
    }

    private fun setRetryBuildContainer(container: Container, executeCount: Int) {
        container.status = null // 重置状态为空
        container.startEpoch = null
        container.elementElapsed = null
        container.systemElapsed = null
        container.startVMStatus = null
        container.executeCount = executeCount
    }

    private fun findPostTask(
        lastTimeBuildTaskRecords: Collection<TPipelineBuildTaskRecord>,
        atomElement: Element,
        containerElements: List<Element>
    ): Pair<TPipelineBuildTaskRecord, Element>? {
        lastTimeBuildTaskRecords.forEach { buildTaskRecord ->
            val additionalOptionsStr = buildTaskRecord.additionalOptions
            if (!additionalOptionsStr.isNullOrBlank() && additionalOptionsStr != "null") {
                val additionalOptions = JsonUtil.to(additionalOptionsStr, ElementAdditionalOptions::class.java)
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

    private fun addBuildTaskToList(
        buildTaskList: MutableList<PipelineBuildTask>,
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
    ) {
        buildTaskList.add(
            PipelineBuildTask(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stage.id!!,
                containerId = container.id!!,
                containerHashId = container.containerHashId ?: "",
                containerType = container.getClassType(),
                taskSeq = taskSeq,
                taskId = atomElement.id!!,
                taskName = CommonUtils.interceptStringInLength(
                    string = atomElement.name,
                    length = ELEMENT_NAME_MAX_LENGTH
                ) ?: atomElement.getAtomCode(),
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
        )
    }
}
