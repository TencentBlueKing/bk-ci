package com.tencent.bkrepo.nuget.service.impl

import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.pojo.request.NugetSearchRequest
import com.tencent.bkrepo.nuget.pojo.response.search.NugetSearchResponse
import com.tencent.bkrepo.nuget.pojo.response.search.SearchResponseData
import com.tencent.bkrepo.nuget.service.NugetSearchService
import com.tencent.bkrepo.nuget.util.NugetUtils
import com.tencent.bkrepo.nuget.util.NugetV3RegistrationUtils
import com.tencent.bkrepo.nuget.util.NugetVersionUtils
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.PackageListOption
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.streams.toList

@Service
class NugetSearchServiceImpl(
    private val packageClient: PackageClient
) : NugetSearchService {
    override fun search(artifactInfo: NugetArtifactInfo, searchRequest: NugetSearchRequest): NugetSearchResponse {
        logger.info("handling search request in repo [${artifactInfo.getRepoIdentify()}], parameter: $searchRequest")
        with(artifactInfo) {
            val v3RegistrationUrl = NugetUtils.getV3Url(artifactInfo) + "/registration-semver2"
            val packageListOption = PackageListOption(packageName = searchRequest.q)
            val packageList = packageClient.listPackagePage(projectId, repoName, packageListOption).data!!.records
            val pagedResultList =
                packageList.stream().skip(searchRequest.skip.toLong()).limit(searchRequest.take.toLong()).toList()
            if (pagedResultList.isEmpty()) return NugetSearchResponse()
            val searchResponseDataList = pagedResultList.map {
                buildSearchResponseData(it, searchRequest, v3RegistrationUrl)
            }
            return NugetSearchResponse(packageList.size, searchResponseDataList)
        }
    }

    private fun buildSearchResponseData(
        packageSummary: PackageSummary,
        searchRequest: NugetSearchRequest,
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
        private val logger = LoggerFactory.getLogger(NugetSearchServiceImpl::class.java)
    }
}
