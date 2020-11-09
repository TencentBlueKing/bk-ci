package com.tencent.devops.experience.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
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
    val experienceSearchRecommendDao: ExperienceSearchRecommendDao,
    val dslContext: DSLContext
) {
    fun search(userId: String, experienceName: String): Result<List<SearchAppInfoVO>> {
        val record =
            experiencePublicDao.listLikeExperienceName(dslContext, experienceName)
                .map {
                    SearchAppInfoVO(
                        experienceHashId = HashUtil.encodeLongId(it.recordId),
                        experienceName = it.experienceName,
                        createTime = it.updateTime.timestamp(),
                        size = it.size,
                        logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
                    )
                }.toList()
        return Result(record)
    }

    fun recommends(userId: String): Result<List<SearchRecommendVO>> {
        val record = experienceSearchRecommendDao.listContent(dslContext)
            ?.map {
                SearchRecommendVO(
                    content = it.value1()
                )
            }?.toList() ?: emptyList()
        return Result(record)
    }
}