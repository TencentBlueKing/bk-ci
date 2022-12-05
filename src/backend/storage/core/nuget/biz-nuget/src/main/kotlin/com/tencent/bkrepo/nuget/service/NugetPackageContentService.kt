package com.tencent.bkrepo.nuget.service

import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDownloadArtifactInfo
import com.tencent.bkrepo.nuget.pojo.response.VersionListResponse

interface NugetPackageContentService {
    /**
     * 下载 .nupkg 包
     */
    fun downloadPackageContent(artifactInfo: NugetDownloadArtifactInfo)

    /**
     * 下载 .nuspec 包
     */
    fun downloadPackageManifest(artifactInfo: NugetDownloadArtifactInfo)

    /**
     * 查询包的所有版本
     */
    fun packageVersions(artifactInfo: NugetArtifactInfo, packageId: String): VersionListResponse
}
