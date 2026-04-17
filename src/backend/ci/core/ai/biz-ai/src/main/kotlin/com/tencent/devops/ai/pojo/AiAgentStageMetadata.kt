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

package com.tencent.devops.ai.pojo

/**
 * 智能体执行阶段（T_AI_AGENT_STAGE）持久化字段取值元数据。
 */
object AiAgentStageMetadata {

    enum class SessionStatus(val value: String) {
        RUNNING("RUNNING"),
        SUCCESS("SUCCESS"),
        ERROR("ERROR"),
        TIMEOUT("TIMEOUT"),
        CANCELLED("CANCELLED")
    }

    /** T_AI_AGENT_STAGE.STAGE_TYPE 等业务侧约定的阶段类型取值。 */
    enum class StageType(val value: String) {
        REASONING("REASONING"),
        TOOL_CALL("TOOL_CALL"),

        /** 单次用户轮次：PreCall → PostCall（含多轮 ReAct） */
        AGENT_CALL("AGENT_CALL"),

        /** AutoContext 触发的上下文摘要 / 压缩（调用模型） */
        CONTEXT_SUMMARY("CONTEXT_SUMMARY"),

        /** 框架 ErrorEvent，无配对 Post */
        ERROR("ERROR")
    }
}
