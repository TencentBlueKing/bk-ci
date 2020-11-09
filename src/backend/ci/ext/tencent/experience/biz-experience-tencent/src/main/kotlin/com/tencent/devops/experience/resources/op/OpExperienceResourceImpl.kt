package com.tencent.devops.experience.resources.op

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.op.OpExperienceResource
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceGroupInnerDao
import com.tencent.devops.experience.dao.ExperienceInnerDao
import com.tencent.devops.model.experience.tables.TExperience
import com.tencent.devops.model.experience.tables.TGroup
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpExperienceResourceImpl @Autowired constructor(
    val dslContext: DSLContext,
    val objectMapper: ObjectMapper,
    val experienceGroupDao: ExperienceGroupDao,
    val experienceInnerDao: ExperienceInnerDao,
    val experienceGroupInnerDao: ExperienceGroupInnerDao
) : OpExperienceResource {
    override fun transform(userId: String) {
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
//            with(TExperience.T_EXPERIENCE) {
//                dslContext.update(this).set(EXPERIENCE_GROUPS, "").execute()
//            }
        }

        // 迁移内部体验人员
        with(TExperience.T_EXPERIENCE) {
            dslContext.selectFrom(this).where(INNER_USERS.ne("")).fetch()
        }.forEach { record ->
            record.innerUsers.split(";", ",").forEach {
                experienceInnerDao.create(
                    dslContext = dslContext,
                    recordId = record.id,
                    username = it
                )
            }
//            with(TExperience.T_EXPERIENCE) {
//                dslContext.update(this).set(INNER_USERS, "").execute()
//            }
        }

        // 迁移组内的内部体验人员
        with(TGroup.T_GROUP) {
            dslContext.selectFrom(this).where(INNER_USERS.ne("")).fetch()
        }.forEach { group ->
            objectMapper.readValue<Set<String>>(group.innerUsers).forEach {
                experienceGroupInnerDao.create(
                    dslContext = dslContext,
                    groupId = group.id,
                    username = it
                )
            }

//            with(TGroup.T_GROUP) {
//                dslContext.update(this).set(INNER_USERS, "").set(INNER_USERS_COUNT, 0).execute()
//            }
        }
    }
}