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

package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.quality.pojo.enum.RuleOperation
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "红线实体类")
data class QualityRule(
    @get:Schema(title = "hashId", required = true)
    val hashId: String,
    @get:Schema(title = "红线名字", required = true)
    val name: String,
    @get:Schema(title = "红线描述", required = true)
    val desc: String,
    @get:Schema(title = "红线指标列表", required = true)
    val indicators: List<QualityIndicator>,
    @get:Schema(title = "控制点", required = true)
    val controlPoint: RuleControlPoint,
    @get:Schema(title = "流水线范围", required = true)
    val range: List<String>,
    @get:Schema(title = "模板范围", required = true)
    val templateRange: List<String>,
    @get:Schema(title = "操作类型", required = true)
    val operation: RuleOperation,
    @get:Schema(title = "通知类型", required = false)
    val notifyTypeList: List<NotifyType>?,
    @get:Schema(title = "通知组名单", required = false)
    val notifyGroupList: List<String>?,
    @get:Schema(title = "通知人员名单", required = false)
    val notifyUserList: List<String>?,
    @get:Schema(title = "审核通知人员", required = false)
    val auditUserList: List<String>?,
    @get:Schema(title = "审核超时时间", required = false)
    val auditTimeoutMinutes: Int?,
    @get:Schema(title = "操作类型列表", required = false)
    val opList: List<RuleOp>? = null,
    @get:Schema(title = "红线匹配的id", required = false)
    val gatewayId: String?,
    @get:Schema(title = "红线把关人", required = false)
    val gateKeepers: List<String>?,
    @get:Schema(title = "红线状态", required = false)
    val status: RuleInterceptResult?,
    @get:Schema(title = "红线所在stage", required = true)
    val stageId: String,
    @get:Schema(title = "红线指定的任务节点名", required = false)
    val taskSteps: List<RuleTask>?
) {
    data class RuleControlPoint(
        val hashId: String,
        val name: String,
        val cnName: String,
        val position: ControlPointPosition,
        val availablePosition: List<ControlPointPosition>
    )

    data class RuleOp(
        @get:Schema(title = "操作类型", required = true)
        val operation: RuleOperation,
        @get:Schema(title = "通知类型", required = false)
        val notifyTypeList: List<NotifyType>?,
        @get:Schema(title = "通知组名单", required = false)
        val notifyGroupList: List<String>?,
        @get:Schema(title = "通知人员名单", required = false)
        val notifyUserList: List<String>?,
        @get:Schema(title = "审核通知人员", required = false)
        val auditUserList: List<String>?,
        @get:Schema(title = "审核超时时间", required = false)
        val auditTimeoutMinutes: Int?
    )

    data class RuleTask(
        @get:Schema(title = "任务节点名", required = false)
        val taskName: String?,
        @get:Schema(title = "指标名", required = false)
        val indicatorEnName: String?
    )
}
