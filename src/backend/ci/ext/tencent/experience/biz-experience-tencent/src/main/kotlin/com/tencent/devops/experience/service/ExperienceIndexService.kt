package com.tencent.devops.experience.service

import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.experience.dao.ExperienceBannerDao
import com.tencent.devops.experience.dao.ExperienceNecessaryDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.pojo.index.IndexAppInfoVO
import com.tencent.devops.experience.pojo.index.IndexBannerVO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExperienceIndexService @Autowired constructor(
    val experienceBannerDao: ExperienceBannerDao,
    val experiencePublicDao: ExperiencePublicDao,
    val experienceNecessaryDao: ExperienceNecessaryDao,
    val dslContext: DSLContext
) {
    fun banners(userId: String, page: Int, pageSize: Int, platform: Int): Result<Pagination<IndexBannerVO>> {
        val offset = (page - 1) * pageSize
        val platformStr = PlatformEnum.of(platform)?.name

        val banners = experienceBannerDao.listAvailable(
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
            experienceBannerDao.count(
                dslContext = dslContext,
                platform = platformStr
            ) >= (offset + pageSize)
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
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
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

    fun necessary(userId: String, page: Int, pageSize: Int, platform: Int): Result<Pagination<IndexAppInfoVO>> {
        val offset = (page - 1) * pageSize
        val platformStr = PlatformEnum.of(platform)?.name

        val records = experienceNecessaryDao.list(
            dslContext = dslContext,
            offset = offset,
            limit = pageSize,
            platform = platformStr
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
            )
        }.toList()

        val hasNext = if (records.size < pageSize) {
            false
        } else {
            experienceNecessaryDao.count(
                dslContext = dslContext,
                platform = platformStr
            ) >= (offset + pageSize)
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
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
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
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
            )
        }.toList()

        val hasNext = if (records.size < pageSize) {
            false
        } else {
            experiencePublicDao.count(
                dslContext = dslContext,
                platform = platformStr,
                category = categoryId
            ) >= (offset + pageSize)
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
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
            )
        }.toList()

        val hasNext = if (records.size < pageSize) {
            false
        } else {
            experiencePublicDao.count(
                dslContext = dslContext,
                platform = platformStr,
                category = categoryId
            ) >= (offset + pageSize)
        }

        return Result(Pagination(hasNext, records))
    }
}