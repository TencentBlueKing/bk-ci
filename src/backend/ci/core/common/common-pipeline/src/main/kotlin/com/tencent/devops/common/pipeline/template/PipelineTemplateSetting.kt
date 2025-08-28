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

package com.tencent.devops.common.pipeline.template

import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.pipeline.pojo.setting.PipelineRunLockType
import com.tencent.devops.common.pipeline.pojo.setting.Subscription
import com.tencent.devops.common.pipeline.utils.PIPELINE_RES_NUM_MIN
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_CONCURRENCY_GROUP_DEFAULT
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "模板配置")
data class PipelineTemplateSetting(
    /* 模板基础配置 */
    @get:Schema(title = "模板id", required = true)
    val templateId: String = "",
    @get:Schema(title = "版本号", required = true)
    val settingVersion: Int = 1,
    @get:Schema(title = "项目id", required = true)
    val projectId: String,
    @get:Schema(title = "标签ID列表", required = false)
    val labels: List<String>?,
    @get:Schema(title = "标签名称列表（仅用于前端展示，不参与数据保存）", required = false)
    val labelNames: List<String> = emptyList(),
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
    val runLockType: PipelineRunLockType?,
    @get:Schema(title = "最大排队时长", required = false)
    val waitQueueTimeMinute: Int = PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT,
    @get:Schema(title = "最大排队数量", required = false)
    val maxQueueSize: Int = PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT,
    @field:BkField(patternStyle = BkStyleEnum.PIPELINE_CONCURRENCY_GROUP_STYLE, required = false)
    @get:Schema(title = "并发时,设定的group", required = false)
    val concurrencyGroup: String? = PIPELINE_SETTING_CONCURRENCY_GROUP_DEFAULT,
    @get:Schema(title = "并发时,是否相同group取消正在执行的流水线", required = false)
    val concurrencyCancelInProgress: Boolean = false,
    @get:Schema(title = "并发构建数量限制", required = false)
    val maxConRunningQueueSize: Int? = null, // MULTIPLE类型时，并发构建数量限制

    /* 平台系统控制相关配置 —— 不作为生成版本的配置 */
    @get:Schema(title = "保存流水线编排的最大个数", required = false)
    val maxPipelineResNum: Int = PIPELINE_RES_NUM_MIN, // 保存流水线编排的最大个数
    @get:Schema(title = "重试时清理引擎变量表", required = false)
    val cleanvaliablesWhenRetry: Boolean? = false,
    @get:Schema(title = "YAML流水线特殊配置", required = false)
    val pipelineAsCodeSettings: PipelineAsCodeSettings?,
    @get:Schema(title = "创建人", required = true)
    val creator: String? = null,
    @get:Schema(title = "更新人", required = false)
    val updater: String? = null
) {

    companion object {
        fun defaultSetting(
            projectId: String,
            templateId: String,
            maxPipelineResNum: Int? = null,
            failSubscription: Subscription? = null,
            inheritedDialectSetting: Boolean? = null,
            pipelineDialectSetting: String? = null,
            creator: String,
            updater: String? = null
        ): PipelineTemplateSetting {
            return PipelineTemplateSetting(
                projectId = projectId,
                templateId = templateId,
                settingVersion = 1,
                maxPipelineResNum = maxPipelineResNum ?: PIPELINE_RES_NUM_MIN,
                waitQueueTimeMinute = PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT,
                maxQueueSize = PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT,
                runLockType = PipelineRunLockType.MULTIPLE,
                successSubscriptionList = emptyList(),
                failSubscriptionList = failSubscription?.let { listOf(it) },
                pipelineAsCodeSettings = PipelineAsCodeSettings.initDialect(
                    inheritedDialect = inheritedDialectSetting,
                    pipelineDialect = pipelineDialectSetting
                ),
                creator = creator,
                updater = updater,
                labels = emptyList()
            )
        }
    }
}
