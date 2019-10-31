package com.tencent.devops.gitci.pojo.yaml

/**
 * model
 */
data class MergeRequest(
    val disable: Boolean?,
    val autoCancel: Boolean?,
    val branches: MatchRule?,
    val paths: MatchRule?
)