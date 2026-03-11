package com.tencent.devops.stream.trigger.git.pojo

/**
 * Stream需要的合并请求信息，带变更文件
 */
interface StreamGitMrChangeInfo {
    // 这次合并请求涉及的变更文件
    val files: List<StreamGitChangeFileInfo>
}
