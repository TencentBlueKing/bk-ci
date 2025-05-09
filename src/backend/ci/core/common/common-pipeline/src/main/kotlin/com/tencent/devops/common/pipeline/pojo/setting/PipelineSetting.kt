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

package com.tencent.devops.common.pipeline.pojo.setting

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.utils.PIPELINE_RES_NUM_MIN
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_CONCURRENCY_GROUP_DEFAULT
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线配置")
data class PipelineSetting(
    @get:Schema(title = "项目id", required = false, readOnly = true)
    var projectId: String = "",
    @get:Schema(title = "流水线id", required = false, readOnly = true)
    var pipelineId: String = "",

    // 流水线基础配置
    @get:Schema(title = "流水线名称", required = false)
    var pipelineName: String = "",
    @get:Schema(title = "版本", required = false)
    var version: Int = 1,
    @get:Schema(title = "描述", required = false)
    var desc: String = "",
    @get:Schema(title = "标签ID列表", required = false)
    var labels: List<String> = emptyList(),
    @get:Schema(title = "标签名称列表（仅用于前端展示，不参与数据保存）", required = false)
    var labelNames: List<String> = emptyList(),
    @field:BkField(patternStyle = BkStyleEnum.BUILD_NUM_RULE_STYLE, required = false)
    @get:Schema(title = "构建号生成规则", required = false)
    var buildNumRule: String? = null, // 构建号生成规则

    // 通知订阅相关配置
    @Deprecated("被successSubscriptionList取代")
    @get:Schema(title = "订阅成功相关", required = false)
    var successSubscription: Subscription? = Subscription(),
    @Deprecated("被failSubscriptionList取代")
    @get:Schema(title = "订阅失败相关", required = false)
    var failSubscription: Subscription? = Subscription(),
    @get:Schema(title = "订阅成功通知组", required = false)
    var successSubscriptionList: List<Subscription>? = null,
    @get:Schema(title = "订阅失败通知组", required = false)
    var failSubscriptionList: List<Subscription>? = null,

    // 运行控制、流水线禁用相关配置
    @get:Schema(title = "Lock 类型", required = false)
    var runLockType: PipelineRunLockType = PipelineRunLockType.SINGLE_LOCK,
    @get:Schema(title = "最大排队时长", required = false)
    var waitQueueTimeMinute: Int = PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT,
    @get:Schema(title = "最大排队数量", required = false)
    var maxQueueSize: Int = PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT,
    @field:BkField(patternStyle = BkStyleEnum.PIPELINE_CONCURRENCY_GROUP_STYLE, required = false)
    @get:Schema(title = "并发时,设定的group", required = false)
    var concurrencyGroup: String? = PIPELINE_SETTING_CONCURRENCY_GROUP_DEFAULT,
    @get:Schema(title = "并发时,是否相同group取消正在执行的流水线", required = false)
    var concurrencyCancelInProgress: Boolean = false,
    @get:Schema(title = "并发构建数量限制", required = false)
    var maxConRunningQueueSize: Int? = null, // MULTIPLE类型时，并发构建数量限制
    @get:Schema(title = "是否配置流水线变量值超长时终止执行", required = false)
    var failIfVariableInvalid: Boolean? = false,

    // 平台系统控制相关配置 —— 不作为生成版本的配置
    @get:Schema(title = "保存流水线编排的最大个数", required = false)
    val maxPipelineResNum: Int = PIPELINE_RES_NUM_MIN, // 保存流水线编排的最大个数
    @get:Schema(title = "重试时清理引擎变量表", required = false)
    val cleanVariablesWhenRetry: Boolean? = false,
    @get:Schema(title = "YAML流水线特殊配置", required = false)
    var pipelineAsCodeSettings: PipelineAsCodeSettings?
) {

    companion object {

        fun defaultSetting(
            projectId: String,
            pipelineId: String,
            pipelineName: String,
            maxPipelineResNum: Int? = null,
            failSubscription: Subscription? = null,
            inheritedDialectSetting: Boolean? = null,
            pipelineDialectSetting: String? = null
        ): PipelineSetting {
            return PipelineSetting(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineName = pipelineName,
                version = 1,
                desc = pipelineName,
                maxPipelineResNum = maxPipelineResNum ?: PIPELINE_RES_NUM_MIN,
                waitQueueTimeMinute = PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT,
                maxQueueSize = PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT,
                runLockType = PipelineRunLockType.MULTIPLE,
                successSubscription = null,
                failSubscription = null,
                successSubscriptionList = emptyList(),
                failSubscriptionList = failSubscription?.let { listOf(it) },
                pipelineAsCodeSettings = PipelineAsCodeSettings.initDialect(
                    inheritedDialect = inheritedDialectSetting,
                    pipelineDialect = pipelineDialectSetting
                )
            )
        }
    }

    // 校验流水线的通知设置是否为空，即用户为配置或使用默认配置
    fun notifySettingIsNull(): Boolean {
        var res = true
        if (!this.successSubscriptionList.isNullOrEmpty() &&
            this.successSubscriptionList!!.any { it.types.isNotEmpty() }
        ) {
            res = false
        }
        if (!this.failSubscriptionList.isNullOrEmpty() &&
            this.failSubscriptionList!!.any { it.types.isNotEmpty() }
        ) {
            res = false
        }
        if (this.successSubscription?.types?.isNotEmpty() == true) {
            res = false
        }
        if (this.failSubscription?.types?.isNotEmpty() == true) {
            res = false
        }
        return res
    }

    // 校验流水线的并发组设置是否为空，即用户为配置或使用默认配置
    fun concurrencySettingIsNull(): Boolean {
        return this.runLockType != PipelineRunLockType.GROUP_LOCK
    }

    fun fixSubscriptions() {
        // 只有旧数据向新数据的更新，取消旧数据的保存
        if (successSubscriptionList == null && this.successSubscription != null) {
            successSubscriptionList = listOf(this.successSubscription!!)
        }
        successSubscription = successSubscriptionList!!.firstOrNull()
        if (failSubscriptionList == null && this.failSubscription != null) {
            failSubscriptionList = listOf(this.failSubscription!!)
        }
        failSubscription = failSubscriptionList!!.firstOrNull()
    }

    fun copySubscriptionSettings(other: PipelineSetting) {
        successSubscription = other.successSubscription
        successSubscriptionList = other.successSubscriptionList
        failSubscription = other.failSubscription
        failSubscriptionList = other.failSubscriptionList
    }

    fun copyConcurrencyGroup(other: PipelineSetting) {
        concurrencyGroup = other.concurrencyGroup
        concurrencyCancelInProgress = other.concurrencyCancelInProgress
        maxConRunningQueueSize = PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
    }
}
