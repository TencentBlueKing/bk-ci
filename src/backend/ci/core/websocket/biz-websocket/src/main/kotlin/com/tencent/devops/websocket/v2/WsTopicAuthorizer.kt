package com.tencent.devops.websocket.v2

import com.tencent.devops.common.websocket.v2.WsTopic

fun interface WsTopicAuthorizer {
    fun authorize(userId: String, topics: Collection<WsTopic>): Set<String>
}
