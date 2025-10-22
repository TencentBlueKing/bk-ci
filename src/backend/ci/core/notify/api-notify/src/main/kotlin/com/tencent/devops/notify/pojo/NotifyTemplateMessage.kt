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
package com.tencent.devops.notify.pojo

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import io.swagger.v3.oas.annotations.media.Schema

data class NotifyTemplateMessage(
    @get:Schema(title = "适用的通知类型（EMAIL:邮件 RTX:企业微信 WECHAT:微信 SMS:短信）", required = true)
    val notifyTypeScope: List<String>,
    @get:Schema(title = "标题（邮件和RTX方式必填）", required = false)
    val title: String? = "",
    @get:Schema(title = "消息内容", required = true)
    val body: String,
    @get:Schema(title = "消息内容(md格式)", required = false)
    val bodyMD: String? = null,
    @get:Schema(title = "邮件格式（邮件方式必填 0:文本 1:html网页）", allowableValues = ["0", "1"], type = "int", required = false)
    val bodyFormat: EnumEmailFormat?,
    @get:Schema(title = "邮件类型（邮件方式必填 0:外部邮件 1:内部邮件）", allowableValues = ["0", "1"], type = "int", required = false)
    val emailType: EnumEmailType?,
    @get:Schema(title = "回调地址")
    val callBackUrl: String?,
    @get:Schema(title = "流程名称")
    val processName: String?
)
