package com.tencent.devops.stream.trigger.git.pojo

/**
 * Stream需要的变更文件的信息
 */
interface StreamGitChangeFileInfo {
    // 变更前路径
    val oldPath: String

    // 变更后路径
    val newPath: String

    // 是否是改名文件
    val renameFile: Boolean

    // 是否是删除文件
    val deletedFile: Boolean
}
