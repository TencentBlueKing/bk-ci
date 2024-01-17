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

package com.tencent.devops.process.plugin.trigger.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.process.tables.records.TPipelineTimerRecord
import com.tencent.devops.process.constant.ProcessMessageCode.ADD_PIPELINE_TIMER_TRIGGER_SAVE_FAIL
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_DEL_PIPELINE_TIMER
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_SAVE_PIPELINE_TIMER
import com.tencent.devops.process.engine.pojo.PipelineTimer
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
        channelCode: ChannelCode
    ): Result<Boolean> {
        val crontabJson = JsonUtil.toJson(crontabExpressions, formatted = false)
        return if (0 < pipelineTimerDao.save(dslContext, projectId, pipelineId, userId, crontabJson, channelCode)) {
            pipelineEventDispatcher.dispatch(
                PipelineTimerChangeEvent(
                    source = "saveTimer",
                    projectId = projectId,
                    pipelineId = pipelineId,
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
                    userId = userId,
                    crontabExpressionJson = crontabJson,
                    actionType = ActionType.TERMINATE
                )
            )
            Result(
                ERROR_SAVE_PIPELINE_TIMER.toInt(),
                MessageUtil.getMessageByLocale(
                    ADD_PIPELINE_TIMER_TRIGGER_SAVE_FAIL,
                    I18nUtil.getLanguage(userId))
            )
        }
    }

    open fun deleteTimer(projectId: String, pipelineId: String, userId: String): Result<Boolean> {
        var count = 0
        val timerRecord = pipelineTimerDao.get(dslContext, projectId, pipelineId)
        if (timerRecord != null) {
            count = pipelineTimerDao.delete(dslContext, projectId, pipelineId)
            // 终止定时器
            pipelineEventDispatcher.dispatch(
                PipelineTimerChangeEvent(
                    source = "deleteTimer",
                    projectId = timerRecord.projectId,
                    pipelineId = pipelineId,
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

    open fun get(projectId: String, pipelineId: String): PipelineTimer? {
        val timerRecord = pipelineTimerDao.get(dslContext, projectId, pipelineId) ?: return null
        return convert(timerRecord)
    }

    private fun convert(timerRecord: TPipelineTimerRecord): PipelineTimer? {
        return PipelineTimer(
            projectId = timerRecord.projectId,
            pipelineId = timerRecord.pipelineId,
            startUser = timerRecord.creator,
            crontabExpressions = try {
                JsonUtil.to(timerRecord.crontab, object : TypeReference<List<String>>() {})
            } catch (ignored: Throwable) {
                listOf(timerRecord.crontab)
            },
            channelCode = try {
                ChannelCode.valueOf(timerRecord.channel)
            } catch (e: IllegalArgumentException) {
                logger.warn("Unkown channel code", e)
                return null
            }

        )
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
}
