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

package com.tencent.devops.metrics.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.db.utils.JooqUtils.sum
import com.tencent.devops.metrics.constant.Constants.BK_QUALITY_PIPELINE_EXECUTE_NUM
import com.tencent.devops.metrics.constant.Constants.BK_QUALITY_PIPELINE_INTERCEPTION_NUM
import com.tencent.devops.metrics.constant.Constants.BK_REPO_CODECC_AVG_SCORE
import com.tencent.devops.metrics.constant.Constants.BK_RESOLVED_DEFECT_NUM
import com.tencent.devops.metrics.constant.Constants.BK_TURBO_SAVE_TIME
import com.tencent.devops.model.metrics.tables.TProjectThirdPlatformData
import com.tencent.devops.metrics.pojo.qo.ThirdPartyOverviewInfoQO
import org.jooq.DSLContext
import org.jooq.Record5
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class ThirdPartyOverviewInfoDao {
    fun queryPipelineSummaryInfo(
        thirdPartyOverviewInfoQO: ThirdPartyOverviewInfoQO,
        dslContext: DSLContext
    ): Record5<BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal>? {
        with(TProjectThirdPlatformData.T_PROJECT_THIRD_PLATFORM_DATA) {
            val startTimeDateTime = DateTimeUtil.stringToLocalDate(thirdPartyOverviewInfoQO.startTime)!!.atStartOfDay()
            val endTimeDateTime = DateTimeUtil.stringToLocalDate(thirdPartyOverviewInfoQO.endTime)!!.atStartOfDay()
            return dslContext.select(
                sum<BigDecimal>(REPO_CODECC_AVG_SCORE).`as`(BK_REPO_CODECC_AVG_SCORE),
                sum<Int>(RESOLVED_DEFECT_NUM).`as`(BK_RESOLVED_DEFECT_NUM),
                sum<Int>(QUALITY_PIPELINE_INTERCEPTION_NUM).`as`(BK_QUALITY_PIPELINE_INTERCEPTION_NUM),
                sum<Int>(QUALITY_PIPELINE_EXECUTE_NUM).`as`(BK_QUALITY_PIPELINE_EXECUTE_NUM),
                sum<BigDecimal>(TURBO_SAVE_TIME).`as`(BK_TURBO_SAVE_TIME)
            ).from(this)
                .where(PROJECT_ID.eq(thirdPartyOverviewInfoQO.projectId))
                .and(STATISTICS_TIME.between(startTimeDateTime, endTimeDateTime))
                .fetchOne()
        }
    }

    fun queryPipelineSummaryCount(
        thirdPartyOverviewInfoQO: ThirdPartyOverviewInfoQO,
        dslContext: DSLContext
    ): Int {
        with(TProjectThirdPlatformData.T_PROJECT_THIRD_PLATFORM_DATA) {
            val startTimeDateTime = DateTimeUtil.stringToLocalDate(thirdPartyOverviewInfoQO.startTime)!!.atStartOfDay()
            val endTimeDateTime = DateTimeUtil.stringToLocalDate(thirdPartyOverviewInfoQO.endTime)!!.atStartOfDay()
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(thirdPartyOverviewInfoQO.projectId))
                .and(STATISTICS_TIME.between(startTimeDateTime, endTimeDateTime))
                .fetchOne(0, Int::class.java) ?: 0
        }
    }
}
