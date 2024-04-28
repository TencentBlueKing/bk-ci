package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceSecurityResource
import com.tencent.devops.experience.service.ExperienceSecurityService

@RestResource
class AppExperienceSecurityResourceImpl constructor(
    val experienceSecurityService: ExperienceSecurityService
) : AppExperienceSecurityResource {
    override fun getClearSign(
        openId: String,
        nickName: String,
        avatar: String
    ): Result<String> {
        return Result(
            experienceSecurityService.getClearSign(
                openId = openId,
                nickName = nickName,
                avatar = avatar
            )
        )
    }
}
