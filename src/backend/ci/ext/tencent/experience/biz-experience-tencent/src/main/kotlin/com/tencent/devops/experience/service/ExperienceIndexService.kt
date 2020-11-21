package com.tencent.devops.experience.service

import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.pojo.index.IndexAppInfoVO
import com.tencent.devops.experience.pojo.index.IndexBannerVO
import com.tencent.devops.experience.util.UrlUtil
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExperienceIndexService @Autowired constructor(
    val experiencePublicDao: ExperiencePublicDao,
    val dslContext: DSLContext
) {
    fun banners(userId: String, page: Int, pageSize: Int, platform: Int): Result<Pagination<IndexBannerVO>> {
        val offset = (page - 1) * pageSize
        val platformStr = PlatformEnum.of(platform)?.name

        val banners = experiencePublicDao.listWithBanner(
            dslContext = dslContext,
            offset = offset,
            limit = pageSize,
            platform = platformStr
        ).map {
            IndexBannerVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                bannerUrl = it.bannerUrl
            )
        }.toList()

        val hasNext = if (banners.size < pageSize) {
            false
        } else {
            experiencePublicDao.count(
                dslContext = dslContext,
                platform = platformStr,
                withBanner = true
            ) > (offset + pageSize)
        }

        return Result(Pagination(hasNext, banners))
    }

    fun hots(userId: String, page: Int, pageSize: Int, platform: Int): Result<Pagination<IndexAppInfoVO>> {
        val offset = (page - 1) * pageSize
        val platformStr = PlatformEnum.of(platform)?.name

        val records = experiencePublicDao.listHot(
            dslContext = dslContext,
            offset = offset,
            limit = pageSize,
            platform = platformStr
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestampmilli(),
                size = it.size,
                logoUrl = UrlUtil.transformLogoAddr(it.logoUrl),
                bundleIdentifier = it.bundleIdentifier
            )
        }.toList()

        val hasNext = if (records.size < pageSize) {
            false
        } else {
            experiencePublicDao.count(
                dslContext = dslContext,
                platform = platformStr
            ) > (offset + pageSize)
        }

        return Result(Pagination(hasNext, records))
    }

    fun necessary(userId: String, page: Int, pageSize: Int, platform: Int): Result<Pagination<IndexAppInfoVO>> {
        val offset = (page - 1) * pageSize
        val platformStr = PlatformEnum.of(platform)?.name

        val records = experiencePublicDao.listNecessary(
            dslContext = dslContext,
            offset = offset,
            limit = pageSize,
            platform = platformStr
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestampmilli(),
                size = it.size,
                logoUrl = UrlUtil.transformLogoAddr(it.logoUrl),
                bundleIdentifier = it.bundleIdentifier
            )
        }.toList()

        val hasNext = if (records.size < pageSize) {
            false
        } else {
            experiencePublicDao.count(
                dslContext = dslContext,
                platform = platformStr,
                necessary = true
            ) > (offset + pageSize)
        }

        return Result(Pagination(hasNext, records))
    }

    fun newest(userId: String, page: Int, pageSize: Int, platform: Int): Result<Pagination<IndexAppInfoVO>> {
        val offset = (page - 1) * pageSize
        val platformStr = PlatformEnum.of(platform)?.name

        val records = experiencePublicDao.listNew(
            dslContext = dslContext,
            offset = offset,
            limit = pageSize,
            platform = platformStr
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestampmilli(),
                size = it.size,
                logoUrl = UrlUtil.transformLogoAddr(it.logoUrl),
                bundleIdentifier = it.bundleIdentifier
            )
        }.toList()

        val hasNext = if (records.size < pageSize) {
            false
        } else {
            experiencePublicDao.count(
                dslContext = dslContext,
                platform = platformStr
            ) >= (offset + pageSize)
        }

        return Result(Pagination(hasNext, records))
    }

    fun hotCategory(
        userId: String,
        categoryId: Int,
        page: Int,
        pageSize: Int,
        platform: Int
    ): Result<Pagination<IndexAppInfoVO>> {
        val offset = (page - 1) * pageSize
        val platformStr = PlatformEnum.of(platform)?.name

        val records = experiencePublicDao.listHot(
            dslContext = dslContext,
            offset = offset,
            limit = pageSize,
            category = categoryId,
            platform = platformStr
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestampmilli(),
                size = it.size,
                logoUrl = UrlUtil.transformLogoAddr(it.logoUrl),
                bundleIdentifier = it.bundleIdentifier
            )
        }.toList()

        val hasNext = if (records.size < pageSize) {
            false
        } else {
            experiencePublicDao.count(
                dslContext = dslContext,
                platform = platformStr,
                category = categoryId
            ) > (offset + pageSize)
        }

        return Result(Pagination(hasNext, records))
    }

    fun newCategory(
        userId: String,
        categoryId: Int,
        page: Int,
        pageSize: Int,
        platform: Int
    ): Result<Pagination<IndexAppInfoVO>> {
        val offset = (page - 1) * pageSize
        val platformStr = PlatformEnum.of(platform)?.name

        val records = experiencePublicDao.listNew(
            dslContext = dslContext,
            offset = offset,
            limit = pageSize,
            category = categoryId,
            platform = platformStr
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestampmilli(),
                size = it.size,
                logoUrl = UrlUtil.transformLogoAddr(it.logoUrl),
                bundleIdentifier = it.bundleIdentifier
            )
        }.toList()

        val hasNext = if (records.size < pageSize) {
            false
        } else {
            experiencePublicDao.count(
                dslContext = dslContext,
                platform = platformStr,
                category = categoryId
            ) > (offset + pageSize)
        }

        return Result(Pagination(hasNext, records))
    }
}
