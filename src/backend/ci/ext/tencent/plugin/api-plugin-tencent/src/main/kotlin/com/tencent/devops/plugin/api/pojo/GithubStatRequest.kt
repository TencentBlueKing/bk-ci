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

package com.tencent.devops.plugin.api.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Github统计数据请求包")
data class GithubStatRequest(
    @ApiModelProperty("日期", required = true)
    val statDate: String,
    @ApiModelProperty("issue总数", required = true)
    val issueCntAll: Int,
    @ApiModelProperty("open 状态的issue数", required = true)
    val issueCntOpen: Int,
    @ApiModelProperty("已关闭的issue数", required = true)
    var issueCntClosed: Int,
    @ApiModelProperty("pr总数", required = true)
    var prCntAll: Int,
    @ApiModelProperty("open状态的pr数", required = true)
    val prCntOpen: Int,
    @ApiModelProperty("已关闭的pr数", required = true)
    val prCntClosed: Int,
    @ApiModelProperty("提交总量", required = false)
    var commitsCnt: Int,
    @ApiModelProperty("每个开发者的提交统计", required = false)
    var perDevStatList: List<GithubDevStat?>
)
