package com.tencent.devops.experience.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "P2P连接事件")
data class P2PConnectEvent(
    @get:Schema(title = "发送用户")
    val sender: String,
    @get:Schema(title = "接收用户")
    val receiver: String,
    @get:Schema(title = "体验ID")
    val experienceHashId: String
)
