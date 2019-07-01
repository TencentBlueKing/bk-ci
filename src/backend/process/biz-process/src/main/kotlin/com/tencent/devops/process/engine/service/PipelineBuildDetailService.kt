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

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.websocket.ChangeType
import com.tencent.devops.process.websocket.PipelineStatusChangeEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineBuildDetailService @Autowired constructor(
    private val dslContext: DSLContext,
    private val buildDetailDao: BuildDetailDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val redisOperation: RedisOperation,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
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

        return ModelDetail(
            id = record.buildId,
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

    fun create(buildId: String, startType: StartType, buildNum: Int, model: Model) {
        buildDetailDao.create(dslContext, buildId, startType, buildNum, JsonUtil.toJson(model))
        pipelineDetailChangeEvent(buildId)
    }

    private fun pipelineDetailChangeEvent(buildId: String) {
        val pipelineBuildInfo = pipelineBuildDao.getBuildInfo(dslContext, buildId) ?: return
        logger.info("dispatch pipelineDetailChangeEvent, buildId: $buildId")
        pipelineEventDispatcher.dispatch(
            PipelineStatusChangeEvent(
                source = "pipelineDetailChangeEvent",
                pipelineId = pipelineBuildInfo.pipelineId,
                changeType = ChangeType.DETAIL,
                buildId = pipelineBuildInfo.buildId,
                projectId = pipelineBuildInfo.projectId,
                userId = pipelineBuildInfo.startUser
            )
        )
    }

    fun updateModel(buildId: String, model: Model) {
        val now = System.currentTimeMillis()
        logger.info("update the build model for the build $buildId and now $now")
        buildDetailDao.update(
            dslContext,
            buildId,
            JsonUtil.getObjectMapper().writeValueAsString(model),
            BuildStatus.RUNNING
        )
        pipelineDetailChangeEvent(buildId)
    }

    fun containerPreparing(buildId: String, containerId: Int) {
        logger.info("Update the container $containerId of build $buildId to prepare status")
        update(buildId, object : ModelInterface {
            var update = false
            override fun onFindContainer(id: Int, container: Container): Traverse {
                if (id == containerId) {
                    container.startEpoch = System.currentTimeMillis()
                    container.status = BuildStatus.PREPARE_ENV.name
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

            override fun onFindContainer(id: Int, container: Container): Traverse {
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

    fun taskEnd(buildId: String, taskId: String, buildStatus: BuildStatus, canRetry: Boolean? = false) {
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

    fun pipelineTaskEnd(buildId: String, elementId: String, buildStatus: BuildStatus) {
        taskEnd(buildId, elementId, buildStatus, BuildStatus.isFailure(buildStatus))
    }

    fun normalContainerSkip(buildId: String, containerId: String) {
        logger.info("Normal container skip of build $buildId")
        update(buildId, object : ModelInterface {

            var update = false

            override fun onFindContainer(id: Int, container: Container): Traverse {
                if (container is NormalContainer) {
                    // 兼容id字段
                    if (container.id == containerId || container.containerId == containerId) {
                        update = true
                        container.status = BuildStatus.SKIP.name
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

            override fun onFindContainer(id: Int, container: Container): Traverse {
                if (container.status == BuildStatus.PREPARE_ENV.name) {
                    container.status = buildStatus.name
                    if (container.startEpoch == null) {
                        logger.warn("The container($id) of build $buildId start epoch is null")
                        container.systemElapsed = 0
                    } else {
                        container.systemElapsed = System.currentTimeMillis() - container.startEpoch!!
                    }

                    update = true
                }
                return Traverse.CONTINUE
            }

            override fun onFindElement(e: Element, c: Container): Traverse {
                if (e.status == BuildStatus.RUNNING.name || e.status == BuildStatus.REVIEWING.name) {
                    e.status = buildStatus.name
                    c.status = buildStatus.name

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

    fun buildEnd(buildId: String, buildStatus: BuildStatus) {
        logger.info("Build end $buildId")

        val record = buildDetailDao.get(dslContext, buildId)
        if (record == null) {
            logger.warn("The build detail of build $buildId is not exist, ignore")
            return
        }

        // 获取状态
        val finalStatus = takeBuildStatus(record, buildStatus)

        try {
            val model: Model = JsonUtil.to(record.model, Model::class.java)

            model.stages.forEach { stage ->
                stage.containers.forEach { c ->
                    reassignStatusWhenRunning(c, finalStatus)
                }
            }

            buildDetailDao.update(dslContext, buildId, JsonUtil.toJson(model), finalStatus)
            pipelineDetailChangeEvent(buildId)
        } catch (ignored: Throwable) {
            logger.warn(
                "Fail to update the build end status of model ${record.model} with status $buildStatus of build $buildId",
                ignored
            )
        }
    }

    private fun takeBuildStatus(
        record: TPipelineBuildDetailRecord,
        buildStatus: BuildStatus
    ): BuildStatus {

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

            override fun onFindContainer(id: Int, container: Container): Traverse {
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

    fun taskStart(buildId: String, taskId: String) {
        logger.info("The task($taskId) start of build $buildId")
        val variables = pipelineRuntimeService.getAllVariable(buildId)
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

    private fun update(buildId: String, modelInterface: ModelInterface, buildStatus: BuildStatus) {

        val lock = RedisLock(redisOperation, "process.build.detail.lock.$buildId", ExpiredTimeInSeconds)

        try {
            lock.lock()
            val record = buildDetailDao.get(dslContext, buildId)
            if (record == null) {
                logger.warn("The build detail of build $buildId is not exist, ignore")
                return
            }
            val model = JsonUtil.to(record.model, Model::class.java)
            if (model.stages.size <= 1) {
                logger.warn("It only contains trigger container of build $buildId - $model")
                return
            }

            update(model, modelInterface)

            if (!modelInterface.needUpdate()) {
                logger.warn("Will not update the $model")
                return
            }

            val now = System.currentTimeMillis()

            val finalStatus = takeBuildStatus(record, buildStatus)
            logger.info("Update the build detail with status $finalStatus for the build $buildId and time $now")

            val modelStr = JsonUtil.toJson(model)
            buildDetailDao.update(dslContext, buildId, modelStr, finalStatus)
            pipelineDetailChangeEvent(buildId)
        } catch (ignored: Throwable) {
            logger.warn("Fail to update the build detail of build $buildId", ignored)
        } finally {
            lock.unlock()
        }
    }

    private fun update(model: Model, modelInterface: ModelInterface) {
        var containerId = 1
        model.stages.forEachIndexed { index, stage ->
            if (index == 0) {
                return@forEachIndexed
            }
            stage.containers.forEach { c ->
                if (Traverse.BREAK == modelInterface.onFindContainer(containerId, c)) {
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

        fun onFindContainer(id: Int, container: Container) = Traverse.CONTINUE

        fun onFindElement(e: Element, c: Container) = Traverse.CONTINUE

        fun needUpdate(): Boolean
    }

    enum class Traverse {
        BREAK,
        CONTINUE
    }
}
