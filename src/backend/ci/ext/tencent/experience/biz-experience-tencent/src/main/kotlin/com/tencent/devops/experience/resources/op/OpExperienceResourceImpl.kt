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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.op.OpExperienceResource
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceGroupInnerDao
import com.tencent.devops.experience.dao.ExperienceInnerDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.ExperienceSearchRecommendDao
import com.tencent.devops.model.experience.tables.TExperience
import com.tencent.devops.model.experience.tables.TGroup
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
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
    private val redisOperation: RedisOperation
) : OpExperienceResource {
    override fun transform(userId: String): Result<String> {
        val recordIdFrom = redisOperation.get("experience:transform:record:from")

        // 兼容老数据
        with(TExperience.T_EXPERIENCE) {
            dslContext.update(this)
                .set(EXPERIENCE_GROUPS, "[]")
                .where(EXPERIENCE_GROUPS.eq(""))
                .execute()
        }

        with(TExperience.T_EXPERIENCE) {
            dslContext.update(this)
                .set(INNER_USERS, "[]")
                .where(INNER_USERS.eq(""))
                .execute()
        }

        // 迁移体验组
        with(TExperience.T_EXPERIENCE) {
            dslContext.selectFrom(this)
                .where(EXPERIENCE_GROUPS.ne(""))
                .let { if (null == recordIdFrom) it else it.and(ID.gt(recordIdFrom.toLong())) }
                .fetch()
        }.forEach { record ->
            objectMapper.readValue<Set<String>>(record.experienceGroups).forEach {
                experienceGroupDao.create(
                    dslContext = dslContext,
                    recordId = record.id,
                    groupId = HashUtil.decodeIdToLong(it)
                )
            }
        }

        // 迁移内部体验人员
        with(TExperience.T_EXPERIENCE) {
            dslContext.selectFrom(this)
                .where(INNER_USERS.ne(""))
                .let { if (null == recordIdFrom) it else it.and(ID.gt(recordIdFrom.toLong())) }
                .fetch()
        }.forEach { record ->
            objectMapper.readValue<Set<String>>(record.innerUsers).forEach {
                experienceInnerDao.create(
                    dslContext = dslContext,
                    recordId = record.id,
                    userId = it
                )
            }
        }

        // 迁移组内的内部体验人员
        with(TGroup.T_GROUP) {
            dslContext.selectFrom(this)
                .where(INNER_USERS.ne(""))
                .fetch()
        }.forEach { group ->
            objectMapper.readValue<Set<String>>(group.innerUsers).forEach {
                experienceGroupInnerDao.create(
                    dslContext = dslContext,
                    groupId = group.id,
                    userId = it
                )
            }
        }

        return Result("同步成功")
    }

    override fun switchNecessary(userId: String, id: Long): Result<String> {
        val record = experiencePublicDao.getById(dslContext, id) ?: throw NotFoundException("找不到该记录")

        experiencePublicDao.updateById(
            dslContext = dslContext,
            id = id,
            necessary = record.necessary.not()
        )

        return Result("更新成功,已置为${record.necessary.not()}")
    }

    override fun setBannerUrl(userId: String, id: Long, bannerUrl: String): Result<String> {
        experiencePublicDao.getById(dslContext, id) ?: throw NotFoundException("找不到该记录")

        experiencePublicDao.updateById(
            dslContext = dslContext,
            id = id,
            bannerUrl = bannerUrl
        )

        return Result("更新成功,已置为$bannerUrl")
    }

    override fun switchOnline(userId: String, id: Long): Result<String> {
        val record = experiencePublicDao.getById(dslContext, id) ?: throw NotFoundException("找不到该记录")

        experiencePublicDao.updateById(
            dslContext = dslContext,
            id = id,
            online = record.online.not()
        )

        return Result("更新成功,已置为${record.online.not()}")
    }

    override fun addRecommend(userId: String, content: String, platform: PlatformEnum): Result<String> {
        experienceSearchRecommendDao.add(dslContext, content, platform.name)
        return Result("新增搜索推荐成功")
    }

    override fun removeRecommend(userId: String, id: Long): Result<String> {
        experienceSearchRecommendDao.remove(dslContext, id)
        return Result("删除搜索推荐成功")
    }
}
