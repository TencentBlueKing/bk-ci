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

package com.tencent.devops.common.pipeline.pojo.element.trigger

import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.TapdEventType
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.v3.oas.annotations.media.Schema

/**
 * TAPD 事件触发器
 *
 * 注意：TAPD 触发器不属于代码库 Webhook 触发器体系，因此不继承 [WebHookTriggerElement]，
 * 而是直接继承 [Element]。TAPD 事件通过独立的 TapdEventTriggerBuildService 进行匹配与触发，
 * 不参与 WebhookElementParamsRegistrar/WebhookStartParamsRegistrar 的注册与查找。
 */
@Schema(title = "TAPD事件触发", description = TapdWebHookTriggerElement.classType)
data class TapdWebHookTriggerElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "TAPD事件触发",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "插件用户ID", required = false)
    override var stepId: String? = null,
    @get:Schema(title = "数据", required = true)
    val data: TapdWebHookTriggerData
) : Element(name, id, status) {
    companion object {
        const val classType = "codeTapdWebHookTrigger"
    }

    override fun getClassType() = classType

    override fun findFirstTaskIdByStartType(startType: StartType): String {
        return if (startType.name == StartType.WEB_HOOK.name) {
            this.id!!
        } else {
            super.findFirstTaskIdByStartType(startType)
        }
    }
}

data class TapdWebHookTriggerData(
    @get:Schema(title = "TAPD事件触发数据", required = false)
    val input: TapdWebHookTriggerInput
)

@Schema(title = "TAPD事件触发数据")
data class TapdWebHookTriggerInput(
    // 需要兼容老数据,应该是不能为空的
    @get:Schema(title = "TAPD workspaceId，关联触发的 TAPD 项目", required = true)
    val workspaceId: String? = "",
    @get:Schema(title = "事件类型 (story/bug)", required = true)
    val eventType: TapdEventType?,
    @get:Schema(
        title = "动作列表",
        required = false
    )
    val includeStoryAction: List<String>? = null,
    @get:Schema(
        title = "动作列表",
        required = false
    )
    val includeBugAction: List<String>? = null,
    @get:Schema(title = "用户白名单 (current_user)", required = false)
    val includeUsers: List<String>? = null,
    @get:Schema(title = "用户黑名单 (current_user)", required = false)
    val excludeUsers: List<String>? = null,
    @get:Schema(
        title = "事件来源",
        required = false
    )
    val includeEventFrom: List<String>? = null,
    @get:Schema(title = "监听标签", required = false)
    val includeLabels: String? = null,
    @get:Schema(title = "忽略标签", required = false)
    val excludeLabels: String? = null,
    @get:Schema(title = "监听优先级", required = false)
    val includePriority: String? = null,
    @get:Schema(title = "监听处理人", required = false)
    val includeOwner: List<String>? = null,
    @get:Schema(title = "忽略处理人", required = false)
    val excludeOwner: List<String>? = null
)
