package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceIndexResource
import com.tencent.devops.experience.pojo.index.IndexAppInfoVO
import com.tencent.devops.experience.pojo.index.IndexBannerVO
import com.tencent.devops.experience.service.ExperienceIndexService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppExperienceIndexResourceImpl @Autowired constructor(
    val experienceIndexService: ExperienceIndexService
) : AppExperienceIndexResource {
    override fun banners(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexBannerVO>> {
        return experienceIndexService.banners(userId, page, pageSize)
    }

    override fun hots(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        return experienceIndexService.hots(userId, page, pageSize)
    }

    override fun necessary(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        return experienceIndexService.necessary(userId, page, pageSize)
    }

    override fun newest(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        return experienceIndexService.newest(userId, page, pageSize)
    }

    override fun hotCategory(
        userId: String,
        categoryId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        return experienceIndexService.hotCategory(userId, categoryId, page, pageSize)
    }

    override fun newCategory(
        userId: String,
        categoryId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        return experienceIndexService.newCategory(userId, categoryId, page, pageSize)
    }
}