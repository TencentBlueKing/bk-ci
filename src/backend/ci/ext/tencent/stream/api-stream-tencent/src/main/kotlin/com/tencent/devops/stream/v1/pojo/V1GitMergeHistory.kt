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

package com.tencent.devops.stream.v1.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("工蜂历史构建模型-对应history页面")
data class V1GitMergeHistory(
    @ApiModelProperty("ID")
    var id: Long,
    @ApiModelProperty("GIT_PROJECT_ID")
    val gitProjectId: Long,
    @ApiModelProperty("MERGE_REQUEST_ID")
    val mergeRequestId: Long,
    @ApiModelProperty("MR_TITLE")
    var mrTitle: String,
    @ApiModelProperty("BRANCH")
    val branch: String,
    @ApiModelProperty("TARGET_BRANCH")
    val targetBranch: String,
    @ApiModelProperty("OPERATION_KIND")
    val operationKind: String?,
    @ApiModelProperty("EXTENSION_ACTION")
    val extensionAction: String?,
    @ApiModelProperty("COMMIT_TIMESTAMP")
    val commitTimeStamp: String?,
    @ApiModelProperty("TOTAL_COMMIT_COUNT")
    val totalCommitCount: Long,
    @ApiModelProperty("用户")
    val userId: String,
    @ApiModelProperty("DESCRIPTION")
    var description: String?,
    @ApiModelProperty("历史构建模型", required = false)
    var buildRecords: MutableList<V1GitCIBuildHistory>? = null
)
