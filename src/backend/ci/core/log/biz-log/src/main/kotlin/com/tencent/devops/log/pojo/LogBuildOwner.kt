/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
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

package com.tencent.devops.log.pojo

/**
 * log 服务本地维护的 buildId 归属，不依赖 process。
 *
 * 字段均可空：旧 Worker 可能只报 projectId、或两者都未报。
 * 已写入的非空字段禁止覆盖，仅允许把空位补全。
 */
data class LogBuildOwner(
    val projectId: String?,
    val pipelineId: String?
) {
    fun hasAny(): Boolean = !projectId.isNullOrBlank() || !pipelineId.isNullOrBlank()

    /**
     * 只补空，不覆盖已有归属。上报值与已有值冲突时保留已有值。
     */
    fun fillEmpty(reported: LogBuildOwner): LogBuildOwner {
        return LogBuildOwner(
            projectId = projectId.takeIf { !it.isNullOrBlank() } ?: reported.projectId,
            pipelineId = pipelineId.takeIf { !it.isNullOrBlank() } ?: reported.pipelineId
        )
    }

    companion object {
        val EMPTY = LogBuildOwner(null, null)

        fun of(projectId: String?, pipelineId: String?): LogBuildOwner {
            return LogBuildOwner(
                projectId = projectId?.takeIf { it.isNotBlank() },
                pipelineId = pipelineId?.takeIf { it.isNotBlank() }
            )
        }
    }
}
