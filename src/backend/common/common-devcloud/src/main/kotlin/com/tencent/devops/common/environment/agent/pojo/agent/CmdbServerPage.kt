package com.tencent.devops.common.environment.agent.pojo.agent

data class CmdbServerPage(
    val nodes: List<RawCmdbNode>,
    val returnRows: Int,
    val totalRows: Int
)
