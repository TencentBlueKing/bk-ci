package com.tencent.bkrepo.nuget.util

import com.github.zafarkhaja.semver.Version
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.nuget.model.nuspec.Dependency
import com.tencent.bkrepo.nuget.model.nuspec.NuspecMetadata
import com.tencent.bkrepo.nuget.model.v3.DependencyGroups
import com.tencent.bkrepo.nuget.model.v3.RegistrationCatalogEntry
import com.tencent.bkrepo.nuget.model.v3.RegistrationIndex
import com.tencent.bkrepo.nuget.model.v3.RegistrationLeaf
import com.tencent.bkrepo.nuget.model.v3.RegistrationPage
import com.tencent.bkrepo.nuget.model.v3.search.SearchRequest
import com.tencent.bkrepo.nuget.model.v3.search.SearchResponseData
import com.tencent.bkrepo.nuget.model.v3.search.SearchResponseDataVersion
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.stream.IntStream
import kotlin.streams.toList

object NugetV3RegistrationUtils {

    private val logger: Logger = LoggerFactory.getLogger(NugetV3RegistrationUtils::class.java)

    fun metadataToRegistrationIndex(
        metadataList: List<Map<String, Any>>,
        v3RegistrationUrl: String
    ): RegistrationIndex {
        val registrationLeafList = metadataList.stream().map {
            metadataToRegistrationLeaf(it, v3RegistrationUrl)
        }.toList()
        // 涉及到分页的问题需要处理
        return registrationLeafListToRegistrationIndex(registrationLeafList, v3RegistrationUrl)
    }

    private fun metadataToRegistrationLeaf(
        metadataMap: Map<String, Any>?,
        v3RegistrationUrl: String
    ): RegistrationLeaf {
        val writeValueAsString = JsonUtils.objectMapper.writeValueAsString(metadataMap)
        val nuspecMetadata = JsonUtils.objectMapper.readValue(writeValueAsString, NuspecMetadata::class.java)
        val registrationLeafId =
            NugetUtils.buildRegistrationLeafUrl(v3RegistrationUrl, nuspecMetadata.id, nuspecMetadata.version)
        val packageContent =
            NugetUtils.buildPackageContentUrl(v3RegistrationUrl, nuspecMetadata.id, nuspecMetadata.version)
        // dependency 需要处理
        val dependencyGroups = metadataToDependencyGroups(nuspecMetadata.dependencies, v3RegistrationUrl)
        val catalogEntry = metadataToRegistrationCatalogEntry(nuspecMetadata, v3RegistrationUrl, dependencyGroups)
        return RegistrationLeaf(
            id = registrationLeafId,
            catalogEntry = catalogEntry,
            packageContent = packageContent,
            registration = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, nuspecMetadata.id)
        )
    }

    private fun metadataToDependencyGroups(
        dependencies: List<Dependency>?,
        v3RegistrationUrl: String
    ): List<DependencyGroups>? {
        return dependencies?.let {
            val dependencyGroups = mutableListOf<DependencyGroups>()
            v3RegistrationUrl + ""
            dependencyGroups
        }
    }

    private fun metadataToRegistrationCatalogEntry(
        nupkgMetadata: NuspecMetadata,
        v3RegistrationUrl: String,
        dependencyGroups: List<DependencyGroups>?
    ): RegistrationCatalogEntry {
        with(nupkgMetadata) {
            return RegistrationCatalogEntry(
                id = URI.create(v3RegistrationUrl),
                authors = authors,
                dependencyGroups = dependencyGroups,
                deprecation = null,
                description = description,
                iconUrl = iconUrl?.let { URI.create(it) },
                packageId = id,
                licenseUrl = licenseUrl?.let { URI.create(it) },
                licenseExpression = null,
                listed = false,
                minClientVersion = minClientVersion,
                projectUrl = projectUrl?.let { URI.create(it) },
                published = null,
                requireLicenseAcceptance = requireLicenseAcceptance,
                summary = summary,
                tags = emptyList(),
                title = title,
                version = version
            )
        }
    }

    private fun registrationLeafListToRegistrationIndex(
        registrationLeafList: List<RegistrationLeaf>,
        v3RegistrationUrl: String
    ): RegistrationIndex {
        if (registrationLeafList.isEmpty()) {
            throw IllegalArgumentException("Cannot build registration with no package version")
        } else {
            val versionCount = registrationLeafList.size
            val pagesCount = versionCount / 64 + if (versionCount % 64 != 0) 1 else 0
            val packageId = registrationLeafList[0].catalogEntry.packageId
            val registrationPageList =
                buildRegistrationPageList(registrationLeafList, v3RegistrationUrl, versionCount, pagesCount, packageId)
            return RegistrationIndex(pagesCount, registrationPageList)
        }
    }

    private fun buildRegistrationPageList(
        registrationLeafList: List<RegistrationLeaf>,
        v3RegistrationUrl: String,
        versionCount: Int,
        pagesCount: Int,
        packageId: String
    ): List<RegistrationPage> {
        return IntStream.range(0, pagesCount).mapToObj { i ->
            // 计算每一页中的最小版本与最大版本
            val lowerVersion = registrationLeafList[64 * i].catalogEntry.version
            val isLastPage = i == pagesCount - 1
            val lastPackageIndexInPage = if (isLastPage) versionCount - 1 else 64 * i + 63
            val upperVersion = registrationLeafList[lastPackageIndexInPage].catalogEntry.version
            val packagesInPageCount = computedPageCount(isLastPage, versionCount)
            RegistrationPage(
                id = NugetUtils.buildRegistrationPageUrl(v3RegistrationUrl, packageId, lowerVersion, upperVersion),
                count = packagesInPageCount,
                items = registrationLeafList,
                lower = lowerVersion,
                parent = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, packageId),
                upper = upperVersion
            )
        }.toList()
    }

    private fun computedPageCount(lastPage: Boolean, versionCount: Int): Int {
        if (!lastPage && versionCount < 64) {
            val message = "Number of packages in a page must be 64 unless it's the last page"
            logger.error(message)
            throw IllegalArgumentException(message)
        }
        val versionCountPrePage = 64
        if (versionCount % versionCountPrePage != 0 && lastPage) {
            return versionCount % versionCountPrePage
        }
        return versionCountPrePage
    }

    private fun isPreRelease(version: String): Boolean {
        return try {
            val v = Version.valueOf(version)
            v.preReleaseVersion.isNotEmpty()
        } catch (ex: Exception) {
            logger.trace("could not parse version: [$version] as semver2.")
            true
        }
    }

    fun versionListToSearchResponse(
        sortedPackageVersionList: List<PackageVersion>,
        packageSummary: PackageSummary,
        searchRequest: SearchRequest,
        v3RegistrationUrl: String
    ): SearchResponseData {
        val latestVersionPackage = sortedPackageVersionList.last()
        val searchResponseDataVersionList =
            sortedPackageVersionList.filter { searchRequest.prerelease || !isPreRelease(latestVersionPackage.name) }
                .map { buildSearchResponseDataVersion(it, packageSummary.name, v3RegistrationUrl) }
        val writeValueAsString = JsonUtils.objectMapper.writeValueAsString(latestVersionPackage.metadata)
        val nuspecMetadata = JsonUtils.objectMapper.readValue(writeValueAsString, NuspecMetadata::class.java)
        return buildSearchResponseData(v3RegistrationUrl, searchResponseDataVersionList, nuspecMetadata, packageSummary)
    }

    private fun buildSearchResponseData(
        v3RegistrationUrl: String,
        searchResponseDataVersionList: List<SearchResponseDataVersion>,
        nuspecMetadata: NuspecMetadata,
        packageSummary: PackageSummary
    ): SearchResponseData {
        with(nuspecMetadata) {
            return SearchResponseData(
                id = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, id),
                version = version,
                packageId = id,
                description = description,
                versions = searchResponseDataVersionList,
                authors = authors.split(','),
                iconUrl = iconUrl?.let { URI.create(it) },
                licenseUrl = licenseUrl?.let { URI.create(it) },
                owners = owners?.split(','),
                projectUrl = projectUrl?.let { URI.create(it) },
                registration = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, id),
                summary = summary,
                tags = tags?.split(','),
                title = title,
                totalDownloads = packageSummary.downloads.toInt(),
                verified = false,
                packageTypes = emptyList()
            )
        }
    }

    private fun buildSearchResponseDataVersion(
        packageVersion: PackageVersion,
        packageId: String,
        v3RegistrationUrl: String
    ): SearchResponseDataVersion {
        with(packageVersion) {
            return SearchResponseDataVersion(
                NugetUtils.buildRegistrationLeafUrl(v3RegistrationUrl, packageId, name),
                name,
                downloads.toInt()
            )
        }
    }
}
