package com.tencent.devops.remotedev.pojo.start

enum class StartMessageDataType(val value: Int) {
    // 邮件
    EMAIL(0),

    // 企业微信
    RTX(0),

    // 云桌面-跑马灯消息
    DESKTOP_MARQUEE(1),

    // 云桌面-复杂消息
    DESKTOP_COMPLEX(2),

    // 蓝盾客户端-通知
    CLIENT(0)
}
