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

package com.tencent.bkrepo.helm.pojo.chart

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("chart包上传请求")
data class ChartUploadRequest(
    @ApiModelProperty("所属项目id", required = true)
    override val projectId: String,
    @ApiModelProperty("所属仓库id", required = true)
    override val repoName: String,
    @ApiModelProperty("chart名称", required = true)
    val name: String,
    @ApiModelProperty("chart版本", required = true)
    val version: String,
    @ApiModelProperty("操作用户id", required = true)
    override val operator: String,
    @ApiModelProperty("chart路径", required = true)
    val fullPath: String,
    @ApiModelProperty("chart元属性信息", required = false)
    val metadataMap: Map<String, Any>?,
    @ApiModelProperty("chart包相关信息", required = false)
    val artifactInfo: ArtifactInfo
) : ChartOperationRequest
