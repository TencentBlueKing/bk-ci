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
 *
 */

package com.tencent.devops.process.pojo.webhook

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import io.swagger.v3.oas.annotations.media.Schema

data class PipelineWebhookVersion(
    @get:Schema(title = "项目id", required = false)
    val projectId: String,
    @get:Schema(title = "流水线id", required = false)
    val pipelineId: String,
    @get:Schema(title = "流水线版本", required = false)
    val version: Int,
    @get:Schema(title = "插件ID", required = false)
    val taskId: String,
    @get:Schema(title = "插件参数", required = false)
    val taskParams: String,
    @get:Schema(title = "插件代码库类型配置， ID 代码库HashId / NAME 别名", required = false)
    val taskRepoType: RepositoryType?,
    @get:Schema(title = "插件配置的代码库HashId，repoHashId与repoName 不能同时为空，如果两个都不为空就用repoName", required = false)
    var taskRepoHashId: String?, // repoHashId 与 repoName 不能同时为空，如果两个都不为空就用repoName
    @get:Schema(title = "代码库别名", required = false)
    val taskRepoName: String?,
    @get:Schema(title = "代码库类型，见ScmType枚举", required = false)
    val repositoryType: ScmType,
    @get:Schema(title = "代码库hashId,插件配置解析后的代码库ID", required = false)
    var repositoryHashId: String,
    @get:Schema(title = "事件类型", required = false)
    var eventType: String
)
