package com.tencent.bkrepo.nuget.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDownloadArtifactInfo
import com.tencent.bkrepo.nuget.pojo.response.VersionListResponse
import com.tencent.bkrepo.nuget.service.NugetPackageContentService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * reference: https://docs.microsoft.com/en-us/nuget/api/package-base-address-resource
 */
@Suppress("MVCPathVariableInspection")
@RestController
@RequestMapping("/{projectId}/{repoName}/v3/flatcontainer")
class NugetPackageContentController(
    private val packageContentService: NugetPackageContentService
) {
    /**
     * Download package content (.nupkg)
     *
     * GET {@id}/{LOWER_ID}/{LOWER_VERSION}/{LOWER_ID}.{LOWER_VERSION}.nupkg
     *
     * return binary stream
     */
    @GetMapping("/{id}/{version}/*.nupkg")
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun downloadPackageContent(
        artifactInfo: NugetDownloadArtifactInfo
    ) {
        packageContentService.downloadPackageContent(artifactInfo)
    }

    /**
     * Download package manifest (.nuspec)
     *
     * GET {@id}/{LOWER_ID}/{LOWER_VERSION}/{LOWER_ID}.nuspec
     *
     * return xml document
     */
    @GetMapping("/{id}/{version}/{id}.nuspec")
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun downloadPackageManifest(
        artifactInfo: NugetDownloadArtifactInfo
    ) {
        packageContentService.downloadPackageManifest(artifactInfo)
    }

    /**
     * Enumerate package versions
     * This list contains both listed and unlisted package versions.
     *
     * GET {@id}/{LOWER_ID}/index.json
     */
    @GetMapping("/{packageId}/index.json")
    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun packageVersions(
        artifactInfo: NugetArtifactInfo,
        @PathVariable packageId: String
    ): ResponseEntity<VersionListResponse> {
        val versionList = packageContentService.packageVersions(artifactInfo, packageId)
        return ResponseEntity.ok(versionList)
    }
}
