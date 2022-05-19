package com.tencent.bkrepo.nuget.artifact.resolver

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoResolver
import com.tencent.bkrepo.common.artifact.resolve.path.Resolver
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.nuget.constant.ID
import com.tencent.bkrepo.nuget.constant.PACKAGE_KEY
import com.tencent.bkrepo.nuget.constant.VERSION
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDeleteArtifactInfo
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@Component
@Resolver(NugetDeleteArtifactInfo::class)
class NugetDeleteArtifactInfoResolver : ArtifactInfoResolver {
    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        // 判断是客户端的请求还是页面发送的请求分别进行处理
        val requestURL = request.requestURL
        return when {
            // 页面删除包请求
            requestURL.contains(PACKAGE_DELETE_PREFIX) -> {
                val packageKey = request.getParameter(PACKAGE_KEY)
                NugetDeleteArtifactInfo(projectId, repoName, packageKey)
            }
            // 页面删除包版本请求
            requestURL.contains(PACKAGE_VERSION_DELETE_PREFIX) -> {
                val packageKey = request.getParameter(PACKAGE_KEY)
                val version = request.getParameter(VERSION)
                NugetDeleteArtifactInfo(projectId, repoName, packageKey, version)
            }
            else -> {
                // 客户端请求删除版本
                val attributes = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
                val id = attributes[ID].toString().trim()
                val version = attributes[VERSION].toString().trim()
                NugetDeleteArtifactInfo(projectId, repoName, PackageKeys.ofHelm(id), version)
            }
        }
    }

    companion object {
        private const val PACKAGE_DELETE_PREFIX = "/ext/package/delete/"
        private const val PACKAGE_VERSION_DELETE_PREFIX = "/ext/version/delete/"
    }
}
