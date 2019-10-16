package com.tencent.devops.experience.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.GwExperienceResource
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.service.ExperienceDownloadService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class GwExperienceResourceImpl @Autowired constructor(private val experienceDownloadService: ExperienceDownloadService) : GwExperienceResource {
    override fun getDownloadUrl(token: String): Result<DownloadUrl> {
        if (token.isBlank()) {
            throw ParamBlankException("Invalid token")
        }
        return Result(experienceDownloadService.getDownloadUrl(token))
    }
}