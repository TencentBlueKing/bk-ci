/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.notify.blueking.sdk.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
@Schema(title = "邮件发送模型")
data class SendMailReq(
    val sender: String?,
    val title: String,
    val content: String,
    val receiver: String?,
    val receiver__username: String?,
    val cc: String?,
    val cc__username: String?,
    val body_format: String?,
    val is_content_base64: String?,

    override var bk_app_code: String? = "",
    override var bk_app_secret: String? = "",
    override var bk_token: String? = "",
    override var bk_username: String? = ""
) : ApiReq(bk_app_code, bk_app_secret, bk_token, bk_username)

// receiver	string	否	邮件接收者，包含邮件完整地址，多个以逗号分隔，若receiver、receiver__username同时存在，以receiver为准
// receiver__username	string	否	邮件接收者，包含用户名，用户需在蓝鲸平台注册，多个以逗号分隔，若receiver、receiver__username同时存在，以receiver为准
// sender	string	否	发件人
// title	string	是	邮件主题
// content	string	是	邮件内容
// cc	string	否	抄送人，包含邮件完整地址，多个以逗号分隔
// cc__username	string	否	抄送人，包含用户名，用户需在蓝鲸平台注册，多个以逗号分隔，若cc、cc__username同时存在，以cc为准
// body_format	string	否	邮件格式，包含'Html', 'Text'，默认为'Html'
// is_content_base64	bool	否	邮件内容是否base64编码，默认False，不编码，请使用base64.b64encode方法编码
// attachments	bool	否	邮件附件
