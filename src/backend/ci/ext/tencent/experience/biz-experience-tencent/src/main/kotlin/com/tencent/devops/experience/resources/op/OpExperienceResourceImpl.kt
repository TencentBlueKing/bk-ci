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

package com.tencent.devops.experience.resources.op

import ExperiencePublicExternalAdd
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.I18NConstant.BK_CREATED_SUCCESSFULLY
import com.tencent.devops.common.api.constant.I18NConstant.BK_DELETE_SEARCH_RECOMMENDATION_SUCCEEDED
import com.tencent.devops.common.api.constant.I18NConstant.BK_NEW_SEARCH_RECOMMENDATION_SUCCEEDED
import com.tencent.devops.common.api.constant.I18NConstant.BK_UPDATED_SUCCESSFULLY
import com.tencent.devops.common.api.constant.I18NConstant.BK_UPDATED_SUCCESSFULLY_AND_SET
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.experience.api.op.OpExperienceResource
import com.tencent.devops.experience.constant.ExperienceMessageCode.RECORD_COULD_NOT_FOUND
import com.tencent.devops.experience.constant.ExperiencePublicType
import com.tencent.devops.experience.dao.ExperienceExtendBannerDao
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceGroupInnerDao
import com.tencent.devops.experience.dao.ExperienceInnerDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.ExperienceSearchRecommendDao
import com.tencent.devops.experience.pojo.ExperienceExtendBanner
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import java.text.MessageFormat
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@RestResource
class OpExperienceResourceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val experienceGroupDao: ExperienceGroupDao,
    private val experienceInnerDao: ExperienceInnerDao,
    private val experienceGroupInnerDao: ExperienceGroupInnerDao,
    private val experiencePublicDao: ExperiencePublicDao,
    private val experienceSearchRecommendDao: ExperienceSearchRecommendDao,
    private val redisOperation: RedisOperation,
    private val experienceExtendBannerDao: ExperienceExtendBannerDao
) : OpExperienceResource {
    override fun switchNecessary(userId: String, id: Long): Result<String> {
        val record = experiencePublicDao.getById(dslContext, id) ?: throw NotFoundException(
            MessageUtil.getMessageByLocale(
                messageCode = RECORD_COULD_NOT_FOUND,
                language = I18nUtil.getLanguage(userId)
        ))

        experiencePublicDao.updateById(
            dslContext = dslContext,
            id = id,
            necessary = record.necessary.not()
        )

        return Result(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_UPDATED_SUCCESSFULLY_AND_SET,
                    language = I18nUtil.getLanguage(userId)
                ) + "${record.necessary.not()}"
            )
    }

    override fun setNecessaryIndex(userId: String, id: Long, necessaryIndex: Int): Result<String> {
        experiencePublicDao.getById(dslContext, id) ?: throw NotFoundException(
            MessageUtil.getMessageByLocale(
                messageCode = RECORD_COULD_NOT_FOUND,
                language = I18nUtil.getLanguage(userId)
            )
        )

        experiencePublicDao.updateById(
            dslContext = dslContext,
            id = id,
            necessaryIndex = necessaryIndex
        )

        return Result(
            MessageUtil.getMessageByLocale(
            messageCode = BK_UPDATED_SUCCESSFULLY,
            language = I18nUtil.getLanguage(userId)
        ))
    }

    override fun setBannerUrl(userId: String, id: Long, bannerUrl: String): Result<String> {
        experiencePublicDao.getById(dslContext, id) ?: throw NotFoundException(
            MessageUtil.getMessageByLocale(
                messageCode = RECORD_COULD_NOT_FOUND,
                language = I18nUtil.getLanguage(userId)
            )
        )

        experiencePublicDao.updateById(
            dslContext = dslContext,
            id = id,
            bannerUrl = bannerUrl
        )

        return Result(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_UPDATED_SUCCESSFULLY_AND_SET,
                    language = I18nUtil.getLanguage(userId)
                ) + "$bannerUrl"
        )
    }

    override fun setBannerIndex(userId: String, id: Long, bannerIndex: Int): Result<String> {
        experiencePublicDao.getById(dslContext, id) ?: throw NotFoundException(
            MessageUtil.getMessageByLocale(
                messageCode = RECORD_COULD_NOT_FOUND,
                language = I18nUtil.getLanguage(userId)
            )
        )

        experiencePublicDao.updateById(
            dslContext = dslContext,
            id = id,
            bannerIndex = bannerIndex
        )

        return Result(
            MessageUtil.getMessageByLocale(
                messageCode = BK_UPDATED_SUCCESSFULLY,
                language = I18nUtil.getLanguage(userId)
            )
        )
    }

    override fun switchOnline(userId: String, id: Long): Result<String> {
        val record = experiencePublicDao.getById(dslContext, id) ?: throw NotFoundException(
            MessageUtil.getMessageByLocale(
                messageCode = RECORD_COULD_NOT_FOUND,
                language = I18nUtil.getLanguage(userId)
            )
        )

        experiencePublicDao.updateById(
            dslContext = dslContext,
            id = id,
            online = record.online.not()
        )

        return Result(
            MessageUtil.getMessageByLocale(
                messageCode = BK_UPDATED_SUCCESSFULLY_AND_SET,
                language = I18nUtil.getLanguage(userId)
            ) + "${record.online.not()}"
        )
    }

    override fun addRecommend(userId: String, content: String, platform: PlatformEnum): Result<String> {
        experienceSearchRecommendDao.add(dslContext, content, platform.name)
        return Result(
            MessageUtil.getMessageByLocale(
                messageCode = BK_NEW_SEARCH_RECOMMENDATION_SUCCEEDED,
                language = I18nUtil.getLanguage(userId)
            )
        )
    }

    override fun removeRecommend(userId: String, id: Long): Result<String> {
        experienceSearchRecommendDao.remove(dslContext, id)
        return Result(
            MessageUtil.getMessageByLocale(
                messageCode = BK_DELETE_SEARCH_RECOMMENDATION_SUCCEEDED,
                language = I18nUtil.getLanguage(userId)
            )
        )
    }

    override fun addExternal(userId: String, externalAdd: ExperiencePublicExternalAdd): Result<String> {
        experiencePublicDao.create(
            dslContext = dslContext,
            recordId = 0,
            projectId = "",
            experienceName = externalAdd.experienceName,
            category = externalAdd.category,
            platform = externalAdd.platform.name,
            bundleIdentifier = RandomStringUtils.randomAlphanumeric(10),
            endDate = LocalDateTime.of(2100, 1, 1, 1, 1),
            size = 0,
            logoUrl = externalAdd.logoUrl,
            type = ExperiencePublicType.FROM_EXTERNAL_URL.id,
            externalUrl = externalAdd.externalLink,
            scheme = "",
            version = ""
        )

        return Result(
            MessageUtil.getMessageByLocale(
                messageCode = BK_CREATED_SUCCESSFULLY,
                language = I18nUtil.getLanguage(userId)
            )
        )
    }

    override fun addExtendBanner(userId: String, experienceExtendBanner: ExperienceExtendBanner): Result<Int> {
        return Result(
            experienceExtendBannerDao.create(
                dslContext = dslContext,
                experienceExtendBanner = experienceExtendBanner
            )
        )
    }

    override fun updateExtendBanner(
        userId: String,
        bannerId: Long,
        experienceExtendBanner: ExperienceExtendBanner
    ): Result<Int> {
        return Result(
            experienceExtendBannerDao.update(
                dslContext = dslContext,
                bannerId = bannerId,
                experienceExtendBanner = experienceExtendBanner
            )
        )
    }
}
