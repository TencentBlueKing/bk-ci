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
package com.tencent.devops.store.pojo.image.request

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("镜像基本信息（不影响执行的信息）修改请求报文体")
data class ImageBaseInfoUpdateRequest(
    @ApiModelProperty("镜像名称", required = false)
    val imageName: String? = null,
    @ApiModelProperty("所属分类ID", required = false)
    val classifyId: String? = null,
    @ApiModelProperty("功能标签", required = false)
    val labelIdList: List<String>? = null,
    @ApiModelProperty("镜像所属范畴CATEGORY_CODE", required = false)
    val category: String? = null,
    @ApiModelProperty("镜像简介", required = false)
    val summary: String? = null,
    @ApiModelProperty("镜像描述", required = false)
    val description: String? = null,
    @ApiModelProperty("镜像logo", required = false)
    val logoUrl: String? = null,
    @ApiModelProperty("icon图标base64字符串", required = false)
    val iconData: String? = null,
    @ApiModelProperty("发布者", required = false)
    val publisher: String? = null,
    @ApiModelProperty(value = "镜像大小", required = false)
    var imageSize: String? = null,
    @ApiModelProperty("dockerFile类型", required = false)
    val dockerFileType: String? = null,
    @ApiModelProperty("dockerFile内容", required = false)
    val dockerFileContent: String? = null,
    @ApiModelProperty("删除标识", required = false)
    val deleteFlag: Boolean? = null
)
