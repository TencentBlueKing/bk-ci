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

package com.tencent.devops.common.kafka

object KafkaTopic {
    const val LANGUAGE_CODE_TOPIC = "tendata-bkdevops-296-topic-language-code"
    const val TASK_DETAIL_TOPIC = "tendata-bkdevops-296-topic-taskdetail"
    const val STATISTIC_TOPIC = "tendata-bkdevops-296-topic-statistic"
    const val GONGFENG_PROJECT_TOPIC = "tendata-bkdevops-296-topic-gongfeng-project"
    const val LINT_STATISTIC_TOPIC = "tendata-bkdevops-296-topic-lint-statistic"
    const val CNN_STATISTIC_TOPIC = "tendata-bkdevops-296-topic-cnn-statistic"
    const val DUPC_STATISTIC_TOPIC = "tendata-bkdevops-296-topic-dupc-statistic"

    const val ACTIVE_GONGFENG_PROJECT_TOPIC = "tendata-bkdevops-296-topic-active-gongfeng-project"
    const val SINGLE_STATISTIC_TOPIC = "tendata-bkdevops-296-topic-single-statistic"
    const val SINGLE_LINT_STATISTIC_TOPIC = "tendata-bkdevops-296-topic-single-lint-statistic"
    const val SINGLE_CCN_STATISTIC_TOPIC = "tendata-bkdevops-296-topic-single-ccn-statistic"
    const val SINGLE_DUPC_STATISTIC_TOPIC = "tendata-bkdevops-296-topic-single-dupc-statistic"

    const val LANDUN_BUILD_HISTORY_TOPIC = "tendata-bkdevops-296-topic-landun-build-history"
    const val LANDUN_BUILD_DETAIL_TOPIC = "tendata-bkdevops-296-topic-landun-build-detail"
    const val LANDUN_BUILD_TASK_TOPIC = "tendata-bkdevops-296-topic-landun-build-task"
    const val LANDUN_GIT_TASK_TOPIC = "tendata-bkdevops-296-topic-landun-git-task"
    const val LANDUN_TASK_DETAIL_TOPIC = "tendata-bkdevops-296-topic-landun-task-detail"
    const val LANDUN_JOB_DETAIL_TOPIC = "tendata-bkdevops-296-topic-landun-job-detail"
    const val LANDUN_PROJECT_INFO_TOPIC = "tendata-bkdevops-topic-landun-project-info"
    const val LANDUN_PIPELINE_INFO_TOPIC = "tendata-bkdevops-topic-landun-pipeline-info"
    const val LANDUN_PIPELINE_RESOURCE_TOPIC = "tendata-bkdevops-topic-landun-pipeline-resource"

    // 标准日志上报topic
    const val LANDUN_LOG_FORMAT_TOPIC = "tendata-bkdevops-296-topic-landun-log-format"

    // APP上报topic
    const val BK_CI_APP_LOGIN_TOPIC = "tendata-bkdevops-296-topic-landun-app-login"

    // Stream上报topic
    const val STREAM_BUILD_INFO_TOPIC = "tendata-bkdevops-topic-stream-build-info"

    // quota topic
    const val JOB_QUOTA_HISYORY_TOPIC = "tendata-bkdevops-topic-job-quota-history"

    const val BUILD_ATOM_METRICS_TOPIC_PREFIX = "tendata-bkdevops-topic-build-atom-metrics"
}
