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

package com.tencent.devops.metrics.service.impl

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.metrics.dao.AtomMonitorDataDao
import com.tencent.devops.metrics.pojo.`do`.AtomMonitorFailDetailDO
import com.tencent.devops.metrics.pojo.vo.AtomMonitorInfoVO
import com.tencent.devops.metrics.service.AtomMonitorDataManageService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AtomMonitorDataManageServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val atomMonitorDataDao: AtomMonitorDataDao
) : AtomMonitorDataManageService {

    companion object {
        private val logger = LoggerFactory.getLogger(AtomMonitorDataManageServiceImpl::class.java)
    }

    override fun queryAtomMonitorStatisticData(
        atomCode: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): AtomMonitorInfoVO {
        logger.info("queryAtomMonitorStatisticData  params:[$atomCode|$startTime|$endTime]")
        val atomMonitorDatas = atomMonitorDataDao.getAtomMonitorDatas(
            dslContext = dslContext,
            atomCode = atomCode,
            startTime = startTime,
            endTime = endTime
        )
        val totalFailDetail = AtomMonitorFailDetailDO()
        val atomMonitorInfoVO = AtomMonitorInfoVO(atomCode = atomCode, totalFailDetail = totalFailDetail)
        var allExecuteCount = 0
        atomMonitorDatas?.forEach { atomMonitorData ->
            val errorType = atomMonitorData.errorType
            val totalExecuteCount = atomMonitorData.totalExecuteCount
            allExecuteCount += totalExecuteCount
            when (errorType) {
                -1 -> {
                    atomMonitorInfoVO.totalSuccessNum = totalExecuteCount
                }

                ErrorType.SYSTEM.num -> {
                    totalFailDetail.totalSystemFailNum = totalExecuteCount
                }

                ErrorType.USER.num -> {
                    totalFailDetail.totalUserFailNum = totalExecuteCount
                }

                ErrorType.THIRD_PARTY.num -> {
                    totalFailDetail.totalThirdFailNum = totalExecuteCount
                }

                ErrorType.PLUGIN.num -> {
                    totalFailDetail.totalComponentFailNum = totalExecuteCount
                }
            }
        }
        atomMonitorInfoVO.totalFailNum = allExecuteCount - atomMonitorInfoVO.totalSuccessNum
        return atomMonitorInfoVO
    }
}
