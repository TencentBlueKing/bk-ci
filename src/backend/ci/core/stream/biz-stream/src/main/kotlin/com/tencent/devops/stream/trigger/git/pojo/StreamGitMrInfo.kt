package com.tencent.devops.stream.trigger.git.pojo

/**
 * Stream需要的合并请求信息
 */
interface StreamGitMrInfo {
    // 当前合并请求的状态，可用来判断是否冲突
    val mergeStatus: String
}
