package com.tencent.bkrepo.nuget.service

import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.model.v2.search.NuGetSearchRequest
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDeleteArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDownloadArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetPublishArtifactInfo

interface NugetClientService {

    /**
     * 获取service_document.xml内容
     */
    fun getServiceDocument(artifactInfo: NugetArtifactInfo)

    /**
     * push nuget package
     */
    fun publish(userId: String, publishInfo: NugetPublishArtifactInfo)

    /**
     * download nuget package
     */
    fun download(userId: String, artifactInfo: NugetDownloadArtifactInfo)

    /**
     * find packages By id
     */
    fun findPackagesById(artifactInfo: NugetArtifactInfo, searchRequest: NuGetSearchRequest)

    /**
     * delete nupkg with version
     */
    fun delete(userId: String, artifactInfo: NugetDeleteArtifactInfo)
}
