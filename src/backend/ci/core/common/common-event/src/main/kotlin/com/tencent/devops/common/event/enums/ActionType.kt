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

package com.tencent.devops.common.event.enums

/**
 * 事件动作
 * @version 1.0
 */
enum class ActionType {
    RETRY, // 重试
    START, // 开始
    REFRESH, // 刷新ElementAdditionalOptions
    END, // 强制结束当前节点，会导致当前构建容器结束
    SKIP, // 跳过-不执行
    TERMINATE, // 终止
    ARCHIVE, // 归档
    ;

    fun isStartOrRefresh() = isStart() || this == REFRESH

    fun isStart() = START == this || RETRY == this

    fun isEnd() = END == this || isTerminate()

    fun isTerminate() = TERMINATE == this

    fun isRetry() = RETRY == this

    companion object {
        @Deprecated(replaceWith = ReplaceWith("isStart"), message = "replace by isStart")
        fun isStart(actionType: ActionType) = actionType.isStart()

        @Deprecated(replaceWith = ReplaceWith("isEnd"), message = "replace by isEnd")
        fun isEnd(actionType: ActionType) = actionType.isEnd()

        @Deprecated(replaceWith = ReplaceWith("isTerminate"), message = "replace by isTerminate")
        fun isTerminate(actionType: ActionType) = actionType.isTerminate()
    }
}
