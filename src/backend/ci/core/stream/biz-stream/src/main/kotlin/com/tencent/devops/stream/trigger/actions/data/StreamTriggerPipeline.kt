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

import com.tencent.devops.model.stream.tables.records.TGitPipelineResourceRecord
import com.tencent.devops.stream.pojo.StreamGitProjectPipeline

/**
 * Stream触发时需要的流水线数据
 * @param gitProjectId 流水线所属的git项目唯一标识
 * @param pipelineId 流水线唯一标识
 * @param filePath 流水线对应的yaml路径
 * @param displayName 流水线别名
 * @param enabled 流水线是否开启
 * @param lastUpdateBranch 最后一次触发的分支
 */
data class StreamTriggerPipeline(
    val gitProjectId: String,
    var pipelineId: String,
    val filePath: String,
    var displayName: String,
    val enabled: Boolean,
    val creator: String?,
    val lastUpdateBranch: String? = "",
    var lastModifier: String? = ""
) {
    constructor(pipeline: StreamGitProjectPipeline) : this(
        gitProjectId = pipeline.gitProjectId.toString(),
        pipelineId = pipeline.pipelineId,
        filePath = pipeline.filePath,
        displayName = pipeline.displayName,
        enabled = pipeline.enabled,
        creator = pipeline.creator,
        lastUpdateBranch = pipeline.lastUpdateBranch
    )

    constructor(pipeline: TGitPipelineResourceRecord) : this(
        gitProjectId = pipeline.gitProjectId.toString(),
        pipelineId = pipeline.pipelineId,
        filePath = pipeline.filePath,
        displayName = pipeline.displayName,
        enabled = pipeline.enabled,
        creator = pipeline.creator,
        lastUpdateBranch = pipeline.lastUpdateBranch
    )

    fun toGitPipeline(): StreamGitProjectPipeline {
        return with(this) {
            StreamGitProjectPipeline(
                gitProjectId = gitProjectId.toLong(),
                displayName = displayName,
                pipelineId = pipelineId,
                filePath = filePath,
                enabled = enabled,
                creator = creator,
                null,
                null,
                lastUpdateBranch = lastUpdateBranch
            )
        }
    }
}
