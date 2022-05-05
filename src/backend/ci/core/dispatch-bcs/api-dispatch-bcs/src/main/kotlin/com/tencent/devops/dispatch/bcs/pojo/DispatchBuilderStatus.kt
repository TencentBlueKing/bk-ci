package com.tencent.devops.dispatch.bcs.pojo

enum class DispatchBuilderStatus(val status: Int) {
    IDLE(0), // 空闲
    BUSY(1); // 占用
}
