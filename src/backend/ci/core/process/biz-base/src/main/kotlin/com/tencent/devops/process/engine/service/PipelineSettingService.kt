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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

@Service
class PipelineSettingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineSettingService::class.java)
        private const val PIPELINE_CURRENT_DAY_BUILD_COUNT_KEY_PREFIX = "PIPELINE_CURRENT_DAY_BUILD_COUNT"
    }

    fun isQueueTimeout(projectId: String, pipelineId: String, startTime: Long): Boolean {
        val setting = pipelineSettingDao.getSetting(dslContext, projectId, pipelineId)
        val waitQueueTimeMills =
            when {
                setting == null -> {
                    TimeUnit.HOURS.toMillis(1)
                }
                setting.runLockType == PipelineRunLockType.toValue(PipelineRunLockType.SINGLE) ||
                    setting.runLockType == PipelineRunLockType.toValue(PipelineRunLockType.GROUP_LOCK) -> {
                    TimeUnit.SECONDS.toMillis(setting.waitQueueTimeSecond.toLong())
                }
                else -> {
                    TimeUnit.HOURS.toMillis(1)
                }
            }
        return System.currentTimeMillis() - startTime > waitQueueTimeMills
    }

    fun getCurrentDayBuildCount(projectId: String, pipelineId: String): Int {
        val currentDayStr = DateTimeUtil.formatDate(Date(), DateTimeUtil.YYYY_MM_DD)
        val currentDayBuildCountKey = getCurrentDayBuildCountKey(pipelineId, currentDayStr)
        // 判断缓存中是否有值，没有值则从db中实时查
        return if (!redisOperation.hasKey(currentDayBuildCountKey)) {
            logger.info("getCurrentDayBuildCount $currentDayBuildCountKey is not exist!")
            getCurrentDayBuildCountFromDb(
                transactionContext = dslContext,
                projectId = projectId,
                currentDayStr = currentDayStr,
                pipelineId = pipelineId
            )
        } else {
            redisOperation.get(currentDayBuildCountKey)!!.toInt()
        }
    }

    fun setCurrentDayBuildCount(transactionContext: DSLContext?, projectId: String, pipelineId: String): Int {
        val currentDayStr = DateTimeUtil.formatDate(Date(), DateTimeUtil.YYYY_MM_DD)
        val currentDayBuildCountKey = getCurrentDayBuildCountKey(pipelineId, currentDayStr)
        // 判断缓存中是否有值，没有值则从db中实时查
        if (!redisOperation.hasKey(currentDayBuildCountKey)) {
            logger.info("setCurrentDayBuildCount $currentDayBuildCountKey is not exist!")
            getCurrentDayBuildCountFromDb(
                transactionContext = transactionContext ?: dslContext,
                projectId = projectId,
                currentDayStr = currentDayStr,
                pipelineId = pipelineId
            )
        }
        // redis有值则每次自增1
        return redisOperation.increment(currentDayBuildCountKey, 1)?.toInt() ?: 1
    }

    private fun getCurrentDayBuildCountKey(pipelineId: String, currentDayStr: String): String {
        return "$PIPELINE_CURRENT_DAY_BUILD_COUNT_KEY_PREFIX:$pipelineId:$currentDayStr"
    }

    private fun getCurrentDayBuildCountFromDb(
        transactionContext: DSLContext,
        projectId: String,
        currentDayStr: String,
        pipelineId: String
    ): Int {
        val startTime = DateTimeUtil.stringToLocalDateTime(
            dateTimeStr = currentDayStr,
            formatStr = DateTimeUtil.YYYY_MM_DD
        )
        val endTime = DateTimeUtil.convertDateToLocalDateTime(
            DateTimeUtil.getFutureDate(
                localDateTime = startTime,
                unit = Calendar.DAY_OF_MONTH,
                timeSpan = 1
            )
        )
        val count = pipelineBuildDao.countBuildNumByTime(
            dslContext = transactionContext,
            projectId = projectId,
            pipelineId = pipelineId,
            startTime = startTime,
            endTime = endTime
        )
        // 把当前流水线当日构建次数存入redis，失效期设置为1天
        redisOperation.set(
            key = getCurrentDayBuildCountKey(pipelineId, currentDayStr),
            value = count.toString(),
            expiredInSecond = TimeUnit.DAYS.toSeconds(1)
        )
        return count
    }
}
