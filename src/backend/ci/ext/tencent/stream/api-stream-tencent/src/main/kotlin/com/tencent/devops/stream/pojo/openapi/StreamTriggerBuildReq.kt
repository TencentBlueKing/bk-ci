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

package com.tencent.devops.stream.pojo.openapi

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "StreamTriggerBuild请求")
data class StreamTriggerBuildReq(
    @Schema(description = "分支")
    val branch: String?,
    @Schema(description = "Custom commit message")
    val customCommitMsg: String?,
    @Schema(description = "yaml")
    val yaml: String?,
    @Schema(description = "描述")
    val description: String?,
    @Schema(description = "用户选择的触发CommitId")
    val commitId: String? = null,
    @Schema(description = "模拟代码库事件请求体")
    val payload: String? = null,
    @Schema(description = "模拟代码库类型,预留字段")
    val scmType: ScmType = ScmType.CODE_GIT,
    @Schema(description = "模拟代码事件类型,预留字段")
    val eventType: String? = null,
    @Schema(description = "触发方式")
    val objectKind: String = OBJECT_KIND_MANUAL,
    @Schema(description = "yaml文件路径")
    val path: String? = null
)
