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

package com.tencent.devops.experience.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("蓝盾APP消息类型")

open class AppNotifyMessage {

    @ApiModelProperty("消息id")
    var messageId: Long = 0

    @ApiModelProperty("experienceHashId")
    var experienceHashId: String = ""

    @ApiModelProperty("通知接收者")
    var receiver: String = ""

    @ApiModelProperty("设备token")
    var token: String = ""

    @ApiModelProperty("通知内容")
    var body: String = ""

    @ApiModelProperty("通知标题")
    var title: String = ""

    @ApiModelProperty("跳转路径")
    var url: String = ""

    @ApiModelProperty("平台")
    var platform: String = ""
    override fun toString(): String {
        return String.format(
            "id (%s), receiver(%s), token(%s), platform(%s), title(%s), body(%s) ,url(%s)",
            messageId, receiver, token, platform, title, body, url
        )
    }
}
