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
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.api.service.ServicePipelineArtifactoryResource
import com.tencent.devops.artifactory.api.service.ServiceShortUrlResource
import com.tencent.devops.artifactory.pojo.CreateShortUrlRequest
import com.tencent.devops.artifactory.pojo.enums.Permission
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.PlatformEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_APP_TITLE
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_ICON
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_NAME
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_SCHEME
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_VERSION
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_ID
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_NO
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.constant.GroupIdTypeEnum
import com.tencent.devops.experience.constant.GroupScopeEnum
import com.tencent.devops.experience.constant.ProductCategoryEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceInnerDao
import com.tencent.devops.experience.dao.ExperienceOuterDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.dao.ExperiencePushSubscribeDao
import com.tencent.devops.experience.dao.GroupDao
import com.tencent.devops.experience.pojo.Experience
import com.tencent.devops.experience.pojo.ExperienceCreate
import com.tencent.devops.experience.pojo.ExperienceCreateResp
import com.tencent.devops.experience.pojo.ExperienceInfoForBuild
import com.tencent.devops.experience.pojo.ExperiencePermission
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.pojo.ExperienceSummaryWithPermission
import com.tencent.devops.experience.pojo.ExperienceUpdate
import com.tencent.devops.experience.pojo.Group
import com.tencent.devops.experience.pojo.NotifyType
import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import com.tencent.devops.experience.pojo.enums.Source
import com.tencent.devops.experience.util.AppNotifyUtil
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.experience.util.EmailUtil
import com.tencent.devops.experience.util.RtxUtil
import com.tencent.devops.experience.util.WechatGroupUtil
import com.tencent.devops.experience.util.WechatUtil
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.process.api.service.ServiceBuildPermissionResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.Executors
import java.util.regex.Pattern
import javax.ws.rs.core.Response

@Service
@SuppressWarnings("LongParameterList", "LargeClass", "TooManyFunctions", "LongMethod", "TooGenericExceptionThrown")
class ExperienceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val experienceDao: ExperienceDao,
    private val experiencePublicDao: ExperiencePublicDao,
    private val experienceGroupDao: ExperienceGroupDao,
    private val experienceInnerDao: ExperienceInnerDao,
    private val experienceOuterDao: ExperienceOuterDao,
    private val groupDao: GroupDao,
    private val groupService: GroupService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val wechatWorkService: WechatWorkService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val experienceBaseService: ExperienceBaseService,
    private val experiencePermissionService: ExperiencePermissionService,
    private val experiencePushService: ExperiencePushService,
    private val experiencePushSubscribeDao: ExperiencePushSubscribeDao
) {
    private val taskResourceType = AuthResourceType.EXPERIENCE_TASK
    private val regex = Pattern.compile("[,;]")
    private val threadPool = Executors.newFixedThreadPool(3)

    fun hasArtifactoryPermission(
        userId: String,
        projectId: String,
        path: String,
        artifactoryType: ArtifactoryType
    ): Boolean {
        val type = com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(artifactoryType.name)
        if (!client.get(ServiceArtifactoryResource::class).check(userId, projectId, type, path).data!!) {
            throw ErrorCodeException(
                statusCode = 404,
                defaultMessage = "文件不存在",
                errorCode = ExperienceMessageCode.EXP_FILE_NOT_FOUND
            )
        }

        val properties = client.get(ServiceArtifactoryResource::class).properties(userId, projectId, type, path).data!!
        val propertyMap = mutableMapOf<String, String>()
        properties.forEach {
            propertyMap[it.key] = it.value
        }
        if (!propertyMap.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            throw ErrorCodeException(
                defaultMessage = "体验未与流水线绑定",
                errorCode = ExperienceMessageCode.EXP_META_DATA_PIPELINE_ID_NOT_EXISTS
            )
        }
        val pipelineId = propertyMap[ARCHIVE_PROPS_PIPELINE_ID]!!
        return client.get(ServicePipelineArtifactoryResource::class).hasPermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = Permission.EXECUTE
        ).data!!
    }

    fun list(userId: String, projectId: String, expired: Boolean?): List<ExperienceSummaryWithPermission> {
        val expireTime = DateUtil.today()
        val searchTime = if (expired == null || expired == false) expireTime else null
        val online = if (expired == null || expired == false) true else null

        val experienceList = experienceDao.list(dslContext, projectId, searchTime, online)
        val recordIds = experienceBaseService.getRecordIdsByUserId(userId, GroupIdTypeEnum.JUST_PRIVATE)
        val experiencePermissionListMap = experiencePermissionService.filterExperience(
            user = userId,
            projectId = projectId,
            authPermissions = setOf(AuthPermission.EDIT)
        )

        return experienceList.map {
            val isExpired = DateUtil.isExpired(it.endDate, expireTime)
            val canExperience = recordIds.contains(it.id) || userId == it.creator

            val canEdit = experiencePermissionListMap[AuthPermission.EDIT]?.contains(it.id) ?: false
            ExperienceSummaryWithPermission(
                experienceHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                platform = PlatformEnum.valueOf(it.platform),
                version = it.version,
                remark = it.remark ?: "",
                expireDate = it.endDate.timestamp(),
                source = Source.valueOf(it.source),
                creator = it.creator,
                expired = isExpired,
                online = it.online,
                permissions = ExperiencePermission(canExperience, canEdit)
            )
        }
    }

    fun get(userId: String, experienceHashId: String, checkPermission: Boolean = true): Experience {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id

        val online = experienceRecord.online
        val isExpired = DateUtil.isExpired(experienceRecord.endDate)
        val canExperience = if (checkPermission) experienceBaseService.userCanExperience(userId, experienceId) else true
        val url = if (canExperience && online && !isExpired) getShortExternalUrl(experienceId) else null

        val groupIds = experienceBaseService.getGroupIdsByRecordId(experienceId)
        val groupIdToInnerUserIds = experienceBaseService.getGroupIdToInnerUserIds(groupIds)
        val groupIdToOuters = experienceBaseService.getGroupIdToOuters(groupIds)
        val innerUserIds =
            experienceInnerDao.listUserIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()
        val outers = experienceOuterDao.listUserIdsByRecordId(dslContext, experienceId).map { it.value1() }.toSet()

        val groupList = groupDao.list(dslContext, groupIds).map {
            Group(
                groupHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                innerUsers = groupIdToInnerUserIds[it.id] ?: emptySet(),
                outerUsers = groupIdToOuters[it.id] ?: emptySet(),
                remark = it.remark ?: ""
            )
        }

        if (groupIdToInnerUserIds.keys.contains(ExperienceConstant.PUBLIC_GROUP)) {
            groupList.add(
                index = 0,
                element = Group(
                    groupHashId = HashUtil.encodeLongId(ExperienceConstant.PUBLIC_GROUP),
                    name = ExperienceConstant.PUBLIC_NAME,
                    innerUsers = ExperienceConstant.PUBLIC_INNER_USERS,
                    outerUsers = emptySet(),
                    remark = ""
                )
            )
        }

        return Experience(
            name = experienceRecord.name,
            path = experienceRecord.artifactoryPath,
            artifactoryType = ArtifactoryType.valueOf(experienceRecord.artifactoryType),
            platform = PlatformEnum.valueOf(experienceRecord.platform),
            version = experienceRecord.version,
            remark = experienceRecord.remark ?: "",
            createDate = experienceRecord.createTime.timestamp(),
            expireDate = experienceRecord.endDate.timestamp(),
            experienceGroups = groupList,
            innerUsers = innerUserIds,
            outerUsers = outers,
            notifyTypes = objectMapper.readValue(experienceRecord.notifyTypes),
            enableWechatGroups = experienceRecord.enableWechatGroups ?: true,
            wechatGroups = experienceRecord.wechatGroups ?: "",
            creator = experienceRecord.creator,
            expired = isExpired,
            canExperience = canExperience,
            online = experienceRecord.online,
            url = url,
            experienceName = experienceRecord.experienceName,
            versionTitle = experienceRecord.versionTitle,
            categoryId = experienceRecord.category,
            productOwner = objectMapper.readValue(experienceRecord.productOwner)
        )
    }

    fun create(userId: String, projectId: String, experience: ExperienceCreate) {
        val isPublic = isPublicGroupAndCheck(experience.experienceGroups) // 是否有公开体验组

        if (!hasArtifactoryPermission(userId, projectId, experience.path, experience.artifactoryType)) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${AuthPermission.EXECUTE.value}",
                defaultMessage = AuthPermission.EXECUTE.alias
            )
            throw ErrorCodeException(
                defaultMessage = "用户没有流水线执行权限",
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                params = arrayOf(permissionMsg)
            )
        }

        val artifactoryType =
            com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(experience.artifactoryType.name)

        val propertyMap = getArtifactoryPropertiesMap(userId, projectId, artifactoryType, experience.path)

        createExperience(
            projectId,
            experience,
            propertyMap,
            Source.WEB,
            userId,
            isPublic,
            artifactoryType
        )
    }

    private fun isPublicGroupAndCheck(experienceGroups: Set<String>): Boolean {
        var isPublic = false // 是否有公开体验组
        experienceGroups.forEach {
            if (HashUtil.decodeIdToLong(it) == ExperienceConstant.PUBLIC_GROUP) {
                isPublic = true
            } else {
                if (!groupService.serviceCheck(it)) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.NOT_FOUND.statusCode,
                        defaultMessage = "体验组($it)不存在",
                        errorCode = ExperienceMessageCode.EXP_GROUP_NOT_EXISTS,
                        params = arrayOf(it)
                    )
                }
            }
        }
        return isPublic
    }

    private fun getArtifactoryPropertiesMap(
        userId: String,
        projectId: String,
        artifactoryType: com.tencent.devops.artifactory.pojo.enums.ArtifactoryType,
        path: String
    ): MutableMap<String, String> {
        val properties =
            client.get(ServiceArtifactoryResource::class).properties(userId, projectId, artifactoryType, path).data!!
        val propertyMap = mutableMapOf<String, String>()
        properties.forEach {
            propertyMap[it.key] = it.value
        }

        if (!propertyMap.containsKey(ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER)) {
            throw RuntimeException("元数据bundleIdentifier不存在")
        }
        if (!propertyMap.containsKey(ARCHIVE_PROPS_APP_VERSION)) {
            throw RuntimeException("元数据appVersion不存在")
        }

        if (!propertyMap.containsKey(ARCHIVE_PROPS_APP_ICON)) {
            val backUpIcon = client.get(ServiceProjectResource::class).get(projectId).data!!.logoAddr!!
            propertyMap[ARCHIVE_PROPS_APP_ICON] = backUpIcon
        }

        return propertyMap
    }

    @SuppressWarnings("ComplexMethod")
    private fun createExperience(
        projectId: String,
        experience: ExperienceCreate,
        propertyMap: MutableMap<String, String>,
        source: Source,
        userId: String,
        isPublic: Boolean,
        artifactoryType: com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
    ): Long {
        val fileDetail =
            client.get(ServiceArtifactoryResource::class).show(userId, projectId, artifactoryType, experience.path).data

        if (null == fileDetail) {
            logger.error(
                "null file detail , projectId:$projectId , " +
                        "artifactoryType:$artifactoryType , path:${experience.path}"
            )
            return -1L
        }

        val encodePublicGroup = HashUtil.encodeLongId(ExperienceConstant.PUBLIC_GROUP)
        val experienceGroups = when (experience.groupScope) {
            GroupScopeEnum.PUBLIC.id -> {
                setOf(encodePublicGroup)
            }
            null -> {
                experience.experienceGroups
            }
            else -> {
                experience.experienceGroups.filterNot { it == encodePublicGroup }.toSet()
            }
        }
        val experienceInnerUsers = if (experience.groupScope == GroupScopeEnum.PUBLIC.id) {
            emptySet()
        } else {
            experience.innerUsers
        }
        val experienceOuterUsers = if (experience.groupScope == GroupScopeEnum.PUBLIC.id) {
            emptySet()
        } else {
            experience.outerUsers
        }

        val appBundleIdentifier = propertyMap[ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER]!!
        val appVersion = propertyMap[ARCHIVE_PROPS_APP_VERSION]!!
        val platform = if (experience.path.endsWith(".ipa")) PlatformEnum.IOS else PlatformEnum.ANDROID
        val artifactorySha1 = makeSha1(experience.artifactoryType, experience.path)
        val logoUrl = propertyMap[ARCHIVE_PROPS_APP_ICON]!!
        val fileSize = fileDetail.size
        val scheme = propertyMap[ARCHIVE_PROPS_APP_SCHEME] ?: ""
        val experienceName = when {
            StringUtils.isNotBlank(experience.experienceName) -> {
                experience.experienceName!!
            }
            StringUtils.isNotBlank(propertyMap[ARCHIVE_PROPS_APP_NAME]) -> {
                propertyMap[ARCHIVE_PROPS_APP_NAME]!!
            }
            StringUtils.isNotBlank(propertyMap[ARCHIVE_PROPS_APP_APP_TITLE]) -> {
                propertyMap[ARCHIVE_PROPS_APP_APP_TITLE]!!
            }
            else -> {
                projectId
            }
        }

        val endDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(experience.expireDate), ZoneId.systemDefault())
            .withHour(23)
            .withMinute(59)
            .withSecond(0)

        val experienceId = experienceDao.create(
            dslContext = dslContext,
            projectId = projectId,
            name = experience.name,
            platform = platform.name,
            path = experience.path,
            artifactoryType = experience.artifactoryType.name,
            artifactorySha1 = artifactorySha1,
            bundleIdentifier = appBundleIdentifier,
            version = appVersion,
            remark = experience.remark,
            endDate = endDate,
            experienceGroups = "[]",
            innerUsers = "[]",
            notifyTypes = objectMapper.writeValueAsString(experience.notifyTypes),
            enableWechatGroup = experience.enableWechatGroups,
            wechatGroups = experience.wechatGroups ?: "",
            online = true,
            source = source.name,
            creator = userId,
            updator = userId,
            experienceName = experienceName,
            versionTitle = experience.versionTitle ?: experience.name,
            category = experience.categoryId ?: ProductCategoryEnum.LIFE.id,
            productOwner = objectMapper.writeValueAsString(experience.productOwner ?: emptyList<String>()),
            logoUrl = logoUrl,
            size = fileSize,
            scheme = scheme,
            buildId = propertyMap[ARCHIVE_PROPS_BUILD_ID] ?: "",
            pipelineId = propertyMap[ARCHIVE_PROPS_PIPELINE_ID] ?: ""
        )

        // 加上权限
        experienceGroups.forEach {
            experienceGroupDao.create(dslContext, experienceId, HashUtil.decodeIdToLong(it))
        }
        experienceInnerUsers.forEach {
            experienceInnerDao.create(dslContext, experienceId, it)
        }
        experienceOuterUsers.forEach {
            experienceOuterDao.create(dslContext, experienceId, it)
        }

        // 公开体验表
        if (isPublic) {
            onlinePublicExperience(
                projectId = projectId,
                size = fileSize,
                experienceName = experienceName,
                categoryId = experience.categoryId ?: ProductCategoryEnum.LIFE.id,
                expireDate = experience.expireDate,
                experienceId = experienceId,
                platform = platform,
                appBundleIdentifier = appBundleIdentifier,
                logoUrl = logoUrl,
                scheme = scheme,
                version = appVersion
            )
        }

        experiencePermissionService.createTaskResource(
            userId,
            projectId,
            experienceId,
            "${experience.name}（$appVersion）"
        )
        sendNotification(experienceId)

        return experienceId
    }

    private fun onlinePublicExperience(
        projectId: String,
        size: Long,
        experienceName: String,
        categoryId: Int,
        expireDate: Long,
        experienceId: Long,
        platform: PlatformEnum,
        appBundleIdentifier: String,
        logoUrl: String,
        scheme: String,
        version: String
    ) {

        experiencePublicDao.create(
            dslContext = dslContext,
            recordId = experienceId,
            projectId = projectId,
            experienceName = experienceName,
            category = categoryId,
            platform = platform.name,
            bundleIdentifier = appBundleIdentifier,
            endDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(expireDate), ZoneId.systemDefault()),
            size = size,
            logoUrl = logoUrl,
            scheme = scheme,
            version = version
        )
    }

    fun edit(userId: String, projectId: String, experienceHashId: String, experience: ExperienceUpdate) {
        val experienceRecord = getExperienceId4Update(experienceHashId, userId, projectId)
        val endDate = if (experience.expireDate != null) {
            LocalDateTime.ofInstant(Instant.ofEpochSecond(experience.expireDate!!), ZoneId.systemDefault())
                .withHour(23)
                .withMinute(59)
                .withSecond(0)
        } else {
            experienceRecord.endDate
        }

        experienceDao.update(
            dslContext = dslContext,
            id = experienceRecord.id,
            name = experience.name ?: experienceRecord.name,
            remark = experience.remark,
            endDate = endDate,
            experienceGroups = "[]",
            innerUsers = "[]",
            notifyTypes = objectMapper.writeValueAsString(experience.notifyTypes),
            enableWechatGroup = experience.enableWechatGroups ?: experienceRecord.enableWechatGroups,
            wechatGroups = experience.wechatGroups ?: "",
            updator = userId,
            experienceName = experience.experienceName ?: projectId,
            versionTitle = experience.versionTitle ?: experienceRecord.versionTitle,
            category = experience.categoryId ?: ProductCategoryEnum.LIFE.id,
            productOwner = objectMapper.writeValueAsString(experience.productOwner ?: emptyList<String>())
        )

        // 更新组
        experience.experienceGroups?.let { groups ->
            experienceGroupDao.deleteByRecordId(
                dslContext,
                experienceRecord.id,
                groups.map { HashUtil.decodeIdToLong(it) }.toSet()
            )
        }
        experience.experienceGroups?.forEach {
            experienceGroupDao.create(dslContext, experienceRecord.id, HashUtil.decodeIdToLong(it))
        }

        // 更新内部成员
        experience.innerUsers?.let { experienceInnerDao.deleteByRecordId(dslContext, experienceRecord.id, it) }
        experience.innerUsers?.forEach {
            experienceInnerDao.create(dslContext, experienceRecord.id, it)
        }

        // 更新外部人员
        experience.outerUsers?.let { experienceOuterDao.deleteByRecordId(dslContext, experienceRecord.id, it) }
        experience.outerUsers?.forEach {
            experienceOuterDao.create(dslContext, experienceRecord.id, it)
        }

        val isPublic = experience.experienceGroups
            ?.let { isPublicGroupAndCheck(it) }
            ?: experienceBaseService.isPublic(HashUtil.decodeIdToLong(experienceHashId), false)
        if (isPublic) {
            onlinePublicExperience(
                projectId = projectId,
                size = experienceRecord.size,
                experienceName = experience.experienceName ?: projectId,
                categoryId = experience.categoryId ?: ProductCategoryEnum.LIFE.id,
                expireDate = experience.expireDate ?: experienceRecord.endDate.timestamp(),
                experienceId = experienceRecord.id,
                platform = PlatformEnum.valueOf(experienceRecord.platform),
                appBundleIdentifier = experienceRecord.bundleIdentifier,
                logoUrl = experienceRecord.logoUrl,
                scheme = experienceRecord.scheme,
                version = experienceRecord.version
            )
        } else {
            experiencePublicDao.updateByRecordId(
                dslContext = dslContext,
                recordId = experienceRecord.id,
                online = false
            )
        }

        sendNotification(experienceRecord.id)
    }

    fun getCreatorById(experienceHashId: String): String {
        return experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId)).creator
    }

    fun updateOnline(userId: String, projectId: String, experienceHashId: String, online: Boolean) {
        val experienceId = getExperienceId4Update(experienceHashId, userId, projectId).id
        experienceDao.updateOnline(dslContext, experienceId, online)

        if (!online) {
            experiencePublicDao.updateByRecordId(
                dslContext = dslContext,
                recordId = experienceId,
                online = false
            )
        }
    }

    private fun getExperienceId4Update(
        experienceHashId: String,
        userId: String,
        projectId: String
    ): TExperienceRecord {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        experiencePermissionService.validateTaskPermission(
            user = userId,
            projectId = projectId,
            experienceId = experienceId,
            authPermission = AuthPermission.EDIT,
            message = "用户在项目($projectId)下没有体验($experienceHashId)的编辑权限"
        )
        return experienceDao.getOrNull(dslContext, experienceId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                defaultMessage = "体验($experienceHashId)不存在",
                errorCode = ExperienceMessageCode.EXP_NOT_EXISTS,
                params = arrayOf(experienceHashId)
            )
    }

    fun externalUrl(userId: String, experienceHashId: String): String {
        checkExperienceAndGetId(experienceHashId, userId)
        return experienceDownloadService.getQrCodeUrl(experienceHashId)
    }

    fun downloadUrl(userId: String, experienceHashId: String): String {
        val experienceId = checkExperienceAndGetId(experienceHashId, userId)
        return experienceDownloadService.getInnerDownloadUrl(userId, experienceId)
    }

    private fun checkExperienceAndGetId(experienceHashId: String, userId: String): Long {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id
        if (!experienceBaseService.userCanExperience(userId, experienceId)) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                defaultMessage = "用户($userId)不在体验用户名单中",
                errorCode = ExperienceMessageCode.USER_NOT_IN_EXP_GROUP,
                params = arrayOf(userId)
            )
        }
        return experienceId
    }

    fun serviceCreate(userId: String, projectId: String, experience: ExperienceServiceCreate): ExperienceCreateResp {
        val isPublic = experience.experienceGroups.contains(HashUtil.encodeLongId(ExperienceConstant.PUBLIC_GROUP))

        val path = experience.path
        val artifactoryType =
            com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(experience.artifactoryType.name)
        if (!client.get(ServiceArtifactoryResource::class).check(userId, projectId, artifactoryType, path).data!!) {
            throw RuntimeException("文件($path)不存在")
        }

        val propertyMap = getArtifactoryPropertiesMap(userId, projectId, artifactoryType, path)

        if (!propertyMap.containsKey(ARCHIVE_PROPS_BUILD_NO)) {
            throw RuntimeException("元数据buildNo不存在")
        }

        val remark = if (experience.description.isNullOrBlank()) {
            "构建号#${propertyMap[ARCHIVE_PROPS_BUILD_NO]!!}"
        } else experience.description

        val experienceCreate = ExperienceCreate(
            name = path.split("/").last(),
            path = experience.path,
            artifactoryType = experience.artifactoryType,
            remark = remark,
            expireDate = experience.expireDate,
            experienceGroups = experience.experienceGroups,
            innerUsers = experience.innerUsers,
            outerUsers = experience.outerUsers,
            notifyTypes = experience.notifyTypes,
            enableWechatGroups = experience.enableWechatGroups,
            wechatGroups = experience.wechatGroups,
            experienceName = experience.experienceName,
            versionTitle = experience.versionTitle,
            categoryId = experience.categoryId,
            productOwner = experience.productOwner
        )

        val experienceId = createExperience(
            projectId,
            experienceCreate,
            propertyMap,
            Source.PIPELINE,
            userId,
            isPublic,
            artifactoryType
        )

        return ExperienceCreateResp(
            url = getShortExternalUrl(experienceId),
            experienceHashId = HashUtil.encodeLongId(experienceId)
        )
    }

    private fun sendNotification(experienceId: Long) {
        threadPool.submit {
            val experienceRecord = experienceDao.get(dslContext, experienceId)
            if (DateUtil.isExpired(experienceRecord.endDate)) {
                logger.info("experience($experienceId) is expired")
                return@submit
            }

            val notifyTypeList = objectMapper.readValue<Set<NotifyType>>(experienceRecord.notifyTypes)
            val groupIds = experienceBaseService.getGroupIdsByRecordId(experienceId)

            // 内部用户
            val innerReceivers = experienceBaseService.getInnerReceivers(
                dslContext = dslContext,
                experienceId = experienceId,
                userId = experienceRecord.creator
            )
            // 外部用户
            val outerReceivers = experienceBaseService.getOuterReceivers(
                dslContext = dslContext,
                experienceId = experienceId,
                groupIds = groupIds
            )
            // 订阅用户
            val subscribeUsers = experiencePushSubscribeDao.listSubscription(
                dslContext = dslContext,
                projectId = experienceRecord.projectId,
                bundle = experienceRecord.bundleIdentifier,
                platform = experienceRecord.platform
            ).map { it.value2() }.toSet().subtract(innerReceivers)
                .subtract(outerReceivers)

            logger.info(
                "innerReceivers: $innerReceivers , outerReceivers:" +
                        " $outerReceivers , subscribeUsers: $subscribeUsers "
            )
            if (innerReceivers.isEmpty() && outerReceivers.isEmpty() && subscribeUsers.isEmpty()) {
                logger.info("empty Receivers , experienceId:$experienceId")
                return@submit
            }

            // 开始发送
            val pcUrl = getPcUrl(experienceRecord.projectId, experienceId)
            val appUrl = getShortExternalUrl(experienceId)
            val projectName =
                client.get(ServiceProjectResource::class).get(experienceRecord.projectId).data!!.projectName
            sendMessageToOuterReceivers(outerReceivers, experienceRecord)
            sendMessageToInnerReceivers(
                notifyTypeList = notifyTypeList,
                projectName = projectName,
                innerReceivers = innerReceivers,
                experienceRecord = experienceRecord,
                pcUrl = pcUrl,
                appUrl = appUrl
            )
            sendMessageToSubscriber(subscribeUsers, experienceRecord)
        }
    }

    /**
     * 发给外部人员
     */
    private fun sendMessageToOuterReceivers(
        outerReceivers: MutableSet<String>,
        experienceRecord: TExperienceRecord
    ) {
        outerReceivers.forEach {
            val appMessage = AppNotifyUtil.makeMessage(
                experienceHashId = HashUtil.encodeLongId(experienceRecord.id),
                experienceName = experienceRecord.experienceName,
                appVersion = experienceRecord.version,
                receiver = it,
                platform = experienceRecord.platform
            )
            experiencePushService.pushMessage(appMessage)
        }
    }

    /**
     * 发给订阅人员
     */
    private fun sendMessageToSubscriber(
        subscribeUsers: Set<String>,
        experienceRecord: TExperienceRecord
    ) {
        subscribeUsers.forEach {
            val appMessage = AppNotifyUtil.makeMessage(
                experienceHashId = HashUtil.encodeLongId(experienceRecord.id),
                experienceName = experienceRecord.experienceName,
                appVersion = experienceRecord.version,
                receiver = it,
                platform = experienceRecord.platform
            )
            experiencePushService.pushMessage(appMessage)
        }
    }

    /**
     * 发给内部人员
     */
    private fun sendMessageToInnerReceivers(
        notifyTypeList: Set<NotifyType>,
        projectName: String,
        innerReceivers: MutableSet<String>,
        experienceRecord: TExperienceRecord,
        pcUrl: String,
        appUrl: String
    ) {
        // 内部邮件
        if (notifyTypeList.contains(NotifyType.EMAIL)) {
            val message = EmailUtil.makeMessage(
                userId = experienceRecord.creator,
                projectName = projectName,
                name = experienceRecord.name,
                version = experienceRecord.version,
                url = pcUrl,
                receivers = innerReceivers.toSet()
            )
            client.get(ServiceNotifyResource::class).sendEmailNotify(message)
        }

        // 内部企业微信群
        if (experienceRecord.enableWechatGroups && !experienceRecord.wechatGroups.isNullOrBlank()) {
            val wechatGroupList = regex.split(experienceRecord.wechatGroups)
            wechatGroupList.forEach {
                val message = WechatGroupUtil.makeRichtextMessage(
                    projectName = projectName,
                    name = experienceRecord.name,
                    version = experienceRecord.version,
                    innerUrl = pcUrl,
                    outerUrl = appUrl,
                    groupId = it
                )
                wechatWorkService.sendRichText(message)
            }
        }

        // 企业微信
        innerReceivers.forEach {
            if (notifyTypeList.contains(NotifyType.RTX)) {
                val message = RtxUtil.makeMessage(
                    projectName = projectName,
                    name = experienceRecord.name,
                    version = experienceRecord.version,
                    pcUrl = pcUrl,
                    appUrl = appUrl,
                    receivers = setOf(it)
                )
                client.get(ServiceNotifyResource::class).sendRtxNotify(message)
            }
            if (notifyTypeList.contains(NotifyType.WECHAT)) {
                val message = WechatUtil.makeMessage(
                    projectName = projectName,
                    name = experienceRecord.name,
                    version = experienceRecord.version,
                    innerUrl = pcUrl,
                    outerUrl = appUrl,
                    receivers = setOf(it)
                )
                client.get(ServiceNotifyResource::class).sendWechatNotify(message)
            }

            // 发送APP通知
            val appMessage = AppNotifyUtil.makeMessage(
                experienceHashId = HashUtil.encodeLongId(experienceRecord.id),
                experienceName = experienceRecord.experienceName,
                appVersion = experienceRecord.version,
                receiver = it,
                platform = experienceRecord.platform
            )
            experiencePushService.pushMessage(appMessage)
        }
    }

    private fun makeSha1(artifactoryType: ArtifactoryType, path: String): String {
        return ShaUtils.sha1((artifactoryType.name + path).toByteArray())
    }

    private fun getPcUrl(projectId: String, experienceId: Long): String {
        val experienceHashId = HashUtil.encodeLongId(experienceId)
        return HomeHostUtil.innerServerHost() +
                "/console/experience/$projectId/experienceDetail/$experienceHashId/detail"
    }

    private fun getShortExternalUrl(experienceId: Long): String {
        val experienceHashId = HashUtil.encodeLongId(experienceId)
        val url =
            HomeHostUtil.outerServerHost() +
                    "/app/download/devops_app_forward.html?flag=experienceDetail&experienceId=$experienceHashId"
        return client.get(ServiceShortUrlResource::class)
            .createShortUrl(CreateShortUrlRequest(url, 24 * 3600 * 30)).data!!
    }

    fun count(projectIds: Set<String>, expired: Boolean?): Map<String, Int> {
        val result = experienceDao.count(dslContext, projectIds, expired ?: false)
        return result?.map {
            val count = it[0] as Int
            val projectId = it[1] as String
            projectId to count
        }?.toMap() ?: mapOf()
    }

    fun lastParams(userId: String, name: String, projectId: String, bundleIdentifier: String): ExperienceCreate? {
        val platform = when {
            name.endsWith(".apk") -> {
                PlatformEnum.ANDROID
            }
            name.endsWith(".ipa") -> {
                PlatformEnum.IOS
            }
            else -> {
                return null
            }
        }

        val experienceRecord =
            experienceDao.getByBundleId(dslContext, projectId, bundleIdentifier, platform.name)
        if (null == experienceRecord) {
            return null
        } else {
            val innerUsers = experienceInnerDao.listUserIdsByRecordId(dslContext, experienceRecord.id)
            val outers = experienceOuterDao.listUserIdsByRecordId(dslContext, experienceRecord.id)
            val groups = experienceGroupDao.listGroupIdsByRecordId(dslContext, experienceRecord.id)
            val groupScope = if (groups.size == 1 && groups[0].value1() == 0L) {
                GroupScopeEnum.PUBLIC
            } else {
                GroupScopeEnum.PRIVATE
            }.id

            return ExperienceCreate(
                name = experienceRecord.name,
                path = experienceRecord.artifactoryPath,
                artifactoryType = ArtifactoryType.valueOf(experienceRecord.artifactoryType),
                remark = experienceRecord.remark,
                expireDate = experienceRecord.endDate.timestamp(),
                experienceGroups = groups.map { HashUtil.encodeLongId(it.value1()) }.toSet(),
                innerUsers = innerUsers.map { it.value1() }.toSet(),
                outerUsers = outers.map { it.value1() }.toSet(),
                notifyTypes = objectMapper.readValue(experienceRecord.notifyTypes),
                enableWechatGroups = experienceRecord.wechatGroups.isNotBlank(),
                wechatGroups = experienceRecord.wechatGroups,
                experienceName = experienceRecord.experienceName,
                versionTitle = experienceRecord.versionTitle,
                categoryId = experienceRecord.category,
                productOwner = objectMapper.readValue(experienceRecord.productOwner),
                groupScope = groupScope
            )
        }
    }

    fun listForBuild(
        userId: String?,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<ExperienceInfoForBuild> {
        // 判断是否有权限
        if (!userId.isNullOrEmpty() && !client.get(ServiceBuildPermissionResource::class)
                .checkViewPermission(userId, projectId, pipelineId, buildId).data!!
        ) {
            throw ErrorCodeException(
                defaultMessage = "用户没有流水线执行权限",
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION
            )
        }

        // 返回信息
        return experienceDao.listForBuild(dslContext, projectId, pipelineId, buildId).map {
            val encodeLongId = HashUtil.encodeLongId(it.id)
            ExperienceInfoForBuild(
                experienceName = it.experienceName,
                versionTitle = it.versionTitle,
                remark = it.remark,
                scheme = if (it.platform == "ANDROID") {
                    "bkdevopsapp://bkdevopsapp/app/experience/expDetail/$encodeLongId"
                } else {
                    "bkdevopsapp://app/experience/expDetail/$encodeLongId"
                },
                experienceId = encodeLongId
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceService::class.java)
    }
}
