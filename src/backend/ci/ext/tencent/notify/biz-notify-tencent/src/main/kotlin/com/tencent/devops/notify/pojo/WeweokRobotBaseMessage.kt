package com.tencent.devops.notify.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

abstract class WeweokRobotBaseMessage(
    /**
     * 会话id，支持最多传100个，用‘|’分隔。可能是群聊会话，也可能是单聊会话或者小黑板会话，通过消息回调获得，也可以是userid。
     * 特殊的，当chatid为“@all_group”时，表示对所有群和小黑板广播，为“@all_subscriber”时表示对订阅范围内员工广播单聊消息，为“@all”时，
     * 表示对所有群、所有订阅范围内员工和所有小黑板广播。不填则默认为“@all_group”
     */
    @ApiModelProperty("会话id")
    open val chatid: String?,
    /**
     * 小黑板帖子id，有且只有chatid指定了一个小黑板的时候生效
     */
    @ApiModelProperty("会话id", name = "post_id")
    @JsonProperty("post_id")
    open val postId: String?,

    open val msgtype: String
)
