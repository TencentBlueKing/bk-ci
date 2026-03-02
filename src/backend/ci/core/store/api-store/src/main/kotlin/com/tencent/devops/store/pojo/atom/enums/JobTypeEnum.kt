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

package com.tencent.devops.store.pojo.atom.enums

enum class JobTypeEnum {
    AGENT,  // 编译环境（PIPELINE）
    AGENT_LESS,  // 无编译环境（PIPELINE）
    CREATIVE_STREAM,  // 创作流编译环境（CREATIVE_STREAM）
    CLOUD_TASK;  // 云任务无编译环境（CREATIVE_STREAM）

    /**
     * 是否属于「编译环境」类型（对应 VMBuildContainer / MarketBuildAtomElement）。
     * AGENT、CREATIVE_STREAM → true；AGENT_LESS、CLOUD_TASK → false。
     */
    fun isBuildEnv(): Boolean = this == AGENT || this == CREATIVE_STREAM

    companion object {
        /**
         * 从 T_ATOM.JOB_TYPE 原始值解析出所有 JobTypeEnum。
         * 兼容三种格式：
         * - 纯字符串："AGENT"
         * - V1 JSON：{"PIPELINE":"AGENT","CREATIVE_STREAM":"CREATIVE_STREAM"}
         * - V2 JSON：{"PIPELINE":["AGENT"],"CREATIVE_STREAM":["CREATIVE_STREAM","CLOUD_TASK"]}
         */
        fun parseAllFromRaw(raw: String?): List<JobTypeEnum> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching { listOf(valueOf(raw)) }.getOrElse {
                entries.filter { raw.contains("\"${it.name}\"") }
            }
        }
    }
}
