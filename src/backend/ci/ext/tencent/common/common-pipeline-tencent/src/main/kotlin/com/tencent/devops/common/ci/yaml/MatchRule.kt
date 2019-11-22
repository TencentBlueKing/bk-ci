package com.tencent.devops.common.ci.yaml

/**
 * model
 */
data class MatchRule(
    val include: List<String>?,
    val exclude: List<String>?
)