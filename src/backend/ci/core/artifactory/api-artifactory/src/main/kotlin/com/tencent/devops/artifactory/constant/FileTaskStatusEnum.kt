package com.tencent.devops.artifactory.constant

enum class FileTaskStatusEnum(val status: Short) {
    WAITING(0), // 等待
    DOWNLOADING(1), // 正在下载
    DONE(2), // 下载完成
    ERROR(3) // 异常状态
}