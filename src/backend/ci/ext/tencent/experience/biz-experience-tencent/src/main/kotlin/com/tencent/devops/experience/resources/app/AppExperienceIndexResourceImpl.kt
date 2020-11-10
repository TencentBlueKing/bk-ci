package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.pojo.Pagination
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
        platform: Int?,
        page: Int?,
        pageSize: Int?
    ): Result<Pagination<IndexBannerVO>> {
        return experienceIndexService.banners(userId, page, pageSize, platform)
    }

    override fun hots(
        userId: String,
        platform: Int?,
        page: Int?,
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.hots(userId, page, pageSize, platform)
    }

    override fun necessary(
        userId: String,
        platform: Int?,
        page: Int?,
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.necessary(userId, page, pageSize, platform)
    }

    override fun newest(
        userId: String,
        platform: Int?,
        page: Int?,
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.newest(userId, page, pageSize, platform)
    }

    override fun hotCategory(
        userId: String,
        platform: Int?,
        categoryId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.hotCategory(userId, categoryId, page, pageSize, platform)
    }

    override fun newCategory(
        userId: String,
        platform: Int?,
        categoryId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<Pagination<IndexAppInfoVO>> {
        return experienceIndexService.newCategory(userId, categoryId, page, pageSize, platform)
    }
}