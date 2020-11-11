package com.tencent.devops.experience.service

import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.ExperienceSearchRecommendDao
import com.tencent.devops.experience.pojo.search.SearchAppInfoVO
import com.tencent.devops.experience.pojo.search.SearchRecommendVO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExperienceSearchService @Autowired constructor(
    val experiencePublicDao: ExperiencePublicDao,
    val experienceDao: ExperienceDao,
    val experienceSearchRecommendDao: ExperienceSearchRecommendDao,
    val dslContext: DSLContext
) {
    fun search(
        userId: String,
        platform: Int?,
        experienceName: String,
        experiencePublic: Boolean
    ): Result<List<SearchAppInfoVO>> {
        val record =
            if (experiencePublic) {
                experiencePublicDao.listLikeExperienceName(
                    dslContext = dslContext,
                    experienceName = experienceName,
                    platform = PlatformEnum.of(platform)?.name
                ).map {
                    SearchAppInfoVO(
                        experienceHashId = HashUtil.encodeLongId(it.recordId),
                        experienceName = it.experienceName,
                        createTime = it.updateTime.timestamp(),
                        size = it.size,
                        logoUrl = it.logoUrl
                    )
                }.toList()
            } else {
                experienceDao.listLikeExperienceName(
                    dslContext = dslContext,
                    experienceName = experienceName,
                    platform = PlatformEnum.of(platform)?.name
                ).map {
                    SearchAppInfoVO(
                        experienceHashId = HashUtil.encodeLongId(it.id),
                        experienceName = it.experienceName,
                        createTime = it.updateTime.timestamp(),
                        size = it.size,
                        logoUrl = it.logoUrl
                    )
                }.toList()
            }
        return Result(record)
    }

    fun recommends(userId: String, platform: Int?): Result<List<SearchRecommendVO>> {
        val record = experienceSearchRecommendDao.listContent(
            dslContext = dslContext,
            platform = PlatformEnum.of(platform)?.name
        )?.map {
            SearchRecommendVO(
                content = it.value1()
            )
        }?.toList() ?: emptyList()
        return Result(record)
    }
}