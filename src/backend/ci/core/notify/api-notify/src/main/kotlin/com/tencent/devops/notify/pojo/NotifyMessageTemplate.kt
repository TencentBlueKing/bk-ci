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

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "消息通知")
data class NotifyMessageTemplate(
    @get:Schema(title = "ID", required = true)
    val id: String,
    @get:Schema(title = "模板代码", required = true)
    val templateCode: String,
    @get:Schema(title = "模板名称", required = true)
    val templateName: String,
    @get:Schema(title = "适用的通知类型（EMAIL:邮件 RTX:企业微信 WECHAT:微信 SMS:短信）", required = true)
    val notifyTypeScope: List<String>,
    @get:Schema(title = "标题（邮件和RTX方式必填）", required = false)
    val title: String? = "",
    @get:Schema(title = "消息内容", required = true)
    val body: String,
    @get:Schema(title = "优先级别", required = true)
    val priority: String,
    @get:Schema(title = "通知来源", required = true)
    val source: Int,
    @get:Schema(title = "邮件格式（邮件方式必填）", required = false)
    val bodyFormat: Int? = null,
    @get:Schema(title = "邮件类型（邮件方式必填）", required = false)
    val emailType: Int? = null,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "修改人", required = true)
    val modifier: String,
    @get:Schema(title = "创建日期", required = true)
    val createTime: Long = 0,
    @get:Schema(title = "更新日期", required = true)
    val updateTime: Long = 0
)
