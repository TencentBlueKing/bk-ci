package com.tencent.devops.common.stream.rabbit

enum class RabbitQueueType(val type: String) {
    CLASSIC("classic"),
    QUORUM("quorum"),
    STREAM("stream"); // 由于k8s部署需要单独为stream queue端口注册，暂时不适用

    companion object {
        fun parse(type: String): RabbitQueueType {
            return if (type == "quorum") {
                QUORUM
            } else if (type == "stream") {
                STREAM
            } else {
                CLASSIC
            }
        }
    }
}
