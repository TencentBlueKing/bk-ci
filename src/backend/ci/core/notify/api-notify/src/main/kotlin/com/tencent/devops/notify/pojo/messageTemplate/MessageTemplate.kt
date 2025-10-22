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
package com.tencent.devops.notify.pojo.messageTemplate

import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "消息通知模板配置")
data class MessageTemplate(
    @get:Schema(title = "配置序号", required = true)
    val index: Int,
    @get:Schema(title = "公共模板ID", required = true)
    val id: String,
    @get:Schema(title = "公共模板代码", required = true)
    val templateCode: String,
    @get:Schema(title = "公共模板名称", required = true)
    var templateName: String,
    @get:Schema(title = "适用的通知类型（EMAIL:邮件 RTX:企业微信 WECHAT:微信 SMS:短信）", required = true)
    val notifyTypeScope: List<String>,
    @get:Schema(title = "优先级别（-1:低 0:普通 1:高）", allowableValues = ["-1", "0", "1"], type = "String", required = true)
    val priority: EnumNotifyPriority,
    @get:Schema(title = "通知来源（0:本地业务 1:操作）", allowableValues = ["0", "1"], type = "int", required = true)
    val source: EnumNotifySource,
    @get:Schema(title = "email通知模板", required = false)
    var emailTemplate: EmailMessageTemplate? = null,
    @get:Schema(title = "企业微信通知模板", required = false)
    var weworkTemplate: WeworkMessageTemplate? = null,
    @get:Schema(title = "微信通知模板", required = false)
    var wechatTemplate: WechatMessageTemplate? = null,
    @get:Schema(title = "微信群模板", required = false)
    var weworkGroupTemplate: WeworkGroupMessageTemplate? = null,
    @get:Schema(title = "语音模板", required = false)
    val voiceTemplate: VoiceTemplate? = null,
    @get:Schema(title = "创建人", required = true)
    val creator: String,
    @get:Schema(title = "修改人", required = true)
    val modifior: String
)
