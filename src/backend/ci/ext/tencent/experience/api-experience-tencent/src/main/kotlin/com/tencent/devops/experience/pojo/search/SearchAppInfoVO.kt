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

package com.tencent.devops.experience.pojo.search

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-搜索-APP信息")
data class SearchAppInfoVO(
    @ApiModelProperty("类型", required = true)
    val type: Int? = null,
    @ApiModelProperty("版本体验ID", required = true)
    val experienceHashId: String,
    @ApiModelProperty("体验名称", required = true)
    val experienceName: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: Long,
    @ApiModelProperty("大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("logo链接", required = true)
    val logoUrl: String,
    @ApiModelProperty("是否过期", required = true)
    val expired: Boolean,
    @ApiModelProperty("上次下载的体验ID", required = true)
    val lastDownloadHashId: String,
    @ApiModelProperty("版本体验BundleIdentifier", required = true)
    val bundleIdentifier: String,
    @ApiModelProperty("版本体验版本号", required = true)
    val version: String? = null,
    @ApiModelProperty("版本标题", required = true)
    val versionTitle: String? = null,
    @ApiModelProperty("应用Scheme", required = false)
    val appScheme: String,
    @ApiModelProperty("外部链接", required = false)
    val externalUrl: String? = null,
    @ApiModelProperty("下载次数", required = false)
    val downloadTime: Int? = null
)
