package com.tencent.bkrepo.nuget.controller

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.model.v3.RegistrationIndex
import com.tencent.bkrepo.nuget.model.v3.search.SearchRequest
import com.tencent.bkrepo.nuget.model.v3.search.SearchResponse
import com.tencent.bkrepo.nuget.service.NugetV3ClientService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v3/{projectId}/{repoName}")
class NugetV3ClientController(
    private val nugetV3ClientService: NugetV3ClientService
) {

    @GetMapping(produces = ["application/json"])
    fun feed(
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo
    ): String {
        return nugetV3ClientService.getFeed(artifactInfo)
    }

    @GetMapping("/registration-semver2/{packageId}/index.json", produces = [MediaTypes.APPLICATION_JSON])
    fun registrationSemver2Index(
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo,
        @PathVariable packageId: String
    ): RegistrationIndex {
        return nugetV3ClientService.registration(artifactInfo, packageId, "registration-semver2", true)
    }

    @GetMapping("/flatcontainer/{packageId}/{packageVersion}/*.nupkg")
    fun download(
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo,
        @PathVariable packageId: String,
        @PathVariable packageVersion: String
    ) {
        nugetV3ClientService.download(artifactInfo, packageId, packageVersion)
    }

    @GetMapping("/query", produces = [MediaTypes.APPLICATION_JSON])
    fun search(
        @ArtifactPathVariable artifactInfo: NugetArtifactInfo,
        searchRequest: SearchRequest
    ): SearchResponse {
        return nugetV3ClientService.search(artifactInfo, searchRequest)
    }
}
