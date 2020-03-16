package com.tencent.devops.dispatch.pojo

enum class VolumeStatus(val status: Int) {
    RUNNING(0), // 构建中
    FINISH(1), // 构建成功，且容器已销毁
    FAILURE(2), // 构建失败
    CANCELED(3); // 取消构建
}