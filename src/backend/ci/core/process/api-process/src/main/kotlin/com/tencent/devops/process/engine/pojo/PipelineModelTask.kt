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

package com.tencent.devops.process.engine.pojo

import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions

/**
 * 流水线模型插件任务
 */
data class PipelineModelTask(
    val projectId: String,
    val pipelineId: String,
    val stageId: String,
    val containerId: String,
    val taskId: String,
    val taskSeq: Int,
    val taskName: String,
    val atomCode: String,
    val atomVersion: String? = null,
    val classType: String,
    val taskAtom: String,
    val taskParams: MutableMap<String, Any>,
    val additionalOptions: ElementAdditionalOptions?,
    val os: String? = "linux",
    val taskPosition: String = "", // 插件在model中的位置，eg: stageSeq-jobSeq-taskSeq
    val stageEnable: Boolean = true,
    val containerEnable: Boolean = true
) {
    fun getTaskParam(paramName: String): String {
        return if (taskParams[paramName] != null) {
            taskParams[paramName].toString().trim()
        } else {
            ""
        }
    }
}
