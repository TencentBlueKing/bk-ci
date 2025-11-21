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

package com.tencent.devops.common.api.context

/**
 * Channel上下文管理，用于统一管理请求头中的X-DEVOPS-CHANNEL信息
 * 支持HTTP请求、跨服务调用、MQ消息、异步调用等场景
 */
object ChannelContext {
    private val channelThreadLocal = ThreadLocal<String?>()
    
    /**
     * 从ThreadLocal获取Channel（HTTP请求场景）
     */
    fun getChannel(): String? {
        return channelThreadLocal.get()
    }
    
    /**
     * 设置Channel（用于显式传递，跨服务调用、MQ等场景）
     */
    fun setChannel(channel: String?) {
        channelThreadLocal.set(channel)
    }
    
    /**
     * 清除ThreadLocal
     */
    fun clear() {
        channelThreadLocal.remove()
    }
    
    /**
     * 在指定上下文中执行代码（用于异步、跨线程场景）
     */
    fun <T> withChannel(channel: String?, block: () -> T): T {
        val oldChannel = getChannel()
        try {
            setChannel(channel)
            return block()
        } finally {
            setChannel(oldChannel)
        }
    }
}

