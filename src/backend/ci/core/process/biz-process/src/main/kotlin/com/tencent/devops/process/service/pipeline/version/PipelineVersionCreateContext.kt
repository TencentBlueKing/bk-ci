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

package com.tencent.devops.process.service.pipeline.version

import com.tencent.devops.common.pipeline.enums.CodeTargetAction
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.enums.OperationLogType
import com.tencent.devops.process.pojo.pipeline.PipelineBasicInfo
import com.tencent.devops.process.pojo.pipeline.PipelineModelBasicInfo
import com.tencent.devops.process.pojo.pipeline.PipelineResourceWithoutVersion
import com.tencent.devops.process.pojo.pipeline.PipelineTemplateInstanceBasicInfo
import com.tencent.devops.process.pojo.pipeline.PipelineYamlFileInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线版本创建上下文")
data class PipelineVersionCreateContext(
    @get:Schema(title = "用户ID", required = true)
    val userId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "版本,发布时才有值", required = true)
    val version: Int? = null,
    @get:Schema(title = "版本变更动作", required = true)
    val versionAction: PipelineVersionAction,

    @get:Schema(title = "是否校验权限", required = true)
    val checkPermission: Boolean = true,

    @get:Schema(title = "流水线信息,新流水线时,值为空", required = true)
    val pipelineInfo: PipelineInfo? = null,
    @get:Schema(title = "流水线信息", required = true)
    val pipelineBasicInfo: PipelineBasicInfo,
    @get:Schema(title = "流水线模型解析后数据", required = true)
    var pipelineModelBasicInfo: PipelineModelBasicInfo,
    @get:Schema(title = "流水线编排信息,没有版本信息", required = true)
    val pipelineResourceWithoutVersion: PipelineResourceWithoutVersion,
    @get:Schema(title = "流水线设置,没有版本信息", required = true)
    val pipelineSettingWithoutVersion: PipelineSetting,
    @get:Schema(title = "模版实例化信息", required = false)
    val templateInstanceBasicInfo: PipelineTemplateInstanceBasicInfo? = null,

    @get:Schema(title = "重置实例推荐版本为基准值", required = false)
    val resetBuildNo: Boolean? = false,
    @get:Schema(title = "是否开启PAC", required = true)
    val enablePac: Boolean = false,
    @get:Schema(title = "yaml文件分支信息", required = true)
    val yamlFileInfo: PipelineYamlFileInfo? = null,
    @get:Schema(title = "发布操作", required = false)
    val targetAction: CodeTargetAction? = null,
    @get:Schema(title = "当targetAction==COMMIT_TO_BRANCH,指定的分支", required = false)
    val targetBranch: String? = null,
    @get:Schema(title = "分支名,代码库推送的分支", required = false)
    val branchName: String? = null,
    @get:Schema(title = "合并请求连接", required = false)
    val pullRequestUrl: String? = null,
    @get:Schema(title = "合并请求ID", required = false)
    val pullRequestId: Long? = null,

    @get:Schema(title = "操作日志类型", required = false)
    var operationLogType: OperationLogType = OperationLogType.NORMAL_SAVE_OPERATION,
    @get:Schema(title = "操作日志参数", required = false)
    var operationLogParams: String = ""
)
