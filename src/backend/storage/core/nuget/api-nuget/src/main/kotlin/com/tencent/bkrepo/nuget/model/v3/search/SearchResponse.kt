package com.tencent.bkrepo.nuget.model.v3.search

data class SearchResponse(
    val totalHist: Int = 0,
    val data: List<SearchResponseData> = emptyList()
)
