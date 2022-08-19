package com.tencent.devops.stream.trigger.git.pojo

/**
 * Stream需要的Git文件列表中的文件信息
 */
interface StreamGitTreeFileInfo {
    // 文件ID
    val id: String?
    // 文件名称
    val name: String
    // 文件类型 文件夹/文件
    val type: String
}

enum class StreamGitTreeFileInfoType(val value: String) {
    BLOB("blob")
}
