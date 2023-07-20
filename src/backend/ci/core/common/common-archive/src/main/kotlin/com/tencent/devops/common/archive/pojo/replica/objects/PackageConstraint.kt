/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.common.archive.pojo.replica.objects

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

/**
 * 包版本限制
 */
@ApiModel("包版本限制")
data class PackageConstraint(
    @ApiModelProperty("包唯一key")
    val packageKey: String? = null,
    @ApiModelProperty("包版本列表")
    val versions: List<String>? = null,
    @ApiModelProperty("目标包存储版本:将源版本经过分发后存储为指定的目标版本，在源版本只有一个时生效,只针对镜像类型")
    val targetVersions: List<String>? = null,
    @ApiModelProperty("包正则匹配规则")
    val packageRegex: List<String>? = null,
    @ApiModelProperty("包版本正则匹配规则")
    val versionRegex: List<String>? = null
)
