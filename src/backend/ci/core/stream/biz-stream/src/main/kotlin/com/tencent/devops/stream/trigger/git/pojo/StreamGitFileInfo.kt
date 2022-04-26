package com.tencent.devops.stream.trigger.git.pojo

/**
 * Stream需要用到的文件内容
 */
interface StreamGitFileInfo {
    // 文件内容
    val content: String

    // git文件的唯一标识
    val blobId: String
}
