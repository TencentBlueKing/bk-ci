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

import com.tencent.devops.metrics.constant.Constants.BK_QUALITY_PIPELINE_EXECUTE_NUM
import com.tencent.devops.metrics.constant.Constants.BK_QUALITY_PIPELINE_INTERCEPTION_NUM
import com.tencent.devops.metrics.constant.Constants.BK_REPO_CODECC_AVG_SCORE
import com.tencent.devops.metrics.constant.Constants.BK_RESOLVED_DEFECT_NUM
import com.tencent.devops.metrics.constant.Constants.BK_TURBO_SAVE_TIME
import com.tencent.devops.metrics.dao.ThirdPartyOverviewInfoDao
import com.tencent.devops.metrics.pojo.`do`.CodeCheckInfoDO
import com.tencent.devops.metrics.pojo.`do`.QualityInfoDO
import com.tencent.devops.metrics.pojo.`do`.TurboInfoDO
import com.tencent.devops.metrics.pojo.dto.QueryPipelineSummaryInfoDTO
import com.tencent.devops.metrics.pojo.qo.ThirdPartyOverviewInfoQO
import com.tencent.devops.metrics.pojo.vo.ThirdPlatformOverviewInfoVO
import com.tencent.devops.metrics.service.ThirdPartyManageService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ThirdPartyServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyOverviewInfoDao: ThirdPartyOverviewInfoDao
) : ThirdPartyManageService {
    override fun queryPipelineSummaryInfo(
        queryPipelineSummaryInfoDTO: QueryPipelineSummaryInfoDTO
    ): ThirdPlatformOverviewInfoVO {
        // 查询第三方平台度量数据
        val result = thirdPartyOverviewInfoDao.queryPipelineSummaryInfo(
            ThirdPartyOverviewInfoQO(
                projectId = queryPipelineSummaryInfoDTO.projectId,
                startTime = queryPipelineSummaryInfoDTO.startTime,
                endTime = queryPipelineSummaryInfoDTO.endTime
            ),
            dslContext
        )
        // 查询项目总执行次数
        val totalExecuteCount = thirdPartyOverviewInfoDao.queryPipelineSummaryCount(
            ThirdPartyOverviewInfoQO(
                projectId = queryPipelineSummaryInfoDTO.projectId,
                startTime = queryPipelineSummaryInfoDTO.startTime,
                endTime = queryPipelineSummaryInfoDTO.endTime
            ),
            dslContext
        )
        // 计算度量数据
        val repoCodeccAvgScore = result?.get(BK_REPO_CODECC_AVG_SCORE, BigDecimal::class.java)?.toDouble()
        val executeNum = result?.get(BK_QUALITY_PIPELINE_EXECUTE_NUM, BigDecimal::class.java)?.toInt()
        val interceptionCount = result?.get(BK_QUALITY_PIPELINE_INTERCEPTION_NUM, BigDecimal::class.java)?.toInt()
        val qualityInterceptionRate =
            if (executeNum == null || interceptionCount == null || executeNum == 0) null
        else {
            if (executeNum == interceptionCount) 100.0
            else String.format("%.2f", interceptionCount.toDouble() * 100 / executeNum.toDouble()).toDouble()
        }
            return ThirdPlatformOverviewInfoVO(
                codeCheckInfo = CodeCheckInfoDO(
                    resolvedDefectNum = result?.get(BK_RESOLVED_DEFECT_NUM, Int::class.java),
                    repoCodeccAvgScore = if (repoCodeccAvgScore == null || totalExecuteCount == 0) null
                    else String.format("%.2f", repoCodeccAvgScore / totalExecuteCount.toDouble()).toDouble()
                ),
                qualityInfo = QualityInfoDO(
                    qualityInterceptionRate = qualityInterceptionRate,
                    totalExecuteCount = executeNum,
                    interceptionCount = interceptionCount
                        ),
                turboInfo = TurboInfoDO(result?.get(BK_TURBO_SAVE_TIME, BigDecimal::class.java)?.toDouble())
            )
        }
}
