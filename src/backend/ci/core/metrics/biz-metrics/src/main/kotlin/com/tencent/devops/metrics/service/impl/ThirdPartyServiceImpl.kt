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

package com.tencent.devops.metrics.service.impl

import com.tencent.devops.metrics.constant.Constants.BK_QUALITY_PIPELINE_EXECUTE_NUM
import com.tencent.devops.metrics.constant.Constants.BK_QUALITY_PIPELINE_INTERCEPTION_NUM
import com.tencent.devops.metrics.constant.Constants.BK_REPO_CODECC_AVG_SCORE
import com.tencent.devops.metrics.constant.Constants.BK_RESOLVED_DEFECT_NUM
import com.tencent.devops.metrics.constant.Constants.BK_TURBO_SAVE_TIME
import com.tencent.devops.metrics.dao.ThirdPartyOverviewInfoDao
import com.tencent.devops.metrics.service.ThirdPartyManageService
import com.tencent.devops.metrics.pojo.`do`.CodeCheckInfoDO
import com.tencent.devops.metrics.pojo.`do`.QualityInfoDO
import com.tencent.devops.metrics.pojo.`do`.TurboInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineSummaryInfoDTO
import com.tencent.devops.metrics.pojo.qo.ThirdPartyOverviewInfoQO
import com.tencent.devops.metrics.pojo.vo.ThirdPlatformOverviewInfoVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ThirdPartyServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyOverviewInfoDao: ThirdPartyOverviewInfoDao,
//    private val measureEventDispatcher: MetricsEventDispatcher
): ThirdPartyManageService {
    override fun queryPipelineSummaryInfo(
        queryPipelineSummaryInfoDTO: QueryPipelineSummaryInfoDTO
    ): ThirdPlatformOverviewInfoVO {
        val result = thirdPartyOverviewInfoDao.queryPipelineSummaryInfo(
            ThirdPartyOverviewInfoQO(
                queryPipelineSummaryInfoDTO.projectId,
                queryPipelineSummaryInfoDTO.startTime,
                queryPipelineSummaryInfoDTO.endTime
            ),
            dslContext
        )
        val totalExecuteCount = thirdPartyOverviewInfoDao.queryPipelineSummaryCount(
            ThirdPartyOverviewInfoQO(
                queryPipelineSummaryInfoDTO.projectId,
                queryPipelineSummaryInfoDTO.startTime,
                queryPipelineSummaryInfoDTO.endTime
            ),
            dslContext
        )

        val repoCodeccAvgScore = result?.get(BK_REPO_CODECC_AVG_SCORE, BigDecimal::class.java)?.toDouble()
        val executeNum = result?.get(BK_QUALITY_PIPELINE_EXECUTE_NUM, BigDecimal::class.java)?.toInt()
        val interceptionCount = result?.get(BK_QUALITY_PIPELINE_INTERCEPTION_NUM, BigDecimal::class.java)?.toInt()
        val qualityInterceptionRate =
            if (executeNum == null || interceptionCount == null || executeNum == 0) null
        else {
            if (executeNum == interceptionCount) 0.0
            else String.format("%.2f", interceptionCount.toDouble() / executeNum.toDouble()).toDouble()
        }
            return ThirdPlatformOverviewInfoVO(
                CodeCheckInfoDO(
                    resolvedDefectNum = result?.get(BK_RESOLVED_DEFECT_NUM, Int::class.java),
                    repoCodeccAvgScore = if (repoCodeccAvgScore == null || totalExecuteCount == 0) null
                    else repoCodeccAvgScore / totalExecuteCount.toDouble()
                ),
                QualityInfoDO(
                    qualityInterceptionRate = qualityInterceptionRate
                    ,
                    totalExecuteCount = executeNum,
                    interceptionCount = interceptionCount
                        ),
                TurboInfoDO(result?.get(BK_TURBO_SAVE_TIME, BigDecimal::class.java)?.toDouble())
            )
        }

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyServiceImpl::class.java)
    }
}