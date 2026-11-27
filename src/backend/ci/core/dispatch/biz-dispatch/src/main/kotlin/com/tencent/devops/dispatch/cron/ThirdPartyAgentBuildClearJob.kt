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

package com.tencent.devops.dispatch.cron

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * 每天清理第三方构建机的构建记录：每台构建机按 ID 只保留最新的 [keepNum] 条，更早的按 ID 正序分批删除，
 * 节点构建记录的查询和计数因此有上限。misc 仍按流水线的构建保留数量清理，两者互不影响。
 * 以下记录即使超出数量也不删除：
 * - 最近 [keepDays] 天内创建的记录，留出时间给数据同步到其他平台；
 * - 排队中/运行中的记录，参与调度的并发计数。
 * 每次最多运行 [maxMinutes] 分钟，到时停止，第二天从头继续，已清理过的构建机很快跳过。
 */
@Component
class ThirdPartyAgentBuildClearJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao,
    @Value("\${dispatch.thirdPartyAgent.buildKeepNum:100000}")
    private val keepNum: Int,
    @Value("\${dispatch.thirdPartyAgent.buildKeepDays:14}")
    private val keepDays: Long,
    @Value("\${dispatch.thirdPartyAgent.buildClearBatchSize:500}")
    private val batchSize: Int,
    @Value("\${dispatch.thirdPartyAgent.buildClearIntervalMills:100}")
    private val intervalMills: Long,
    @Value("\${dispatch.thirdPartyAgent.buildClearMaxMinutes:240}")
    private val maxMinutes: Long
) {

    @Scheduled(cron = "0 30 3 * * ?")
    fun clear() {
        if (keepNum <= 0) {
            return
        }
        // 锁按日期区分且不主动释放：各实例的触发时间有偏差时，先执行完的实例释放锁会让后触发的实例再扫一遍
        val lockKey = "$LOCK_KEY_PREFIX:${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}"
        if (!RedisLock(redisOperation, lockKey, LOCK_SECONDS).tryLock()) {
            logger.info("clear third party agent builds is running or finished on another instance")
            return
        }
        try {
            clearAll()
        } catch (e: Throwable) {
            logger.error("clear third party agent builds failed", e)
        }
    }

    internal fun clearAll(): Long {
        val start = System.currentTimeMillis()
        val deadline = start + TimeUnit.MINUTES.toMillis(maxMinutes)
        var deleted = 0L
        var agents = 0
        var afterAgentId = ""
        while (System.currentTimeMillis() < deadline) {
            val agentId = thirdPartyAgentBuildDao.getNextAgentId(dslContext, afterAgentId) ?: break
            // 单个构建机失败不影响其他构建机，下次定时任务会重试
            deleted += try {
                clearAgent(agentId, deadline)
            } catch (e: Exception) {
                logger.warn("clear third party agent builds failed|$agentId", e)
                0L
            }
            afterAgentId = agentId
            // 每台构建机都要沿索引找保留的边界，构建机多时分散读取压力
            if (++agents % AGENT_PAUSE_EVERY == 0) {
                Thread.sleep(intervalMills)
            }
        }
        logger.info(
            "clear third party agent builds finished|keepNum=$keepNum|keepDays=$keepDays|agents=$agents|" +
                "deleted=$deleted|timeout=${System.currentTimeMillis() >= deadline}|" +
                "cost=${System.currentTimeMillis() - start}ms"
        )
        return deleted
    }

    internal fun clearAgent(agentId: String, deadline: Long = Long.MAX_VALUE): Long {
        val maxId = thirdPartyAgentBuildDao.getClearBoundaryId(dslContext, agentId, keepNum) ?: return 0L
        val createdBefore = LocalDateTime.now().minusDays(keepDays)
        var deleted = 0L
        var afterId = 0L
        while (System.currentTimeMillis() < deadline) {
            val rows = thirdPartyAgentBuildDao.listClearCandidates(dslContext, agentId, afterId, maxId, batchSize)
            deleted += thirdPartyAgentBuildDao.deleteByIds(
                dslContext = dslContext,
                ids = rows.filter { it.createdTime.isBefore(createdBefore) && it.status in FINISHED_STATUS }
                    .map { it.id }
            )
            // ID 与创建时间同序：本批最后一条已在 keepDays 天内，之后的记录都更新，本次不再继续。
            // 只看最后一条，个别创建时间异常的记录只会被跳过，不会挡住后面的旧记录
            if (rows.size < batchSize || !rows.last().createdTime.isBefore(createdBefore)) {
                break
            }
            afterId = rows.last().id
            // 首次清理的数据量大，批次之间稍作停顿，减轻主库和同步的压力
            Thread.sleep(intervalMills)
        }
        logger.info("clear third party agent builds|$agentId|maxId=$maxId|deleted=$deleted")
        return deleted
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentBuildClearJob::class.java)
        private const val LOCK_KEY_PREFIX = "dispatch:thirdPartyAgent:buildClearLock"

        // 锁的有效期不超过一天，执行的节点异常退出时不影响第二天执行
        private const val LOCK_SECONDS = 23 * 3600L
        private const val AGENT_PAUSE_EVERY = 100
        private val FINISHED_STATUS = setOf(PipelineTaskStatus.DONE.status, PipelineTaskStatus.FAILURE.status)
    }
}
