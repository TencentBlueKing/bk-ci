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

package com.tencent.devops.quality.cron

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.quality.dao.v2.QualityHisMetadataDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Suppress("ALL")
@Component
class QualityHisMetadataJob @Autowired constructor(
    private val qualityHisMetadataDao: QualityHisMetadataDao,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext
) {

    private val logger = LoggerFactory.getLogger(QualityHisMetadataJob::class.java)

    @Value("\${quality.metadata.clean.timeGap:31}")
    var cleanTimeGapDay: Long = 31

    @Value("\${quality.metadata.clean.round:400}")
    var cleanRound: Long = 400

    @Value("\${quality.metadata.clean.roundSize:10000}")
    var roundSize: Long = 10000

    @Value("\${quality.metadata.clean.roundGap:5}")
    var roundGap: Long = 5

    @Value("\${quality.metadata.clean.enable:#{false}}")
    val cleanEnable: Boolean = false

    @Scheduled(cron = "0 0 6 * * ?")
    fun clean() {
        if (!cleanEnable) {
            logger.info("quality metadata daily clean disabled.")
            return
        }
        val key = this::class.java.name + "#" + Thread.currentThread().stackTrace[1].methodName
        val lock = RedisLock(redisOperation, key, 3600L)
        try {
            if (!lock.tryLock()) {
                logger.info("get lock failed, skip: $key")
                return
            }

            val deleteTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(cleanTimeGapDay)

            logger.info("start to delete quality his meta data: " +
                "$deleteTime, $cleanTimeGapDay, $cleanRound, $roundSize, $roundGap")

            // 执行cleanRound轮清理详情数据操作
            for (i in 1..cleanRound) {
                // 分页读取12个小时前的数据和创建时间为null的历史数据
                val result =
                    qualityHisMetadataDao.getHisMetadataByCreateTime(dslContext, deleteTime, roundSize.toInt())

                // 分成两批，nullResultIds是待更新的ID，resultIds是待删除的ID
                val nullResultIds = mutableSetOf<Long>()
                val resultIds = mutableSetOf<Long>()

                result.forEach {
                    if (it.createTime == null || it.createTime <= 0L) {
                        nullResultIds.add(it.id)
                    } else {
                        resultIds.add(it.id)
                    }
                }

                if (nullResultIds.isEmpty() && resultIds.isEmpty()) {
                    break
                }

                logger.info("start to delete quality his detail meta data before: $deleteTime, ${resultIds.size}")
                qualityHisMetadataDao.deleteHisMetadataById(dslContext, resultIds)

                // 更新创建时间为null的数据为当前时间，本次不清理，下一次定时任务清理
                logger.info("start to update quality his detail meta data before: $deleteTime, ${nullResultIds.size}")
                qualityHisMetadataDao.updateHisMetadataTimeById(dslContext, nullResultIds)

                Thread.sleep(roundGap * 1000)

                if (result.size < roundSize) {
                    break
                }
            }

            // 执行cleanRound轮清理原始数据操作
            for (i in 1..cleanRound) {
                val originCount =
                    qualityHisMetadataDao.deleteHisOriginMetadataByCreateTime(dslContext, deleteTime, roundSize)

                logger.info("finish to delete quality his origin meta data before: $deleteTime, $originCount")

                if (originCount < roundSize) {
                    break
                }

                Thread.sleep(roundGap * 1000)
            }
        } finally {
            lock.unlock()
        }
    }
}
