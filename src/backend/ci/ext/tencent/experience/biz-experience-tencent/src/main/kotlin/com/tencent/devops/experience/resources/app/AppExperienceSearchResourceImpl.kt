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

    override fun search(userId: String, experienceName: String): Result<List<SearchAppInfoVO>> {
        return experienceSearchService.search(userId, experienceName)
    }

    override fun recommends(userId: String): Result<List<SearchRecommendVO>> {
        return experienceSearchService.recommends(userId)
    }
}