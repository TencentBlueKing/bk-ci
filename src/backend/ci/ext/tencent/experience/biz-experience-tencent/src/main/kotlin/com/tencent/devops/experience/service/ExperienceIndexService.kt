package com.tencent.devops.experience.service

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
    fun banners(userId: String, page: Int?, pageSize: Int?): Result<List<IndexBannerVO>> {
        val offset = (page ?: 1 - 1) * (pageSize ?: 10)
        val banners = experienceBannerDao.listAvailable(
            dslContext,
            offset,
            pageSize ?: 10
        ).map {
            IndexBannerVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                bannerUrl = it.bannerUrl
            )
        }.toList()

        return Result(banners)
    }

    fun hots(userId: String, page: Int?, pageSize: Int?): Result<List<IndexAppInfoVO>> {
        val offset = (page ?: 1 - 1) * (pageSize ?: 10)
        val records = experiencePublicDao.listHot(
            dslContext,
            offset,
            pageSize ?: 10
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
            )
        }.toList()

        return Result(records)
    }

    fun necessary(userId: String, page: Int?, pageSize: Int?): Result<List<IndexAppInfoVO>> {
        val offset = (page ?: 1 - 1) * (pageSize ?: 10)
        val record = experienceNecessaryDao.list(
            dslContext,
            offset,
            pageSize ?: 10
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
            )
        }.toList()

        return Result(record)
    }

    fun newest(userId: String, page: Int?, pageSize: Int?): Result<List<IndexAppInfoVO>> {
        val offset = (page ?: 1 - 1) * (pageSize ?: 10)
        val records = experiencePublicDao.listNew(
            dslContext,
            offset,
            pageSize ?: 10
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
            )
        }.toList()

        return Result(records)
    }

    fun hotCategory(
        userId: String,
        categoryId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        val offset = (page ?: 1 - 1) * (pageSize ?: 10)
        val records = experiencePublicDao.listHot(
            dslContext,
            offset,
            pageSize ?: 10,
            categoryId
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
            )
        }.toList()

        return Result(records)
    }

    fun newCategory(
        userId: String,
        categoryId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<List<IndexAppInfoVO>> {
        val offset = (page ?: 1 - 1) * (pageSize ?: 10)
        val records = experiencePublicDao.listNew(
            dslContext,
            offset,
            pageSize ?: 10,
            categoryId
        ).map {
            IndexAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestamp(),
                size = it.size,
                iconUrl = it.iconUrl
            )
        }.toList()

        return Result(records)
    }
}