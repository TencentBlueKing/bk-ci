package com.tencent.bkrepo.nuget.service

import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDeleteArtifactInfo
import com.tencent.bkrepo.nuget.pojo.domain.NugetDomainInfo
import com.tencent.bkrepo.nuget.pojo.user.PackageVersionInfo

interface NugetPackageService {
    /**
     * 删除包
     */
    fun deletePackage(userId: String, artifactInfo: NugetDeleteArtifactInfo)

    /**
     * 删除版本
     */
    fun deleteVersion(userId: String, artifactInfo: NugetDeleteArtifactInfo)

    /**
     * 根据[packageKey]和[version]查询版本信息
     */
    fun detailVersion(artifactInfo: NugetArtifactInfo, packageKey: String, version: String): PackageVersionInfo

    /**
     * 获取nuget域名信息
     */
    fun getRegistryDomain(): NugetDomainInfo
}
