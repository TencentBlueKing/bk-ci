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

package com.tencent.devops.stream.cron

import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.notify.utils.HashUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.stream.config.StreamSlaConfig
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class StreamActiveProjectsReportJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val streamSlaConfig: StreamSlaConfig,
    private val redisOperation: RedisOperation,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StreamActiveProjectsReportJob::class.java)
        private const val STREAM_ACTIVE_PROJECT_SLA_REPORT_KEY =
            "stream:active:project:sla:report"
    }
    @Scheduled(cron = "0 0 2 * * ?")
    fun reportActiveProjectsDaily() {

        // 增加逻辑判断：只在灰度环境执行
        if (!streamSlaConfig.switch.toBoolean()) {
            logger.info("StreamActiveProjectsReportJob|reportActiveProjectsDaily|switch is false , no start")
            return
        }
        if (illegalConfig()) {
            logger.info("StreamActiveProjectsReportJob|reportActiveProjectsDaily|some params is null")
            return
        }
        val redisLock = RedisLock(redisOperation, STREAM_ACTIVE_PROJECT_SLA_REPORT_KEY, 60L)
        try {
            logger.info("StreamActiveProjectsReportJob|reportActiveProjectsDaily|start")
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                doReport()
                logger.info("StreamActiveProjectsReportJob|reportActiveProjectsDaily|finish")
            } else {
                logger.info("StreamActiveProjectsReportJob|reportActiveProjectsDaily|running")
            }
        } catch (e: Throwable) {
            logger.warn("StreamActiveProjectsReportJob|reportActiveProjectsDail|error", e)
        }
    }

    private fun doReport() {
        // 获取v2版本的指定日期的日活跃项目
        val startDay = LocalDateTime.now().minusDays(7)
        val endDay = LocalDateTime.now().minusDays(1)
        val startTime = startDay.withHour(0).withMinute(0).withSecond(0).timestampmilli()
        val endTime = endDay.withHour(23).withMinute(59).withSecond(59).timestampmilli()
        val projectCount = gitRequestEventBuildDao.getBuildActiveProjectCount(dslContext, startTime, endTime)
        val repoHookProjectCount = gitRequestEventBuildDao.getBuildRepoHookActiveProjectCount(
            dslContext = dslContext,
            startTime = startTime,
            endTime = endTime
        )
        // 上报数据
        oteamStatus(
            data = projectCount.toDouble() + repoHookProjectCount.toDouble(),
            targetId = streamSlaConfig.oteamActiveProjectTarget,
            startTime = endTime
        )
    }
    private fun illegalConfig() =
        null == streamSlaConfig.oteamUrl || null == streamSlaConfig.oteamToken ||
            null == streamSlaConfig.oteamTechmap || null == streamSlaConfig.oteamActiveProjectTarget

    /**
     * 上报数据到oteam
     */
    @SuppressWarnings("MagicNumber", "TooGenericExceptionCaught")
    private fun oteamStatus(
        data: Double,
        targetId: Int?,
        startTime: Long
    ) {
        if (null == streamSlaConfig.oteamUrl) {
            logger.warn(
                "StreamActiveProjectsReportJob|oteamStatus" +
                    "|null oteamUrl , can not oteam status , targetId: $targetId , data: $data"
            )
            return
        }
        try {
            val yesterday = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault())
            val timestamp = "${System.currentTimeMillis() / 1000}"
            val token = streamSlaConfig.oteamToken
            val techmapId = streamSlaConfig.oteamTechmap
            val techmapType = "oteam"
            val signature = HashUtils.sha256(timestamp + techmapType + techmapId + token + timestamp)
            val response = OkhttpUtils.doPost(
                url = streamSlaConfig.oteamUrl!!,
                jsonParam = """
                        {
                          "method":"measureReport",
                          "params": {
                            "type":"daily",
                            "year":${yesterday.year},
                            "month":${yesterday.month.value},
                            "day":${yesterday.dayOfMonth},
                            "targetId":${targetId!!}, 
                            "value":$data
                          },
                          "jsonrpc":"2.0",
                          "id":"$timestamp"
                        }
                """.trimIndent(),
                headers = mapOf(
                    "timestamp" to timestamp,
                    "techmapType" to techmapType,
                    "techmapId" to techmapId!!,
                    "signature" to signature,
                    "content-type" to "application/json;charset=UTF-8"
                )
            )
            logger.info(
                "StreamActiveProjectsReportJob|oteamStatus" +
                    "|oteam status , id:{} , resp:{}",
                timestamp, response.body!!.string()
            )
        } catch (e: Exception) {
            logger.warn("StreamActiveProjectsReportJob|oteamStatus|error", e)
        }
    }
}
