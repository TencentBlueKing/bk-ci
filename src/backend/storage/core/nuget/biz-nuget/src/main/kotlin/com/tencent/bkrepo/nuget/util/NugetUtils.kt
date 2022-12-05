package com.tencent.bkrepo.nuget.util

import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.nuget.constant.PACKAGE
import com.tencent.bkrepo.nuget.pojo.nuspec.NuspecMetadata
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.apache.commons.io.IOUtils
import java.net.URI
import java.util.StringJoiner

object NugetUtils {
    private const val NUGET_FULL_PATH = "/%s/%s.%s.nupkg"
    private const val NUGET_PACKAGE_NAME = "%s.%s.nupkg"

    fun getNupkgFullPath(id: String, version: String): String {
        return String.format(NUGET_FULL_PATH, id, id, version).toLowerCase()
    }

    private fun getNupkgFileName(id: String, version: String): String {
        return String.format(NUGET_PACKAGE_NAME, id, version).toLowerCase()
    }

    fun getServiceDocumentResource(): String {
        val inputStream = this.javaClass.classLoader.getResourceAsStream("service_document.xml")
        return inputStream.use { IOUtils.toString(it, "UTF-8") }
    }

    fun getFeedResource(): String {
        val inputStream = this.javaClass.classLoader.getResourceAsStream("v3/nugetRootFeedIndex.json")
        return inputStream.use { IOUtils.toString(it, "UTF-8") }
    }

    fun getV2Url(artifactInfo: ArtifactInfo): String {
        val url = HttpContextHolder.getRequest().requestURL
        val domain = url.delete(url.length - HttpContextHolder.getRequest().requestURI.length, url.length)
        return domain.append(artifactInfo.getRepoIdentify()).toString()
    }

    fun getV3Url(artifactInfo: ArtifactInfo): String {
        val url = HttpContextHolder.getRequest().requestURL
        val domain = url.delete(url.length - HttpContextHolder.getRequest().requestURI.length, url.length)
        return domain.append(artifactInfo.getRepoIdentify()).append(CharPool.SLASH).append("v3").toString()
    }

    fun buildPackageContentUrl(v3RegistrationUrl: String, packageId: String, version: String): URI {
        val packageContentUrl = StringJoiner("/")
            .add(
                UrlFormatter.formatUrl(
                    v3RegistrationUrl.removeSuffix("registration-semver2").plus("flatcontainer")
                )
            )
            .add(packageId.toLowerCase()).add(version).add(getNupkgFileName(packageId, version))
        return URI.create(packageContentUrl.toString())
    }

    fun buildRegistrationLeafUrl(v3RegistrationUrl: String, packageId: String, version: String): URI {
        val packageContentUrl = StringJoiner("/").add(UrlFormatter.format(v3RegistrationUrl))
            .add(packageId.toLowerCase()).add("$version.json")
        return URI.create(packageContentUrl.toString())
    }

    fun buildRegistrationPageUrl(v3RegistrationUrl: String, packageId: String, lower: String, upper: String): URI {
        val packageContentUrl = StringJoiner("/").add(UrlFormatter.format(v3RegistrationUrl))
            .add(packageId.toLowerCase()).add("page").add(lower).add("$upper.json")
        return URI.create(packageContentUrl.toString())
    }

    fun buildRegistrationIndexUrl(v3RegistrationUrl: String, packageId: String): URI {
        val packageContentUrl = StringJoiner("/").add(UrlFormatter.format(v3RegistrationUrl))
            .add(packageId.toLowerCase()).add("index.json")
        return URI.create(packageContentUrl.toString())
    }

    /**
     * 从[versionPackage]中解析[NuspecMetadata]
     */
    fun resolveVersionMetadata(versionPackage: PackageVersion): NuspecMetadata {
        return versionPackage.extension[PACKAGE].toString().readJsonString()
    }
}
