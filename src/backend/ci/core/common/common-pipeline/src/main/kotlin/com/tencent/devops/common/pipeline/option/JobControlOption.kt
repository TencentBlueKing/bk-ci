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

package com.tencent.devops.common.pipeline.option

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.DependOnType
import com.tencent.devops.common.pipeline.enums.JobRunCondition
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Job流程控制
 * @version 1.0
 */
@Schema(title = "job流程控制模型")
data class JobControlOption(
    @get:Schema(title = "是否启用Job", required = false)
    val enable: Boolean = true, // 是否启用Job
    @get:Schema(title = "Job准备环境的超时时间 分钟Minutes", required = false)
    val prepareTimeout: Int? = null, // Job准备环境的超时时间 分钟Minutes
    @get:Schema(title = "ob执行的超时时间 分钟Minutes", required = false)
    var timeout: Int? = 900, // Job执行的超时时间 分钟Minutes
    @get:Schema(title = "新的Job执行的超时时间，支持变量 分钟Minutes，出错则取timeout的值", required = false)
    var timeoutVar: String? = null, // Job执行的超时时间 分钟Minutes
    @get:Schema(title = "运行条件", required = false)
    val runCondition: JobRunCondition = JobRunCondition.STAGE_RUNNING, // 运行条件
    @get:Schema(title = "自定义变量", required = false)
    val customVariables: List<NameAndValue>? = emptyList(), // 自定义变量
    @get:Schema(title = "自定义条件", required = false)
    val customCondition: String? = null, // 自定义条件
    @get:Schema(title = "job依赖", required = false) // job依赖
    val dependOnType: DependOnType? = null,
    @get:Schema(title = "需要过滤不存在的job，定义为var类型", required = false)
    var dependOnId: List<String>? = null, // 需要过滤不存在的job，定义为var类型
    @get:Schema(title = "job依赖名称", required = false)
    val dependOnName: String? = null,
    @get:Schema(title = "containerId与jobId映射，depend on运行时使用的是containerId", required = false)
    var dependOnContainerId2JobIds: Map<String, String>? = null, // containerId与jobId映射，depend on运行时使用的是containerId
    @get:Schema(title = "是否失败继续", required = false)
    val continueWhenFailed: Boolean? = false, // 失败继续
    @get:Schema(title = "第三方构建机集群-单节点并发限制")
    val singleNodeConcurrency: Int? = null,
    @get:Schema(title = "第三方构建机集群-所有节点并发限制")
    val allNodeConcurrency: Int? = null
)
