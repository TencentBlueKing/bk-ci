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

package com.tencent.devops.process.yaml.mq

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinding
import com.tencent.devops.process.yaml.pojo.YamlFileActionType
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.credential.AuthRepository
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "yaml文件事件")
@Event(StreamBinding.PIPELINE_YAML_LISTENER_FILE)
data class PipelineYamlFileEvent(
    @get:Schema(title = "触发用户", required = true)
    val userId: String,
    @get:Schema(title = "PAC授权用户,git默认是代码库oauth用户,用于创建/更新流水线时身份", required = true)
    val authUser: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "事件ID", required = true)
    val eventId: Long,
    @get:Schema(title = "代码库", required = true)
    val repository: Repository,
    @get:Schema(title = "默认分支", required = true)
    val defaultBranch: String,
    @get:Schema(title = "文件操作类型", required = true)
    val actionType: YamlFileActionType,
    @get:Schema(title = "文件路径", required = true)
    val filePath: String,
    @get:Schema(title = "旧的文件路径,重命名时才有值", required = false)
    val oldFilePath: String? = null,
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
    @get:Schema(
        title = "授权代码库,在fork仓库下,是使用fork仓库鉴权,yaml文件有变更时不能为空,其他使用repository权限",
        required = true
    )
    val authRepository: AuthRepository? = null,
    @get:Schema(title = "文件commit信息,yaml文件有变更时必传", required = true)
    val commit: FileCommit? = null,

    @get:Schema(title = "文件来源于fork仓库", required = true)
    val fork: Boolean = false,
    @get:Schema(title = "mr是否已合并", required = true)
    val merged: Boolean = false,
    @get:Schema(title = "源分支", required = true)
    val sourceBranch: String? = null,
    @get:Schema(title = "目标分支", required = true)
    val targetBranch: String? = null,
    @get:Schema(title = "源仓库URL", required = true)
    val sourceUrl: String? = null,
    @get:Schema(title = "源仓库全名", required = true)
    val sourceFullName: String? = null
) : IEvent() {
    @JsonIgnore
    val repoHashId = repository.repoHashId!!
}

data class FileCommit(
    @get:Schema(title = "提交ID", required = true)
    val commitId: String,
    @get:Schema(title = "提交信息", required = true)
    val commitMsg: String,
    @get:Schema(title = "提交时间", required = true)
    val commitTime: LocalDateTime,
    @get:Schema(title = "提交者", required = true)
    val committer: String
)
