package com.tencent.devops.gitci.pojo.yaml

/**
 * model
 */
data class Trigger(
    val disable: Boolean?,
    val branches: MatchRule?,
    val tags: MatchRule?,
    val paths: MatchRule?
)