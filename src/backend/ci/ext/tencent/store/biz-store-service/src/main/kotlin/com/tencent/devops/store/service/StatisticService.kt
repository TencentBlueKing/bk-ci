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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.store.dao.ExtStoreProjectRelDao
import com.tencent.devops.store.pojo.ExtServiceInstallTrendReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class StatisticService @Autowired constructor(
    val storeProjectRelDao: ExtStoreProjectRelDao,
    val dslContext: DSLContext
) {

    fun getInstallTrend(serviceCode: String, days: Long): Result<List<ExtServiceInstallTrendReq>> {
        val startTime: Long = if (days > 30) {
            LocalDateTime.now().timestamp() - TimeUnit.DAYS.toSeconds(30)
        } else {
            LocalDateTime.now().timestamp() - TimeUnit.DAYS.toSeconds(days)
        }
        logger.info("getInstallTrend startTime[$startTime], serviceCode[$serviceCode]")
        val installRecords = storeProjectRelDao.getStoreInstall(
            dslContext = dslContext,
            storeCode = serviceCode,
            storeType = StoreTypeEnum.SERVICE,
            startTime = startTime
        )
            ?: return Result(emptyList())

        val installTrendList = mutableListOf<ExtServiceInstallTrendReq>()
        val installDayMap = mutableMapOf<String, Int>()
        installRecords.forEach {
            val projectCreateTime = it.createTime.dayOfYear.toString()
            if (installDayMap.containsKey(projectCreateTime)) {
                val count = installDayMap[projectCreateTime]
                installDayMap[projectCreateTime] = count!! + 1
            } else {
                installDayMap[projectCreateTime] = 1
            }
        }

        installDayMap.forEach { (day, count) ->
            installTrendList.add(
                ExtServiceInstallTrendReq(
                    installCount = count,
                    day = day
                )
            )
        }
        return Result(installTrendList)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StatisticService::class.java)
    }
}
