package com.tencent.devops.experience.resources.open

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.open.OpenExperienceResource
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.pojo.ExperienceJumpInfo
import com.tencent.devops.experience.pojo.outer.OuterLoginParam
import com.tencent.devops.experience.pojo.outer.OuterProfileVO
import com.tencent.devops.experience.service.ExperienceAppService
import com.tencent.devops.experience.service.ExperienceDownloadService
import com.tencent.devops.experience.service.ExperienceOuterService
import com.tencent.devops.experience.service.ExperienceService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class OpenExperienceResourceImpl @Autowired constructor(
    private val experienceOuterService: ExperienceOuterService,
    private val experienceAppService: ExperienceAppService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val experiencePublicDao: ExperiencePublicDao,
    private val dslContext: DSLContext
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

    override fun appStoreRedirect(id: String, userId: String): Response {
        return experienceAppService.appStoreRedirect(id, userId)
    }
}
