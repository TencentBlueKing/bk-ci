package com.tencent.devops.process.yaml.git.pojo

/**
 * Stream需要的合并请求信息，带变更文件
 */
interface PacGitMrChangeInfo {
    // 这次合并请求涉及的变更文件
    val files: List<PacGitChangeFileInfo>
}
