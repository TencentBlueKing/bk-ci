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

package com.tencent.devops.process.engine.service.record

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineBuildSummaryDao
import com.tencent.devops.process.engine.dao.PipelineResDao
import com.tencent.devops.process.engine.dao.PipelineTriggerReviewDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.engine.common.BuildTimeCostUtils
import com.tencent.devops.process.engine.dao.PipelineBuildStageDao
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.record.PipelineRecordModelService
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList", "ComplexMethod", "ReturnCount")
@Service
class PipelineBuildRecordService @Autowired constructor(
    private val pipelineBuildDetailService: PipelineBuildDetailService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineTriggerReviewDao: PipelineTriggerReviewDao,
    private val dslContext: DSLContext,
    private val pipelineBuildDao: PipelineBuildDao,
    private val buildStageDao: PipelineBuildStageDao,
    private val recordModelDao: BuildRecordModelDao,
    private val recordStageDao: BuildRecordStageDao,
    private val recordContainerDao: BuildRecordContainerDao,
    private val recordTaskDao: BuildRecordTaskDao,
    private val recordModelService: PipelineRecordModelService,
    private val pipelineResDao: PipelineResDao,
    redisOperation: RedisOperation,
    stageTagService: StageTagService,
    pipelineEventDispatcher: PipelineEventDispatcher
) : BaseBuildRecordService(
    dslContext = dslContext,
    buildRecordModelDao = recordModelDao,
    stageTagService = stageTagService,
    pipelineEventDispatcher = pipelineEventDispatcher,
    redisOperation = redisOperation
) {

    @Value("\${pipeline.build.retry.limit_days:21}")
    private var retryLimitDays: Int = 0

    companion object {
        val logger = LoggerFactory.getLogger(PipelineBuildRecordService::class.java)!!
    }

    fun batchGet(
        transactionContext: DSLContext?,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int
    ): Triple<List<BuildRecordStage>, List<BuildRecordContainer>, List<BuildRecordTask>> {
        val context = transactionContext ?: dslContext
        return Triple(
            recordStageDao.getRecords(context, projectId, pipelineId, buildId, executeCount),
            recordContainerDao.getRecords(context, projectId, pipelineId, buildId, executeCount, null),
            recordTaskDao.getRecords(context, projectId, pipelineId, buildId, executeCount)
        )
    }

    fun batchSave(
        transactionContext: DSLContext?,
        model: BuildRecordModel,
        stageList: List<BuildRecordStage>,
        containerList: List<BuildRecordContainer>,
        taskList: List<BuildRecordTask>
    ) {
        recordModelDao.createRecord(transactionContext ?: dslContext, model)
        recordStageDao.batchSave(transactionContext ?: dslContext, stageList)
        recordTaskDao.batchSave(transactionContext ?: dslContext, taskList)
        recordContainerDao.batchSave(transactionContext ?: dslContext, containerList)
    }

    private fun checkPassDays(startTime: Long?): Boolean {
        if (retryLimitDays < 0 || startTime == null) {
            return true
        }
        return (System.currentTimeMillis() - startTime) < TimeUnit.DAYS.toMillis(retryLimitDays.toLong())
    }

    /**
     * 查询ModelRecord
     * @param projectId: 项目Id
     * @param buildId: 构建Id
     * @param refreshStatus: 是否刷新状态
     */
    fun get(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?,
        refreshStatus: Boolean = true
    ): ModelRecord? {

        val buildInfo = pipelineBuildDao.convert(
            pipelineBuildDao.getBuildInfo(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId
            )
        ) ?: return null

        // 获取流水线级别变量数据
        var fixedExecuteCount = executeCount ?: buildInfo.executeCount ?: 1
        val buildRecordPipeline = recordModelDao.getRecord(
            dslContext, projectId, pipelineId, buildId, fixedExecuteCount
        )

        // TODO #7983 当不传executeCount时使用原detail接口暂时兼容，后续全部切换到record进行
        val model = executeCount?.let {
            val resourceStr = pipelineResDao.getVersionModelString(
                dslContext, projectId, pipelineId, buildInfo.version
            )
            try {
                val recordMap = recordModelService.generateFieldRecordModelMap(
                    projectId, pipelineId, buildId, fixedExecuteCount, buildRecordPipeline!!
                )
                ModelUtils.generatePipelineBuildModel(
                    baseModelMap = JsonUtil.getObjectMapper().readValue(resourceStr!!),
                    modelFieldRecordMap = recordMap
                )
            } catch (t: Throwable) {
                logger.warn("RECORD|parse record($buildId)-$executeCount with error: ", t)
                // 遇到解析问题直接返回最新记录，表现为前端无法切换
                fixedExecuteCount = buildInfo.executeCount ?: executeCount
                null
            }
        } ?: run {
            // TODO #7983 临时填充流水线级的timeCost
            val detail = pipelineBuildDetailService.getBuildModel(projectId, buildId) ?: return null
            buildRecordPipeline?.modelVar?.get(Model::timeCost.name)?.let {
                detail.timeCost = JsonUtil.anyTo(it, object : TypeReference<BuildRecordTimeCost>() {})
            }
            detail
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId, buildInfo.pipelineId
        ) ?: return null

        val buildSummaryRecord = pipelineBuildSummaryDao.get(dslContext, projectId, buildInfo.pipelineId)

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
                var elementElapsed = 0L
                container.elements.forEach { element ->
                    element.timeCost?.executeCost?.let {
                        element.elapsed = it
                        elementElapsed += it
                    }
                }
                container.elementElapsed = container.elementElapsed ?: elementElapsed
                container.systemElapsed = container.systemElapsed ?: container.timeCost?.systemCost
            }
            stage.elapsed = stage.elapsed ?: stage.timeCost?.totalCost
        }
        val triggerReviewers = pipelineTriggerReviewDao.getTriggerReviewers(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineInfo.pipelineId,
            buildId = buildId
        )

        val startUserList = recordModelDao.getRecordStartUserList(
            dslContext = dslContext,
            pipelineId = pipelineInfo.pipelineId,
            projectId = projectId,
            buildId = buildId
        )

        return ModelRecord(
            id = buildInfo.buildId,
            pipelineId = buildInfo.pipelineId,
            pipelineName = model.name,
            userId = buildInfo.startUser ?: "",
            triggerUser = buildInfo.triggerUser,
            trigger = StartType.toReadableString(buildInfo.trigger, buildInfo.channelCode),
            queueTime = buildInfo.queueTime,
            startTime = buildInfo.startTime ?: LocalDateTime.now().timestampmilli(),
            endTime = buildInfo.endTime,
            status = buildInfo.status.name,
            model = model,
            currentTimestamp = System.currentTimeMillis(),
            buildNum = buildInfo.buildNum,
            cancelUserId = buildRecordPipeline?.cancelUser,
            curVersion = buildInfo.version,
            latestVersion = pipelineInfo.version,
            latestBuildNum = buildSummaryRecord?.buildNum ?: -1,
            lastModifyUser = pipelineInfo.lastModifyUser,
            executeTime = buildInfo.executeTime,
            errorInfoList = buildInfo.errorInfoList,
            triggerReviewers = triggerReviewers,
            executeCount = fixedExecuteCount,
            startUserList = startUserList,
            buildMsg = buildInfo.buildMsg,
            material = buildInfo.material,
            remark = buildInfo.remark,
            webhookInfo = buildInfo.webhookInfo
        )
    }

    // TODO #7983 代替detail能力
    fun updateModel(projectId: String, buildId: String, model: Model) {
//        buildDetailDao.update(
//            dslContext = dslContext,
//            projectId = projectId,
//            buildId = buildId,
//            model = JsonUtil.toJson(model, formatted = false),
//            buildStatus = BuildStatus.RUNNING
//        )
//        pipelineDetailChangeEvent(projectId, buildId)
    }

    fun buildCancel(
        projectId: String,
        pipelineId: String,
        buildId: String,
        buildStatus: BuildStatus,
        cancelUser: String
    ) {
        // TODO #7983 修正所有Record状态，自行获取最新executeCount
        logger.info("Cancel the build $buildId by $cancelUser")
//        update(
//            projectId = projectId, buildId = buildId,
//            modelInterface = object : ModelInterface {
//
//                var update = false
//
//                override fun onFindStage(stage: Stage, model: Model): Traverse {
//                    if (BuildStatus.parse(stage.status).isRunning()) {
//                        stage.status = buildStatus.name
//                        if (stage.startEpoch == null) {
//                            stage.elapsed = 0
//                        } else {
//                            stage.elapsed = System.currentTimeMillis() - stage.startEpoch!!
//                        }
//                        update = true
//                    }
//                    return Traverse.CONTINUE
//                }
//
//                override fun onFindContainer(container: Container, stage: Stage): Traverse {
//                    val status = BuildStatus.parse(container.status)
//                    if (status == BuildStatus.PREPARE_ENV) {
//                        if (container.startEpoch == null) {
//                            container.systemElapsed = 0
//                        } else {
//                            container.systemElapsed = System.currentTimeMillis() - container.startEpoch!!
//                        }
//                        update = true
//                    }
//                    // #3138 状态实时刷新
//                    val refreshFlag = status.isRunning() && container.elements[0].status.isNullOrBlank() &&
//                        container.containPostTaskFlag != true
//                    if (status == BuildStatus.PREPARE_ENV || refreshFlag) {
//                        ContainerUtils.clearQueueContainerName(container)
//                        container.status = buildStatus.name
//                    }
//                    return Traverse.CONTINUE
//                }
//
//                override fun onFindElement(index: Int, e: Element, c: Container): Traverse {
//                    if (e.status == BuildStatus.RUNNING.name || e.status == BuildStatus.REVIEWING.name) {
//                        val status = if (e.status == BuildStatus.RUNNING.name) {
//                            val runCondition = e.additionalOptions?.runCondition
//                            // 当task的runCondition为PRE_TASK_FAILED_EVEN_CANCEL，点击取消还需要运行
//                            if (runCondition == RunCondition.PRE_TASK_FAILED_EVEN_CANCEL) {
//                                BuildStatus.RUNNING.name
//                            } else {
//                                BuildStatus.CANCELED.name
//                            }
//                        } else buildStatus.name
//                        e.status = status
//                        if (c.containPostTaskFlag != true) {
//                            c.status = status
//                        }
//                        if (BuildStatus.parse(status).isFinish()) {
//                            if (e.startEpoch != null) {
//                                e.elapsed = System.currentTimeMillis() - e.startEpoch!!
//                            }
//                            var elementElapsed = 0L
//                            run lit@{
//                                c.elements.forEach {
//                                    elementElapsed += it.elapsed ?: 0
//                                    if (it == e) {
//                                        return@lit
//                                    }
//                                }
//                            }
//
//                            c.elementElapsed = elementElapsed
//                        }
//
//                        update = true
//                    }
//                    return Traverse.CONTINUE
//                }
//
//                override fun needUpdate(): Boolean {
//                    return update
//                }
//            },
//            buildStatus = BuildStatus.RUNNING, cancelUser = cancelUser, operation = "buildCancel"
//        )
    }

    fun buildEnd(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        errorMsg: String?
    ): List<BuildStageStatus> {
        logger.info("[$buildId]|BUILD_END|buildStatus=$buildStatus")
        var allStageStatus: List<BuildStageStatus> = emptyList()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val recordModel = recordModelDao.getRecord(dslContext, projectId, pipelineId, buildId, executeCount) ?: run {
                logger.warn(
                    "ENGINE|$buildId|buildEnd| get model($buildId) record failed."
                )
                return@transaction
            }
            val buildInfo = pipelineBuildDao.convert(
                pipelineBuildDao.getBuildInfo(
                    dslContext = context,
                    projectId = projectId,
                    buildId = buildId
                )
            ) ?: run {
                logger.warn(
                    "ENGINE|$buildId|buildEnd| get build ($buildId) info failed."
                )
                return@transaction
            }
            val recordStages = recordStageDao.getRecords(
                context, projectId, pipelineId, buildId, executeCount
            )
            val buildStageMap = buildStageDao.getByBuildId(
                dslContext = context, projectId = projectId, buildId = buildId
            ).associateBy { it.stageId }
            val stagePairs = recordStages.map { stage ->
                stage to buildStageMap[stage.stageId]
            }
            val modelVar = mutableMapOf<String, Any>()
            modelVar[Model::timeCost.name] = BuildTimeCostUtils.generateBuildTimeCost(
                buildInfo, stagePairs
            )
            recordModelDao.updateRecord(
                context, projectId, pipelineId, buildId, executeCount, buildStatus,
                recordModel.modelVar.plus(modelVar), null, null, LocalDateTime.now(), null
            )
        }

        return allStageStatus
    }

    fun updateBuildCancelUser(projectId: String, buildId: String, cancelUserId: String) {
        recordModelDao.updateBuildCancelUser(
            dslContext = dslContext,
            projectId = projectId,
            buildId = buildId,
            cancelUser = cancelUserId
        )
    }

    fun saveBuildVmInfo(projectId: String, pipelineId: String, buildId: String, containerId: String, vmInfo: VmInfo) {
//        update(
//            projectId = projectId,
//            buildId = buildId,
//            modelInterface = object : ModelInterface {
//                var update = false
//
//                override fun onFindContainer(container: Container, stage: Stage): Traverse {
//                    val targetContainer = container.getContainerById(containerId)
//                    if (targetContainer != null) {
//                        if (targetContainer is VMBuildContainer && targetContainer.showBuildResource == true) {
//                            targetContainer.name = vmInfo.name
//                        }
//                        update = true
//                        return Traverse.BREAK
//                    }
//                    return Traverse.CONTINUE
//                }
//
//                override fun needUpdate(): Boolean {
//                    return update
//                }
//            },
//            buildStatus = BuildStatus.RUNNING,
//            operation = "saveBuildVmInfo($projectId,$pipelineId)"
//        )
    }
}
