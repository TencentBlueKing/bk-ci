package com.tencent.devops.experience.resources.op

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.op.OpExperienceResource
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceGroupInnerDao
import com.tencent.devops.experience.dao.ExperienceInnerDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
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
    private val experiencePublicDao: ExperiencePublicDao
) : OpExperienceResource {
    override fun transform(userId: String): Result<String> {
        // 迁移体验组
        with(TExperience.T_EXPERIENCE) {
            dslContext.selectFrom(this).where(EXPERIENCE_GROUPS.ne("")).fetch()
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
            dslContext.selectFrom(this).where(INNER_USERS.ne("")).fetch()
        }.forEach { record ->
            record.innerUsers.split(";", ",").forEach {
                experienceInnerDao.create(
                    dslContext = dslContext,
                    recordId = record.id,
                    userId = it
                )
            }
        }

        // 迁移组内的内部体验人员
        with(TGroup.T_GROUP) {
            dslContext.selectFrom(this).where(INNER_USERS.ne("")).fetch()
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
}
