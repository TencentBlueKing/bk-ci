package com.tencent.bkrepo.nuget.artifact.repository

import com.tencent.bkrepo.common.artifact.repository.core.ArtifactRepository
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import org.springframework.http.ResponseEntity

interface NugetRepository : ArtifactRepository {

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

    /**
     * 查找服务的索引文件index.json
     */
    fun feed(artifactInfo: NugetArtifactInfo): ResponseEntity<Any>
}
