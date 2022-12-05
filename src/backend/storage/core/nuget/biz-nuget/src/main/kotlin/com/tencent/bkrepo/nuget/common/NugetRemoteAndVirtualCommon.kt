package com.tencent.bkrepo.nuget.common

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.nuget.constant.REMOTE_URL
import com.tencent.bkrepo.nuget.exception.NugetFeedNofFoundException
import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import com.tencent.bkrepo.nuget.pojo.v3.metadata.feed.Feed
import com.tencent.bkrepo.nuget.pojo.v3.metadata.index.RegistrationIndex
import com.tencent.bkrepo.nuget.pojo.v3.metadata.leaf.RegistrationLeaf
import com.tencent.bkrepo.nuget.pojo.v3.metadata.page.RegistrationPage
import com.tencent.bkrepo.nuget.util.NugetUtils
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class NugetRemoteAndVirtualCommon {

    final val originalToBkrepoConverters = mutableMapOf<String, UrlConvert>()

    fun downloadRemoteFeed(): Feed {
        val context = ArtifactQueryContext()
        val configuration = context.getRemoteConfiguration()
        val requestUrl = UrlFormatter.format(configuration.url, "/v3/index.json")
        context.putAttribute(REMOTE_URL, requestUrl)
        val repository = ArtifactContextHolder.getRepository()
        return repository.query(context)?.let { JsonUtils.objectMapper.readValue(it as InputStream, Feed::class.java) }
            ?: throw NugetFeedNofFoundException(
                "query remote feed index.json for [${context.getRemoteConfiguration().url}] failed!"
            )
    }

    fun downloadRemoteRegistrationIndex(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        v2BaseUrl: String,
        v3BaseUrl: String
    ): RegistrationIndex? {
        val registrationBaseUrl = "$v3BaseUrl/$registrationPath".trimEnd('/')
        val originalRegistrationBaseUrl =
            convertToOriginalUrl(registrationBaseUrl, v2BaseUrl, v3BaseUrl)
        val originalRegistrationIndexUrl = NugetUtils.buildRegistrationIndexUrl(
            originalRegistrationBaseUrl, artifactInfo.packageName
        ).toString()
        val context = ArtifactQueryContext()
        context.putAttribute(REMOTE_URL, originalRegistrationIndexUrl)
        val repository = ArtifactContextHolder.getRepository()
        return repository.query(context)?.let {
            JsonUtils.objectMapper.readValue(it as InputStream, RegistrationIndex::class.java)
        }
    }

    fun downloadRemoteRegistrationPage(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        v2BaseUrl: String,
        v3BaseUrl: String
    ): RegistrationPage {
        val registrationBaseUrl = "$v3BaseUrl/$registrationPath".trimEnd('/')
        val originalRegistrationBaseUrl =
            convertToOriginalUrl(registrationBaseUrl, v2BaseUrl, v3BaseUrl)
        val originalRegistrationPageUrl = NugetUtils.buildRegistrationPageUrl(
            originalRegistrationBaseUrl, artifactInfo.packageName, artifactInfo.lowerVersion, artifactInfo.upperVersion
        )
        val context = ArtifactQueryContext()
        context.putAttribute(REMOTE_URL, originalRegistrationPageUrl)
        val repository = ArtifactContextHolder.getRepository()
        return repository.query(context)?.let {
            JsonUtils.objectMapper.readValue(it as InputStream, RegistrationPage::class.java)
        }
        // 这里不应该抛这个异常
            ?: throw NugetFeedNofFoundException(
                "query remote registrationIndex for [$originalRegistrationPageUrl] failed!"
            )
    }

    fun downloadRemoteRegistrationLeaf(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        v2BaseUrl: String,
        v3BaseUrl: String
    ): RegistrationLeaf {
        val registrationBaseUrl = "$v3BaseUrl/$registrationPath".trimEnd('/')
        val originalRegistrationBaseUrl =
            convertToOriginalUrl(registrationBaseUrl, v2BaseUrl, v3BaseUrl)
        val originalRegistrationLeafUrl = NugetUtils.buildRegistrationLeafUrl(
            originalRegistrationBaseUrl, artifactInfo.packageName, artifactInfo.version
        )
        val context = ArtifactQueryContext()
        context.putAttribute(REMOTE_URL, originalRegistrationLeafUrl)
        val repository = ArtifactContextHolder.getRepository()
        return repository.query(context)?.let {
            JsonUtils.objectMapper.readValue(it as InputStream, RegistrationLeaf::class.java)
        }
        // 这里不应该抛这个异常
            ?: throw NugetFeedNofFoundException(
                "query remote registrationIndex for [$originalRegistrationLeafUrl] failed!"
            )
    }

    private fun convertToOriginalUrl(
        registrationBaseUrl: String,
        v2BaseUrl: String,
        v3BaseUrl: String
    ): String {
        val feed = downloadRemoteFeed()
        val type = originalToBkrepoConverters.entries.stream().filter { e ->
            registrationBaseUrl == e.value.convert(v2BaseUrl, v3BaseUrl).trimEnd('/')
        }.findFirst().orElseThrow {
            throw IllegalStateException("failed to extract type by url $registrationBaseUrl")
        }.key
        return feed.resources.stream().filter { e ->
            e.type == type
        }.findFirst().orElseThrow {
            throw IllegalStateException("Failed to extract url for type: $type")
        }.id
    }

    init {
        originalToBkrepoConverters["RegistrationsBaseUrl"] = registrationsBaseUrl
        originalToBkrepoConverters["SearchQueryService"] = searchQueryService
        originalToBkrepoConverters["LegacyGallery"] = legacyGallery
        originalToBkrepoConverters["LegacyGallery/2.0.0"] = legacyGallery
        originalToBkrepoConverters["PackagePublish/2.0.0"] = packagePublish
        originalToBkrepoConverters["SearchQueryService/3.0.0-rc"] = searchQueryService
        originalToBkrepoConverters["RegistrationsBaseUrl/3.0.0-rc"] = registrationsBaseUrl
        originalToBkrepoConverters["PackageDisplayMetadataUriTemplate/3.0.0-rc"] = packageDisplayMetadataUriTemplate
        originalToBkrepoConverters["packageVersionDisplayMetadataUriTemplate/3.0.0-rc"] =
            packageVersionDisplayMetadataUriTemplate
        originalToBkrepoConverters["SearchQueryService/3.0.0-beta"] = searchQueryService
        originalToBkrepoConverters["RegistrationsBaseUrl/3.0.0-beta"] = registrationsBaseUrl
        originalToBkrepoConverters["RegistrationsBaseUrl/3.4.0"] = registrationsBaseUrl
        originalToBkrepoConverters["RegistrationsBaseUrl/3.6.0"] = registrationsBaseSemver2Url
        originalToBkrepoConverters["RegistrationsBaseUrl/Versioned"] = registrationsBaseSemver2Url
    }

    companion object {
        private val registrationsBaseUrl = object : UrlConvert {
            override fun convert(v2BaseUrl: String, v3BaseUrl: String): String {
                return registrationsBaseUrlId(v3BaseUrl)
            }
        }

        private val registrationsBaseSemver2Url = object : UrlConvert {
            override fun convert(v2BaseUrl: String, v3BaseUrl: String): String {
                return registrationsBaseUrWithSemVer2lId(v3BaseUrl)
            }
        }

        private val searchQueryService = object : UrlConvert {
            override fun convert(v2BaseUrl: String, v3BaseUrl: String): String {
                return searchQueryServiceId(v3BaseUrl)
            }
        }

        private val legacyGallery = object : UrlConvert {
            override fun convert(v2BaseUrl: String, v3BaseUrl: String): String {
                return UrlFormatter.formatUrl(v2BaseUrl)
            }
        }

        private val packagePublish = object : UrlConvert {
            override fun convert(v2BaseUrl: String, v3BaseUrl: String): String {
                return packagePublish(v2BaseUrl)
            }
        }

        private val packageDisplayMetadataUriTemplate = object : UrlConvert {
            override fun convert(v2BaseUrl: String, v3BaseUrl: String): String {
                return packageDisplayMetadataUriTemplate(v3BaseUrl)
            }
        }

        private val packageVersionDisplayMetadataUriTemplate = object : UrlConvert {
            override fun convert(v2BaseUrl: String, v3BaseUrl: String): String {
                return packageVersionDisplayMetadataUriTemplate(v3BaseUrl)
            }
        }

        private fun registrationsBaseUrlId(v3BaseUrl: String): String {
            return UrlFormatter.formatUrl(v3BaseUrl) + "/registration/"
        }

        private fun packagePublish(v2BaseUrl: String): String {
            return UrlFormatter.formatUrl(v2BaseUrl) + "/v2/package"
        }

        private fun registrationsBaseUrWithSemVer2lId(v3BaseUrl: String): String {
            return UrlFormatter.formatUrl(v3BaseUrl) + "/registration-semver2/"
        }

        private fun searchQueryServiceId(v3BaseUrl: String): String {
            return UrlFormatter.formatUrl(v3BaseUrl) + "/query"
        }

        private fun packageDisplayMetadataUriTemplate(v3BaseUrl: String): String {
            return UrlFormatter.formatUrl(v3BaseUrl) + "/registration/{id-lower}/index.json"
        }

        private fun packageVersionDisplayMetadataUriTemplate(v3BaseUrl: String): String {
            return UrlFormatter.formatUrl(v3BaseUrl) + "/registration/{id-lower}/{version-lower}.json"
        }
    }
}
