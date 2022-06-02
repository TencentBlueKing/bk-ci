package com.tencent.bkrepo.nuget.service.impl

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.artifact.repository.NugetRepository
import com.tencent.bkrepo.nuget.service.NugetServiceIndexService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class NugetServiceIndexServiceImpl : NugetServiceIndexService {
    override fun getFeed(artifactInfo: NugetArtifactInfo): ResponseEntity<Any> {
        val repository = ArtifactContextHolder.getRepository() as NugetRepository
        return repository.feed(artifactInfo)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NugetServiceIndexServiceImpl::class.java)
    }
}
