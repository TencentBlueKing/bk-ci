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

package com.tencent.devops.stream.pojo

import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.webhook.enums.code.StreamGitObjectKind
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("V2版本多选搜索过滤历史参数")
data class StreamBuildHistorySearch(
    @ApiModelProperty("第几页", required = false)
    val page: Int?,
    @ApiModelProperty("每页多少条", required = false)
    val pageSize: Int?,
    @ApiModelProperty("分支", required = false)
    val branch: Set<String>?,
    @ApiModelProperty("fork库分支", required = false)
    val sourceGitProjectId: Set<String>?,
    @ApiModelProperty("触发人", required = false)
    val triggerUser: Set<String>?,
    @ApiModelProperty("流水线ID", required = false)
    val pipelineId: String?,
    @ApiModelProperty("Commit Msg", required = false)
    val commitMsg: String?,
    @ApiModelProperty("Event", required = false)
    val event: Set<StreamGitObjectKind>?,
    @ApiModelProperty("构建状态", required = false)
    val status: Set<BuildStatus>?,
    @ApiModelProperty("流水线列表", required = false)
    val pipelineIds: Set<String>?
)
