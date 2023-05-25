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

package com.tencent.devops.process.engine.interceptor

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.setting.PipelineRunLockType
import io.swagger.annotations.ApiModelProperty

/**
 *
 *
 * @version 1.0
 */
data class InterceptData(
    val pipelineInfo: PipelineInfo,
    val model: Model?,
    val startType: StartType,
    val buildId: String,
    @ApiModelProperty("Lock 类型", required = false)
    val runLockType: PipelineRunLockType,
    @ApiModelProperty("最大排队时长", required = false)
    val waitQueueTimeMinute: Int,
    @ApiModelProperty("最大排队数量", required = false)
    val maxQueueSize: Int,
    @ApiModelProperty("并发时,设定的group", required = false)
    var concurrencyGroup: String?,
    @ApiModelProperty("并发时,是否相同group取消正在执行的流水线", required = false)
    val concurrencyCancelInProgress: Boolean = false,
    @ApiModelProperty("并发构建数量限制", required = false)
    val maxConRunningQueueSize: Int? // MULTIPLE类型时，并发构建数量限制
)
