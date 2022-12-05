package com.tencent.bkrepo.nuget.util

import com.tencent.bkrepo.nuget.common.NugetRemoteAndVirtualCommon
import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import com.tencent.bkrepo.nuget.pojo.v3.metadata.feed.Resource
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationCatalogEntry
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationIndex
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationItem
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationPageItem
import com.tencent.bkrepo.nuget.pojo.v3.metadata.leaf.RegistrationLeaf
import com.tencent.bkrepo.nuget.pojo.v3.metadata.page.RegistrationPage
import java.net.URI
import java.util.Objects

object NugetV3RemoteRepositoryUtils {
    fun convertOriginalToBkrepoResource(
        original: Resource,
        v2BaseUrl: String,
        v3BaseUrl: String
    ): Resource? {
        return if (!NugetRemoteAndVirtualCommon().originalToBkrepoConverters.containsKey(original.type)) {
            null
        } else {
            val urlConvert = NugetRemoteAndVirtualCommon().originalToBkrepoConverters[original.type]!!
            val convertedUrl: String = urlConvert.convert(v2BaseUrl, v3BaseUrl)
            Resource(convertedUrl, original.type, original.comment, original.clientVersion)
        }
    }

    fun rewriteRegistrationIndexUrls(
        originalRegistrationIndex: RegistrationIndex,
        artifactInfo: NugetRegistrationArtifactInfo,
        v2BaseUrl: String,
        v3BaseUrl: String,
        registrationPath: String
    ): RegistrationIndex {
        val v3RegistrationUrl = "$v3BaseUrl/$registrationPath".trimEnd('/')
        val itemList = originalRegistrationIndex.items.map { item ->
            registrationResultItemRewriter(item, artifactInfo.packageName, v2BaseUrl, v3RegistrationUrl)
        }
        return RegistrationIndex(originalRegistrationIndex.count, itemList)
    }

    private fun registrationResultItemRewriter(
        originalItem: RegistrationItem,
        packageName: String,
        v2BaseUrl: String,
        v3RegistrationUrl: String
    ): RegistrationItem {
        val isPaged = Objects.isNull(originalItem.items)
        val registrationIndexUrl = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, packageName)
        val registrationPageUrl =
            NugetUtils.buildRegistrationPageUrl(v3RegistrationUrl, packageName, originalItem.lower, originalItem.upper)
        val pageItemList = originalItem.items?.map { item ->
            registrationResultPageItemRewriter(item, packageName, v2BaseUrl, v3RegistrationUrl)
        }
        return RegistrationItem(
            id = if (isPaged) registrationPageUrl else registrationIndexUrl,
            count = originalItem.count,
            items = pageItemList,
            lower = originalItem.lower,
            upper = originalItem.upper,
            parent = if (isPaged) null else registrationIndexUrl
        )
    }

    fun registrationResultPageItemRewriter(
        originalPageItem: RegistrationPageItem,
        packageName: String,
        v2BaseUrl: String,
        v3RegistrationUrl: String
    ): RegistrationPageItem {
        val version = originalPageItem.catalogEntry.version
        val packageContentUrl = NugetUtils.buildPackageContentUrl(v3RegistrationUrl, packageName, version)
        val id = NugetUtils.buildRegistrationLeafUrl(v3RegistrationUrl, packageName, version)
        val rewriteCatalogEntry =
            registrationCatalogEntryRewriter(originalPageItem.catalogEntry, packageContentUrl, v3RegistrationUrl)
        return RegistrationPageItem(id, rewriteCatalogEntry, packageContentUrl)
    }

    /**
     * 这里如果有dependency依赖，需要重写dependency中的registration
     */
    private fun registrationCatalogEntryRewriter(
        originalCatalogEntry: RegistrationCatalogEntry,
        rewrittenPackageContentUrl: URI,
        v3RegistrationUrl: String
    ): RegistrationCatalogEntry {
        return with(originalCatalogEntry) {
            RegistrationCatalogEntry(
                id = URI.create(v3RegistrationUrl),
                authors = authors,
                dependencyGroups = dependencyGroups,
                deprecation = null,
                description = description,
                iconUrl = iconUrl,
                packageId = packageId,
                licenseUrl = licenseUrl,
                licenseExpression = licenseExpression,
                listed = listed,
                minClientVersion = minClientVersion,
                projectUrl = projectUrl,
                published = published,
                requireLicenseAcceptance = requireLicenseAcceptance,
                summary = summary,
                tags = tags,
                title = title,
                version = version
            )
        }
    }

    fun rewriteRegistrationPageUrls(
        originalRegistrationPage: RegistrationPage,
        artifactInfo: NugetRegistrationArtifactInfo,
        v2BaseUrl: String,
        v3BaseUrl: String,
        registrationPath: String
    ): RegistrationPage {
        val v3RegistrationUrl = "$v3BaseUrl/$registrationPath".trimEnd('/')
        val rewrittenPageUrl = NugetUtils.buildRegistrationPageUrl(
            v3RegistrationUrl, artifactInfo.packageName, originalRegistrationPage.lower, originalRegistrationPage.upper
        )
        val rewrittenParentUrl = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, artifactInfo.packageName)
        val itemList = originalRegistrationPage.items.map { item ->
            registrationResultPageItemRewriter(item, artifactInfo.packageName, v2BaseUrl, v3RegistrationUrl)
        }
        return RegistrationPage(
            id = rewrittenPageUrl,
            count = originalRegistrationPage.count,
            items = itemList,
            lower = originalRegistrationPage.lower,
            upper = originalRegistrationPage.upper,
            parent = rewrittenParentUrl
        )
    }

    fun rewriteRegistrationLeafUrls(
        originalRegistrationLeaf: RegistrationLeaf,
        artifactInfo: NugetRegistrationArtifactInfo,
        v2BaseUrl: String,
        v3BaseUrl: String,
        registrationPath: String
    ): RegistrationLeaf {
        val v3RegistrationUrl = "$v3BaseUrl/$registrationPath".trimEnd('/')
        val rewrittenIndexUrl = NugetUtils.buildRegistrationIndexUrl(v3RegistrationUrl, artifactInfo.packageName)
        val rewrittenLeafUrl = NugetUtils.buildRegistrationLeafUrl(
            v3RegistrationUrl, artifactInfo.packageName, artifactInfo.version
        )
        val rewritePackageContentUrl =
            NugetUtils.buildPackageContentUrl(v3RegistrationUrl, artifactInfo.packageName, artifactInfo.version)
        return RegistrationLeaf(
            id = rewrittenLeafUrl,
            // 这个接口展示没提供，
            catalogEntry = originalRegistrationLeaf.catalogEntry,
            listed = originalRegistrationLeaf.listed,
            packageContent = rewritePackageContentUrl,
            published = originalRegistrationLeaf.published,
            registration = rewrittenIndexUrl
        )
    }
}
