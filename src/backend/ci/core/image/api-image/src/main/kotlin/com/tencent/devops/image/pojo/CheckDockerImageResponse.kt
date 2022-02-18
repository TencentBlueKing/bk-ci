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

package com.tencent.devops.image.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("检查镜像信息返回模型")
data class CheckDockerImageResponse(
    @ApiModelProperty("错误代码")
    val errorCode: Int,
    @ApiModelProperty("错误信息")
    val errorMessage: String? = "",
    @ApiModelProperty("架构")
    val arch: String? = "",
    @ApiModelProperty("作者")
    val author: String? = "",
    @ApiModelProperty("评论")
    val comment: String? = "",
    @ApiModelProperty("创建时间")
    val created: String? = "",
    @ApiModelProperty("docker版本")
    val dockerVersion: String? = "",
    @ApiModelProperty("id")
    val id: String? = "",
    @ApiModelProperty("操作系统")
    val os: String? = "",
    @ApiModelProperty("操作系统版本")
    val osVersion: String? = "",
    @ApiModelProperty("父容器")
    val parent: String? = "",
    @ApiModelProperty("大小")
    val size: Long? = 0,
    @ApiModelProperty("仓库标签")
    val repoTags: List<String>? = null,
    @ApiModelProperty("image存储属性")
    val repoDigests: List<String>? = null,
    @ApiModelProperty("虚拟大小")
    val virtualSize: Long? = 0
)
