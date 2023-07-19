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

import com.google.common.base.Preconditions
import com.tencent.devops.common.api.constant.BUILD_CANCELED
import com.tencent.devops.common.api.constant.BUILD_COMPLETED
import com.tencent.devops.common.api.constant.BUILD_FAILED
import com.tencent.devops.common.api.constant.BUILD_REVIEWING
import com.tencent.devops.common.api.constant.BUILD_RUNNING
import com.tencent.devops.common.api.constant.BUILD_STAGE_SUCCESS
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.control.lock.PipelineBuildDetailLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.service.StageTagService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

open class BaseBuildDetailService constructor(
    val dslContext: DSLContext,
    val pipelineBuildDao: PipelineBuildDao,
    val buildDetailDao: BuildDetailDao,
    private val stageTagService: StageTagService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    val redisOperation: RedisOperation
) {
    val logger = LoggerFactory.getLogger(BaseBuildDetailService::class.java)!!

    companion object {
        const val TRIGGER_STAGE = "stage-1"
    }

    fun getBuildModel(projectId: String, buildId: String): Model? {
        val record = buildDetailDao.get(dslContext, projectId, buildId) ?: return null
        return JsonUtil.to(record.model, Model::class.java)
    }

    @Suppress("LongParameterList", "ComplexMethod")
    protected fun update(
        projectId: String,
        buildId: String,
        modelInterface: ModelInterface,
        buildStatus: BuildStatus,
        cancelUser: String? = null,
        operation: String = ""
    ): Model {
        val watcher = Watcher(id = "updateDetail#$buildId#$operation")
        var message = "nothing"
        var record: TPipelineBuildDetailRecord? = null // 在异常catch处共享，减少一次db查询
        val lock = PipelineBuildDetailLock(redisOperation, buildId)

        try {
            watcher.start("lock")
            lock.lock()

            watcher.start("getDetail")
            record = buildDetailDao.get(dslContext, projectId, buildId)
            Preconditions.checkArgument(record != null, "The build detail is not exist")

            watcher.start("model")
            val model = JsonUtil.to(record!!.model, Model::class.java)

            watcher.start("traverseModel")
            traverseModel(model, modelInterface)
            watcher.stop()

            val modelStr: String? = if (!modelInterface.needUpdate()) {
                null
            } else {
                watcher.start("toJson")
                JsonUtil.toJson(model, formatted = false)
            }

            val (change, finalStatus) = takeBuildStatus(record, buildStatus)
            if (modelStr.isNullOrBlank() && !change) {
                message = "Will not update"
                return model
            }

            watcher.start("updateModel")
            buildDetailDao.update(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                model = modelStr,
                buildStatus = finalStatus,
                cancelUser = cancelUser // 系统行为导致的取消状态(仅当在取消状态时，还没有设置过取消人，才默认为System)
                    ?: if (buildStatus.isCancel() && record.cancelUser.isNullOrBlank()) "System" else null
            )

            message = "update done"
            return model
        } catch (ignored: Throwable) {
            message = ignored.message ?: ""
            logger.warn("[$buildId]| Fail to update the build detail: ${ignored.message}", ignored)
            watcher.start("getDetail")
            Preconditions.checkArgument(record != null, "The build detail is not exist")
            watcher.start("model")
            return JsonUtil.to(record!!.model, Model::class.java)
        } finally {
            lock.unlock()
//            logger.info("[$buildId|$buildStatus]|$operation|update_detail_model| $message")
//            if (message == "update done") { // 防止MQ异常导致锁时间过长，将推送事件移出锁定范围
//                watcher.start("dispatchEvent")
//                pipelineDetailChangeEvent(projectId, buildId)
//            }
            LogUtils.printCostTimeWE(watcher)
        }
    }

    protected fun fetchHistoryStageStatus(
        model: Model,
        buildStatus: BuildStatus,
        reviewers: List<String>? = null,
        errorMsg: String? = null,
        cancelUser: String? = null
    ): List<BuildStageStatus> {
        val stageTagMap: Map<String, String>
            by lazy { stageTagService.getAllStageTag().data!!.associate { it.id to it.stageTagName } }
        // 更新Stage状态至BuildHistory

        val (statusMessage, reason) = when {
            buildStatus == BuildStatus.REVIEWING -> Pair(BUILD_REVIEWING, reviewers?.joinToString(","))
            buildStatus == BuildStatus.STAGE_SUCCESS -> Pair(BUILD_STAGE_SUCCESS, null)
            buildStatus.isFailure() -> Pair(BUILD_FAILED, errorMsg ?: buildStatus.name)
            buildStatus.isCancel() -> Pair(BUILD_CANCELED, cancelUser)
            buildStatus.isSuccess() -> Pair(BUILD_COMPLETED, null)
            else -> Pair(BUILD_RUNNING, null)
        }
        return model.stages.map { stage ->
            BuildStageStatus(
                stageId = stage.id!!,
                name = stage.name ?: stage.id!!,
                status = stage.status,
                startEpoch = stage.startEpoch,
                elapsed = stage.elapsed,
                tag = stage.tag?.map { tag ->
                    stageTagMap.getOrDefault(tag, "null")
                },
                // #6655 利用stageStatus中的第一个stage传递构建的状态信息
                showMsg = if (stage.id == TRIGGER_STAGE) {
                    I18nUtil.getCodeLanMessage(statusMessage) + (reason?.let { ": $reason" } ?: "")
                } else null
            )
        }
    }

    private fun takeBuildStatus(
        record: TPipelineBuildDetailRecord,
        buildStatus: BuildStatus
    ): Pair<Boolean, BuildStatus> {
        val oldStatus = BuildStatus.parse(record.status)
        return if (!oldStatus.isFinish()) {
            true to buildStatus
        } else {
            false to oldStatus
        }
    }

    @Suppress("NestedBlockDepth", "ReturnCount", "ComplexMethod")
    private fun traverseModel(model: Model, modelInterface: ModelInterface) {
        // 不应该过滤掉第一个Stage，交由子类决定
        for (stage in model.stages) {
            val traverse = modelInterface.onFindStage(stage, model)
            if (Traverse.BREAK == traverse) {
                return
            } else if (Traverse.SKIP == traverse) {
                continue
            }

            for (container in stage.containers) {
                val cTraverse = modelInterface.onFindContainer(container, stage)
                if (Traverse.BREAK == cTraverse) {
                    return
                } else if (Traverse.SKIP == cTraverse) {
                    continue
                }

                container.elements.forEachIndexed { index, e ->
                    if (Traverse.BREAK == modelInterface.onFindElement(index, e, container)) {
                        return
                    }
                }

                // 进入矩阵组内做遍历查询
                if (container.matrixGroupFlag == true) {
                    for (groupContainer in container.fetchGroupContainers() ?: emptyList()) {
                        val gTraverse = modelInterface.onFindContainer(groupContainer, stage)
                        if (Traverse.BREAK == gTraverse) {
                            return
                        } else if (Traverse.SKIP == gTraverse) {
                            continue
                        }

                        groupContainer.elements.forEachIndexed { index, e ->
                            if (Traverse.BREAK == modelInterface.onFindElement(index, e, groupContainer)) {
                                return
                            }
                        }
                    }
                }
            }
        }
    }

    protected fun pipelineDetailChangeEvent(projectId: String, buildId: String) {
        val pipelineBuildInfo = pipelineBuildDao.getBuildInfo(dslContext, projectId, buildId) ?: return
        // 异步转发，解耦核心
        pipelineEventDispatcher.dispatch(
            PipelineBuildWebSocketPushEvent(
                source = "recordDetail",
                projectId = pipelineBuildInfo.projectId,
                pipelineId = pipelineBuildInfo.pipelineId,
                userId = pipelineBuildInfo.startUser,
                buildId = buildId,
                refreshTypes = RefreshType.DETAIL.binary or RefreshType.RECORD.binary,
                executeCount = pipelineBuildInfo.executeCount
            )
        )
    }

    protected interface ModelInterface {

        fun onFindStage(stage: Stage, model: Model) = Traverse.CONTINUE

        fun onFindContainer(container: Container, stage: Stage) = Traverse.CONTINUE

        fun onFindElement(index: Int, e: Element, c: Container) = Traverse.CONTINUE

        fun needUpdate(): Boolean
    }

    enum class Traverse { BREAK, CONTINUE, SKIP }
}
