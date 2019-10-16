package com.tencent.devops.common.misc.pojo.agent

data class CmdbServerPage(
    val nodes: List<RawCmdbNode>,
    val returnRows: Int,
    val totalRows: Int
)
