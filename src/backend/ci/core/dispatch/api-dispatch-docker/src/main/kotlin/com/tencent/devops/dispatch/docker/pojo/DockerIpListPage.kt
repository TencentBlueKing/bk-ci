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

package com.tencent.devops.dispatch.docker.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import kotlin.math.ceil

@ApiModel("IDC IP数据包装模型")
data class DockerIpListPage<out T>(
    @ApiModelProperty("总记录行数", required = true)
    val count: Long,
    @ApiModelProperty("第几页", required = true)
    val page: Int,
    @ApiModelProperty("每页多少条", required = true)
    val pageSize: Int,
    @ApiModelProperty("总共多少页", required = true)
    val totalPages: Int,
    @ApiModelProperty("数据", required = true)
    val records: List<T>
) {
    constructor(page: Int, pageSize: Int, count: Long, records: List<T>) :
            this(count, page, pageSize, ceil(count * 1.0 / pageSize).toInt(), records)
}
