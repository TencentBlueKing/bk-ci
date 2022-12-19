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

package com.tencent.devops.stream.trigger.actions.data

import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.stream.trigger.actions.data.context.StreamTriggerContext

/**
 * 保存action需要用到的元数据
 * @param event 各源的事件原文
 * @param context stream触发过程中会用到的上下文数据
 */
@Suppress("MaxLineLength")
data class ActionData(
    val event: CodeWebhookEvent,
    var context: StreamTriggerContext
) {
    // 需要根据各事件源的event去拿的通用数据，随event改变可能会不同
    lateinit var eventCommon: EventCommonData

    // Stream触发时需要的配置信息
    lateinit var setting: StreamTriggerSetting
    val isSettingInitialized get() = this::setting.isInitialized

    // 方便日志打印
    fun format() = "${event::class.qualifiedName}|$context|$eventCommon|$setting"

    /**
     * 提供拿取gitProjectId的公共方法
     * 因为会存在跨库触发导致的event的gitProjectId和触发的不一致的问题
     * 所以会优先拿取pipeline的gitProjectId
     */
    fun getGitProjectId() = context.pipeline?.gitProjectId ?: eventCommon.gitProjectId

    /**
     *
     *  buildUserID只会是远程仓库触发时才会存在，并且是在校验token后塞入了。此后触发逻辑使用userId为配置凭证对应的userId
     *  其余情况使用触发event的userId
     *  @see com.tencent.devops.stream.trigger.actions.streamActions.StreamRepoTriggerAction.triggerCheckRepoTriggerCredentials
     */
    fun getUserId() = context.repoTrigger?.buildUserID ?: eventCommon.userId
}
