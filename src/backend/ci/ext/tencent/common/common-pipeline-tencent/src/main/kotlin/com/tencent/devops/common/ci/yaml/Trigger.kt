package com.tencent.devops.common.ci.yaml

/**
 * model
 */
data class Trigger(
    val disable: Boolean?,
    val branches: MatchRule?,
    val tags: MatchRule?,
    val paths: MatchRule?
)