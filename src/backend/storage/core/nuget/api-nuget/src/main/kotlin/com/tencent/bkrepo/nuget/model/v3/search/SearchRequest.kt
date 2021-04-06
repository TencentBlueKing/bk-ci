package com.tencent.bkrepo.nuget.model.v3.search

/**
 * v3 search 搜索条件（要求客户端版本在5.8+）
 */
data class SearchRequest(
    val q: String?,
    val skip: Int = 0,
    val take: Int = 20,
    val prerelease: Boolean,
    val semVerLevel: String?
)
