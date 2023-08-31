package com.tencent.devops.dispatch.devcloud.pojo

enum class ContainerBuildStatus(val status: Int) {
    IDLE(0), // 空闲
    BUSY(1); // 占用
}
