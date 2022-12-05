package com.tencent.bkrepo.nuget.util

import com.github.zafarkhaja.semver.Version
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.nuget.pojo.nuspec.Dependency
import com.tencent.bkrepo.nuget.pojo.nuspec.NuspecMetadata
import com.tencent.bkrepo.nuget.pojo.request.NugetSearchRequest
import com.tencent.bkrepo.nuget.pojo.response.search.SearchResponseData
import com.tencent.bkrepo.nuget.pojo.response.search.SearchResponseDataVersion
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.DependencyGroups
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationCatalogEntry
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationIndex
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationItem
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationPageItem
import com.tencent.bkrepo.nuget.pojo.v3.metadata.leaf.RegistrationLeaf
import com.tencent.bkrepo.nuget.pojo.v3.metadata.page.RegistrationPage
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.stream.IntStream
import kotlin.streams.toList

object NugetV3RegistrationUtils {

    private val logger: Logger = LoggerFactory.getLogger(NugetV3RegistrationUtils::class.java)

    fun metadataToRegistrationLeaf(
        packageId: String,
        version: String,
        listed: Boolean,
        v3RegistrationUrl: String
    ): RegistrationLeaf {
        return RegistrationLeaf(
            id = NugetUtils.buildRegistrationLeafUrl(v3RegistrationUrl, packageId, version),
            listed = listed,
            packageContent = NugetUtils.buildPackageContentUrl(v3RegistrationUrl, packageId, version),
            registration = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, packageId)
        )
    }

    fun metadataToRegistrationPage(
        sortedPackageVersionList: List<PackageVersion>,
        packageId: String,
        lowerVersion: String,
        upperVersion: String,
        v3RegistrationUrl: String
    ): RegistrationPage {
        val registrationPageItemList = sortedPackageVersionList.stream().filter {
            betweenVersions(lowerVersion, upperVersion, it.name)
        }.map {
            metadataToRegistrationPageItem(it, v3RegistrationUrl)
        }.toList()
        // 涉及到分页的问题需要处理
        return registrationPageItemToRegistrationPage(
            registrationPageItemList, packageId, lowerVersion, upperVersion, v3RegistrationUrl
        )
    }

    private fun betweenVersions(lowerVersion: String, upperVersion: String, version: String): Boolean {
        return NugetVersionUtils.compareSemVer(lowerVersion, version) <= 0 &&
            NugetVersionUtils.compareSemVer(upperVersion, version) >= 0
    }

    fun registrationPageItemToRegistrationPage(
        registrationPageItemList: List<RegistrationPageItem>,
        packageId: String,
        lowerVersion: String,
        upperVersion: String,
        v3RegistrationUrl: String
    ): RegistrationPage {
        if (registrationPageItemList.isEmpty()) {
            throw IllegalArgumentException("Cannot build registration with no package version")
        } else {
            val pageURI =
                NugetUtils.buildRegistrationPageUrl(v3RegistrationUrl, packageId, lowerVersion, upperVersion)
            val count = registrationPageItemList.size
            val registrationUrl: URI = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, packageId)
            return RegistrationPage(
                pageURI, count, registrationPageItemList, lowerVersion, registrationUrl, upperVersion
            )
        }
    }

    fun metadataToRegistrationIndex(
        sortedPackageVersionList: List<PackageVersion>,
        v3RegistrationUrl: String
    ): RegistrationIndex {
        val registrationLeafList = sortedPackageVersionList.stream().map {
            metadataToRegistrationPageItem(it, v3RegistrationUrl)
        }.toList()
        // 涉及到分页的问题需要处理
        return registrationPageItemToRegistrationIndex(registrationLeafList, v3RegistrationUrl)
    }

    fun metadataToRegistrationPageItem(
        packageVersion: PackageVersion,
        v3RegistrationUrl: String
    ): RegistrationPageItem {
//        val writeValueAsString = JsonUtils.objectMapper.writeValueAsString(metadataMap)
//        val nuspecMetadata = JsonUtils.objectMapper.readValue(writeValueAsString, NuspecMetadata::class.java)
        val nuspecMetadata = NugetUtils.resolveVersionMetadata(packageVersion)
        val registrationPageItemId =
            NugetUtils.buildRegistrationLeafUrl(v3RegistrationUrl, nuspecMetadata.id, nuspecMetadata.version)
        val packageContent =
            NugetUtils.buildPackageContentUrl(v3RegistrationUrl, nuspecMetadata.id, nuspecMetadata.version)
        // dependency 需要处理
        val dependencyGroups = metadataToDependencyGroups(nuspecMetadata.dependencies, v3RegistrationUrl)
        val catalogEntry = metadataToRegistrationCatalogEntry(nuspecMetadata, v3RegistrationUrl, dependencyGroups)
        return RegistrationPageItem(
            id = registrationPageItemId,
            catalogEntry = catalogEntry,
            packageContent = packageContent
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

    fun registrationPageItemToRegistrationIndex(
        registrationLeafList: List<RegistrationPageItem>,
        v3RegistrationUrl: String
    ): RegistrationIndex {
        if (registrationLeafList.isEmpty()) {
            throw IllegalArgumentException("Cannot build registration with no package version")
        } else {
            val versionCount = registrationLeafList.size
            val pagesCount = versionCount / 64 + if (versionCount % 64 != 0) 1 else 0
            val packageId = registrationLeafList[0].catalogEntry.packageId
            val registrationPageList =
                buildRegistrationItems(registrationLeafList, v3RegistrationUrl, versionCount, pagesCount, packageId)
            return RegistrationIndex(pagesCount, registrationPageList)
        }
    }

    private fun buildRegistrationItems(
        registrationLeafList: List<RegistrationPageItem>,
        v3RegistrationUrl: String,
        versionCount: Int,
        pagesCount: Int,
        packageId: String
    ): List<RegistrationItem> {
        return IntStream.range(0, pagesCount).mapToObj { i ->
            // 计算每一页中的最小版本与最大版本
            val lowerVersion = registrationLeafList[64 * i].catalogEntry.version
            val isLastPage = i == pagesCount - 1
            val lastPackageIndexInPage = if (isLastPage) versionCount - 1 else 64 * i + 63
            val upperVersion = registrationLeafList[lastPackageIndexInPage].catalogEntry.version
            val packagesInPageCount = computedPageCount(isLastPage, versionCount)
            RegistrationItem(
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
        searchRequest: NugetSearchRequest,
        v3RegistrationUrl: String
    ): SearchResponseData {
        val latestVersionPackage = sortedPackageVersionList.last()
        val searchResponseDataVersionList =
            sortedPackageVersionList.filter {
                searchRequest.prerelease ?: false || !isPreRelease(latestVersionPackage.name)
            }.map { buildSearchResponseDataVersion(it, packageSummary.name, v3RegistrationUrl) }
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
