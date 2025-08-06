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

package com.tencent.devops.process.plugin.trigger.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.records.TPipelineTimerBranchRecord
import com.tencent.devops.model.process.tables.records.TPipelineTimerRecord
import com.tencent.devops.process.constant.ProcessMessageCode.ADD_PIPELINE_TIMER_TRIGGER_SAVE_FAIL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_DEL_PIPELINE_TIMER
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_SAVE_PIPELINE_TIMER
import com.tencent.devops.process.engine.pojo.PipelineTimer
import com.tencent.devops.process.plugin.trigger.dao.PipelineTimerBranchDao
import com.tencent.devops.process.plugin.trigger.dao.PipelineTimerDao
import com.tencent.devops.process.plugin.trigger.pojo.event.PipelineTimerChangeEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线定时服务
 * @version 1.0
 */
@Service
open class PipelineTimerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineTimerDao: PipelineTimerDao,
    private val pipelineTimerBranchDao: PipelineTimerBranchDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTimerService::class.java)
    }

    open fun saveTimer(
        projectId: String,
        pipelineId: String,
        userId: String,
        crontabExpressions: Set<String>,
        channelCode: ChannelCode,
        repoHashId: String?,
        branchs: Set<String>?,
        taskId: String,
        noScm: Boolean?,
        startParam: Map<String, String>?
    ): Result<Boolean> {
        val crontabJson = JsonUtil.toJson(crontabExpressions, formatted = false)
        return if (0 < pipelineTimerDao.save(
                dslContext,
                projectId,
                pipelineId,
                userId,
                crontabJson,
                channelCode,
                repoHashId,
                branchs?.let { JsonUtil.toJson(it) },
                noScm,
                startParam?.let { JsonUtil.toJson(it) },
                taskId
            )
        ) {
            pipelineEventDispatcher.dispatch(
                PipelineTimerChangeEvent(
                    source = "saveTimer",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = taskId,
                    userId = userId,
                    crontabExpressionJson = crontabJson
                )
            )
            Result(true)
        } else { // 终止定时器
            pipelineEventDispatcher.dispatch(
                PipelineTimerChangeEvent(
                    source = "saveTimer_fail",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = taskId,
                    userId = userId,
                    crontabExpressionJson = crontabJson,
                    actionType = ActionType.TERMINATE
                )
            )
            Result(
                ERROR_SAVE_PIPELINE_TIMER.toInt(),
                MessageUtil.getMessageByLocale(
                    ADD_PIPELINE_TIMER_TRIGGER_SAVE_FAIL,
                    I18nUtil.getLanguage(userId)
                )
            )
        }
    }

    open fun deleteTimer(projectId: String, pipelineId: String, userId: String, taskId: String?): Result<Boolean> {
        var count = 0
        val timerRecord = pipelineTimerDao.get(dslContext, projectId, pipelineId, taskId ?: "")
        if (timerRecord != null) {
            count = pipelineTimerDao.delete(dslContext, projectId, pipelineId, timerRecord.taskId)
            if (taskId.isNullOrBlank()) {
                // 删除旧的定时任务信息
                logger.info("clean the old timer record|$projectId|$pipelineId|$taskId|changeCount[$count]")
            }
            // 终止定时器
            pipelineEventDispatcher.dispatch(
                PipelineTimerChangeEvent(
                    source = "deleteTimer",
                    projectId = timerRecord.projectId,
                    pipelineId = pipelineId,
                    taskId = taskId,
                    userId = userId,
                    crontabExpressionJson = timerRecord.crontab,
                    actionType = ActionType.TERMINATE
                )
            )
        }
        return if (count > 0) Result(true)
        else Result(
            ERROR_DEL_PIPELINE_TIMER.toInt(),
            MessageUtil.getMessageByLocale(
                ERROR_DEL_PIPELINE_TIMER,
                I18nUtil.getLanguage(userId),
                arrayOf(pipelineId)
            )
        )
    }

    open fun get(projectId: String, pipelineId: String, taskId: String?): PipelineTimer? {
        val timerRecord = if (taskId.isNullOrBlank()) {
            // 如果taskId为空或空白，则尝试获取指定项目和流水线的定时器记录
            pipelineTimerDao.get(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = ""
            ) ?: pipelineTimerDao.get(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            ).let {
                if (it.size <= 1) {
                    it.firstOrNull()
                } else {
                    // 存在多条匹配的数据，无法判读取哪条，跳过
                    logger.warn("skipping|multiple records exist|$projectId|$pipelineId")
                    null
                }
            }
        } else {
            // 如果taskId不为空，则尝试获取指定taskId的定时器记录
            pipelineTimerDao.get(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId
            ) ?: pipelineTimerDao.get(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = ""
            )
        } ?: return null
        return convert(timerRecord)
    }

    private fun convert(timerRecord: TPipelineTimerRecord): PipelineTimer? {
        with(timerRecord) {
            return PipelineTimer(
                projectId = projectId,
                pipelineId = pipelineId,
                startUser = creator,
                crontabExpressions = try {
                    JsonUtil.to(crontab, object : TypeReference<List<String>>() {})
                } catch (ignored: Throwable) {
                    listOf(crontab)
                },
                channelCode = try {
                    ChannelCode.valueOf(channel)
                } catch (e: IllegalArgumentException) {
                    logger.warn("Unkown channel code", e)
                    return null
                },
                repoHashId = repoHashId,
                branchs = branchs?.let {
                    JsonUtil.to(it, object : TypeReference<List<String>>() {})
                },
                noScm = noScm,
                taskId = taskId,
                startParam = startParam?.let { JsonUtil.to(it, object : TypeReference<Map<String, String>>() {}) }
            )
        }
    }

    open fun list(start: Int, limit: Int): Result<Collection<PipelineTimer>> {
        if (start < 0) {
            return Result(emptyList())
        }
        val list = pipelineTimerDao.list(dslContext, start, limit)
        val timerList = mutableListOf<PipelineTimer>()
        list.forEach { record ->
            timerList.add(convert(record) ?: return@forEach)
        }
        return Result(timerList)
    }

    fun saveTimerBranch(
        projectId: String,
        pipelineId: String,
        taskId: String,
        repoHashId: String,
        branch: String,
        revision: String
    ) {
        pipelineTimerBranchDao.save(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = taskId,
            repoHashId = repoHashId,
            branch = branch,
            revision = revision
        )
    }

    fun getTimerBranch(
        projectId: String,
        pipelineId: String,
        taskId: String,
        repoHashId: String,
        branch: String
    ): TPipelineTimerBranchRecord? {
        return pipelineTimerBranchDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = taskId,
            repoHashId = repoHashId,
            branch = branch
        ) ?: pipelineTimerBranchDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = "",
            repoHashId = repoHashId,
            branch = branch
        )
    }

    fun getTimerBranch(
        projectId: String?,
        pipelineId: String?,
        limit: Int?,
        offset: Int?
    ): List<Pair<String, String>> {
        return pipelineTimerBranchDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            limit = limit,
            offset = offset
        )
    }

    fun getTimerBranch(
        projectId: String,
        pipelineId: String
    ): org.jooq.Result<TPipelineTimerBranchRecord> {
        return pipelineTimerBranchDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun deleteTimerBranch(
        projectId: String,
        pipelineId: String,
        repoHashId: String?,
        branch: String?,
        taskId: String?
    ): Int {
        return pipelineTimerBranchDao.delete(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            repoHashId = repoHashId,
            branch = branch,
            taskId = taskId
        )
    }

    fun updateTimerBranch(
        projectId: String,
        pipelineId: String,
        sourceRepoHashId: String?,
        sourceBranch: String?,
        sourceTaskId: String?,
        targetTaskId: String
    ): Int {
        return pipelineTimerBranchDao.updateTimerBranch(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            sourceRepoHashId = sourceRepoHashId,
            sourceBranch = sourceBranch,
            sourceTaskId = sourceTaskId,
            targetTaskId = targetTaskId
        )
    }

    fun listTimer(projectId: String, pipelineId: String): List<TPipelineTimerRecord> {
        return pipelineTimerDao.list(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    fun listPipeline(projectId: String?, pipelineId: String?, limit: Int, offset: Int): List<Pair<String, String>> {
        return pipelineTimerDao.listPipeline(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            limit = limit,
            offset = offset
        )
    }

    /**
     * 修改定时任务record，并更新quartz定时任务
     */
    fun updateTimer(
        projectId: String,
        pipelineId: String,
        taskId: String,
        userId: String,
        startParam: Map<String, String>?,
        crontabExpressionJson: String
    ): Result<Boolean> {
        return if (pipelineTimerDao.update(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId,
                startParam = startParam?.let {
                    JsonUtil.toJson(it, false)
                }
            ) > 0
        ) {
            pipelineEventDispatcher.dispatch(
                PipelineTimerChangeEvent(
                    source = "saveTimer",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    taskId = taskId,
                    userId = userId,
                    crontabExpressionJson = crontabExpressionJson
                )
            )
            return Result(true)
        } else Result(false)
    }

    fun cleanTimer(
        projectId: String,
        pipelineId: String
    ): Int {
        return pipelineTimerDao.delete(
            dslContext = dslContext,
            pipelineId = pipelineId,
            projectId = projectId
        )
    }
}
