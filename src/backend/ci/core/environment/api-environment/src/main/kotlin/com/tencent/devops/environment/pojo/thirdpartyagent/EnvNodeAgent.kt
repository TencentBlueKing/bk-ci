package com.tencent.devops.environment.pojo.thirdpartyagent

/**
 * 第三方构建机包含环境节点信息的封装类
 */
data class EnvNodeAgent(
    val agent: ThirdPartyAgent,
    val enableNode: Boolean,
    val nodeDisplayName: String?
)
