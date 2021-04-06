package com.tencent.bkrepo.nuget.service

import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.model.v2.search.NuGetSearchRequest

interface NugetClientService {

    /**
     * 获取service_document.xml内容
     */
    fun getServiceDocument(artifactInfo: NugetArtifactInfo): String

    /**
     * push nuget package
     */
    fun publish(userId: String, artifactInfo: NugetArtifactInfo, artifactFileMap: ArtifactFileMap): String

    /**
     * download nuget package
     */
    fun download(userId: String, artifactInfo: NugetArtifactInfo, packageId: String, packageVersion: String)

    /**
     * find packages By id
     */
    fun findPackagesById(artifactInfo: NugetArtifactInfo, searchRequest: NuGetSearchRequest)

    /**
     * 删除 [packageId].[packageVersion].nupkg 包
     */
    fun delete(userId: String, artifactInfo: NugetArtifactInfo, packageId: String, packageVersion: String)
}
