package com.tencent.devops.process.yaml.git.pojo

/**
 * PAC需要用到的文件内容
 */
interface PacGitFileInfo {
    // 文件内容
    val content: String

    // git文件的唯一标识
    val blobId: String
}
