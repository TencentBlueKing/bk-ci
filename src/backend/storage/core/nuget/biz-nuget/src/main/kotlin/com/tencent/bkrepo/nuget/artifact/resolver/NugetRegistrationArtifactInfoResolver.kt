package com.tencent.bkrepo.nuget.artifact.resolver

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoResolver
import com.tencent.bkrepo.common.artifact.resolve.path.Resolver
import com.tencent.bkrepo.nuget.constant.ID
import com.tencent.bkrepo.nuget.constant.LOWER_VERSION
import com.tencent.bkrepo.nuget.constant.UPPER_VERSION
import com.tencent.bkrepo.nuget.constant.VERSION
import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@Component
@Resolver(NugetRegistrationArtifactInfo::class)
class NugetRegistrationArtifactInfoResolver : ArtifactInfoResolver {
    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        val attributes = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
        val id = attributes[ID].toString().trim()
        checkIdLowerCase(id)
        val version = attributes[VERSION].toString().trim()
        val lowerVersion = attributes[LOWER_VERSION].toString().trim()
        val upperVersion = attributes[UPPER_VERSION].toString().trim()
        return NugetRegistrationArtifactInfo(projectId, repoName, id, version, lowerVersion, upperVersion)
    }

    fun checkIdLowerCase(id: String) {
        // 判断id 必须为小写
        val regex = Regex(ALPHA_PATTERN)
        if (regex.matches(id)) {
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, id)
        }
    }

    companion object {
        const val ALPHA_PATTERN = "[A-Z]"
    }
}
