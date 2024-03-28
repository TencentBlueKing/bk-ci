package com.tencent.devops.process.yaml.git.pojo

/**
 * PAC需要的Git文件列表中的文件信息
 */
interface PacGitTreeFileInfo {
    // 文件ID
    val id: String?
    // 文件名称
    val name: String
    // 文件类型 文件夹/文件
    val type: String
}

enum class PacGitTreeFileInfoType(val value: String) {
    BLOB("blob")
}
