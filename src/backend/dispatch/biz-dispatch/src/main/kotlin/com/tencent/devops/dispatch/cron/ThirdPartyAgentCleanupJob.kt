/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

import com.tencent.devops.dispatch.dao.ThirdPartyAgentBuildDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * deng
 * 2019-06-11
 * 周期性清理第三方构建机任务的状态
 */
@Component
class ThirdPartyAgentCleanupJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentBuildDao: ThirdPartyAgentBuildDao
) {

    // every 30 minutes
    @Scheduled(initialDelay = 10 * 60 * 1000, fixedDelay = 30 * 60 * 1000)
    fun cleanup() {
        logger.info("Start to clean up the third party agent")
        try {
            val expiredBuilds = thirdPartyAgentBuildDao.getExpireBuilds(dslContext)
            if (expiredBuilds.isEmpty()) {
                logger.info("Expire build is empty")
                return
            }
            val ids = expiredBuilds.map { it.id }.toSet()
            logger.info("Get the expire builds - [$expiredBuilds] - [$ids]")
            val count = thirdPartyAgentBuildDao.updateExpireBuilds(dslContext, ids)
            logger.info("Update $count expired agent builds")
        } catch (t: Throwable) {
            logger.warn("Fail to clean up the third party agent")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentCleanupJob::class.java)
    }
}