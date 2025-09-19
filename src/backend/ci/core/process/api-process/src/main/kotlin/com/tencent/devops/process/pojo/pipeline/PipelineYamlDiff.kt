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

package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.process.pojo.pipeline.enums.YamDiffFileStatus
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileActionType
import com.tencent.devops.process.pojo.pipeline.enums.YamlFileType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "yaml文件变更记录")
data class PipelineYamlDiff(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "事件ID", required = true)
    val eventId: Long,
    @get:Schema(title = "事件类型", required = true)
    val eventType: String,
    @get:Schema(title = "代码库hashId", required = true)
    val repoHashId: String,
    @get:Schema(title = "默认分支", required = true)
    val defaultBranch: String,
    @get:Schema(title = "文件路径", required = true)
    val filePath: String,
    @get:Schema(title = "文件类型", required = true)
    val fileType: YamlFileType,
    @get:Schema(title = "文件操作类型", required = true)
    val actionType: YamlFileActionType,
    @get:Schema(title = "文件处理状态", required = true)
    val status: YamDiffFileStatus = YamDiffFileStatus.PENDING,
    @get:Schema(title = "触发用户", required = true)
    val triggerUser: String,
    @get:Schema(
        title = "文件来源分支, " +
                "push: 提交的分支," +
                "mr: 根据mr的状态,可能是源分支也可能是目标分支，合并前是源分支,合并后是目标分支," +
                "fork库，源分支由组+分支组成",
        required = true
    )
    val ref: String,
    @get:Schema(title = "文件blob, 删除action时可为空", required = true)
    val blobId: String? = null,
    @get:Schema(title = "提交ID", required = true)
    val commitId: String? = null,
    @get:Schema(title = "提交信息", required = true)
    val commitMsg: String? = null,
    @get:Schema(title = "提交时间", required = true)
    val commitTime: LocalDateTime? = null,
    @get:Schema(title = "提交者", required = true)
    val committer: String? = null,

    @get:Schema(title = "文件来源于fork仓库", required = false)
    val fork: Boolean = false,
    @get:Schema(
        title = "使用fork仓库token,当文件内容需要从fork仓库读取时,则使用fork仓库凭证,也就是触发人的token",
        required = false
    )
    val useForkToken: Boolean = false,
    @get:Schema(title = "mr是否已合并", required = false)
    val merged: Boolean = false,
    @get:Schema(title = "合并请求ID", required = false)
    val pullRequestId: Long? = null,
    @get:Schema(title = "合并请求编号", required = false)
    val pullRequestNumber: Int? = null,
    @get:Schema(title = "合并请求连接", required = false)
    val pullRequestUrl: String? = null,
    @get:Schema(title = "源分支", required = false)
    val sourceBranch: String? = null,
    @get:Schema(title = "目标分支", required = false)
    val targetBranch: String? = null,
    @get:Schema(title = "源仓库URL", required = false)
    val sourceRepoUrl: String? = null,
    @get:Schema(title = "源仓库全名", required = false)
    val sourceFullName: String? = null,
    @get:Schema(title = "目标仓库URL", required = false)
    val targetRepoUrl: String? = null,
    @get:Schema(title = "目标仓库全名", required = false)
    val targetFullName: String? = null,
    @get:Schema(title = "旧的文件路径,当action为RENAME时有值", required = false)
    val oldFilePath: String? = null,
    @get:Schema(
        title = "依赖的文件路径,当action为DEPENDENCY_UPGRADE和DEPENDENCY_UPGRADE_AND_TRIGGER有值",
        required = false
    )
    val dependentFilePath: String? = null,
    @get:Schema(
        title = "依赖的分支,当action为DEPENDENCY_UPGRADE和DEPENDENCY_UPGRADE_AND_TRIGGER有值",
        required = false
    )
    val dependentRef: String? = null,
    @get:Schema(
        title = "依赖的文件blobId,当action为DEPENDENCY_UPGRADE和DEPENDENCY_UPGRADE_AND_TRIGGER有值",
        required = false
    )
    val dependentBlobId: String? = null
)
