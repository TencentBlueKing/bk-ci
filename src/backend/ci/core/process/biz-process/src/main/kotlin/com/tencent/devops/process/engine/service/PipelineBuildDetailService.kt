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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.process.engine.pojo.PipelineBuildStageControlOption
import com.tencent.devops.process.service.BuildVariableService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.time.LocalDateTime

@Service
class PipelineBuildDetailService @Autowired constructor(
    private val dslContext: DSLContext,
    private val buildDetailDao: BuildDetailDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildVariableService: BuildVariableService,
    private val redisOperation: RedisOperation,
    private val webSocketDispatcher: WebSocketDispatcher,
    private val pipelineWebsocketService: PipelineWebsocketService,
    private val pipelineBuildDao: PipelineBuildDao
) {

    companion object {
        val logger = LoggerFactory.getLogger(PipelineBuildDetailService::class.java)!!
        private const val ExpiredTimeInSeconds: Long = 10
    }

    /**
     * 查询ModelDetail
     * @param buildId: 构建Id
     * @param refreshStatus: 是否刷新状态
     */
    fun get(buildId: String, refreshStatus: Boolean = true): ModelDetail? {

        val record = buildDetailDao.get(dslContext, buildId) ?: run {
            logger.warn("[$buildId]| detail record is null")
            return null
        }

        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: run {
            logger.warn("[$buildId]| history info record is null")
            return null
        }

        val latestVersion = pipelineRepositoryService.getPipelineInfo(buildInfo.pipelineId)?.version ?: -1

        val buildSummaryRecord = pipelineRuntimeService.getBuildSummaryRecord(buildInfo.pipelineId)

        val model = JsonUtil.to(record.model, Model::class.java)

        // 构建机环境的会因为构建号不一样工作空间可能被覆盖的问题, 所以构建号不同不允许重试
        val canRetry =
            buildSummaryRecord?.buildNum == buildInfo.buildNum && BuildStatus.isFailure(buildInfo.status) // 并且是失败后

        // 判断需要刷新状态，目前只会改变canRetry状态
        if (refreshStatus) {
            ModelUtils.refreshCanRetry(model, canRetry, buildInfo.status)
        }

        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val params = triggerContainer.params
        val newParams = mutableListOf<BuildFormProperty>()
        params.forEach {
            // 变量名从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.id)
            if (!newVarName.isNullOrBlank()) {
                newParams.add(
                    BuildFormProperty(
                        id = newVarName!!,
                        required = it.required,
                        type = it.type,
                        defaultValue = it.defaultValue,
                        options = it.options,
                        desc = it.desc,
                        repoHashId = it.repoHashId,
                        relativePath = it.relativePath,
                        scmType = it.scmType,
                        containerType = it.containerType,
                        glob = it.glob,
                        properties = it.properties
                    )
                )
            } else newParams.add(it)
        }
        triggerContainer.params = newParams

        val defaultTagIds = listOf(pipelineStageService.getDefaultStageTagId())
        model.stages.forEach {
            if (it.name.isNullOrBlank()) it.name = it.id
            if (it.tag == null) it.tag = defaultTagIds
        }

        return ModelDetail(
            id = record.buildId,
            pipelineId = buildInfo.pipelineId,
            pipelineName = model.name,
            userId = record.startUser ?: "",
            trigger = StartType.toReadableString(buildInfo.trigger, buildInfo.channelCode),
            startTime = record.startTime?.timestampmilli() ?: LocalDateTime.now().timestampmilli(),
            endTime = record.endTime?.timestampmilli(),
            status = record.status ?: "",
            model = model,
            currentTimestamp = System.currentTimeMillis(),
            buildNum = buildInfo.buildNum,
            cancelUserId = record.cancelUser ?: "",
            curVersion = buildInfo.version,
            latestVersion = latestVersion,
            latestBuildNum = buildSummaryRecord?.buildNum ?: -1
        )
    }

    fun getBuildModel(buildId: String): Model? {
        val record = buildDetailDao.get(dslContext, buildId) ?: return null
        return JsonUtil.to(record.model, Model::class.java)
    }

    fun pipelineDetailChangeEvent(buildId: String) {
        val pipelineBuildInfo = pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: return
        logger.info("dispatch pipelineDetailChangeEvent, buildId: $buildId")
        webSocketDispatcher.dispatch(
            pipelineWebsocketService.buildDetailMessage(
                buildId = pipelineBuildInfo.buildId,
                projectId = pipelineBuildInfo.projectId,
                pipelineId = pipelineBuildInfo.pipelineId,
                userId = pipelineBuildInfo.startUser
            )
        )
    }

    fun pipelineHistoryChangeEvent(buildId: String) {
        val pipelineBuildInfo = pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: return
        logger.info("dispatch pipelineHistoryChangeEvent, buildId: $buildId")
        webSocketDispatcher.dispatch(
            pipelineWebsocketService.buildHistoryMessage(
                buildId = pipelineBuildInfo.buildId,
                projectId = pipelineBuildInfo.projectId,
                pipelineId = pipelineBuildInfo.pipelineId,
                userId = pipelineBuildInfo.startUser
            )
        )
    }

    fun updateModel(buildId: String, model: Model) {
        val now = System.currentTimeMillis()
        logger.info("update the build model for the build $buildId and now $now")
        buildDetailDao.update(
            dslContext = dslContext,
            buildId = buildId,
            model = JsonUtil.getObjectMapper().writeValueAsString(model),
            buildStatus = BuildStatus.RUNNING
        )
        pipelineDetailChangeEvent(buildId)
    }

    fun containerPreparing(buildId: String, containerId: Int) {
        logger.info("Update the container $containerId of build $buildId to prepare status")
        update(buildId, object : ModelInterface {
            var update = false
            override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                if (id == containerId) {
                    container.startEpoch = System.currentTimeMillis()
                    container.status = BuildStatus.PREPARE_ENV.name
                    container.startVMStatus = BuildStatus.RUNNING.name
                    update = true
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                if (!update) {
                    logger.info("The container prepare of build $buildId with container $containerId is not update")
                }
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun containerStart(buildId: String, containerId: Int) {
        logger.info("Update the container $containerId of build $buildId to start status")
        update(buildId, object : ModelInterface {
            var update = false

            override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                if (id == containerId) {
                    if (container.startEpoch == null) {
                        logger.warn("The start epoch of container $id is null of build $buildId")
                    } else {
                        container.systemElapsed = System.currentTimeMillis() - container.startEpoch!!
                    }
                    container.status = BuildStatus.RUNNING.name
                    update = true
                    return Traverse.BREAK
                }
                return Traverse.BREAK
            }

            override fun needUpdate(): Boolean {
                if (!update) {
                    logger.info("The container start is not update of build $buildId with container $containerId")
                }
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun taskEnd(
        buildId: String,
        taskId: String,
        buildStatus: BuildStatus,
        canRetry: Boolean? = false,
        errorType: ErrorType? = null,
        errorCode: Int? = null,
        errorMsg: String? = null
    ) {
        logger.info("The build task $taskId end of build $buildId with status $buildStatus")
        update(buildId, object : ModelInterface {

            var update = false
            override fun onFindElement(e: Element, c: Container): Traverse {
                if (e.id == taskId) {

//                    if (BuildStatus.isFinish(buildStatus) && buildStatus != BuildStatus.SKIP) {
//                        c.status = buildStatus.name
//                    }
                    e.canRetry = canRetry
                    e.status = buildStatus.name
                    if (e.startEpoch == null) {
                        logger.warn("The task($taskId) of build $buildId start epoch is null")
                        e.elapsed = 0
                    } else {
                        e.elapsed = System.currentTimeMillis() - e.startEpoch!!
                    }
                    c.canRetry = canRetry ?: false
                    if (errorType != null) {
                        e.errorType = errorType.name
                        e.errorCode = errorCode
                        e.errorMsg = errorMsg
                    }

                    var elementElapsed = 0L
                    run lit@{
                        c.elements.forEach {
                            if (it.elapsed == null) {
                                logger.warn("The task($taskId) of build $buildId elapse is null")
                                return@forEach
                            }
                            elementElapsed += it.elapsed!!
                            if (it == e) {
                                return@lit
                            }
                        }
                    }

                    c.elementElapsed = elementElapsed
                    update = true
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                if (!update) {
                    logger.info("The task end is not update of build $buildId with task $taskId and status $buildStatus")
                }
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun pipelineTaskEnd(
        buildId: String,
        taskId: String,
        buildStatus: BuildStatus,
        errorType: ErrorType?,
        errorCode: Int?,
        errorMsg: String?
    ) {
        taskEnd(buildId, taskId, buildStatus, BuildStatus.isFailure(buildStatus), errorType, errorCode, errorMsg)
    }

    fun normalContainerSkip(buildId: String, containerId: String) {
        logger.info("[$buildId|$containerId] Normal container skip")
        update(buildId, object : ModelInterface {

            var update = false

            override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                if (container !is TriggerContainer) {
                    // 兼容id字段
                    if (container.id == containerId || container.containerId == containerId) {
                        update = true
                        container.status = BuildStatus.SKIP.name
                        container.startVMStatus = BuildStatus.SKIP.name
                        container.elements.forEach {
                            it.status = BuildStatus.SKIP.name
                        }
                        return Traverse.BREAK
                    }
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun buildCancel(buildId: String, buildStatus: BuildStatus) {
        logger.info("Cancel the build $buildId")
        update(buildId, object : ModelInterface {

            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.status == BuildStatus.RUNNING.name) {
                    stage.status = buildStatus.name
                    if (stage.startEpoch == null) {
                        logger.warn("The stage(${stage.id}) of build $buildId start epoch is null")
                        stage.elapsed = 0
                    } else {
                        stage.elapsed = System.currentTimeMillis() - stage.startEpoch!!
                    }
                    update = true
                }
                return Traverse.CONTINUE
            }

            override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                if (container.status == BuildStatus.PREPARE_ENV.name) {
                    if (container.startEpoch == null) {
                        logger.warn("The container($id) of build $buildId start epoch is null")
                        container.systemElapsed = 0
                    } else {
                        container.systemElapsed = System.currentTimeMillis() - container.startEpoch!!
                    }

                    var containerElapsed = 0L
                    run lit@{
                        stage.containers.forEach {
                            containerElapsed += it.elementElapsed ?: 0
                            if (it == container) {
                                return@lit
                            }
                        }
                    }

                    stage.elapsed = containerElapsed

                    update = true
                }
                return Traverse.CONTINUE
            }

            override fun onFindElement(e: Element, c: Container): Traverse {
                if (e.status == BuildStatus.RUNNING.name || e.status == BuildStatus.REVIEWING.name) {
                    val status = if (e.status == BuildStatus.RUNNING.name) {
                        BuildStatus.TERMINATE.name
                    } else buildStatus.name
                    e.status = status
                    c.status = status

                    if (e.startEpoch == null) {
                        logger.warn("The element(${e.name}|${e.id}) start epoch is null of build $buildId")
                    } else {
                        e.elapsed = System.currentTimeMillis() - e.startEpoch!!
                    }

                    var elementElapsed = 0L
                    run lit@{
                        c.elements.forEach {
                            elementElapsed += it.elapsed ?: 0
                            if (it == e) {
                                return@lit
                            }
                        }
                    }

                    c.elementElapsed = elementElapsed

                    update = true
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                if (!update) {
                    logger.info("The build cancel is not update of build $buildId with status $buildStatus")
                }
                return update
            }
        }, buildStatus)
    }

    fun buildEnd(
        buildId: String,
        buildStatus: BuildStatus,
        cancelUser: String? = null,
        errorInfos: List<ErrorInfo>? = null
    ) {
        logger.info("Build end $buildId")

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val record = buildDetailDao.get(context, buildId)
            if (record == null) {
                logger.warn("The build detail of build $buildId is not exist, ignore")
                return@transaction
            }
            val status = record.status
            val oldStatus = if (status.isNullOrBlank()) {
                null
            } else {
                BuildStatus.valueOf(status)
            }
            val finalStatus = if (oldStatus == null) {
                buildStatus
            } else {
                if (BuildStatus.isFinish(oldStatus)) {
                    logger.info("The build $buildId is already finished by status $oldStatus, not replace with the sta†us $buildStatus")
                    oldStatus
                } else {
                    logger.info("Update the build $buildId to status $buildStatus from $oldStatus")
                    buildStatus
                }
            }

            logger.info("[$buildId]|BUILD_END|buildStatus=$buildStatus|finalStatus=$finalStatus|cancelUser=$cancelUser|errorInfo=$errorInfos")
            try {
                val model: Model = JsonUtil.to(record.model, Model::class.java)
                val allStageStatus = mutableListOf<BuildStageStatus>()
                model.stages.forEach { stage ->
                    stage.containers.forEach { container ->
                        if (!container.status.isNullOrBlank()) {
                            val s = BuildStatus.valueOf(container.status!!)
                            if (BuildStatus.isRunning(s)) {
                                container.status = finalStatus.name
                            }
                        }
                        container.elements.forEach { e ->
                            if (!e.status.isNullOrBlank()) {
                                val s = BuildStatus.valueOf(e.status!!)
                                if (BuildStatus.isRunning(s)) {
                                    e.status = finalStatus.name
                                }
                            }
                        }
                    }
                    if (!stage.status.isNullOrBlank()) {
                        val s = BuildStatus.valueOf(stage.status!!)
                        if (BuildStatus.isRunning(s)) {
                            stage.status = finalStatus.name
                        }
                    }
                    allStageStatus.add(
                        BuildStageStatus(
                            stageId = stage.id!!,
                            name = stage.name ?: stage.id!!,
                            status = stage.status,
                            startEpoch = stage.startEpoch,
                            elapsed = stage.elapsed
                        )
                    )
                }
                pipelineBuildDao.updateBuildStageStatus(dslContext, buildId, allStageStatus)
                buildDetailDao.update(
                    dslContext = context,
                    buildId = buildId,
                    model = JsonUtil.toJson(model),
                    buildStatus = finalStatus,
                    cancelUser = cancelUser
                )
                pipelineDetailChangeEvent(buildId)
                pipelineHistoryChangeEvent(buildId)
            } catch (t: Throwable) {
                logger.warn(
                    "Fail to update the build end status of model ${record.model} with status $buildStatus of build $buildId",
                    t
                )
            }
        }
    }

    private fun takeBuildStatus(record: TPipelineBuildDetailRecord, buildStatus: BuildStatus): BuildStatus {

        val status = record.status
        val oldStatus = if (status.isNullOrBlank()) {
            null
        } else {
            BuildStatus.valueOf(status)
        }

        return if (oldStatus == null || !BuildStatus.isFinish(oldStatus)) {
            logger.info("[${record.buildId}]|Update the build to status $buildStatus from $oldStatus")
            buildStatus
        } else {
            logger.info("[${record.buildId}]|The build is already finished by status $oldStatus, not replace with the sta†us $buildStatus")
            oldStatus
        }
    }

    private fun reassignStatusWhenRunning(c: Container, finalStatus: BuildStatus) {
        if (!c.status.isNullOrBlank()) {
            val s = BuildStatus.valueOf(c.status!!)
            if (BuildStatus.isRunning(s)) {
                c.status = finalStatus.name
            }
        }
        c.elements.forEach { e ->
            if (!e.status.isNullOrBlank()) {
                val s = BuildStatus.valueOf(e.status!!)
                if (BuildStatus.isRunning(s)) {
                    e.status = finalStatus.name
                }
            }
        }
    }

    fun updateBuildCancelUser(buildId: String, cancelUserId: String) {
        buildDetailDao.updateBuildCancelUser(dslContext, buildId, cancelUserId)
        pipelineDetailChangeEvent(buildId)
    }

    fun updateContainerStatus(buildId: String, containerId: String, buildStatus: BuildStatus) {
        logger.info("[$buildId]|container_end|containerId=$containerId|status=$buildStatus")
        update(buildId, object : ModelInterface {

            var update = false

            override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                if (container.id == containerId) {
                    update = true
                    container.status = buildStatus.name
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun updateStageStatus(buildId: String, stageId: String, buildStatus: BuildStatus) {
        logger.info("[$buildId]|update_stage_status|stageId=$stageId|status=$buildStatus")
        update(buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = buildStatus.name
                    if (BuildStatus.isRunning(buildStatus) && stage.startEpoch == null) {
                        stage.startEpoch = System.currentTimeMillis()
                    } else if (BuildStatus.isFinish(buildStatus) && stage.startEpoch != null) {
                        stage.elapsed = System.currentTimeMillis() - stage.startEpoch!!
                    }
                    updateHistoryStage(buildId, model)
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun stageSkip(buildId: String, stageId: String) {
        logger.info("[$buildId]|stage_skip|stageId=$stageId")
        update(buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = BuildStatus.SKIP.name
                    stage.containers.forEach {
                        it.status = BuildStatus.SKIP.name
                    }
                    updateHistoryStage(buildId, model)
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun stagePause(
        pipelineId: String,
        buildId: String,
        stageId: String,
        controlOption: PipelineBuildStageControlOption
    ) {
        logger.info("[$buildId]|stage_pause|stageId=$stageId")
        update(buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = BuildStatus.PAUSE.name
                    stage.reviewStatus = BuildStatus.REVIEWING.name
                    stage.stageControlOption!!.triggerUsers = controlOption.stageControlOption.triggerUsers
                    stage.startEpoch = System.currentTimeMillis()
                    pipelineBuildDao.updateStatus(dslContext, buildId, BuildStatus.RUNNING, BuildStatus.STAGE_SUCCESS)
                    // 被暂停的流水线不占构建队列，在执行数-1
                    pipelineStageService.updatePipelineRunningCount(pipelineId, buildId, -1)
                    updateHistoryStage(buildId, model)
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.STAGE_SUCCESS)
    }

    fun stageCancel(buildId: String, stageId: String) {
        logger.info("[$buildId]|stage_cancel|stageId=$stageId")
        update(buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = ""
                    stage.reviewStatus = BuildStatus.REVIEW_ABORT.name
                    pipelineBuildDao.updateStageCancelStatus(dslContext, buildId)
                    updateHistoryStage(buildId, model)
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.STAGE_SUCCESS)
    }

    fun stageStart(pipelineId: String, buildId: String, stageId: String) {
        logger.info("[$buildId]|stage_start|stageId=$stageId")
        update(buildId, object : ModelInterface {
            var update = false

            override fun onFindStage(stage: Stage, model: Model): Traverse {
                if (stage.id == stageId) {
                    update = true
                    stage.status = BuildStatus.QUEUE.name
                    stage.reviewStatus = BuildStatus.REVIEW_PROCESSED.name
                    pipelineBuildDao.updateStatus(dslContext, buildId, BuildStatus.STAGE_SUCCESS, BuildStatus.RUNNING)
                    pipelineStageService.updatePipelineRunningCount(pipelineId, buildId, 1)
                    updateHistoryStage(buildId, model)
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun taskSkip(buildId: String, taskId: String) {
        logger.info("[$buildId|$taskId] Task skip")
        update(buildId, object : ModelInterface {
            var update = false
            override fun onFindElement(e: Element, c: Container): Traverse {
                if (e.id == taskId) {
                    update = true
                    e.status = BuildStatus.SKIP.name
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                if (!update) {
                    logger.info("The task start is not update of build $buildId with element $taskId")
                }
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun taskStart(buildId: String, taskId: String) {
        logger.info("The task($taskId) start of build $buildId")
        val variables = buildVariableService.getAllVariable(buildId)
        update(buildId, object : ModelInterface {
            var update = false
            override fun onFindElement(e: Element, c: Container): Traverse {
                if (e.id == taskId) {
                    if (e is ManualReviewUserTaskElement) {
                        e.status = BuildStatus.REVIEWING.name
//                        c.status = BuildStatus.REVIEWING.name
                        // Replace the review user with environment
                        val list = mutableListOf<String>()
                        e.reviewUsers.forEach { reviewUser ->
                            list.addAll(EnvUtils.parseEnv(reviewUser, variables).split(","))
                        }
                        e.reviewUsers.clear()
                        e.reviewUsers.addAll(list)
                    } else if (e is QualityGateInElement || e is QualityGateOutElement) {
                        e.status = BuildStatus.REVIEWING.name
                        c.status = BuildStatus.REVIEWING.name
                    } else {
                        c.status = BuildStatus.RUNNING.name
                        e.status = BuildStatus.RUNNING.name
                    }
                    e.startEpoch = System.currentTimeMillis()
                    if (c.startEpoch == null) {
                        c.startEpoch = e.startEpoch
                    }
                    update = true
                    return Traverse.BREAK
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                if (!update) {
                    logger.info("The task start is not update of build $buildId with element $taskId")
                }
                return update
            }
        }, BuildStatus.RUNNING)
    }

    fun updateStartVMStatus(
        buildId: String,
        containerId: String,
        buildStatus: BuildStatus
    ) {
        logger.info("[$buildId|$containerId] update container startVMStatus to $buildStatus")
        update(buildId, object : ModelInterface {
            var update = false
            override fun onFindContainer(id: Int, container: Container, stage: Stage): Traverse {
                if (container !is TriggerContainer) {
                    // 兼容id字段
                    if (container.id == containerId || container.containerId == containerId) {
                        update = true
                        container.startVMStatus = buildStatus.name
                        // #2074 如果是失败的，则将Job整体状态设置为失败
                        if (BuildStatus.isFailure(buildStatus)) {
                            container.status = buildStatus.name
                        }
                        return Traverse.BREAK
                    }
                }
                return Traverse.CONTINUE
            }

            override fun needUpdate(): Boolean {
                return update
            }
        }, BuildStatus.RUNNING)
    }

    private fun updateHistoryStage(buildId: String, model: Model) {
        // 更新Stage状态至BuildHistory
        val allStageStatus = model.stages.map {
            BuildStageStatus(
                stageId = it.id!!,
                name = it.name ?: it.id!!,
                status = it.status,
                startEpoch = it.startEpoch,
                elapsed = it.elapsed
            )
        }
        pipelineBuildDao.updateBuildStageStatus(dslContext, buildId, allStageStatus)

        // Stage状态更新单独发送构建历史页的消息推送
        val pipelineBuildInfo = pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: return
        webSocketDispatcher.dispatch(
            pipelineWebsocketService.buildHistoryMessage(
                buildId = pipelineBuildInfo.buildId,
                projectId = pipelineBuildInfo.projectId,
                pipelineId = pipelineBuildInfo.pipelineId,
                userId = pipelineBuildInfo.startUser
            )
        )
    }

    private fun update(buildId: String, modelInterface: ModelInterface, buildStatus: BuildStatus) {
        val stopWatch = StopWatch()
        var message = "nothing"
        val lock = RedisLock(redisOperation, "process.build.detail.lock.$buildId", ExpiredTimeInSeconds)

        try {
            stopWatch.start("lock")
            lock.lock()
            stopWatch.stop()
            stopWatch.start("getDetail")
            val record = buildDetailDao.get(dslContext, buildId)
            stopWatch.stop()
            if (record == null) {
                message = "WARN: The build detail is not exist, ignore"
                return
            }
            stopWatch.start("model")
            val model = JsonUtil.to(record.model, Model::class.java)
            stopWatch.stop()
            if (model.stages.size <= 1) {
                message = "Trigger container only"
                return
            }

            stopWatch.start("updateModel")
            update(model, modelInterface)
            stopWatch.stop()

            if (!modelInterface.needUpdate()) {
                message = "Will not update"
                return
            }

            val finalStatus = takeBuildStatus(record, buildStatus)

            stopWatch.start("toJson")
            val modelStr = JsonUtil.toJson(model)
            stopWatch.stop()

            stopWatch.start("updateModel")
            buildDetailDao.update(dslContext, buildId, modelStr, finalStatus)
            stopWatch.stop()

            stopWatch.start("dispatchEvent")
            pipelineDetailChangeEvent(buildId)
            stopWatch.stop()
            message = "update done"
        } catch (ignored: Throwable) {
            if (stopWatch.isRunning) {
                stopWatch.stop()
            }
            message = "${ignored.message}"
            logger.warn("[$buildId]| Fail to update the build detail: ${ignored.message}", ignored)
        } finally {
            lock.unlock()
            logger.info("[$buildId|$buildStatus]|update_detail_model| $message| watch=$stopWatch")
        }
    }

    private fun update(model: Model, modelInterface: ModelInterface) {
        var containerId = 1
        model.stages.forEachIndexed { index, stage ->
            if (index == 0) {
                return@forEachIndexed
            }

            if (Traverse.BREAK == modelInterface.onFindStage(stage, model)) {
                return
            }

            stage.containers.forEach { c ->
                if (Traverse.BREAK == modelInterface.onFindContainer(containerId, c, stage)) {
                    return
                }
                containerId++
                c.elements.forEach { e ->
                    if (Traverse.BREAK == modelInterface.onFindElement(e, c)) {
                        return
                    }
                }
            }
        }
    }

    protected interface ModelInterface {

        fun onFindStage(stage: Stage, model: Model) = Traverse.CONTINUE

        fun onFindContainer(id: Int, container: Container, stage: Stage) = Traverse.CONTINUE

        fun onFindElement(e: Element, c: Container) = Traverse.CONTINUE

        fun needUpdate(): Boolean
    }

    enum class Traverse {
        BREAK,
        CONTINUE
    }
}
