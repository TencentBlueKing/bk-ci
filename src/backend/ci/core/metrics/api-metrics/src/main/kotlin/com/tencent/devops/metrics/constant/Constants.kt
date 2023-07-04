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

package com.tencent.devops.metrics.constant

object Constants {
    const val BK_REPO_CODECC_AVG_SCORE = "repoCodeccAvgScore"
    const val BK_RESOLVED_DEFECT_NUM = "resolvedDefectNum"
    const val BK_QUALITY_PIPELINE_INTERCEPTION_NUM = "qualityPipelineInterceptionNum"
    const val BK_QUALITY_PIPELINE_EXECUTE_NUM = "qualityPipelineExecuteNum"
    const val BK_TURBO_SAVE_TIME = "turboSaveTime"

    const val BK_PIPELINE_NAME = "pipelineName"
    const val BK_CHANNEL_CODE = "channelCode"
    const val BK_STATISTICS_TIME = "statisticsTime"
    const val BK_AVG_COST_TIME = "avgCostTime"
    const val BK_ATOM_CODE = "atomCode"
    const val BK_ATOM_NAME = "atomName"
    const val BK_ATOM_POSITION = "atomPosition"
    const val BK_SUCCESS_RATE = "successRate"
    const val BK_TOTAL_EXECUTE_COUNT = "totalExecuteCount"
    const val BK_FAIL_EXECUTE_COUNT = "failExecuteCount"
    const val BK_TOTAL_AVG_COST_TIME = "totalAvgCostTime"
    const val BK_FAIL_AVG_COST_TIME = "failAvgCostTime"
    const val BK_PROJECT_ID = "projectId"
    const val BK_PIPELINE_ID = "pipelineId"
    const val BK_ERROR_TYPE = "errorType"
    const val BK_ERROR_NAME = "errorName"
    const val BK_CLASSIFY_CODE = "classifyCode"
    const val BK_SUCCESS_EXECUTE_COUNT = "successExecuteCount"
    const val BK_ERROR_CODE = "errorCode"
    const val BK_ERROR_MSG = "errorMsg"
    const val BK_START_USER = "startUser"
    const val BK_START_TIME = "startTime"
    const val BK_END_TIME = "endTime"
    const val BK_BUILD_ID = "buildId"
    const val BK_BUILD_NUM = "buildNum"

    const val BK_ERROR_COUNT_SUM = "errorCountSum"
    const val BK_ERROR_COUNT = "errorCount"
    const val BK_TOTAL_EXECUTE_COUNT_SUM = "totalExecuteCountSum"
    const val BK_SUCCESS_EXECUTE_COUNT_SUM = "successExecuteCountSum"
    const val BK_TOTAL_COST_TIME_SUM = "totalCostTimeSum"
    const val BK_FAIL_COST_TIME_SUM = "failCostTimeSum"
    const val BK_FAIL_COMPLIANCE_COUNT = "failComplianceCount"

    const val ERROR_TYPE_NAME_PREFIX = "METRICS_ERROR_TYPE_"

    // 常量标志对应code
    const val BK_ATOM_CODE_FIELD_NAME_ENGLISH = "METRICS_ATOM_STATISTICS_HEADER_PLUG" // 插件
    const val BK_CLASSIFY_CODE_FIELD_NAME_ENGLISH = "METRICS_ATOM_STATISTICS_HEADER_TYPE" // 类型
    const val BK_SUCCESS_RATE_FIELD_NAME_ENGLISH = "METRICS_ATOM_STATISTICS_HEADER_SUCCESS_RATE" // 成功率
    const val BK_AVG_COST_TIME_FIELD_NAME_ENGLISH = "METRICS_ATOM_STATISTICS_HEADER_AVERAGE_TIME" // 平均耗时
    const val BK_TOTAL_EXECUTE_COUNT_FIELD_NAME_ENGLISH = "METRICS_ATOM_STATISTICS_HEADER_EXECUTE_COUNT" // 执行次数
    // 成功执行次数
    const val BK_SUCCESS_EXECUTE_COUNT_FIELD_NAME_ENGLISH = "METRICS_ATOM_STATISTICS_HEADER_SUCCESS_EXECUTE_COUNT"
}
