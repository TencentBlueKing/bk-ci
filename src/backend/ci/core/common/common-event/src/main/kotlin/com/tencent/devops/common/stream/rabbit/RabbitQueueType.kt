package com.tencent.devops.common.stream.rabbit

enum class RabbitQueueType(val type: String) {
    CLASSIC("classic"),
    QUORUM("quorum"),
    STREAM("stream");

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
