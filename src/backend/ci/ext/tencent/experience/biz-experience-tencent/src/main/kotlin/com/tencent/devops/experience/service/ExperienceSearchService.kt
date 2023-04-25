/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.experience.service

import com.tencent.devops.artifactory.util.UrlUtil
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.experience.constant.ExperienceConstant.ORGANIZATION_OUTER
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.ExperienceSearchRecommendDao
import com.tencent.devops.experience.pojo.search.SearchAppInfoVO
import com.tencent.devops.experience.pojo.search.SearchRecommendVO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Service
class ExperienceSearchService @Autowired constructor(
    val experienceBaseService: ExperienceBaseService,
    val experiencePublicDao: ExperiencePublicDao,
    val experienceDao: ExperienceDao,
    val experienceSearchRecommendDao: ExperienceSearchRecommendDao,
    val dslContext: DSLContext
) {
    @Value("\${minigame.projectid:#{null}}")
    private var minigameProjectId: String? = null
    fun search(
        userId: String,
        platform: Int?,
        experienceName: String,
        experiencePublic: Boolean,
        organization: String?,
        minigame: Boolean?
    ): Result<List<SearchAppInfoVO>> {
        val record = if (experiencePublic) {
            val projectId = if (minigame != null && minigame == true) {
                if (minigameProjectId == null) {
                    throw NotFoundException("MiniGame projectId not found")
                }
                minigameProjectId
            } else {
                null
            }
            publicSearch(
                userId = userId,
                experienceName = experienceName,
                platform = platform,
                projectId = projectId
            )
        } else {
            privateSearch(userId, platform, experienceName, organization)
        }
        return Result(record)
    }

    private fun privateSearch(
        userId: String,
        platform: Int?,
        experienceName: String,
        organization: String?
    ) = experienceBaseService.list(
        userId = userId,
        offset = 0,
        limit = 100,
        groupByBundleId = false,
        platform = platform,
        experienceName = experienceName,
        isOuter = organization == ORGANIZATION_OUTER
    ).records.map {
        SearchAppInfoVO(
            experienceHashId = it.experienceHashId,
            experienceName = it.experienceName,
            createTime = it.createDate,
            size = it.size,
            logoUrl = it.logoUrl,
            expired = it.expired,
            lastDownloadHashId = it.lastDownloadHashId,
            bundleIdentifier = it.bundleIdentifier,
            version = it.version,
            versionTitle = it.versionTitle,
            appScheme = it.appScheme
        )
    }

    private fun publicSearch(
        userId: String,
        experienceName: String,
        platform: Int?,
        projectId: String?
    ): List<SearchAppInfoVO> {
        val lastDownloadMap = experienceBaseService.getLastDownloadMap(userId)
        val now = LocalDateTime.now()

        return experiencePublicDao.listLikeExperienceName(
            dslContext = dslContext,
            experienceName = experienceName.trim(),
            platform = PlatformEnum.of(platform)?.name,
            projectId = projectId
        ).map {
            SearchAppInfoVO(
                experienceHashId = HashUtil.encodeLongId(it.recordId),
                experienceName = it.experienceName,
                createTime = it.updateTime.timestampmilli(),
                size = it.size,
                logoUrl = UrlUtil.toOuterPhotoAddr(it.logoUrl),
                expired = now.isAfter(it.endDate),
                lastDownloadHashId = lastDownloadMap[it.projectId + it.bundleIdentifier + it.platform]
                    ?.let { l -> HashUtil.encodeLongId(l) } ?: "",
                bundleIdentifier = it.bundleIdentifier,
                appScheme = it.scheme,
                type = it.type,
                externalUrl = it.externalLink,
                version = it.version,
                downloadTime = it.downloadTime
            )
        }.toList()
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
