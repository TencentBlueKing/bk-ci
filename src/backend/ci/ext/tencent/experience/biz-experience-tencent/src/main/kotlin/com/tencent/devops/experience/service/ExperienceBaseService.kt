package com.tencent.devops.experience.service

import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.constant.GroupIdTypeEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceGroupInnerDao
import com.tencent.devops.experience.dao.ExperienceInnerDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// 服务共用部分在这里
@Service
class ExperienceBaseService @Autowired constructor(
    private val experienceGroupDao: ExperienceGroupDao,
    private val experienceGroupInnerDao: ExperienceGroupInnerDao,
    private val experienceInnerDao: ExperienceInnerDao,
    private val experienceDao: ExperienceDao,
    private val dslContext: DSLContext
) {
    fun getRecordIdsByUserId(
        userId: String,
        groupIdType: GroupIdTypeEnum
    ): MutableSet<Long> {
        val recordIds = mutableSetOf<Long>()
        // 把有自己的组的experience拿出来 && 把公开的experience拿出来
        val groupIds = mutableSetOf<Long>()
        if (groupIdType == GroupIdTypeEnum.JUST_PRIVATE || groupIdType == GroupIdTypeEnum.ALL) {
            groupIds.addAll(experienceGroupInnerDao.listGroupIdsByUserId(dslContext, userId).map { it.value1() }
                .toMutableSet())
        }
        if (groupIdType == GroupIdTypeEnum.JUST_PUBLIC || groupIdType == GroupIdTypeEnum.ALL) {
            groupIds.add(ExperienceConstant.PUBLIC_GROUP)
        }
        recordIds.addAll(experienceGroupDao.listRecordIdByGroupIds(dslContext, groupIds).map { it.value1() }.toSet())
        // 把有自己的experience拿出来
        recordIds.addAll(experienceInnerDao.listRecordIdsByUserId(dslContext, userId).map { it.value1() }.toSet())
        return recordIds
    }

    fun userCanExperience(userId: String, experienceId: Long): Boolean {
        val experienceRecord = experienceDao.get(dslContext, experienceId)
        val groupIdToUserIdsMap = getGroupIdToUserIdsMap(experienceId)
        return groupIdToUserIdsMap.values.asSequence().flatMap { it.asSequence() }.toSet().contains(userId) ||
            userId == experienceRecord.creator
    }

    fun getGroupIdToUserIdsMap(experienceId: Long): MutableMap<Long, MutableSet<String>> {
        val groupIds = experienceGroupDao.listGroupIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()
        return getGroupIdToUserIds(groupIds)
    }

    fun getGroupIdToUserIds(groupIds: Set<Long>): MutableMap<Long, MutableSet<String>> {
        val groupIdToUserIds = mutableMapOf<Long, MutableSet<String>>()
        experienceGroupInnerDao.listByGroupIds(dslContext, groupIds).forEach {
            var userIds = groupIdToUserIds[it.groupId]
            if (null == userIds) {
                userIds = mutableSetOf()
            }
            userIds.add(it.userId)
            groupIdToUserIds[it.groupId] = userIds
        }
        if (groupIds.contains(ExperienceConstant.PUBLIC_GROUP)) {
            groupIdToUserIds[0] = ExperienceConstant.PUBLIC_INNER_USERS
        }
        return groupIdToUserIds
    }
}
