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

package com.tencent.devops.process.engine.service.detail

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.pojo.PipelineTaskStatusInfo
import com.tencent.devops.process.pojo.task.TaskBuildEndParam
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.pojo.atom.AtomClassifyInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList", "MagicNumber", "ReturnCount", "TooManyFunctions", "ComplexCondition")
@Service
class TaskBuildDetailService(
    private val client: Client,
    private val buildVariableService: BuildVariableService,
    dslContext: DSLContext,
    pipelineBuildDao: PipelineBuildDao,
    buildDetailDao: BuildDetailDao,
    pipelineEventDispatcher: PipelineEventDispatcher,
    stageTagService: StageTagService,
    redisOperation: RedisOperation
) : BaseBuildDetailService(
    dslContext,
    pipelineBuildDao,
    buildDetailDao,
    stageTagService,
    pipelineEventDispatcher,
    redisOperation
) {

    fun taskPause(
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        taskId: String,
        buildStatus: BuildStatus
    ) {
        update(
            projectId = projectId, buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false

                override fun onFindElement(index: Int, e: Element, c: Container): Traverse {
                    if (c.id.equals(containerId)) {
                        if (e.id.equals(taskId)) {
                            logger.info(
                                "ENGINE|$buildId|pauseTask|$stageId|j($containerId)|" +
                                    "t($taskId)|${buildStatus.name}"
                            )
                            update = true
                            e.status = buildStatus.name
                            return Traverse.BREAK
                        }
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "taskPause#$taskId"
        )
    }

    fun updateTaskStatus(
        projectId: String,
        buildId: String,
        taskId: String,
        taskStatus: BuildStatus,
        buildStatus: BuildStatus,
        operation: String
    ) {
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false
                override fun onFindElement(index: Int, e: Element, c: Container): Traverse {
                    if (e.id == taskId) {
                        update = true
                        e.status = taskStatus.name
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = buildStatus,
            operation = "$operation#$taskId"
        )
    }

    fun taskStart(projectId: String, buildId: String, taskId: String) {
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false
                val delimiters = ","
                override fun onFindElement(index: Int, e: Element, c: Container): Traverse {
                    if (e.id == taskId) {
                        if (e is ManualReviewUserTaskElement) {
                            e.status = BuildStatus.REVIEWING.name
                            // Replace the review user with environment
                            val list = mutableListOf<String>()
                            e.reviewUsers.forEach { reviewUser ->
                                list.addAll(
                                    buildVariableService.replaceTemplate(projectId, buildId, reviewUser)
                                        .split(delimiters)
                                )
                            }
                            e.reviewUsers.clear()
                            e.reviewUsers.addAll(list)
                        } else if (e is MatrixStatusElement &&
                            e.originClassType == ManualReviewUserTaskElement.classType
                        ) {
                            e.status = BuildStatus.REVIEWING.name
                            // Replace the review user with environment
                            val list = mutableListOf<String>()
                            e.reviewUsers?.forEach { reviewUser ->
                                list.addAll(
                                    buildVariableService.replaceTemplate(projectId, buildId, reviewUser)
                                        .split(delimiters)
                                )
                            }
                            e.reviewUsers = list
                        } else if (e is QualityGateInElement || e is QualityGateOutElement ||
                            e.getTaskAtom() == QualityGateInElement.classType ||
                            e.getTaskAtom() == QualityGateOutElement.classType
                        ) {
                            e.status = BuildStatus.REVIEWING.name
                            c.status = BuildStatus.REVIEWING.name
                        } else {
                            c.status = BuildStatus.RUNNING.name
                            e.status = BuildStatus.RUNNING.name
                        }

                        if (e.startEpoch == null) { // 自动重试，startEpoch 不会为null，所以不需要查redis来确认
                            val currentTimeMillis = System.currentTimeMillis()
                            e.startEpoch = currentTimeMillis
                            if (c.startEpoch == null) {
                                c.startEpoch = e.startEpoch
                            }
                        }
                        e.elapsed = null
                        e.errorType = null
                        e.errorCode = null
                        e.errorMsg = null
                        update = true
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "taskStart#$taskId"
        )
    }

    fun taskCancel(projectId: String, buildId: String, containerId: String, taskId: String, cancelUser: String?) {
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false

                override fun onFindElement(index: Int, e: Element, c: Container): Traverse {
                    if (c.id.equals(containerId)) {
                        if (e.id.equals(taskId)) {
                            c.status = BuildStatus.CANCELED.name
                            e.status = BuildStatus.CANCELED.name
                            update = true
                            return Traverse.BREAK
                        }
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            cancelUser = cancelUser,
            operation = "taskCancel#$taskId"
        )
    }

    fun taskEnd(taskBuildEndParam: TaskBuildEndParam): List<PipelineTaskStatusInfo> {
        val projectId = taskBuildEndParam.projectId
        val buildId = taskBuildEndParam.buildId
        val taskId = taskBuildEndParam.taskId
        val buildStatus = taskBuildEndParam.buildStatus
        val atomVersion = taskBuildEndParam.atomVersion
        val errorType = taskBuildEndParam.errorType
        val updateTaskStatusInfos = mutableListOf<PipelineTaskStatusInfo>()
        update(
            projectId, buildId,
            object : ModelInterface {

                var update = false
                override fun onFindElement(index: Int, e: Element, c: Container): Traverse {
                    if (e.id == taskId) {
                        // 判断取消的task任务对应的container是否包含post任务
                        val cancelTaskPostFlag = buildStatus == BuildStatus.CANCELED && c.containPostTaskFlag == true
                        e.status = buildStatus.name
                        if (e.startEpoch == null) {
                            e.elapsed = 0
                        } else {
                            e.elapsed = System.currentTimeMillis() - e.startEpoch!!
                        }
                        if (errorType != null) {
                            e.errorType = errorType.name
                            e.errorCode = taskBuildEndParam.errorCode
                            e.errorMsg = taskBuildEndParam.errorMsg
                        }
                        if (atomVersion != null) {
                            when (e) {
                                is MarketBuildAtomElement -> {
                                    e.version = atomVersion
                                }
                                is MarketBuildLessAtomElement -> {
                                    e.version = atomVersion
                                }
                                else -> {
                                    e.version = INIT_VERSION
                                }
                            }
                        }
                        val atomClassify = getAtomClassify(e.getAtomCode())
                        e.atomName = atomClassify?.atomName
                        e.classifyCode = atomClassify?.classifyCode
                        e.classifyName = atomClassify?.classifyName
                        var elementElapsed = 0L
                        run lit@{
                            val elements = c.elements
                            elements.forEachIndexed { tmpIndex, it ->
                                val elapsed = it.elapsed
                                if (elapsed != null) {
                                    elementElapsed += elapsed
                                }
                                if (handleUpdateTaskStatusInfos(
                                        containerId = c.containerId ?: "",
                                        buildStatus = buildStatus,
                                        cancelTaskPostFlag = cancelTaskPostFlag,
                                        tmpElement = it,
                                        tmpElementIndex = tmpIndex,
                                        elements = elements,
                                        endElementIndex = index,
                                        updateTaskStatusInfos = updateTaskStatusInfos,
                                        endElement = e
                                    )
                                ) return@lit
                            }
                        }
                        c.elementElapsed = elementElapsed
                        update = true
                        return Traverse.BREAK
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "taskEnd#$taskId"
        )
        return updateTaskStatusInfos
    }

    private val atomClassifyCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterAccess(6, TimeUnit.HOURS)
        .build<String, AtomClassifyInfo>()

    fun getAtomClassify(atomCode: String): AtomClassifyInfo? {
        var atomClassify = atomClassifyCache.getIfPresent(atomCode)
        if (atomClassify == null) {
            atomClassify = client.get(ServiceAtomResource::class).getAtomClassifyInfo(atomCode).data
        }
        atomClassify?.let { atomClassifyCache.put(atomCode, it) }
        return atomClassify
    }

    private fun handleUpdateTaskStatusInfos(
        containerId: String,
        buildStatus: BuildStatus,
        cancelTaskPostFlag: Boolean,
        endElement: Element,
        endElementIndex: Int,
        tmpElement: Element,
        tmpElementIndex: Int,
        elements: List<Element>,
        updateTaskStatusInfos: MutableList<PipelineTaskStatusInfo>?
    ): Boolean {
        when {
            cancelTaskPostFlag -> {
                return handleCancelTaskPost(
                    containerId = containerId,
                    endElement = endElement,
                    endElementIndex = endElementIndex,
                    tmpElement = tmpElement,
                    tmpElementIndex = tmpElementIndex,
                    elements = elements,
                    updateTaskStatusInfos = updateTaskStatusInfos
                )
            }
            buildStatus.isCancel() -> {
                return handleCancelTaskNormal(
                    tmpElement = tmpElement,
                    endElement = endElement,
                    buildStatus = buildStatus,
                    endElementIndex = endElementIndex,
                    elements = elements,
                    containerId = containerId,
                    updateTaskStatusInfos = updateTaskStatusInfos
                )
            }
            buildStatus.isSkip() -> {
                updateTaskStatusInfos?.add(
                    PipelineTaskStatusInfo(
                        taskId = endElement.id!!,
                        containerHashId = containerId,
                        buildStatus = buildStatus,
                        executeCount = endElement.executeCount,
                        message = endElement.errorMsg
                    )
                )
                return false
            }
        }
        return false
    }

    private fun handleCancelTaskNormal(
        tmpElement: Element,
        endElement: Element,
        buildStatus: BuildStatus,
        endElementIndex: Int,
        elements: List<Element>,
        containerId: String,
        updateTaskStatusInfos: MutableList<PipelineTaskStatusInfo>?
    ): Boolean {
        if (tmpElement == endElement) {
            if (buildStatus == BuildStatus.CANCELED &&
                endElement.additionalOptions?.runCondition != RunCondition.PRE_TASK_FAILED_EVEN_CANCEL
            ) {
                val startIndex = endElementIndex + 1
                val endIndex = elements.size - 1
                if (endIndex >= startIndex) {
                    addCancelTaskStatusInfo(
                        containerId = containerId,
                        startIndex = startIndex,
                        endIndex = endIndex,
                        elements = elements,
                        updateTaskStatusInfos = updateTaskStatusInfos
                    )
                }
            }
            return true
        }
        return false
    }

    private fun handleCancelTaskPost(
        containerId: String,
        endElement: Element,
        endElementIndex: Int,
        tmpElement: Element,
        tmpElementIndex: Int,
        elements: List<Element>,
        updateTaskStatusInfos: MutableList<PipelineTaskStatusInfo>?
    ): Boolean {
        val elementPostInfo = tmpElement.additionalOptions?.elementPostInfo
        if (elementPostInfo != null) {
            // 判断post任务的父任务是否执行过
            val parentElementJobIndex = elementPostInfo.parentElementJobIndex
            val parentElement = elements[parentElementJobIndex]
            val taskStatus = BuildStatus.parse(parentElement.status)
            if (!(taskStatus.isFinish() || parentElement.id == endElement.id)) {
                handleCancelTaskStatusInfo(
                    containerId = containerId,
                    tmpElementIndex = tmpElementIndex,
                    elements = elements,
                    endElementIndex = endElementIndex,
                    updateTaskStatusInfos = updateTaskStatusInfos
                )
                return false
            }
            // 把post任务和取消任务之间的任务置为UNEXEC状态
            val startIndex = endElementIndex + 1
            val endIndex = tmpElementIndex - 1
            if (endIndex < startIndex) {
                return true
            }
            addCancelTaskStatusInfo(
                containerId = containerId,
                startIndex = startIndex,
                endIndex = endIndex,
                elements = elements,
                updateTaskStatusInfos = updateTaskStatusInfos
            )
            return true
        }
        return false
    }

    private fun handleCancelTaskStatusInfo(
        containerId: String,
        tmpElementIndex: Int,
        elements: List<Element>,
        endElementIndex: Int,
        updateTaskStatusInfos: MutableList<PipelineTaskStatusInfo>?
    ) {
        if (tmpElementIndex == elements.size - 1) {
            val startIndex = endElementIndex + 1
            val endIndex = elements.size - 1
            if (endIndex > startIndex) {
                addCancelTaskStatusInfo(
                    containerId = containerId,
                    startIndex = startIndex,
                    endIndex = endIndex,
                    elements = elements,
                    updateTaskStatusInfos = updateTaskStatusInfos
                )
            }
        }
    }

    private fun addCancelTaskStatusInfo(
        containerId: String,
        startIndex: Int,
        endIndex: Int,
        elements: List<Element>,
        updateTaskStatusInfos: MutableList<PipelineTaskStatusInfo>?
    ) {
        for (i in startIndex..endIndex) {
            val element = elements[i]
            val taskId = element.id
            val additionalOptions = element.additionalOptions
            // 排除构建状态为结束态的构建任务
            if (taskId != null && !BuildStatus.parse(element.status).isFinish() &&
                additionalOptions?.elementPostInfo == null
            ) {
                val unExecBuildStatus = BuildStatus.UNEXEC
                element.status = unExecBuildStatus.name
                updateTaskStatusInfos?.add(
                    PipelineTaskStatusInfo(
                        taskId = taskId,
                        containerHashId = containerId,
                        buildStatus = unExecBuildStatus,
                        executeCount = element.executeCount,
                        message = "Do not meet the run conditions, ignored."
                    )
                )
            }
        }
    }

    @Suppress("NestedBlockDepth")
    fun taskContinue(
        projectId: String,
        buildId: String,
        stageId: String,
        containerId: String,
        taskId: String,
        element: Element?
    ) {
        update(
            projectId = projectId,
            buildId = buildId,
            modelInterface = object : ModelInterface {

                var update = false

                override fun onFindStage(stage: Stage, model: Model): Traverse {
                    // 不是当前Stage要跳过，不再进入onFindContainer循环
                    return if (stage.id.equals(stageId)) Traverse.CONTINUE else Traverse.SKIP
                }

                override fun onFindContainer(container: Container, stage: Stage): Traverse {
                    val targetContainer = container.getContainerById(containerId)
                    if (targetContainer != null) {
                        val newElement: ArrayList<Element> by lazy { ArrayList(targetContainer.elements.size) }
                        targetContainer.elements.forEach { e ->
                            if (e.id.equals(taskId)) {
                                // 设置插件状态为排队状态
                                targetContainer.status = BuildStatus.QUEUE.name
                                update = true
                                if (element != null) { // 若element不为null，说明element内的input有改动，需要替换
                                    element.status = null
                                    newElement.add(element)
                                } else {
                                    // 若element为null，需把status至空，用户展示
                                    e.status = null
                                }
                            } else {
                                if (element != null) {
                                    newElement.add(e)
                                }
                            }
                        }
                        if (element != null) {
                            targetContainer.elements = newElement
                        }
                        return Traverse.BREAK
                    }
                    return Traverse.SKIP
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING,
            operation = "updateElementWhenPauseContinue#$taskId"
        )
    }
}
