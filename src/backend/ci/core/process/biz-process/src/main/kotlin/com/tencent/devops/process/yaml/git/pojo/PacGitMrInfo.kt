package com.tencent.devops.process.yaml.git.pojo

/**
 * PAC需要的合并请求信息
 */
interface PacGitMrInfo {
    // 当前合并请求的状态，可用来判断是否冲突
    val mergeStatus: String
}
