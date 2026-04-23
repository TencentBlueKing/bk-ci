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
 * AI 模块错误码常量（模块编号 33）。
 *
 * 错误码规则：21 + 33(AI模块) + 3位序号 = 7位数字
 */
object AiMessageCode {

    // ── 对话相关 (001-010) ──
    const val RUN_ALREADY_ACTIVE = "2133001"

    // ── 会话相关 (011-020) ──
    const val CREATE_SESSION_FAILED = "2133011"
    const val SESSION_NOT_FOUND = "2133012"
    const val SESSION_NO_PERMISSION = "2133013"

    // ── 提示词相关 (021-030) ──
    const val CREATE_PROMPT_FAILED = "2133021"
    const val PROMPT_NOT_FOUND = "2133022"
    const val PROMPT_NO_PERMISSION = "2133023"

    // ── 技能相关 (031-040) ──
    const val CREATE_SKILL_FAILED = "2133031"
    const val SKILL_NOT_FOUND = "2133032"
    const val SKILL_NO_PERMISSION = "2133033"

    // ── 外部智能体相关 (041-050) ──
    const val CREATE_EXTERNAL_AGENT_FAILED = "2133041"
    const val EXTERNAL_AGENT_NOT_FOUND = "2133042"
    const val EXTERNAL_AGENT_NO_PERMISSION = "2133043"

    // ── MCP 服务相关 (051-060) ──
    const val CREATE_MCP_SERVER_FAILED = "2133051"
    const val MCP_CLIENT_BUILD_FAILED = "2133052"
    const val MCP_SERVER_NOT_FOUND = "2133053"
    const val MCP_SERVER_NO_PERMISSION = "2133054"

    // ── 智能体服务间调用相关 (061-070) ──
    const val AGENT_NOT_FOUND = "2133061"
    const val AGENT_RUN_TIMEOUT = "2133062"
    const val AGENT_RUN_FAILED = "2133063"
}
