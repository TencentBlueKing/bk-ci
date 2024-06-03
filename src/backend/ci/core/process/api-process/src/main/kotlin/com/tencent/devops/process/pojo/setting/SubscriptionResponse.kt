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

package com.tencent.devops.process.pojo.setting

import com.tencent.devops.common.pipeline.pojo.setting.PipelineSubscriptionType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "设置-订阅消息")
data class SubscriptionResponse(
    @get:Schema(title = "通知人员", required = false)
    val users: String = "",
    @get:Schema(title = "通知方式(email, rtx)", required = true)
    val types: List<PipelineSubscriptionType> = listOf(),
    @get:Schema(title = "分组id", required = false)
    val groups: List<String> = listOf(),
    @get:Schema(title = "企业微信群通知开关", required = false)
    val wechatGroupFlag: Boolean = false,
    @get:Schema(title = "企业微信群通知群ID", required = false)
    val wechatGroup: String = "",
    @get:Schema(title = "企业微信群通知转为Markdown格式开关", required = false)
    val wechatGroupMarkDownFlag: Boolean = false,
    @get:Schema(title = "通知内容带上流水线详情连接", required = false)
    val detailFlag: Boolean = false,
    @get:Schema(title = "自定义通知内容", required = false)
    val content: String = ""
)
