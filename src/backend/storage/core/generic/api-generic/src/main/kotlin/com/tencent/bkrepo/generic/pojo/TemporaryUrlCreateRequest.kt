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

package com.tencent.bkrepo.generic.pojo

import com.tencent.bkrepo.repository.pojo.token.TokenType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.Duration

@ApiModel("创建临时访问url请求")
data class TemporaryUrlCreateRequest(
    @ApiModelProperty("项目id")
    val projectId: String,
    @ApiModelProperty("仓库名称")
    val repoName: String,
    @ApiModelProperty("授权路径列表")
    val fullPathSet: Set<String>,
    @ApiModelProperty("授权用户")
    val authorizedUserSet: Set<String> = emptySet(),
    @ApiModelProperty("授权IP")
    val authorizedIpSet: Set<String> = emptySet(),
    @ApiModelProperty("有效时间，单位秒")
    val expireSeconds: Long = Duration.ofDays(1).seconds,
    @ApiModelProperty("允许访问次数，为空表示无限制")
    val permits: Int? = null,
    @ApiModelProperty("token类型")
    val type: TokenType,
    @ApiModelProperty("指定临时访问链接host")
    val host: String? = null,
    @ApiModelProperty("是否通知用户")
    val needsNotify: Boolean = false
)
