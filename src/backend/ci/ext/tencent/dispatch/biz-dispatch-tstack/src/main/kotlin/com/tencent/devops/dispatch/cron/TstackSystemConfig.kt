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

package com.tencent.devops.dispatch.cron

import com.tencent.devops.dispatch.dao.TstackSystemDao
import com.tencent.devops.model.dispatch.tables.records.TDispatchTstackSystemRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * deng
 * 17/01/2018
 */
@Component
class TstackSystemConfig @Autowired constructor(
    private val dslContext: DSLContext,
    private val tstackSystemDao: TstackSystemDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TstackSystemConfig::class.java)
    }

    private var tstackSystemConfig: TDispatchTstackSystemRecord? = null

    private var tstackFloatingIps: Set<String>? = null

    /**
     * 动态更新Tsatck构建系统配置
     */
    @Scheduled(initialDelay = 1000 * 60, fixedDelay = 1000 * 60 /* 1分钟 */)
    fun reloadTstackSystemConfig() {
        logger.info("Start reloading TStack system config")
        tstackSystemConfig = tstackSystemDao.getSystemConfig(dslContext)
        logger.info("Reload TStack system config end")
    }

    /**
     * 动态更新Tstack可用IP信息
     */
    @Scheduled(initialDelay = 1000 * 60, fixedDelay = 1000 * 60 /* 1分钟 */)
    fun reloadTstackFloatingIps() {
        logger.info("Start reloading TStack floating IP(s)")
        tstackFloatingIps = tstackSystemDao.getFloatingIpList(dslContext).map { it.floatingIp }.toSet()
        logger.info("Reload TStack floating IP(s) end")
    }

    fun getTstackSystemConfig(): TDispatchTstackSystemRecord {
        if (tstackSystemConfig == null) {
            tstackSystemConfig = tstackSystemDao.getSystemConfig(dslContext)
        }
        return tstackSystemConfig!!
    }

    fun getTstackFloatingIps(): Set<String> {
        if (tstackFloatingIps == null) {
            tstackFloatingIps = tstackSystemDao.getFloatingIpList(dslContext).map { it.floatingIp }.toSet()
        }
        return tstackFloatingIps!!
    }
}