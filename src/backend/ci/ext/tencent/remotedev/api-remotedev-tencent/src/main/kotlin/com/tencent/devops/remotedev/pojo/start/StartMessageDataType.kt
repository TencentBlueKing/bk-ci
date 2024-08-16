package com.tencent.devops.remotedev.pojo.start

enum class StartMessageDataType(val value: Int) {
    // 跑马灯消息
    MARQUEE(1),

    // 复杂消息
    COMPLEX(2)
}
