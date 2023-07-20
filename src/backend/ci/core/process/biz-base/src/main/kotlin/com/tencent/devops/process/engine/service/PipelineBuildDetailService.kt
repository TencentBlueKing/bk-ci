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
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.RunCondition
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineTriggerReviewDao
import com.tencent.devops.process.engine.service.detail.BaseBuildDetailService
import com.tencent.devops.process.engine.utils.ContainerUtils
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.utils.PipelineVarUtil
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Suppress("LongParameterList", "ComplexMethod", "ReturnCount")
@Service
class PipelineBuildDetailService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineTriggerReviewDao: PipelineTriggerReviewDao,
    dslContext: DSLContext,
    pipelineBuildDao: PipelineBuildDao,
    buildDetailDao: BuildDetailDao,
    redisOperation: RedisOperation,
    stageTagService: StageTagService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseBuildDetailService(
    dslContext,
    pipelineBuildDao,
    buildDetailDao,
    stageTagService,
    pipelineEventDispatcher,
    redisOperation
) {

    @Value("\${pipeline.build.retry.limit_days:21}")
    private var retryLimitDays: Int = 0

    companion object {
        val logger = LoggerFactory.getLogger(PipelineBuildDetailService::class.java)!!
    }

    private fun checkPassDays(startTime: Long?): Boolean {
        if (retryLimitDays < 0 || startTime == null) {
            return true
        }
        return (System.currentTimeMillis() - startTime) < TimeUnit.DAYS.toMillis(retryLimitDays.toLong())
    }

    /**
     * 查询ModelDetail
     * @param projectId: 项目Id
     * @param buildId: 构建Id
     * @param refreshStatus: 是否刷新状态
     */
    fun get(projectId: String, buildId: String, refreshStatus: Boolean = true): ModelDetail? {

        val record = buildDetailDao.get(dslContext, projectId, buildId) ?: return null

        val buildInfo = pipelineBuildDao.convert(
            pipelineBuildDao.getBuildInfo(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId
            )
        ) ?: return null

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, buildInfo.pipelineId) ?: return null

        val buildSummaryRecord = pipelineBuildSummaryDao.get(dslContext, projectId, buildInfo.pipelineId)

        val model = JsonUtil.to(record.model, Model::class.java)

        // 判断需要刷新状态，目前只会改变canRetry & canSkip 状态
        if (refreshStatus) {
            // #4245 仅当在有限时间内并已经失败或者取消(终态)的构建上可尝试重试或跳过
            // #6400 无需流水线是终态就可以进行task重试
            if (checkPassDays(buildInfo.startTime)) {
                ModelUtils.refreshCanRetry(model)
            }
        }

        val triggerContainer = model.stages[0].containers[0] as TriggerContainer
        val buildNo = triggerContainer.buildNo
        if (buildNo != null) {
            buildNo.buildNo = buildSummaryRecord?.buildNo ?: buildNo.buildNo
        }
        val params = triggerContainer.params
        val newParams = ArrayList<BuildFormProperty>(params.size)
        params.forEach {
            // 变量名从旧转新: 兼容从旧入口写入的数据转到新的流水线运行
            val newVarName = PipelineVarUtil.oldVarToNewVar(it.id)
            if (!newVarName.isNullOrBlank()) newParams.add(it.copy(id = newVarName)) else newParams.add(it)
        }
        triggerContainer.params = newParams

        // #4531 兼容历史构建的页面显示
        model.stages.forEach { stage ->
            stage.resetBuildOption()
            // #4518 兼容历史构建的containerId作为日志JobId，发布后新产生的groupContainers无需校准
            stage.containers.forEach { container ->
                container.containerHashId = container.containerHashId ?: container.containerId
                container.containerId = container.id
            }
        }
        val triggerReviewers = pipelineTriggerReviewDao.getTriggerReviewers(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineInfo.pipelineId,
            buildId = buildId
        )
        return ModelDetail(
            id = record.buildId,
            pipelineId = buildInfo.pipelineId,
            pipelineName = model.name,
            userId = record.startUser ?: "",
            triggerUser = buildInfo.triggerUser,
            trigger = StartType.toReadableString(
                buildInfo.trigger,
                buildInfo.channelCode,
                I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            ),
            startTime = record.startTime?.timestampmilli() ?: LocalDateTime.now().timestampmilli(),
            endTime = record.endTime?.timestampmilli(),
            status = record.status ?: "",
            model = model,
            currentTimestamp = System.currentTimeMillis(),
            buildNum = buildInfo.buildNum,
            cancelUserId = record.cancelUser ?: "",
            curVersion = buildInfo.version,
            latestVersion = pipelineInfo.version,
            latestBuildNum = buildSummaryRecord?.buildNum ?: -1,
            lastModifyUser = pipelineInfo.lastModifyUser,
            executeTime = buildInfo.executeTime,
            triggerReviewers = triggerReviewers
        )
    }

    fun updateModel(projectId: String, buildId: String, model: Model) {
        buildDetailDao.update(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            model = JsonUtil.toJson(model, formatted = false),
            buildStatus = BuildStatus.RUNNING
        )
//        pipelineDetailChangeEvent(projectId, buildId)
    }

    fun buildCancel(projectId: String, buildId: String, buildStatus: BuildStatus, cancelUser: String) {
        logger.info("Cancel the build $buildId by $cancelUser")
        update(
            projectId = projectId, buildId = buildId,
            modelInterface = object : ModelInterface {

                var update = false

                override fun onFindStage(stage: Stage, model: Model): Traverse {
                    if (BuildStatus.parse(stage.status).isRunning()) {
                        stage.status = buildStatus.name
                        if (stage.startEpoch == null) {
                            stage.elapsed = 0
                        } else {
                            stage.elapsed = System.currentTimeMillis() - stage.startEpoch!!
                        }
                        update = true
                    }
                    return Traverse.CONTINUE
                }

                override fun onFindContainer(container: Container, stage: Stage): Traverse {
                    val status = BuildStatus.parse(container.status)
                    if (status == BuildStatus.PREPARE_ENV) {
                        if (container.startEpoch == null) {
                            container.systemElapsed = 0
                        } else {
                            container.systemElapsed = System.currentTimeMillis() - container.startEpoch!!
                        }
                        update = true
                    }
                    // #3138 状态实时刷新
                    val refreshFlag = status.isRunning() && container.elements[0].status.isNullOrBlank() &&
                        container.containPostTaskFlag != true
                    if (status == BuildStatus.PREPARE_ENV || refreshFlag) {
                        container.name = ContainerUtils.getClearedQueueContainerName(container.name)
                        container.status = buildStatus.name
                    }
                    return Traverse.CONTINUE
                }

                override fun onFindElement(index: Int, e: Element, c: Container): Traverse {
                    if (e.status == BuildStatus.RUNNING.name || e.status == BuildStatus.REVIEWING.name) {
                        val status = if (e.status == BuildStatus.RUNNING.name) {
                            val runCondition = e.additionalOptions?.runCondition
                            // 当task的runCondition为PRE_TASK_FAILED_EVEN_CANCEL，点击取消还需要运行
                            if (runCondition == RunCondition.PRE_TASK_FAILED_EVEN_CANCEL) {
                                BuildStatus.RUNNING.name
                            } else {
                                BuildStatus.CANCELED.name
                            }
                        } else buildStatus.name
                        e.status = status
                        if (c.containPostTaskFlag != true) {
                            c.status = status
                        }
                        if (BuildStatus.parse(status).isFinish()) {
                            if (e.startEpoch != null) {
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
                        }

                        update = true
                    }
                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = BuildStatus.RUNNING, cancelUser = cancelUser, operation = "buildCancel"
        )
    }

    fun buildEnd(
        projectId: String,
        buildId: String,
        buildStatus: BuildStatus,
        errorMsg: String?
    ): Pair<Model, List<BuildStageStatus>> {
        logger.info("[$buildId]|BUILD_END|buildStatus=$buildStatus")
        var allStageStatus: List<BuildStageStatus> = emptyList()
        val model = update(
            projectId = projectId, buildId = buildId,
            modelInterface = object : ModelInterface {
                var update = false

                override fun onFindContainer(container: Container, stage: Stage): Traverse {
                    if (BuildStatus.parse(container.status).isRunning()) {
                        container.status = buildStatus.name
                        update = true
                        if (container.startEpoch == null) {
                            container.elementElapsed = 0
                        } else {
                            container.elementElapsed = System.currentTimeMillis() - container.startEpoch!!
                        }
                        container.name = ContainerUtils.getClearedQueueContainerName(container.name)
                    }
                    return Traverse.CONTINUE
                }

                override fun onFindStage(stage: Stage, model: Model): Traverse {
                    if (allStageStatus.isEmpty()) {
                        allStageStatus = fetchHistoryStageStatus(
                            model = model,
                            buildStatus = buildStatus,
                            errorMsg = errorMsg
                        )
                    }
                    if (BuildStatus.parse(stage.status).isRunning()) {
                        stage.status = buildStatus.name
                        update = true
                        if (stage.startEpoch == null) {
                            stage.elapsed = 0
                        } else {
                            stage.elapsed = System.currentTimeMillis() - stage.startEpoch!!
                        }
                    }
                    return Traverse.CONTINUE
                }

                override fun onFindElement(index: Int, e: Element, c: Container): Traverse {
                    if (!e.status.isNullOrBlank() && BuildStatus.valueOf(e.status!!).isRunning()) {
                        e.status = buildStatus.name
                        update = true
                        if (e.startEpoch != null) {
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
                    }

                    return Traverse.CONTINUE
                }

                override fun needUpdate(): Boolean {
                    return update
                }
            },
            buildStatus = buildStatus, operation = "buildEnd"
        )
        return model to allStageStatus
    }

    fun updateBuildCancelUser(projectId: String, buildId: String, cancelUserId: String) {
        buildDetailDao.updateBuildCancelUser(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            cancelUser = cancelUserId
        )
    }

    fun getBuildDetailPipelineId(projectId: String, buildId: String): String? {
        return pipelineBuildDao.getBuildInfo(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId
        )?.pipelineId
    }
}
