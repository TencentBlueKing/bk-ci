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

import com.tencent.devops.common.notify.enums.WeworkMediaType
import com.tencent.devops.common.notify.enums.WeworkReceiverType
import io.swagger.v3.oas.annotations.media.Schema
import java.io.InputStream

@Schema(title = "企业微信文本消息")
data class WeworkNotifyMediaMessage(
    @get:Schema(title = "接收人Id", required = true)
    val receivers: Collection<String>,
    @get:Schema(title = "接收人类型", required = true)
    val receiverType: WeworkReceiverType,
    @get:Schema(title = "媒体内容", required = true)
    var mediaInputStream: InputStream,
    @get:Schema(title = "媒体内容类型", required = true)
    var mediaType: WeworkMediaType,
    @get:Schema(title = "媒体名称", required = true)
    var mediaName: String
)
