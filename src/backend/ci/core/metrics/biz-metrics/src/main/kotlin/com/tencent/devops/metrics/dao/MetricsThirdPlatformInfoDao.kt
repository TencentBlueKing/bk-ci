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

import com.tencent.devops.metrics.pojo.po.ThirdPlatformDatePO
import com.tencent.devops.model.metrics.tables.TProjectThirdPlatformData
import com.tencent.devops.model.metrics.tables.records.TProjectThirdPlatformDataRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL.defaultValue
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MetricsThirdPlatformInfoDao {

    fun getMetricsThirdPlatformInfo(
        dslContext: DSLContext,
        projectId: String,
        statisticsTime: LocalDateTime
    ): TProjectThirdPlatformDataRecord? {
        with(TProjectThirdPlatformData.T_PROJECT_THIRD_PLATFORM_DATA) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(STATISTICS_TIME.eq(statisticsTime)))
                .fetchOne()
        }
    }

    fun saveMetricsThirdPlatformData(
        dslContext: DSLContext,
        thirdPlatformDate: ThirdPlatformDatePO
    ) {
        with(TProjectThirdPlatformData.T_PROJECT_THIRD_PLATFORM_DATA) {
            dslContext.insertInto(this)
                .set(ID, thirdPlatformDate.id)
                .set(PROJECT_ID, thirdPlatformDate.projectId)
                .set(REPO_CODECC_AVG_SCORE, thirdPlatformDate.repoCodeccAvgScore)
                .set(RESOLVED_DEFECT_NUM, thirdPlatformDate.resolvedDefectNum)
                .set(QUALITY_PIPELINE_EXECUTE_NUM, thirdPlatformDate.qualityPipelineExecuteNum)
                .set(QUALITY_PIPELINE_INTERCEPTION_NUM, thirdPlatformDate.qualityPipelineInterceptionNum)
                .set(TURBO_SAVE_TIME, thirdPlatformDate.turboSaveTime)
                .set(STATISTICS_TIME, thirdPlatformDate.statisticsTime)
                .set(CREATOR, defaultValue(CREATOR))
                .set(MODIFIER, defaultValue(MODIFIER))
                .set(UPDATE_TIME, thirdPlatformDate.updateTime)
                .set(CREATE_TIME, thirdPlatformDate.createTime)
                .onDuplicateKeyUpdate()
                .set(REPO_CODECC_AVG_SCORE, thirdPlatformDate.repoCodeccAvgScore)
                .set(RESOLVED_DEFECT_NUM, thirdPlatformDate.resolvedDefectNum)
                .set(QUALITY_PIPELINE_EXECUTE_NUM, thirdPlatformDate.qualityPipelineExecuteNum)
                .set(QUALITY_PIPELINE_INTERCEPTION_NUM, thirdPlatformDate.qualityPipelineInterceptionNum)
                .set(TURBO_SAVE_TIME, thirdPlatformDate.turboSaveTime)
                .set(UPDATE_TIME, thirdPlatformDate.updateTime)
                .execute()
        }
    }

    fun updateMetricsThirdPlatformData(
        dslContext: DSLContext,
        thirdPlatformDate: ThirdPlatformDatePO
    ) {
        with(TProjectThirdPlatformData.T_PROJECT_THIRD_PLATFORM_DATA) {
            dslContext.update(this)
                .set(REPO_CODECC_AVG_SCORE, thirdPlatformDate.repoCodeccAvgScore)
                .set(RESOLVED_DEFECT_NUM, thirdPlatformDate.resolvedDefectNum)
                .set(QUALITY_PIPELINE_EXECUTE_NUM, thirdPlatformDate.qualityPipelineExecuteNum)
                .set(QUALITY_PIPELINE_INTERCEPTION_NUM, thirdPlatformDate.qualityPipelineInterceptionNum)
                .set(TURBO_SAVE_TIME, thirdPlatformDate.turboSaveTime)
                .set(UPDATE_TIME, thirdPlatformDate.updateTime)
                .where(PROJECT_ID.eq(thirdPlatformDate.projectId).and(STATISTICS_TIME.eq(STATISTICS_TIME)))
                .execute()
        }
    }
}
