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

package com.tencent.devops.process.pojo.classify

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("")
data class PipelineNewViewSummary(
    @ApiModelProperty("视图id", required = false)
    val id: String,
    @ApiModelProperty("项目id", required = false)
    val projectId: String,
    @ApiModelProperty("视图名称", required = false)
    val name: String,
    @ApiModelProperty("是否项目", required = false)
    val projected: Boolean,
    @ApiModelProperty("创建时间", required = false)
    val createTime: Long,
    @ApiModelProperty("更新时间", required = false)
    val updateTime: Long,
    @ApiModelProperty("创建者", required = false)
    val creator: String,
    @ApiModelProperty("是否置顶", required = false)
    val top: Boolean = false,
    @ApiModelProperty("流水线组类型,1--动态,2--静态", required = true)
    val viewType: Int,
    @ApiModelProperty("流水线个数", required = true)
    val pipelineCount: Int
)
