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
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 阶段流程控制
 * @version 1.0
 */
@Schema(title = "阶段流程控制模型")
data class StageControlOption(
    @get:Schema(title = "是否启用该阶段", required = false)
    val enable: Boolean = true, // 是否启用该阶段
    @get:Schema(title = "运行条件", required = false)
    val runCondition: StageRunCondition = StageRunCondition.AFTER_LAST_FINISHED, // 运行条件
    @get:Schema(title = "自定义变量", required = false)
    val customVariables: List<NameAndValue>? = emptyList(), // 自定义变量
    @get:Schema(title = "自定义条件", required = false)
    val customCondition: String? = null, // 自定义条件

    // 废弃旧数据字段
    @get:Schema(title = "是否人工触发", required = false)
    @Deprecated("被StagePauseCheck.manualTrigger代替")
    val manualTrigger: Boolean? = false,
    @get:Schema(title = "可触发用户，支持引用变量", required = false)
    @Deprecated("被StagePauseCheck.reviewGroups代替")
    var triggerUsers: List<String>? = null, // 可触发用户，支持引用变量
    @get:Schema(title = "已通过审核", required = false)
    @Deprecated("被StagePauseCheck.status代替")
    var triggered: Boolean? = null, // 已通过审核
    @get:Schema(title = "等待审核的超时时间", required = false)
    @Deprecated("被StagePauseCheck.timeout代替")
    val timeout: Int? = null, // 等待审核的超时时间
    @get:Schema(title = "审核变量", required = false)
    @Deprecated("被StagePauseCheck.reviewParams代替")
    var reviewParams: List<ManualReviewParam>? = null, // 审核变量
    @get:Schema(title = "审核说明", required = false)
    @Deprecated("被StagePauseCheck.reviewDesc代替")
    var reviewDesc: String? = null // 审核说明
)
