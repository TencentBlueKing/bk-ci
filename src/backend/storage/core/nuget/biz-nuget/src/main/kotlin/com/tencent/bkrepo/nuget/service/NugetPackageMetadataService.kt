package com.tencent.bkrepo.nuget.service

import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import org.springframework.http.ResponseEntity

interface NugetPackageMetadataService {
    /**
     * 根据RegistrationsBaseUrl获取registration index metadata
     */
    fun registrationIndex(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any>

    /**
     * 根据RegistrationsBaseUrl获取registration page metadata
     */
    fun registrationPage(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any>

    /**
     * 根据RegistrationsBaseUrl获取registration leaf metadata
     */
    fun registrationLeaf(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any>
}
