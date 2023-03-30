package com.tencent.devops.dispatch.codecc.pojo

enum class VolumeStatus(val status: Int) {
    RUNNING(0), // 构建中
    SUCCESS(1), // 构建成功
    FAILURE(2), // 构建失败
    CANCELED(3); // 取消构建
}
