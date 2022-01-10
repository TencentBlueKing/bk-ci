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
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.model.process.tables.records.TPipelineBuildDetailRecord
import com.tencent.devops.process.dao.BuildDetailDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

open class BaseBuildDetailService constructor(
    val dslContext: DSLContext,
    val pipelineBuildDao: PipelineBuildDao,
    val buildDetailDao: BuildDetailDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    val redisOperation: RedisOperation

) {
    val logger = LoggerFactory.getLogger(BaseBuildDetailService::class.java)!!

    companion object {
        private const val ExpiredTimeInSeconds: Long = 10
    }

    fun getBuildModel(projectId: String, buildId: String): Model? {
        val record = buildDetailDao.get(dslContext, projectId, buildId) ?: return null
        return JsonUtil.to(record.model, Model::class.java)
    }

    @Suppress("LongParameterList")
    protected fun update(
        projectId: String,
        buildId: String,
        modelInterface: ModelInterface,
        buildStatus: BuildStatus,
        cancelUser: String? = null,
        operation: String = ""
    ) {
        val watcher = Watcher(id = "updateDetail#$buildId#$operation")
        var message = "nothing"
        val lock = RedisLock(redisOperation, "process.build.detail.lock.$buildId", ExpiredTimeInSeconds)

        try {
            watcher.start("lock")
            lock.lock()

            watcher.start("getDetail")
            val record = buildDetailDao.get(dslContext, projectId, buildId)
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
                return
            }

            watcher.start("updateModel")
            buildDetailDao.update(
                dslContext = dslContext,
                projectId = projectId,
                buildId = buildId,
                model = modelStr,
                buildStatus = finalStatus,
                cancelUser = cancelUser
            )

            watcher.start("dispatchEvent")
            pipelineDetailChangeEvent(projectId, buildId)
            message = "update done"
        } catch (ignored: Throwable) {
            message = ignored.message ?: ""
            logger.warn("[$buildId]| Fail to update the build detail: ${ignored.message}", ignored)
        } finally {
            lock.unlock()
            watcher.stop()
            logger.info("[$buildId|$buildStatus]|$operation|update_detail_model| $message")
            LogUtils.printCostTimeWE(watcher)
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
        for (i in 1 until model.stages.size) {
            val stage = model.stages[i]
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
                source = "pauseTask",
                projectId = pipelineBuildInfo.projectId,
                pipelineId = pipelineBuildInfo.pipelineId,
                userId = pipelineBuildInfo.startUser,
                buildId = buildId,
                refreshTypes = RefreshType.DETAIL.binary
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
