package com.tencent.bkrepo.nuget.service.impl

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.manager.PackageManager
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.constant.NugetProperties
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDeleteArtifactInfo
import com.tencent.bkrepo.nuget.pojo.domain.NugetDomainInfo
import com.tencent.bkrepo.nuget.pojo.user.BasicInfo
import com.tencent.bkrepo.nuget.pojo.user.PackageVersionInfo
import com.tencent.bkrepo.nuget.service.NugetPackageService
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NugetPackageServiceImpl(
    private val nugetProperties: NugetProperties,
    private val packageManager: PackageManager,
    private val nodeClient: NodeClient
) : NugetPackageService, ArtifactService() {

    override fun deletePackage(userId: String, artifactInfo: NugetDeleteArtifactInfo) {
        repository.remove(ArtifactRemoveContext())
    }

    override fun deleteVersion(userId: String, artifactInfo: NugetDeleteArtifactInfo) {
        repository.remove(ArtifactRemoveContext())
    }

    override fun detailVersion(
        artifactInfo: NugetArtifactInfo,
        packageKey: String,
        version: String
    ): PackageVersionInfo {
        return with(artifactInfo) {
            val packageVersion = packageManager.findVersionByName(projectId, repoName, packageKey, version)
            val fullPath = packageVersion.contentPath.orEmpty()
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: run {
                logger.warn("node [$fullPath] don't found.")
                throw NotFoundException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
            }
            val basicInfo = buildBasicInfo(nodeDetail, packageVersion)
            PackageVersionInfo(basicInfo, emptyMap())
        }
    }

    override fun getRegistryDomain(): NugetDomainInfo {
        return NugetDomainInfo(UrlFormatter.formatHost(nugetProperties.domain))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NugetPackageServiceImpl::class.java)

        fun buildBasicInfo(nodeDetail: NodeDetail, packageVersion: PackageVersion): BasicInfo {
            with(nodeDetail) {
                return BasicInfo(
                    packageVersion.name,
                    fullPath,
                    size,
                    sha256!!,
                    md5!!,
                    packageVersion.stageTag,
                    projectId,
                    repoName,
                    packageVersion.downloads,
                    createdBy,
                    createdDate,
                    lastModifiedBy,
                    lastModifiedDate
                )
            }
        }
    }
}
