package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceSearchResource
import com.tencent.devops.experience.pojo.search.SearchAppInfoVO
import com.tencent.devops.experience.pojo.search.SearchRecommendVO
import com.tencent.devops.experience.service.ExperienceSearchService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppExperienceSearchResourceImpl @Autowired constructor(
    val experienceSearchService: ExperienceSearchService
) : AppExperienceSearchResource {

    override fun search(
        userId: String,
        platform: Int?,
        experienceName: String,
        experiencePublic: Boolean
    ): Result<List<SearchAppInfoVO>> {
        return experienceSearchService.search(userId, platform, experienceName, experiencePublic)
    }

    override fun recommends(userId: String, platform: Int?): Result<List<SearchRecommendVO>> {
        return experienceSearchService.recommends(userId, platform)
    }
}