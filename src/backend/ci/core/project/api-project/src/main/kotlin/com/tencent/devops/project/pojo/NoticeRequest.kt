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
package com.tencent.devops.project.pojo
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
@ApiModel("公告请求报文体")
data class NoticeRequest(
    @ApiModelProperty("公告标题")
    val noticeTitle: String = "",
    @ApiModelProperty("生效日期")
    val effectDate: Long = 0,
    @ApiModelProperty("失效日期")
    val invalidDate: Long = 0,
    @ApiModelProperty("公告内容")
    val noticeContent: String = "",
    @ApiModelProperty("跳转地址")
    val redirectUrl: String = "",
    @ApiModelProperty("公告类型：0 弹框 1跑马灯")
    val noticeType: Int = 0,
    @ApiModelProperty("公告服务")
    val noticeService: List<String>? = null
)
