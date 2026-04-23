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

package com.tencent.devops.ai.constant

/**
 * AI 欢迎页快捷操作 / 用户提示词的交互方式（与库表 INTERACTION_TYPE 一致）。
 */
object AiInteractionType {
    /** 将预置文案填入输入框，用户补全后发送 */
    const val PROMPT_COMPLETION = "PROMPT_COMPLETION"

    /** 弹出结构化表单，提交后拼装消息（仅系统欢迎引导 ACTION） */
    const val FORM_COLLECT = "FORM_COLLECT"

    /** 点击即发送，无需用户输入 */
    const val DIRECT_TRIGGER = "DIRECT_TRIGGER"

    val USER_PROMPT_ALLOWED = setOf(PROMPT_COMPLETION, DIRECT_TRIGGER)

    val WELCOME_ACTION_ALLOWED = setOf(
        PROMPT_COMPLETION,
        FORM_COLLECT,
        DIRECT_TRIGGER
    )

    fun normalizeUserPromptType(raw: String?): String {
        val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: PROMPT_COMPLETION
        return if (t in USER_PROMPT_ALLOWED) t else PROMPT_COMPLETION
    }
}
