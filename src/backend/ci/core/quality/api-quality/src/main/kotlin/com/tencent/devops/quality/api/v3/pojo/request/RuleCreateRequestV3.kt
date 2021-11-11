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

package com.tencent.devops.quality.api.v3.pojo.request

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.quality.pojo.enum.RuleOperation
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("规则创建请求")
data class RuleCreateRequestV3(
    @ApiModelProperty("规则名称", required = true)
    val name: String,
    @ApiModelProperty("规则描述", required = true)
    val desc: String?,
    @ApiModelProperty("指标类型", required = true)
    val indicators: List<CreateRequestIndicator>,
    @ApiModelProperty("控制点位置", required = true)
    val position: String,
    @ApiModelProperty("生效的流水线id集合", required = true)
    val range: List<String>?,
    @ApiModelProperty("生效的流水线模板id集合", required = true)
    val templateRange: List<String>?,
    @ApiModelProperty("操作类型结合", required = false)
    val opList: List<CreateRequestOp>?,
    @ApiModelProperty("红线匹配的id", required = false)
    val gatewayId: String?,
    @ApiModelProperty("红线把关人", required = false)
    val gateKeepers: List<String>?,
    @ApiModelProperty("红线所在stage", required = false)
    val stageId: String,
    @ApiModelProperty("红线指定的任务节点", required = false)
    val taskSteps: List<CreateRequestTask>?
) {
    data class CreateRequestIndicator(
        val atomCode: String,
        val enName: String,
        val operation: String,
        val threshold: String
    )

    data class CreateRequestOp(
        @ApiModelProperty("操作类型", required = true)
        val operation: RuleOperation,
        @ApiModelProperty("通知类型", required = false)
        val notifyTypeList: List<NotifyType>?,
        @ApiModelProperty("通知组名单", required = false)
        val notifyGroupList: List<String>?,
        @ApiModelProperty("通知人员名单", required = false)
        val notifyUserList: List<String>?,
        @ApiModelProperty("审核通知人员", required = false)
        val auditUserList: List<String>?,
        @ApiModelProperty("审核超时时间", required = false)
        val auditTimeoutMinutes: Int?
    )

    data class CreateRequestTask(
        @ApiModelProperty("任务节点名", required = false)
        val taskName: String?,
        @ApiModelProperty("指标名", required = false)
        val indicatorEnName: String?
    )
}
