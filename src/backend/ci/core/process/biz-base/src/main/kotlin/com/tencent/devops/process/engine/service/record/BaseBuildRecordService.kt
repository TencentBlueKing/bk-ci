/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.api.constant.BUILD_CANCELED
import com.tencent.devops.common.api.constant.BUILD_COMPLETED
import com.tencent.devops.common.api.constant.BUILD_FAILED
import com.tencent.devops.common.api.constant.BUILD_REVIEWING
import com.tencent.devops.common.api.constant.BUILD_RUNNING
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.pipeline.utils.ModelUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.websocket.enum.RefreshType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.engine.control.lock.PipelineBuildRecordLock
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineResourceDao
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.event.PipelineBuildWebSocketPushEvent
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.pojo.BuildStageStatus
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.MergeBuildRecordParam
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.record.PipelineRecordModelService
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PROJECT_ID
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Response

@Suppress("LongParameterList", "MagicNumber", "ReturnCount", "ComplexMethod")
open class BaseBuildRecordService(
    private val dslContext: DSLContext,
    private val buildRecordModelDao: BuildRecordModelDao,
    val pipelineBuildDao: PipelineBuildDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val stageTagService: StageTagService,
    private val recordModelService: PipelineRecordModelService,
    private val pipelineResourceDao: PipelineResourceDao,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao
) {

    protected fun update(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int,
        buildStatus: BuildStatus,
        cancelUser: String? = null,
        operation: String = "",
        refreshOperation: () -> Unit
    ) {
        val watcher = Watcher(id = "updateRecord#$buildId#$operation")
        var message = "nothing"
        val lock = PipelineBuildRecordLock(redisOperation, buildId, executeCount)
        var startUser: String? = null
        try {
            watcher.start("lock")
            lock.lock()

            watcher.start("getRecord")
            val record = buildRecordModelDao.getRecord(
                dslContext = dslContext, projectId = projectId, pipelineId = pipelineId,
                buildId = buildId, executeCount = executeCount
            ) ?: run {
                message = "Model record is empty"
                return
            }
            startUser = record.startUser

            watcher.start("refreshOperation")
            refreshOperation()
            watcher.stop()

            watcher.start("updatePipelineRecord")
            val (change, finalStatus) = takeBuildStatus(record, buildStatus)
            if (!change && cancelUser.isNullOrBlank()) {
                message = "Build status did not change"
                return
            }
            buildRecordModelDao.updateRecord(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = executeCount,
                buildStatus = finalStatus,
                modelVar = record.modelVar, // 暂时没有变量，保留修改可能
                startTime = null,
                endTime = null,
                errorInfoList = null,
                cancelUser = cancelUser // 系统行为导致的取消状态(仅当在取消状态时，还没有设置过取消人，才默认为System)
                    ?: if (buildStatus.isCancel() && record.cancelUser.isNullOrBlank()) "System" else null,
                timestamps = null
            )
            message = "Will not update"
        } catch (ignored: Throwable) {
            message = ignored.message ?: ""
            logger.warn("[$buildId]| Fail to update the build record: ${ignored.message}", ignored)
        } finally {
            lock.unlock()
            logger.info("[$buildId|$buildStatus]|$operation|update_detail_record| $message")
            watcher.start("dispatchEvent")
            pipelineRecordChangeEvent(projectId, pipelineId, buildId, startUser, executeCount)
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
        return
    }

    fun getRecordModel(
        projectId: String,
        pipelineId: String,
        version: Int,
        buildId: String,
        executeCount: Int? = null,
        queryDslContext: DSLContext? = null,
        debug: Boolean? = null
    ): Model? {
        val fixedExecuteCount = fixedExecuteCount(
            projectId = projectId,
            buildId = buildId,
            executeCount = executeCount,
            queryDslContext = queryDslContext
        )
        val buildRecordModel = buildRecordModelDao.getRecord(
            dslContext = queryDslContext ?: dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = fixedExecuteCount
        )
        return if (buildRecordModel != null) {
            getRecordModel(
                projectId = projectId,
                pipelineId = pipelineId,
                version = version,
                buildId = buildId,
                fixedExecuteCount = fixedExecuteCount,
                buildRecordModel = buildRecordModel,
                queryDslContext = queryDslContext,
                debug = debug
            )
        } else {
            null
        }
    }

    fun fixedExecuteCount(
        projectId: String,
        buildId: String,
        executeCount: Int?,
        queryDslContext: DSLContext? = null,
        buildInfo: BuildInfo? = null
    ): Int {
        return if (executeCount == null || executeCount < 1) {
            val dbBuildInfo = buildInfo ?: pipelineBuildDao.getBuildInfo(
                dslContext = queryDslContext ?: dslContext, projectId = projectId, buildId = buildId
            ) ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
            logger.warn(
                "[$buildId]|getRecordModel|the parameter executeCount($executeCount) passed in is " +
                        "invalid (null or <1). query the latest executeCount(${dbBuildInfo.executeCount}) from " +
                        "the database for correction."
            )
            dbBuildInfo.executeCount
        } else {
            executeCount
        }
    }

    fun getRecordModel(
        projectId: String,
        pipelineId: String,
        version: Int,
        buildId: String,
        fixedExecuteCount: Int,
        buildRecordModel: BuildRecordModel,
        queryDslContext: DSLContext? = null,
        debug: Boolean? = null
    ): Model? {
        val watcher = Watcher(id = "getRecordModel#$buildId")
        watcher.start("getVersionModelString")
        val finalDSLContext = queryDslContext ?: dslContext
        // 当debug没有传的时候，从数据库中获取数据判断是否是调试产生的构建
        val debugFlag = debug ?: pipelineBuildDao.getDebugFlag(finalDSLContext, projectId, buildId)
        val resourceStr = if (debugFlag) {
            pipelineBuildDao.getDebugResourceStr(
                dslContext = finalDSLContext,
                projectId = projectId,
                buildId = buildId
            )
        } else {
            pipelineResourceVersionDao.getVersionModelString(
                dslContext = finalDSLContext,
                projectId = projectId,
                pipelineId = pipelineId,
                version = version
            )
        } ?: pipelineResourceDao.getVersionModelString(
            dslContext = finalDSLContext,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
            params = arrayOf("$KEY_PROJECT_ID:$projectId,$KEY_PIPELINE_ID:$pipelineId,$KEY_VERSION:$version")
        )
        var recordMap: Map<String, Any>? = null
        return try {
            watcher.start("fillElementWhenNewBuild")
            val fullModel = JsonUtil.to(resourceStr, Model::class.java)
            fullModel.stages.forEach {
                PipelineUtils.transformUserIllegalReviewParams(it.checkIn?.reviewParams)
            }
            val baseModelMap = JsonUtil.toMutableMap(bean = fullModel, skipEmpty = false)
            val mergeBuildRecordParam = MergeBuildRecordParam(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                executeCount = fixedExecuteCount,
                recordModelMap = buildRecordModel.modelVar,
                pipelineBaseModelMap = baseModelMap
            )
            watcher.start("generateFieldRecordModelMap")
            recordMap = recordModelService.generateFieldRecordModelMap(mergeBuildRecordParam, queryDslContext)
            watcher.start("generatePipelineBuildModel")
            ModelUtils.generatePipelineBuildModel(
                baseModelMap = baseModelMap,
                modelFieldRecordMap = recordMap
            )
        } catch (ignore: Throwable) {
            logger.warn(
                "RECORD|parse record with error|$projectId|$pipelineId|$buildId|$fixedExecuteCount" +
                    "|recordMap: ${JsonUtil.toJson(recordMap ?: "")}",
                ignore
            )
            null
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

    private fun takeBuildStatus(
        record: BuildRecordModel,
        buildStatus: BuildStatus
    ): Pair<Boolean, BuildStatus> {
        val oldStatus = BuildStatus.parse(record.status)
        return if (!oldStatus.isFinish()) {
            (oldStatus != buildStatus) to buildStatus
        } else {
            false to oldStatus
        }
    }

    protected fun pipelineRecordChangeEvent(
        projectId: String,
        pipelineId: String,
        buildId: String,
        startUser: String?,
        executeCount: Int
    ) {
        val userId = startUser
            ?: pipelineBuildDao.getUserBuildInfo(dslContext, projectId, buildId)?.startUser
            ?: return
        pipelineEventDispatcher.dispatch(
            PipelineBuildWebSocketPushEvent(
                source = "recordChange",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                executeCount = executeCount,
                refreshTypes = RefreshType.RECORD.binary
            )
        )
    }

    protected fun fetchHistoryStageStatus(
        recordStages: List<BuildRecordStage>,
        buildStatus: BuildStatus,
        timeCost: BuildRecordTimeCost? = null,
        reviewers: List<String>? = null,
        errorMsg: String? = null,
        cancelUser: String? = null
    ): List<BuildStageStatus> {
        val stageTagMap: Map<String, String> by lazy {
            stageTagService.getAllStageTag().data!!.associate { it.id to it.stageTagName }
        }
        // 更新Stage状态至BuildHistory
        val (statusMessage, reason) = if (buildStatus == BuildStatus.REVIEWING) {
            Pair(BUILD_REVIEWING, reviewers?.joinToString(","))
        } else if (buildStatus.isFailure()) {
            Pair(BUILD_FAILED, errorMsg ?: buildStatus.name)
        } else if (buildStatus.isCancel()) {
            Pair(BUILD_CANCELED, cancelUser)
        } else if (buildStatus.isSuccess()) {
            Pair(BUILD_COMPLETED, null)
        } else {
            Pair(BUILD_RUNNING, null)
        }
        return recordStages.map {
            BuildStageStatus(
                stageId = it.stageId,
                name = it.stageVar[Stage::name.name]?.toString() ?: it.stageId,
                status = it.status,
                startEpoch = it.stageVar[Stage::startEpoch.name]?.toString()?.toLong(),
                elapsed = it.stageVar[Stage::elapsed.name]?.toString()?.toLong(),
                timeCost = timeCost,
                tag = it.stageVar[Stage::tag.name]?.let { tags ->
                    JsonUtil.anyTo(tags, object : TypeReference<List<String>>() {})
                        .map { tag -> stageTagMap.getOrDefault(tag, "null") }
                },
                // #6655 利用stageStatus中的第一个stage传递构建的状态信息
                showMsg = if (it.stageId == StageBuildRecordService.TRIGGER_STAGE) {
                    I18nUtil.getCodeLanMessage(statusMessage) + (reason?.let { ": $reason" } ?: "")
                } else null
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BaseBuildRecordService::class.java)

        fun mergeTimestamps(
            newTimestamps: Map<BuildTimestampType, BuildRecordTimeStamp>,
            oldTimestamps: Map<BuildTimestampType, BuildRecordTimeStamp>
        ): MutableMap<BuildTimestampType, BuildRecordTimeStamp> {
            // 针对各时间戳的开始结束时间分别写入，避免覆盖
            val result = mutableMapOf<BuildTimestampType, BuildRecordTimeStamp>()
            result.putAll(oldTimestamps)
            newTimestamps.forEach { (type, new) ->
                val old = oldTimestamps[type]
                result[type] = if (old != null) {
                    // 如果时间戳已存在，开始时间不变，则结束时间将新值覆盖旧值
                    BuildRecordTimeStamp(
                        startTime = old.startTime ?: new.startTime,
                        endTime = new.endTime ?: old.endTime
                    )
                } else {
                    // 如果时间戳不存在，则直接新增
                    new
                }
            }
            return result
        }
    }
}
