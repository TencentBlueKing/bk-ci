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

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("企业微信机器人消息")
data class WeworkRobotSingleTextMessage(
    /**
     * 会话id，支持最多传100个，用‘|’分隔。可能是群聊会话，也可能是单聊会话或者小黑板会话，通过消息回调获得，也可以是userid。
     * 特殊的，当chatid为“@all_group”时，表示对所有群和小黑板广播，为“@all_subscriber”时表示对订阅范围内员工广播单聊消息，为“@all”时，
     * 表示对所有群、所有订阅范围内员工和所有小黑板广播。不填则默认为“@all_group”
     */
    @ApiModelProperty("会话id")
    override val chatid: String?,
    /**
     * 小黑板帖子id，有且只有chatid指定了一个小黑板的时候生效
     */
    @ApiModelProperty("会话id", name = "post_id")
    @JsonProperty("post_id")
    override val postId: String?,
    @ApiModelProperty("消息类型")
    override val msgtype: String = "text",
    @ApiModelProperty("文本内容")
    val text: WeworkRobotContentMessage,
    /**
     * 该消息只有指定的群成员或小黑板成员可见（其他成员不可见），有且只有chatid指定了一个群或一个小黑板的时候生效，多个userid用‘|’分隔
     */
    @ApiModelProperty("会话id", name = "visible_to_user")
    @JsonProperty("visible_to_user")
    val visibleToUser: String?
) : WeweokRobotBaseMessage(chatid, postId, msgtype)
