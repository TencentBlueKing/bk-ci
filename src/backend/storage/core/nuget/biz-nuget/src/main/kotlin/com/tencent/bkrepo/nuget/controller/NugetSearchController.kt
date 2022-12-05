package com.tencent.bkrepo.nuget.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.pojo.request.NugetSearchRequest
import com.tencent.bkrepo.nuget.pojo.response.search.NugetSearchResponse
import com.tencent.bkrepo.nuget.service.NugetSearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RequestMapping("/{projectId}/{repoName}/v3", produces = [MediaTypes.APPLICATION_JSON])
@RestController
class NugetSearchController(
    private val nugetSearchService: NugetSearchService
) {
    @GetMapping("/query", produces = [MediaTypes.APPLICATION_JSON])
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun search(
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo,
        searchRequest: NugetSearchRequest
    ): NugetSearchResponse {
        return nugetSearchService.search(artifactInfo, searchRequest)
    }
}
