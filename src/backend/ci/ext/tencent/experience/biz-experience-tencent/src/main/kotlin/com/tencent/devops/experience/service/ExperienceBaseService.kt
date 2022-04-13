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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.util.UrlUtil
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.constant.GroupIdTypeEnum
import com.tencent.devops.experience.constant.ProductCategoryEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceDownloadDetailDao
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceGroupInnerDao
import com.tencent.devops.experience.dao.ExperienceGroupOuterDao
import com.tencent.devops.experience.dao.ExperienceInnerDao
import com.tencent.devops.experience.dao.ExperienceLastDownloadDao
import com.tencent.devops.experience.dao.ExperienceOuterDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.ExperiencePushSubscribeDao
import com.tencent.devops.experience.pojo.AppExperience
import com.tencent.devops.experience.pojo.enums.Source
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.model.experience.tables.records.TExperiencePublicRecord
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

// 服务共用部分在这里
@Service
class ExperienceBaseService @Autowired constructor(
    private val experienceGroupDao: ExperienceGroupDao,
    private val experienceGroupInnerDao: ExperienceGroupInnerDao,
    private val experienceGroupOuterDao: ExperienceGroupOuterDao,
    private val experienceInnerDao: ExperienceInnerDao,
    private val experienceOuterDao: ExperienceOuterDao,
    private val experienceDao: ExperienceDao,
    private val experiencePushSubscribeDao: ExperiencePushSubscribeDao,
    private val experiencePublicDao: ExperiencePublicDao,
    private val experienceLastDownloadDao: ExperienceLastDownloadDao,
    private val experienceDownloadDetailDao: ExperienceDownloadDetailDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceBaseService::class.java)
    }

    fun list(
        userId: String,
        offset: Int,
        limit: Int,
        groupByBundleId: Boolean,
        platform: Int? = null,
        experienceName: String? = null,
        isOuter: Boolean = false
    ): Pagination<AppExperience> {
        val expireTime = DateUtil.today()

        var recordIds = getRecordIdsByUserId(userId, GroupIdTypeEnum.JUST_PRIVATE, isOuter)

        if (groupByBundleId) {
            recordIds = experienceDao.listIdsGroupByBundleId(
                dslContext,
                recordIds,
                expireTime,
                true
            ).map { it.value1() }.toMutableSet()
        }

        val platformStr = PlatformEnum.of(platform)?.name

        val records = experienceDao.listByIds(
            dslContext = dslContext,
            ids = recordIds,
            platform = platformStr,
            expireTime = expireTime,
            online = true,
            offset = offset,
            limit = limit,
            experienceName = experienceName
        )

        // 同步图片
        syncIcon(records)

        val result = toAppExperiences(userId, records)

        val hasNext = if (result.size < limit) {
            false
        } else {
            experienceDao.countByIds(dslContext, recordIds, platformStr, expireTime, true) > offset + limit
        }

        return Pagination(hasNext, result)
    }

    fun toAppExperiences(
        userId: String,
        records: List<TExperienceRecord>
    ): List<AppExperience> {
        val lastDownloadMap = getLastDownloadMap(userId)
        val now = LocalDateTime.now()
        val redPointIds = redisOperation.getSetMembers(ExperienceConstant.redPointKey(userId)) ?: emptySet()
        val subscribeSet = experiencePushSubscribeDao.listByUserId(dslContext, userId, 1000)
            .map { "${it.projectId}-${it.bundleIdentifier}-${it.platform}" }.toSet()

        val result = records.map {
            AppExperience(
                experienceHashId = HashUtil.encodeLongId(it.id),
                platform = PlatformEnum.valueOf(it.platform),
                source = Source.valueOf(it.source),
                logoUrl = UrlUtil.toOuterPhotoAddr(it.logoUrl),
                name = it.projectId,
                version = it.version,
                bundleIdentifier = it.bundleIdentifier,
                experienceName = it.experienceName ?: it.projectId,
                versionTitle = it.versionTitle ?: it.name,
                categoryId = if (it.category == null || it.category < 0) ProductCategoryEnum.LIFE.id else it.category,
                productOwner = objectMapper.readValue(it.productOwner),
                size = it.size,
                createDate = it.updateTime.timestampmilli(),
                appScheme = it.scheme,
                lastDownloadHashId = lastDownloadMap[it.projectId + it.bundleIdentifier + it.platform]
                    ?.let { l -> HashUtil.encodeLongId(l) } ?: "",
                expired = now.isAfter(it.endDate),
                subscribe = subscribeSet.contains("${it.projectId}-${it.bundleIdentifier}-${it.platform}") ||
                        userId == it.creator,
                redPointEnabled = redPointIds.contains(it.id.toString())
            )
        }
        return result
    }

    /**
     * 列出用户能够访问的体验
     */
    fun getRecordIdsByUserId(
        userId: String,
        groupIdType: GroupIdTypeEnum,
        isOuter: Boolean = false
    ): MutableSet<Long> {
        val recordIds = mutableSetOf<Long>()
        val groupIds = mutableSetOf<Long>()
        if (groupIdType == GroupIdTypeEnum.JUST_PRIVATE || groupIdType == GroupIdTypeEnum.ALL) {
            if (isOuter) {
                groupIds.addAll(experienceGroupOuterDao.listGroupIdsByUserId(dslContext, userId).map { it.value1() }
                    .toMutableSet())
            } else {
                groupIds.addAll(experienceGroupInnerDao.listGroupIdsByUserId(dslContext, userId).map { it.value1() }
                    .toMutableSet())
            }
        }
        if (groupIdType == GroupIdTypeEnum.JUST_PUBLIC || groupIdType == GroupIdTypeEnum.ALL) {
            groupIds.add(ExperienceConstant.PUBLIC_GROUP)
        }
        recordIds.addAll(experienceGroupDao.listRecordIdByGroupIds(dslContext, groupIds).map { it.value1() }.toSet())
        if (isOuter) {
            recordIds.addAll(experienceOuterDao.listRecordIdsByOuter(dslContext, userId).map { it.value1() }.toSet())
        } else {
            recordIds.addAll(experienceInnerDao.listRecordIdsByUserId(dslContext, userId).map { it.value1() }.toSet())
            recordIds.addAll(experienceDao.listIdsByCreator(dslContext, userId))
        }
        return recordIds
    }

    /**
     * 判断用户是否能体验
     */
    fun userCanExperience(userId: String, experienceId: Long, isOuter: Boolean = false): Boolean {
        val isPublic = lazy { isPublic(experienceId, isOuter) }
        val isInPrivate = lazy { isInPrivate(experienceId, userId, isOuter) }

        return isPublic.value || isInPrivate.value
    }

    fun isPublic(experienceId: Long, isOuter: Boolean) = !isOuter &&
            (experiencePublicDao.countByRecordId(dslContext, experienceId, true, LocalDateTime.now())) > 0

    fun isPrivate(experience: TExperienceRecord, isOuter: Boolean = false, userId: String): Boolean {
        if (experience.creator == userId) {
            return true
        }
        return experienceGroupDao.listGroupIdsByRecordId(dslContext, experience.id)
            .filterNot { it.value1() == ExperienceConstant.PUBLIC_GROUP }.isNotEmpty() ||
                (if (isOuter) experienceOuterDao.countByRecordId(dslContext, experience.id)
                else experienceInnerDao.countByRecordId(dslContext, experience.id)) > 0
    }

    fun isInPrivate(experienceId: Long, userId: String, isOuter: Boolean = false): Boolean {
        val inGroup = lazy {
            val groupIds = getGroupIdsByRecordId(experienceId)
            if (isOuter) {
                getGroupIdToOuters(groupIds)
            } else {
                getGroupIdToInnerUserIds(groupIds)
            }.values.asSequence().flatMap { it.asSequence() }.toSet()
                .contains(userId)
        }
        val isInnerUser = lazy {
            if (isOuter) {
                experienceOuterDao.listUserIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()
                    .contains(userId)
            } else {
                experienceInnerDao.listUserIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()
                    .contains(userId)
            }
        }
        val isCreator = lazy { experienceDao.get(dslContext, experienceId).creator == userId }

        return inGroup.value || isInnerUser.value || isCreator.value
    }

    /**
     *  判断是否订阅
     */
    fun isSubscribe(
        experienceId: Long,
        userId: String,
        platform: String,
        bundleIdentifier: String,
        projectId: String
    ): Boolean {
        logger.info("userId:$userId,platform:$platform,bundleIdentifier:$bundleIdentifier,projectId:$projectId")
        val subscriptionRecord = lazy {
            experiencePushSubscribeDao.getSubscription(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                bundle = bundleIdentifier,
                platform = platform
            ) != null
        }
        val isExperienceGroups = isExperienceGroups(experienceId, userId)
        return subscriptionRecord.value || isExperienceGroups
    }

    /**
     * 判断是否为体验组成员
     */
    fun isExperienceGroups(
        experienceId: Long,
        userId: String
    ): Boolean {
        val groupIds = getGroupIdsByRecordId(experienceId)
        val isOuterGroup = lazy {
            getGroupIdToOuters(groupIds).values.asSequence().flatMap { it.asSequence() }.toSet()
                .contains(userId)
        }
        val isInnerGroup = lazy {
            getGroupIdToInnerUserIds(groupIds).values.asSequence().flatMap { it.asSequence() }.toSet()
                .contains(userId)
        }
        val isInnerUser = lazy {
            experienceInnerDao.listUserIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()
                .contains(userId)
        }
        val isOuterUser = lazy {
            experienceOuterDao.listUserIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()
                .contains(userId)
        }
        val isCreator = lazy { experienceDao.get(dslContext, experienceId).creator == userId }

        logger.info(
            "isOuterGroup:${isOuterGroup.value}, " +
                    "isInnerGroup:${isInnerGroup.value}, isInnerUser:${isInnerUser.value}, " +
                    "isOuterUser:${isOuterUser.value} ,isCreator:${isCreator.value}"
        )
        return isOuterGroup.value || isInnerGroup.value ||
                isInnerUser.value || isOuterUser.value || isCreator.value
    }

    /**
     * 判断是否为公开体验
     */
    fun isPublicExperience(
        experienceId: Long
    ): Boolean {
        return experiencePublicDao.getByRecordId(
            dslContext = dslContext,
            recordId = experienceId
        ) != null
    }

    /**
     * 判断是否为首次下载
     */
    fun isFirstDownload(
        platform: String,
        bundleIdentifier: String,
        projectId: String,
        userId: String
    ): Boolean {
        return experienceDownloadDetailDao.countDownloadHistory(
            dslContext = dslContext,
            projectId = projectId,
            bundleIdentifier = bundleIdentifier,
            platform = platform,
            userId = userId
        ) == 0
    }

    /**
     * 根据体验获取组号列表
     */
    fun getGroupIdsByRecordId(experienceId: Long): Set<Long> {
        return experienceGroupDao.listGroupIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()
    }

    /**
     * 内部用户,根据组号获取<组号,用户列表>
     */
    fun getGroupIdToInnerUserIds(groupIds: Set<Long>): MutableMap<Long, MutableSet<String>> {
        val groupIdToUserIds = mutableMapOf<Long, MutableSet<String>>()

        if (groupIds.contains(ExperienceConstant.PUBLIC_GROUP)) {
            groupIdToUserIds[ExperienceConstant.PUBLIC_GROUP] = ExperienceConstant.PUBLIC_INNER_USERS
        }

        experienceGroupInnerDao.listByGroupIds(dslContext, groupIds).forEach {
            var userIds = groupIdToUserIds[it.groupId]
            if (null == userIds) {
                userIds = mutableSetOf()
                groupIdToUserIds[it.groupId] = userIds
            }
            userIds.add(it.userId)
        }
        return groupIdToUserIds
    }

    /**
     * 获取内部用户列表
     */
    fun getInnerReceivers(
        dslContext: DSLContext,
        experienceId: Long,
        userId: String
    ): MutableSet<String> {
        val innerReceivers = mutableSetOf<String>()
        val extraUsers =
            experienceInnerDao.listUserIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()
        val groupIdToUserIdsMap = getGroupIdToInnerUserIds(
            getGroupIdsByRecordId(experienceId)
        )
        innerReceivers.addAll(extraUsers)
        innerReceivers.addAll(groupIdToUserIdsMap.values.flatMap { it.asIterable() }.toSet())
        innerReceivers.add(userId)
        return innerReceivers
    }

    /**
     * 外部用户,根据组号获取<组号,用户列表>
     */
    fun getGroupIdToOuters(groupIds: Set<Long>): MutableMap<Long, MutableSet<String>> {
        val groupIdToUserIds = mutableMapOf<Long, MutableSet<String>>()
        experienceGroupOuterDao.listByGroupIds(dslContext, groupIds).forEach {
            var userIds = groupIdToUserIds[it.groupId]
            if (null == userIds) {
                userIds = mutableSetOf()
                groupIdToUserIds[it.groupId] = userIds
            }
            userIds.add(it.outer)
        }
        return groupIdToUserIds
    }

    /**
     * 获取外部用户列表
     */
    fun getOuterReceivers(
        dslContext: DSLContext,
        experienceId: Long,
        groupIds: Set<Long>
    ): MutableSet<String> {
        val outerReceivers = mutableSetOf<String>()
        val outerGroup =
            getGroupIdToOuters(groupIds).values.asSequence().flatMap { it.asSequence() }
                .toSet()
        val outerUser =
            experienceOuterDao.listUserIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()
        outerReceivers.addAll(outerGroup)
        outerReceivers.addAll(outerUser)
        return outerReceivers
    }

    /**
     * 获取上次下载的 map<projectId+bundleId+platform , experienceId>
     */
    fun getLastDownloadMap(userId: String): Map<String, Long> {
        return experienceLastDownloadDao.listByUserId(dslContext, userId)?.map {
            it.projectId + it.bundleIdentifier + it.platform to it.lastDonwloadRecordId
        }?.toMap() ?: emptyMap()
    }

    private fun syncIcon(records: Result<TExperienceRecord>) {
        // 同步图片
        val projectToIcon = mutableMapOf<String, String>()
        val unSyncIconProjectIds = mutableSetOf<String>()
        records.forEach {
            if (StringUtils.isBlank(it.logoUrl)) {
                unSyncIconProjectIds.add(it.projectId)
            } else {
                projectToIcon[it.projectId] = it.logoUrl
            }
        }
        if (unSyncIconProjectIds.isNotEmpty()) {
            val projectList =
                client.get(ServiceProjectResource::class).listByProjectCode(unSyncIconProjectIds).data ?: listOf()
            projectList.forEach {
                projectToIcon[it.projectCode] = it.logoAddr ?: ""
            }
            unSyncIconProjectIds.forEach {
                experienceDao.updateIconByProjectIds(dslContext, it, projectToIcon[it] ?: "")
            }

            records.forEach {
                if (StringUtils.isBlank(it.logoUrl)) {
                    it.logoUrl = projectToIcon[it.projectId] ?: ""
                }
            }
        }
    }

    fun getNewestPublic(projectId: String, bundleIdentifier: String, platform: String): TExperiencePublicRecord? {
        return experiencePublicDao.getNewestRecord(dslContext, projectId, bundleIdentifier, platform)
    }
}
