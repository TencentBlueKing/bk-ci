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

package com.tencent.devops.process.pojo.setting

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线版本记录")
data class PipelineSettingVersion(
    @get:Schema(title = "项目id", required = false)
    val projectId: String,
    @get:Schema(title = "流水线id", required = false)
    val pipelineId: String,

    // 流水线基础配置
    @get:Schema(title = "流水线名称", required = false)
    var pipelineName: String? = "",
    @get:Schema(title = "版本", required = false)
    var version: Int,
    @get:Schema(title = "描述", required = false)
    val desc: String?,
    @get:Schema(title = "标签列表", required = false)
    var labels: List<String>?,
    @field:BkField(patternStyle = BkStyleEnum.BUILD_NUM_RULE_STYLE, required = false)
    @get:Schema(title = "构建号生成规则", required = false)
    val buildNumRule: String?, // 构建号生成规则

    // 通知订阅相关配置
    @get:Schema(title = "订阅成功通知组", required = false)
    var successSubscriptionList: List<Subscription>?,
    @get:Schema(title = "订阅失败通知组", required = false)
    var failSubscriptionList: List<Subscription>?,

    // 运行控制、流水线禁用相关配置
    @get:Schema(title = "Lock 类型", required = false)
    val runLockType: PipelineRunLockType?,
    @get:Schema(title = "最大排队时长", required = false)
    val waitQueueTimeMinute: Int?,
    @get:Schema(title = "最大排队数量", required = false)
    val maxQueueSize: Int?,
    @field:BkField(patternStyle = BkStyleEnum.PIPELINE_CONCURRENCY_GROUP_STYLE, required = false)
    @get:Schema(title = "并发时,设定的group", required = false)
    var concurrencyGroup: String?,
    @get:Schema(title = "并发时,是否相同group取消正在执行的流水线", required = false)
    var concurrencyCancelInProgress: Boolean?,
    @get:Schema(title = "并发构建数量限制", required = false)
    var maxConRunningQueueSize: Int? = null, // MULTIPLE类型时，并发构建数量限制
    @get:Schema(title = "是否配置流水线变量值超长时终止执行", required = false)
    val failIfVariableInvalid: Boolean? = false,
    @get:Schema(title = "YAML流水线特殊配置", required = false)
    var pipelineAsCodeSettings: PipelineAsCodeSettings? = null
) {
    companion object {

        // 该方法列出所有需要作为修改比对版本的参数，如果有修改则增加版本
        fun convertFromSetting(setting: PipelineSetting) = PipelineSettingVersion(
            projectId = setting.projectId,
            pipelineId = setting.pipelineId,
            version = setting.version,
            pipelineName = setting.pipelineName,
            desc = setting.desc,
            runLockType = setting.runLockType,
            successSubscriptionList = setting.successSubscriptionList,
            failSubscriptionList = setting.failSubscriptionList,
            labels = setting.labels,
            waitQueueTimeMinute = setting.waitQueueTimeMinute,
            maxQueueSize = setting.maxQueueSize,
            buildNumRule = setting.buildNumRule,
            concurrencyCancelInProgress = setting.concurrencyCancelInProgress,
            concurrencyGroup = setting.concurrencyGroup,
            maxConRunningQueueSize = setting.maxConRunningQueueSize,
            pipelineAsCodeSettings = setting.pipelineAsCodeSettings
        )
    }
}
