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

package com.tencent.devops.experience.pojo.download

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-下载记录出参")
data class DownloadRecordVO(
    @ApiModelProperty("版本体验ID", required = true)
    val experienceHashId: String,
    @ApiModelProperty("文件大小(byte)", required = true)
    val size: Long,
    @ApiModelProperty("logo链接", required = true)
    val logoUrl: String,
    @ApiModelProperty("体验名称", required = true)
    val experienceName: String,
    @ApiModelProperty("版本标题", required = true)
    val versionTitle: String,
    @ApiModelProperty("创建时间", required = true)
    val createTime: Long,
    @ApiModelProperty("下载时间", required = true)
    val downloadTime: Long,
    @ApiModelProperty("版本体验BundleIdentifier", required = true)
    val bundleIdentifier: String,
    @ApiModelProperty("应用Scheme", required = false)
    val appScheme: String,
    @ApiModelProperty("是否过期", required = true)
    val expired: Boolean,
    @ApiModelProperty("上次下载的体验ID", required = true)
    val lastDownloadHashId: String
)
