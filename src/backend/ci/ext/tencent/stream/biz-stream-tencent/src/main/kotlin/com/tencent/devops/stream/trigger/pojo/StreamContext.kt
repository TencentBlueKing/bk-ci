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

package com.tencent.devops.stream.trigger.pojo

import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEventForHandle
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting

// TODO:  统一的上下文参数，除了消息队列，逐步替代stream触发中的所有参数传递对象
// Stream 触发构建时的上下文参数
interface StreamContext {
    // gitWebHook过来的事件原文
    val gitEvent: GitEvent

    // stream 当前对git event 做了预处理的部分数据
    val gitRequestEventForHandle: GitRequestEventForHandle

    // stream 项目的设置
    val streamSetting: GitCIBasicSetting
}

data class StreamRequestContext(
    override val gitEvent: GitEvent,
    override val gitRequestEventForHandle: GitRequestEventForHandle,
    override val streamSetting: GitCIBasicSetting
) : StreamContext

data class StreamTriggerContext(
    override val gitEvent: GitEvent,
    override val gitRequestEventForHandle: GitRequestEventForHandle,
    override val streamSetting: GitCIBasicSetting,
    // 用来构建的流水线
    val pipeline: GitProjectPipeline,
    // yaml原文配置
    val originYaml: String,
    // 变更文件列表
    val mrChangeSet: Set<String>?
) : StreamContext
