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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.atom

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.atom.MarketAtomOfflineDao
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * 插件市场定时任务
 * since: 2019-01-24
 */

@Service
class AtomCrontabService @Autowired constructor(
    private val marketAtomDao: MarketAtomDao,
    private val marketAtomOfflineDao: MarketAtomOfflineDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation
) {
    private val logger = LoggerFactory.getLogger(AtomCrontabService::class.java)

    /**
     * 执行下架插件操作
     */
    @Scheduled(cron = "0 * * * * ?") // 每小时执行一次
    fun doOfflineAtom() {
        val lock = RedisLock(redisOperation, "doOfflineAtom", 60L)
        try {
            lock.lock()
            // 获取到期插件
            val atoms = marketAtomOfflineDao.getExpiredAtoms(dslContext)

            atoms.forEach {
                // 执行下架操作
                logger.info("expired atom is: {}", it.atomCode)
                dslContext.transaction { t ->
                    val context = DSL.using(t)
                    marketAtomDao.setAtomStatusByCode(
                        context, it.atomCode,
                        AtomStatusEnum.UNDERCARRIAGING.status.toByte(),
                        AtomStatusEnum.UNDERCARRIAGED.status.toByte(), "system", null
                    )
                    marketAtomOfflineDao.setStatus(context, it.id, 1, "system")
                }

                // 通知开发者、使用方插件已下架 -- todo

                logger.info("offline atom {} success", it.atomCode)
            }
        } catch (ignored: Throwable) {
            logger.warn("Fail to offline atom: {}", ignored)
        } finally {
            lock.unlock()
        }
    }
}