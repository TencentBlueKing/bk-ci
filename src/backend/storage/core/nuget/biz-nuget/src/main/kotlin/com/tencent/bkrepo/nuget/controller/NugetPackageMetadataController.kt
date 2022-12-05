package com.tencent.bkrepo.nuget.controller

import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import com.tencent.bkrepo.nuget.service.NugetPackageMetadataService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * reference: https://docs.microsoft.com/en-us/nuget/api/registration-base-url-resource
 */
@Suppress("MVCPathVariableInspection")
@RestController
@RequestMapping("/{projectId}/{repoName}/v3")
class NugetPackageMetadataController(
    private val nugetPackageMetadataService: NugetPackageMetadataService
) {
    /**
     * GET {@id}/{LOWER_ID}/index.json
     *
     * Registration index
     *
     * RegistrationsBaseUrl/3.6.0+
     */
    @GetMapping("/registration-semver2/{id}/index.json", produces = [MediaTypes.APPLICATION_JSON])
    fun registrationSemver2Index(
        artifactInfo: NugetRegistrationArtifactInfo
    ): ResponseEntity<Any> {
        return nugetPackageMetadataService.registrationIndex(artifactInfo, "registration-semver2", true)
    }

    @GetMapping(
        "/registration-semver2/{id}/page/{lowerVersion}/{upperVersion}.json",
        produces = [MediaTypes.APPLICATION_JSON]
    )
    fun registrationSemver2Page(
        artifactInfo: NugetRegistrationArtifactInfo
    ): ResponseEntity<Any> {
        return nugetPackageMetadataService.registrationPage(artifactInfo, "registration-semver2", true)
    }

    @GetMapping("/registration-semver2/{id}/{version}.json", produces = [MediaTypes.APPLICATION_JSON])
    fun registrationSemver2Leaf(
        artifactInfo: NugetRegistrationArtifactInfo
    ): ResponseEntity<Any> {
        return nugetPackageMetadataService.registrationLeaf(artifactInfo, "registration-semver2", true)
    }
}
