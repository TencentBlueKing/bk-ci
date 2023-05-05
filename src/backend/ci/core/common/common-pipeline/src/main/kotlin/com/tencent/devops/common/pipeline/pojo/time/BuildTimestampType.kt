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
package com.tencent.devops.common.pipeline.pojo.time

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import io.swagger.annotations.ApiModel

@ApiModel("构建详情记录-时间戳类型（勿随意删除）")
enum class BuildTimestampType(
    @BkFieldI18n(
        translateType = I18nTranslateTypeEnum.VALUE,
        keyPrefixName = "buildTimestampType",
        reusePrefixFlag = false
    )
    val action: String
) {
    BUILD_REVIEW_WAITING("buildReviewWaiting"), // 流水线触发审核等待
    BUILD_CONCURRENCY_QUEUE("buildConcurrencyQueue"), // 流水线并发排队
    STAGE_CHECK_IN_WAITING("stageCheckInWaiting"), // stage准入等待
    STAGE_CHECK_OUT_WAITING("stageCheckOutWaiting"), // stage准出等待
    JOB_MUTEX_QUEUE("jobMutexQueue"), // job互斥并发排队
    JOB_THIRD_PARTY_QUEUE("jobThirdPartyQueue"), // job第三方构建机资源排队
    JOB_CONTAINER_STARTUP("jobContainerStartup"), // job构建机启动（包含了第三方构建机资源等待）
    JOB_CONTAINER_SHUTDOWN("jobContainerShutdown"), // job构建机关闭
    TASK_REVIEW_PAUSE_WAITING("taskReviewPauseWaiting"); // task等待（包括插件暂停、人工审核、质量红线审核）

    /*使插件处于等待的类型*/
    fun taskCheckWait() = this == TASK_REVIEW_PAUSE_WAITING

    /*使container处于排队的类型*/
    fun containerCheckQueue() = this == JOB_MUTEX_QUEUE || this == JOB_THIRD_PARTY_QUEUE

    /*使stage处于等待的类型*/
    fun stageCheckWait() = this == STAGE_CHECK_IN_WAITING || this == STAGE_CHECK_OUT_WAITING
}
