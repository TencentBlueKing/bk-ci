package com.tencent.bkrepo.nuget.artifact.resolver

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoResolver
import com.tencent.bkrepo.common.artifact.resolve.path.Resolver
import com.tencent.bkrepo.common.artifact.util.version.SemVersion
import com.tencent.bkrepo.nuget.constant.VERSION
import com.tencent.bkrepo.nuget.exception.NugetArtifactReceiveException
import com.tencent.bkrepo.nuget.pojo.artifact.NugetPublishArtifactInfo
import com.tencent.bkrepo.nuget.util.DecompressUtil.resolverNuspec
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import javax.servlet.http.HttpServletRequest

@Component
@Resolver(NugetPublishArtifactInfo::class)
class NugetPublishInfoResolver : ArtifactInfoResolver {
    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        val artifactFileMap = ArtifactFileMap()
        if (request is MultipartHttpServletRequest) {
            request.fileMap.forEach { (key, value) -> artifactFileMap[key] = resolveMultipartFile(value) }
        } else throw ErrorCodeException(CommonMessageCode.PARAMETER_MISSING, "multipart file")
        val artifactFile = artifactFileMap["package"] ?: run {
            throw NugetArtifactReceiveException("Unable to find 'package' field in request form data.")
        }
        val nupkgPackage = artifactFile.getInputStream().use { it.resolverNuspec() }
        val packageName = nupkgPackage.metadata.id
        val version = nupkgPackage.metadata.version
        // 校验
        Preconditions.checkArgument(SemVersion.validate(version), VERSION)
        val size = artifactFile.getSize()
        val publishInfo = NugetPublishArtifactInfo(projectId, repoName, packageName, version, nupkgPackage, size)
        publishInfo.artifactFile = artifactFile
        return publishInfo
    }

    private fun resolveMultipartFile(multipartFile: MultipartFile): ArtifactFile {
        return ArtifactFileFactory.build(multipartFile)
    }
}
