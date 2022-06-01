package com.tencent.bkrepo.nuget.pojo.response.search

data class NugetSearchResponse(
    val totalHist: Int = 0,
    val data: List<SearchResponseData> = emptyList()
)
