package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceDownloadResource
import com.tencent.devops.experience.pojo.download.CheckVersionParam
import com.tencent.devops.experience.pojo.download.CheckVersionVO
import com.tencent.devops.experience.service.ExperienceDownloadService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppExperienceDownloadResourceImpl @Autowired constructor(
    private val experienceDownloadService: ExperienceDownloadService
) : AppExperienceDownloadResource {
    override fun checkVersion(
        userId: String,
        platform: Int,
        params: List<CheckVersionParam>
    ): Result<List<CheckVersionVO>> {
        return Result(experienceDownloadService.checkVersion(userId, platform, params))
    }
}
