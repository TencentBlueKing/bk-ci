package com.tencent.bkrepo.nuget.service

import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.pojo.request.NugetSearchRequest
import com.tencent.bkrepo.nuget.pojo.response.search.NugetSearchResponse

interface NugetSearchService {
    /**
     * 根据[searchRequest]里面的条件进行搜索
     */
    fun search(artifactInfo: NugetArtifactInfo, searchRequest: NugetSearchRequest): NugetSearchResponse
}
