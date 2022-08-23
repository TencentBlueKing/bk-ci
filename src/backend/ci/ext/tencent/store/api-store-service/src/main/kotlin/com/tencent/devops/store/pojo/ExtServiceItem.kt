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

package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务首页信息")
data class ExtServiceItem(
    @ApiModelProperty("ID")
    val id: String,
    @ApiModelProperty("名称")
    val name: String,
    @ApiModelProperty("标识")
    val code: String,
    @ApiModelProperty("分类")
    val classifyCode: String?,
    @ApiModelProperty("logo链接")
    val logoUrl: String?,
    @ApiModelProperty("发布者")
    val publisher: String,
    @ApiModelProperty("下载量")
    val downloads: Int?,
    @ApiModelProperty("评分")
    val score: Double?,
    @ApiModelProperty("简介")
    val summary: String?,
    @ApiModelProperty("是否有权限安装标识")
    val flag: Boolean,
    @ApiModelProperty("是否公共标识")
    val publicFlag: Boolean,
    @ApiModelProperty("修改人")
    val modifier: String,
    @ApiModelProperty("修改时间")
    val updateTime: String,
    @ApiModelProperty("是否推荐标识 true：推荐，false：不推荐", required = false)
    val recommendFlag: Boolean? = null
)
