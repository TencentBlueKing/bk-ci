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

package com.tencent.devops.process.pojo.template.v2

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.pojo.setting.BuildCancelPolicy
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.utils.PIPELINE_RES_NUM_MIN
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板配置更新")
data class PipelineTemplateSettingUpdateInfo(
    @get:Schema(title = "模板名称", required = false)
    val name: String? = null,
    @get:Schema(title = "模板描述", required = false)
    val desc: String? = null,
    @get:Schema(title = "标签ID列表", required = false)
    val labels: List<String>? = null,
    @get:Schema(title = "标签名称列表（仅用于前端展示，不参与数据保存）", required = false)
    val labelNames: List<String>? = null,
    @field:BkField(patternStyle = BkStyleEnum.BUILD_NUM_RULE_STYLE, required = false)
    @get:Schema(title = "构建号生成规则", required = false)
    val buildNumRule: String? = null, // 构建号生成规则

    /* 通知订阅相关配置 */
    @get:Schema(title = "订阅成功通知组", required = false)
    val successSubscriptionList: List<Subscription>? = null,
    @get:Schema(title = "订阅失败通知组", required = false)
    val failSubscriptionList: List<Subscription>? = null,

    /* 运行控制、流水线禁用相关配置 */
    @get:Schema(title = "Lock 类型", required = false)
    val runLockType: PipelineRunLockType? = null,
    @get:Schema(title = "最大排队时长", required = false)
    val waitQueueTimeMinute: Int? = null,
    @get:Schema(title = "最大排队数量", required = false)
    val maxQueueSize: Int? = null,
    @field:BkField(patternStyle = BkStyleEnum.PIPELINE_CONCURRENCY_GROUP_STYLE, required = false)
    @get:Schema(title = "并发时,设定的group", required = false)
    val concurrencyGroup: String? = null,
    @get:Schema(title = "并发时,是否相同group取消正在执行的流水线", required = false)
    val concurrencyCancelInProgress: Boolean? = null,
    @get:Schema(title = "并发构建数量限制", required = false)
    val maxConRunningQueueSize: Int? = null, // MULTIPLE类型时，并发构建数量限制
    @get:Schema(title = "是否配置流水线变量值超长时终止执行", required = false)
    var failIfVariableInvalid: Boolean? = false,
    @get:Schema(title = "构建取消权限策略", required = false)
    var buildCancelPolicy: BuildCancelPolicy? = null,

    /* 平台系统控制相关配置 —— 不作为生成版本的配置 */
    @get:Schema(title = "保存流水线编排的最大个数", required = false)
    val maxPipelineResNum: Int = PIPELINE_RES_NUM_MIN, // 保存流水线编排的最大个数
    @get:Schema(title = "重试时清理引擎变量表", required = false)
    val cleanvaliablesWhenRetry: Boolean? = null,
    @get:Schema(title = "YAML流水线特殊配置", required = false)
    val pipelineAsCodeSettings: PipelineAsCodeSettings? = null,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null
) {
    constructor(userId: String, pipelineSetting: PipelineSetting) : this(
        name = pipelineSetting.pipelineName,
        desc = pipelineSetting.desc,
        labels = pipelineSetting.labels,
        labelNames = pipelineSetting.labelNames,
        buildNumRule = pipelineSetting.buildNumRule,
        successSubscriptionList = pipelineSetting.successSubscriptionList,
        failSubscriptionList = pipelineSetting.failSubscriptionList,
        runLockType = pipelineSetting.runLockType,
        waitQueueTimeMinute = pipelineSetting.waitQueueTimeMinute,
        maxQueueSize = pipelineSetting.maxQueueSize,
        concurrencyGroup = pipelineSetting.concurrencyGroup,
        concurrencyCancelInProgress = pipelineSetting.concurrencyCancelInProgress,
        maxConRunningQueueSize = pipelineSetting.maxConRunningQueueSize,
        failIfVariableInvalid = pipelineSetting.failIfVariableInvalid,
        buildCancelPolicy = pipelineSetting.buildCancelPolicy,
        maxPipelineResNum = pipelineSetting.maxPipelineResNum,
        pipelineAsCodeSettings = pipelineSetting.pipelineAsCodeSettings,
        updater = userId
    )
}
