package com.tencent.devops.experience.resources.open

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.open.OpenExperienceResource
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.pojo.ExperienceJumpInfo
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import com.tencent.devops.experience.service.ExperienceAppService
import com.tencent.devops.experience.service.ExperienceOuterService
import com.tencent.devops.experience.service.ExperienceService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class OpenExperienceResourceImpl @Autowired constructor(
    private val experienceOuterService: ExperienceOuterService,
    private val experienceAppService: ExperienceAppService,
    private val experienceService: ExperienceService
) : OpenExperienceResource {
    override fun outerLogin(
        platform: Int,
        appVersion: String?,
        realIp: String,
        params: OuterLoginParam
    ): Result<String> {
        return Result(experienceOuterService.outerLogin(platform, appVersion, realIp, params))
    }

    override fun outerAuth(token: String): Result<OuterProfileVO> {
        return Result(experienceOuterService.outerAuth(token))
    }

    override fun jumpInfo(projectId: String, bundleIdentifier: String, platform: String): Result<ExperienceJumpInfo> {
        if (platform != "ANDROID" && platform != "IOS") {
            throw ErrorCodeException(
                statusCode = 403,
                defaultMessage = "平台错误",
                errorCode = ExperienceMessageCode.EXPERIENCE_NO_AVAILABLE
            )
        }
        return Result(ExperienceJumpInfo("test", "test"))
    }

    override fun appStoreRedirect(id: String, userId: String): Response {
        return experienceAppService.appStoreRedirect(id, userId)
    }
}
