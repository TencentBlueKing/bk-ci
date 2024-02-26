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

package com.tencent.devops.common.notify.pojo.elements

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "发送企业微信通知", description = SendRTXNotifyElement.classType)
data class SendRTXNotifyElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "发送企业微信通知",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "接收人集合", required = true)
    val receivers: Set<String> = setOf(),
    @get:Schema(title = "通知标题", required = true)
    val title: String = "",
    @get:Schema(title = "通知内容", required = true)
    val body: String = "",
    @get:Schema(title = "启动企业微信群", required = true)
    val wechatGroupFlag: Boolean?,
    @get:Schema(title = "企业微信群Id", required = true)
    val wechatGroup: String?,
    @get:Schema(title = "通知内容带上流水线详情连接", required = true)
    val detailFlag: Boolean?
) : Element(name, id, status) {
    companion object {
        const val classType = "sendRTXNotify"
    }

    override fun getTaskAtom() = "rtxTaskAtom"

    private fun getReceiverStr(receivers: Set<String>): String {
        val str = StringBuilder(0)
        receivers.forEach {
            // 第一个不加逗号隔开
            if (str.isNotEmpty()) str.append(",")
            str.append(it)
        }
        return str.toString()
    }

    override fun getClassType() = classType
}
