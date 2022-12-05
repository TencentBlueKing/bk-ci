package com.tencent.bkrepo.nuget.service.impl

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.exception.NugetVersionListNotFoundException
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDownloadArtifactInfo
import com.tencent.bkrepo.nuget.pojo.response.VersionListResponse
import com.tencent.bkrepo.nuget.service.NugetPackageContentService
import com.tencent.bkrepo.repository.api.PackageClient
import org.springframework.stereotype.Service

@Service
class NugetPackageContentServiceImpl(
    private val packageClient: PackageClient
) : NugetPackageContentService, ArtifactService() {
    override fun downloadPackageContent(artifactInfo: NugetDownloadArtifactInfo) {
        repository.download(ArtifactDownloadContext())
    }

    override fun downloadPackageManifest(artifactInfo: NugetDownloadArtifactInfo) {
        repository.download(ArtifactDownloadContext())
    }

    override fun packageVersions(artifactInfo: NugetArtifactInfo, packageId: String): VersionListResponse {
        return with(artifactInfo) {
            val versionList = packageClient.listExistPackageVersion(
                projectId, repoName, PackageKeys.ofNuget(packageId)
            ).data ?: emptyList()
            // If the package source has no versions of the provided package ID, a 404 status code is returned.
            if (versionList.isEmpty()) throw NugetVersionListNotFoundException("The specified blob does not exist.")
            VersionListResponse(versionList)
        }
    }
}
