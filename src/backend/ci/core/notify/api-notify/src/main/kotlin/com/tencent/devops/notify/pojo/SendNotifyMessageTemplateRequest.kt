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
package com.tencent.devops.notify.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("使用模板发送消息通知请求报文体")
data class SendNotifyMessageTemplateRequest(
    @ApiModelProperty("通知模板代码", required = true)
    val templateCode: String,
    @ApiModelProperty("通知接收者", required = true)
    val receivers: MutableSet<String> = mutableSetOf(),
    @ApiModelProperty("指定消息类型", required = false)
    val notifyType: MutableSet<String>? = null, // 枚举保护：使用NotifyType.name传值
    @ApiModelProperty("标题动态参数", required = false)
    val titleParams: Map<String, String>? = null,
    @ApiModelProperty("内容动态参数", required = false)
    val bodyParams: Map<String, String>? = null,
    @ApiModelProperty("邮件抄送接收者", required = false)
    val cc: MutableSet<String>? = null,
    @ApiModelProperty("消息内容", required = false)
    val bcc: MutableSet<String>? = null,
    @ApiModelProperty("是否以markdown格式发送通知内容, 目前仅企业微信群支持markdown", required = false)
    val markdownContent: Boolean? = false,
    @ApiModelProperty("回调内容", required = false)
    val callbackData: Map<String, String>? = null
)
