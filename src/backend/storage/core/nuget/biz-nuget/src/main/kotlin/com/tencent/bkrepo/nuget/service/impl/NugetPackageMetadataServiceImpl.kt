package com.tencent.bkrepo.nuget.service.impl

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.nuget.artifact.repository.NugetRepository
import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import com.tencent.bkrepo.nuget.service.NugetPackageMetadataService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class NugetPackageMetadataServiceImpl : NugetPackageMetadataService, ArtifactService() {
    override fun registrationIndex(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any> {
        val repository = ArtifactContextHolder.getRepository() as NugetRepository
        return repository.registrationIndex(artifactInfo, registrationPath, isSemver2Endpoint)
    }

    override fun registrationPage(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any> {
        val repository = ArtifactContextHolder.getRepository() as NugetRepository
        return repository.registrationPage(artifactInfo, registrationPath, isSemver2Endpoint)
    }

    override fun registrationLeaf(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any> {
        val repository = ArtifactContextHolder.getRepository() as NugetRepository
        return repository.registrationLeaf(artifactInfo, registrationPath, isSemver2Endpoint)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NugetPackageMetadataServiceImpl::class.java)
    }
}
