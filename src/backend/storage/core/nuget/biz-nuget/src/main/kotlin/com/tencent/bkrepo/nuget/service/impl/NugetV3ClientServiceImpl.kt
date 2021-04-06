package com.tencent.bkrepo.nuget.service.impl

import com.fasterxml.jackson.core.JsonProcessingException
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.handler.NugetPackageHandler
import com.tencent.bkrepo.nuget.constants.FULL_PATH
import com.tencent.bkrepo.nuget.exception.NugetException
import com.tencent.bkrepo.nuget.model.v3.RegistrationIndex
import com.tencent.bkrepo.nuget.model.v3.search.SearchRequest
import com.tencent.bkrepo.nuget.model.v3.search.SearchResponse
import com.tencent.bkrepo.nuget.model.v3.search.SearchResponseData
import com.tencent.bkrepo.nuget.service.NugetV3ClientService
import com.tencent.bkrepo.nuget.util.NugetUtils
import com.tencent.bkrepo.nuget.util.NugetV3RegistrationUtils
import com.tencent.bkrepo.nuget.util.NugetVersionUtils
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import kotlin.streams.toList

@Service
class NugetV3ClientServiceImpl(
    private val nugetPackageHandler: NugetPackageHandler
) : NugetV3ClientService, NugetAbstractService() {

    override fun getFeed(artifactInfo: NugetArtifactInfo): String {
        return try {
            val feedResource = NugetUtils.getFeedResource()
            feedResource.replace(
                "@NugetV2Url", getV2Url(artifactInfo)
            ).replace(
                "@NugetV3Url", getV3Url(artifactInfo)
            )
        } catch (exception: IOException) {
            logger.error("unable to read resource: $exception")
            StringPool.EMPTY
        }
    }

    override fun registration(
        artifactInfo: NugetArtifactInfo,
        packageId: String,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): RegistrationIndex {
        with(artifactInfo) {
            val packageVersionList =
                packageClient.listVersionPage(projectId, repoName, PackageKeys.ofNuget(packageId)).data!!.records
            if (packageVersionList.isEmpty()) {
                throw NugetException(
                    "nuget metadata not found for package [$packageId] in repo [${this.getRepoIdentify()}]"
                )
            }
            val metadataList = packageVersionList.map { it.metadata }.stream()
                .sorted { o1, o2 -> NugetVersionUtils.compareSemVer(o1["version"] as String, o2["version"] as String) }
                .toList()
            try {
                val v3RegistrationUrl = getV3Url(artifactInfo) + '/' + registrationPath
                return NugetV3RegistrationUtils.metadataToRegistrationIndex(metadataList, v3RegistrationUrl)
            } catch (ignored: JsonProcessingException) {
                logger.error("failed to deserialize metadata to registration index json")
                throw ignored
            }
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    override fun download(artifactInfo: NugetArtifactInfo, packageId: String, packageVersion: String) {
        val nupkgFileName = NugetUtils.getNupkgFileName(packageId, packageVersion)
        val context = ArtifactDownloadContext()
        context.putAttribute(FULL_PATH, nupkgFileName)
        ArtifactContextHolder.getRepository().download(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    override fun search(artifactInfo: NugetArtifactInfo, searchRequest: SearchRequest): SearchResponse {
        logger.info("handling search request in repo [${artifactInfo.getRepoIdentify()}], parameter: $searchRequest")
        with(artifactInfo) {
            val v3RegistrationUrl = getV3Url(artifactInfo) + "/registration-semver2"
            val packageListOption = PackageListOption(packageName = searchRequest.q)
            val packageList = packageClient.listPackagePage(projectId, repoName, packageListOption).data!!.records
            val pagedResultList =
                packageList.stream().skip(searchRequest.skip.toLong()).limit(searchRequest.take.toLong()).toList()
            if (pagedResultList.isEmpty()) return SearchResponse()
            val searchResponseDataList = pagedResultList.map {
                buildSearchResponseData(it, searchRequest, v3RegistrationUrl)
            }
            return SearchResponse(packageList.size, searchResponseDataList)
        }
    }

    private fun buildSearchResponseData(
        packageSummary: PackageSummary,
        searchRequest: SearchRequest,
        v3RegistrationUrl: String
    ): SearchResponseData {
        with(packageSummary) {
            val packageVersionList = packageClient.listVersionPage(projectId, repoName, key).data!!.records
            // preRelease需要处理
            val sortedPackageVersionList =
                packageVersionList.stream()
                    .sorted { o1, o2 -> NugetVersionUtils.compareSemVer(o1.name, o2.name) }.toList()
            return NugetV3RegistrationUtils.versionListToSearchResponse(
                sortedPackageVersionList, this, searchRequest, v3RegistrationUrl
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NugetV3ClientServiceImpl::class.java)
    }
}
