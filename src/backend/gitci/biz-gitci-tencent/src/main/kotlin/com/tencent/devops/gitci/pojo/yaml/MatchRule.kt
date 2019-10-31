package com.tencent.devops.gitci.pojo.yaml

/**
 * model
 */
data class MatchRule(
    val include: List<String>?,
    val exclude: List<String>?
)