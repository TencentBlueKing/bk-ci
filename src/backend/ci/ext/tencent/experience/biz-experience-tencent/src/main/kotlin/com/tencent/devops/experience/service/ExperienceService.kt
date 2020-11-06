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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_VERSION
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_NO
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_ICON_URL
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.code.BSExperienceAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.constant.ProductCategoryEnum
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperiencePublicDao
import com.tencent.devops.experience.pojo.Experience
import com.tencent.devops.experience.pojo.ExperienceCreate
import com.tencent.devops.experience.pojo.ExperiencePermission
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.pojo.ExperienceSummaryWithPermission
import com.tencent.devops.experience.pojo.ExperienceUpdate
import com.tencent.devops.experience.pojo.NotifyType
import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import com.tencent.devops.experience.pojo.enums.Platform
import com.tencent.devops.experience.pojo.enums.Source
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.experience.util.EmailUtil
import com.tencent.devops.experience.util.RtxUtil
import com.tencent.devops.experience.util.UrlUtil
import com.tencent.devops.experience.util.WechatGroupUtil
import com.tencent.devops.experience.util.WechatUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.Pattern
import javax.ws.rs.core.Response

@Service
class ExperienceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val experienceDao: ExperienceDao,
    private val experiencePublicDao: ExperiencePublicDao,
    private val groupService: GroupService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val wechatWorkService: WechatWorkService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val bsAuthPermissionApi: BSAuthPermissionApi,
    private val bsAuthResourceApi: BSAuthResourceApi,
    private val experienceServiceCode: BSExperienceAuthServiceCode

) {
    private val taskResourceType = AuthResourceType.EXPERIENCE_TASK
    private val regex = Pattern.compile("[,;]")
    private val publicGroup = "0";

    fun hasArtifactoryPermission(
        userId: String,
        projectId: String,
        path: String,
        artifactoryType: ArtifactoryType
    ): Boolean {
        val type = com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(artifactoryType.name)
        if (!client.get(ServiceArtifactoryResource::class).check(projectId, type, path).data!!) {
            throw ErrorCodeException(
                statusCode = 404,
                defaultMessage = "文件不存在",
                errorCode = ExperienceMessageCode.EXP_FILE_NOT_FOUND
            )
        }

        val properties = client.get(ServiceArtifactoryResource::class).properties(projectId, type, path).data!!
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
        val experiencePermissionListMap = filterExperience(userId, projectId, setOf(AuthPermission.EDIT))
        val expireTime = DateUtil.today()
        val searchTime = if (expired == null || expired == false) expireTime else null
        val online = if (expired == null || expired == false) true else null

        val experienceList = experienceDao.list(dslContext, projectId, searchTime, online)
        val groupIdSet = mutableSetOf<String>()
        experienceList.forEach {
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            experienceGroups.forEach { group ->
                groupIdSet.add(group)
            }
        }
        val groupMap = groupService.serviceGet(groupIdSet)

        return experienceList.map {
            val userSet = mutableSetOf<String>()
            val innerUsers = objectMapper.readValue<Set<String>>(it.innerUsers)
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            userSet.addAll(innerUsers)
            experienceGroups.forEach { group ->
                if (groupMap.containsKey(group)) {
                    userSet.addAll(groupMap[group]!!.innerUsers)
                }
            }

            val isExpired = DateUtil.isExpired(it.endDate, expireTime)
            val canExperience = userSet.contains(userId) || userId == it.creator
            val canEdit = experiencePermissionListMap[AuthPermission.EDIT]!!.contains(it.id)
            ExperienceSummaryWithPermission(
                experienceHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                platform = Platform.valueOf(it.platform),
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

    fun get(userId: String, projectId: String, experienceHashId: String, checkPermission: Boolean = true): Experience {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id

        val online = experienceRecord.online
        val isExpired = DateUtil.isExpired(experienceRecord.endDate)
        val canExperience = if (checkPermission) userCanExperience(userId, experienceId) else true
        val url = if (canExperience && online && !isExpired) getShortExternalUrl(experienceId) else null

        val groupHashIds = objectMapper.readValue<List<String>>(experienceRecord.experienceGroups)
        val groupList = groupService.serviceList(groupHashIds.toSet())

        return Experience(
            name = experienceRecord.name,
            path = experienceRecord.artifactoryPath,
            artifactoryType = ArtifactoryType.valueOf(experienceRecord.artifactoryType),
            platform = Platform.valueOf(experienceRecord.platform),
            version = experienceRecord.version,
            remark = experienceRecord.remark ?: "",
            createDate = experienceRecord.createTime.timestamp(),
            expireDate = experienceRecord.endDate.timestamp(),
            experienceGroups = groupList,
            innerUsers = objectMapper.readValue(experienceRecord.innerUsers),
            outerUsers = experienceRecord.outerUsers,
            notifyTypes = objectMapper.readValue(experienceRecord.notifyTypes),
            enableWechatGroups = experienceRecord.enableWechatGroups ?: true,
            wechatGroups = experienceRecord.wechatGroups ?: "",
            creator = experienceRecord.creator,
            expired = isExpired,
            canExperience = canExperience,
            online = experienceRecord.online,
            url = url
        )
    }

    fun create(userId: String, projectId: String, experience: ExperienceCreate) {
        var isPublic = false //是否有公开体验组
        experience.experienceGroups.forEach {
            if (it == publicGroup) {
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

        val propertyMap = getArtifactoryPropertiesMap(projectId, artifactoryType, experience.path)

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

    private fun getArtifactoryPropertiesMap(
        projectId: String,
        artifactoryType: com.tencent.devops.artifactory.pojo.enums.ArtifactoryType,
        path: String
    ): MutableMap<String, String> {
        val properties =
            client.get(ServiceArtifactoryResource::class).properties(projectId, artifactoryType, path).data!!
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

        if (!propertyMap.containsKey(ARCHIVE_PROPS_ICON_URL)) {
            val backUpIcon = client.get(ServiceProjectResource::class).get(projectId).data!!.logoAddr!!
            propertyMap[ARCHIVE_PROPS_ICON_URL] = UrlUtil.transformLogoAddr(backUpIcon)
        }

        return propertyMap
    }

    private fun createExperience(
        projectId: String,
        experience: ExperienceCreate,
        propertyMap: MutableMap<String, String>,
        source: Source,
        userId: String,
        isPublic: Boolean,
        artifactoryType: com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
    ) {
        val appBundleIdentifier = propertyMap[ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER]!!
        val appVersion = propertyMap[ARCHIVE_PROPS_APP_VERSION]!!
        val platform = if (experience.path.endsWith(".ipa")) Platform.IOS else Platform.ANDROID
        val artifactorySha1 = makeSha1(experience.artifactoryType, experience.path)
        val iconUrl = propertyMap[ARCHIVE_PROPS_ICON_URL]!!

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
            endDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(experience.expireDate), ZoneId.systemDefault()),
            experienceGroups = objectMapper.writeValueAsString(experience.experienceGroups),
            innerUsers = objectMapper.writeValueAsString(experience.innerUsers),
            outerUsers = experience.outerUsers,
            notifyTypes = objectMapper.writeValueAsString(experience.notifyTypes),
            enableWechatGroup = experience.enableWechatGroups,
            wechatGroups = experience.wechatGroups ?: "",
            online = true,
            source = source.name,
            creator = userId,
            updator = userId,
            experienceName = experience.experienceName ?: projectId,
            versionTitle = experience.versionTitle ?: experience.name,
            category = experience.categoryId ?: ProductCategoryEnum.LIFE.id,
            productOwner = objectMapper.writeValueAsString(experience.productOwner ?: emptyList<String>()),
            iconUrl = iconUrl
        )

        //公开体验表
        if (isPublic) {
            onlinePublicExperience(
                projectId,
                artifactoryType,
                experience,
                experienceId,
                platform,
                appBundleIdentifier,
                iconUrl
            )
        } else {
            offlinePublicExperience(projectId, platform, appBundleIdentifier)
        }

        createTaskResource(userId, projectId, experienceId, "${experience.name}（${experience.version}）")
        sendNotification(experienceId)
    }

    private fun offlinePublicExperience(projectId: String, platform: Platform, appBundleIdentifier: String) {
        experiencePublicDao.update(
            dslContext = dslContext,
            projectId = projectId,
            platform = platform.name,
            bundleIdentifier = appBundleIdentifier,
            online = false
        )
    }

    private fun onlinePublicExperience(
        projectId: String,
        artifactoryType: com.tencent.devops.artifactory.pojo.enums.ArtifactoryType,
        experience: ExperienceCreate,
        experienceId: Long,
        platform: Platform,
        appBundleIdentifier: String,
        iconUrl: String
    ) {
        val fileDetail =
            client.get(ServiceArtifactoryResource::class).show(projectId, artifactoryType, experience.path).data

        if (null == fileDetail) {
            logger.error("null file detail , experienceId:{}", experienceId)
            return
        }

        experiencePublicDao.create(
            dslContext = dslContext,
            recordId = experienceId,
            projectId = projectId,
            experienceName = experience.experienceName ?: projectId,
            category = experience.categoryId ?: ProductCategoryEnum.LIFE.id,
            platform = platform.name,
            bundleIdentifier = appBundleIdentifier,
            endDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(experience.expireDate), ZoneId.systemDefault()),
            size = fileDetail.size,
            iconUrl = iconUrl
        )
    }

    fun edit(userId: String, projectId: String, experienceHashId: String, experience: ExperienceUpdate) {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        validateTaskPermission(
            user = userId,
            projectId = projectId,
            experienceId = experienceId,
            authPermission = AuthPermission.EDIT,
            message = "用户在项目($projectId)下没有体验($experienceHashId)的编辑权限"
        )
        if (experienceDao.getOrNull(dslContext, experienceId) == null) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                defaultMessage = "体验($experienceHashId)不存在",
                errorCode = ExperienceMessageCode.EXP_NOT_EXISTS,
                params = arrayOf(experienceHashId)
            )
        }
        experience.experienceGroups.forEach {
            if (!groupService.serviceCheck(it)) {
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    defaultMessage = "体验($experienceHashId)不存在",
                    errorCode = ExperienceMessageCode.EXP_GROUP_NOT_EXISTS
                )
            }
        }

        experienceDao.update(
            dslContext = dslContext,
            id = experienceId,
            name = experience.name,
            remark = experience.remark,
            endDate = LocalDateTime.ofInstant(Instant.ofEpochSecond(experience.expireDate), ZoneId.systemDefault()),
            experienceGroups = objectMapper.writeValueAsString(experience.experienceGroups),
            innerUsers = objectMapper.writeValueAsString(experience.innerUsers),
            outerUsers = experience.outerUsers,
            notifyTypes = objectMapper.writeValueAsString(experience.notifyTypes),
            enableWechatGroup = experience.enableWechatGroups,
            wechatGroups = experience.wechatGroups ?: "",
            updator = userId
        )
        sendNotification(experienceId)
    }

    fun updateOnline(userId: String, projectId: String, experienceHashId: String, online: Boolean) {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        validateTaskPermission(
            user = userId,
            projectId = projectId,
            experienceId = experienceId,
            authPermission = AuthPermission.EDIT,
            message = "用户在项目($projectId)下没有体验($experienceHashId)的编辑权限"
        )
        if (experienceDao.getOrNull(dslContext, experienceId) == null) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                defaultMessage = "体验($experienceHashId)不存在",
                errorCode = ExperienceMessageCode.EXP_NOT_EXISTS,
                params = arrayOf(experienceHashId)
            )
        }
        experienceDao.updateOnline(dslContext, experienceId, online)
    }

    fun externalUrl(userId: String, projectId: String, experienceHashId: String): String {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id
        if (!userCanExperience(userId, experienceId)) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                defaultMessage = "用户($userId)不在体验用户名单中",
                errorCode = ExperienceMessageCode.USER_NOT_IN_EXP_GROUP,
                params = arrayOf(userId)
            )
        }
        val url =
            "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=experienceDetail&experienceId=$experienceHashId"
        return client.get(ServiceShortUrlResource::class)
            .createShortUrl(CreateShortUrlRequest(url, 24 * 3600 * 3)).data!!
    }

    fun downloadUrl(userId: String, projectId: String, experienceHashId: String): String {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id
        if (!userCanExperience(userId, experienceId)) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                defaultMessage = "用户($userId)不在体验用户名单中",
                errorCode = ExperienceMessageCode.USER_NOT_IN_EXP_GROUP,
                params = arrayOf(userId)
            )
        }
        return experienceDownloadService.serviceGetInnerDownloadUrl(userId, experienceId)
    }

    fun serviceCreate(userId: String, projectId: String, experience: ExperienceServiceCreate) {
        val isPublic = experience.experienceGroups.contains(publicGroup)

        val path = experience.path
        val artifactoryType =
            com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(experience.artifactoryType.name)
        if (!client.get(ServiceArtifactoryResource::class).check(projectId, artifactoryType, path).data!!) {
            throw RuntimeException("文件($path)不存在")
        }

        val propertyMap = getArtifactoryPropertiesMap(projectId, artifactoryType, path)

        if (!propertyMap.containsKey(ARCHIVE_PROPS_BUILD_NO)) {
            throw RuntimeException("元数据buildNo不存在")
        }

        val remark =
            if (experience.description.isNullOrBlank()) "构建号#${propertyMap[ARCHIVE_PROPS_BUILD_NO]!!}" else experience.description

        val experienceCreate = ExperienceCreate(
            name = path.split("/").last(),
            path = experience.path,
            artifactoryType = experience.artifactoryType,
            version = propertyMap[ARCHIVE_PROPS_APP_VERSION]!!,
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

        createExperience(
            projectId,
            experienceCreate,
            propertyMap,
            Source.PIPELINE,
            userId,
            isPublic,
            artifactoryType
        )
    }

    private fun sendNotification(experienceId: Long) {
        val experienceRecord = experienceDao.get(dslContext, experienceId)
        if (DateUtil.isExpired(experienceRecord.endDate)) {
            logger.info("experience($experienceId) is expired")
            return
        }

        val projectId = experienceRecord.projectId
        val name = experienceRecord.name
        val version = experienceRecord.version
        val userId = experienceRecord.creator
        val groups = objectMapper.readValue<Set<String>>(experienceRecord.experienceGroups)
        val notifyTypeList = objectMapper.readValue<Set<NotifyType>>(experienceRecord.notifyTypes)
        val extraUsers = objectMapper.readValue<Set<String>>(experienceRecord.innerUsers)

        val receivers = mutableSetOf<String>()
        receivers.addAll(extraUsers)
        groups.forEach {
            val groupUsers = groupService.serviceGetUsers(it)
            receivers.addAll(groupUsers.innerUsers)
        }

        val innerUrl = getInnerUrl(projectId, experienceId)
        val outerUrl = getShortExternalUrl(experienceId)

        val projectName = client.get(ServiceProjectResource::class).get(projectId).data!!.projectName

        if (notifyTypeList.contains(NotifyType.EMAIL)) {
            val message = EmailUtil.makeMessage(
                userId = userId,
                projectName = projectName,
                name = name,
                version = version,
                url = innerUrl,
                receivers = receivers.toSet()
            )
            client.get(ServiceNotifyResource::class).sendEmailNotify(message)
        }

        receivers.forEach {
            if (notifyTypeList.contains(NotifyType.RTX)) {
                val message = RtxUtil.makeMessage(
                    projectName = projectName,
                    name = name,
                    version = version,
                    innerUrl = innerUrl,
                    outerUrl = outerUrl,
                    receivers = setOf(it)
                )
                client.get(ServiceNotifyResource::class).sendRtxNotify(message)
            }
            if (notifyTypeList.contains(NotifyType.WECHAT)) {
                val message = WechatUtil.makeMessage(
                    projectName = projectName,
                    name = name,
                    version = version,
                    innerUrl = innerUrl,
                    outerUrl = outerUrl,
                    receivers = setOf(it)
                )
                client.get(ServiceNotifyResource::class).sendWechatNotify(message)
            }
        }
        if (experienceRecord.enableWechatGroups && !experienceRecord.wechatGroups.isNullOrBlank()) {
            val wechatGroupList = regex.split(experienceRecord.wechatGroups)
            wechatGroupList.forEach {
                val message = WechatGroupUtil.makeRichtextMessage(
                    projectName = projectName,
                    name = name,
                    version = version,
                    innerUrl = innerUrl,
                    outerUrl = outerUrl,
                    groupId = it
                )
                wechatWorkService.sendRichText(message)
            }
        }
    }

    private fun validateTaskPermission(
        user: String,
        projectId: String,
        experienceId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!bsAuthPermissionApi.validateUserResourcePermission(
                user = user,
                serviceCode = experienceServiceCode,
                resourceType = taskResourceType,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(experienceId),
                permission = authPermission
            )
        ) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                defaultMessage = authPermission.alias
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ExperienceMessageCode.USER_NEED_EXP_X_PERMISSION,
                defaultMessage = message,
                params = arrayOf(permissionMsg)
            )
        }
    }

    private fun createTaskResource(user: String, projectId: String, experienceId: Long, experienceName: String) {
        bsAuthResourceApi.createResource(
            user = user,
            serviceCode = experienceServiceCode,
            resourceType = taskResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(experienceId),
            resourceName = experienceName
        )
    }

    private fun filterExperience(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bsAuthPermissionApi.getUserResourcesByPermissions(
            user = user,
            serviceCode = experienceServiceCode,
            resourceType = taskResourceType,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = null
        )
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { (key, value) ->
            map[key] = value.map { HashUtil.decodeIdToLong(it) }
        }
        return map
    }

    private fun makeSha1(artifactoryType: ArtifactoryType, path: String): String {
        return ShaUtils.sha1((artifactoryType.name + path).toByteArray())
    }

    private fun getInnerUrl(projectId: String, experienceId: Long): String {
        val experienceHashId = HashUtil.encodeLongId(experienceId)
        return "${HomeHostUtil.innerServerHost()}/console/experience/$projectId/experienceDetail/$experienceHashId/detail"
    }

    private fun getShortExternalUrl(experienceId: Long): String {
        val experienceHashId = HashUtil.encodeLongId(experienceId)
        val url =
            "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=experienceDetail&experienceId=$experienceHashId"
        return client.get(ServiceShortUrlResource::class)
            .createShortUrl(CreateShortUrlRequest(url, 24 * 3600 * 30)).data!!
    }

    fun userCanExperience(userId: String, experienceId: Long): Boolean {
        val experienceRecord = experienceDao.get(dslContext, experienceId)
        val experienceGroups = objectMapper.readValue<Set<String>>(experienceRecord.experienceGroups)
        val innerUsers = objectMapper.readValue<Set<String>>(experienceRecord.innerUsers)

        val allUsers = mutableSetOf<String>()
        allUsers.addAll(innerUsers)
        val groupUserMap = groupService.serviceGet(experienceGroups)
        groupUserMap.forEach { (_, value) ->
            allUsers.addAll(value.innerUsers)
        }
        return allUsers.contains(userId) || userId == experienceRecord.creator
    }

    fun count(projectIds: Set<String>, expired: Boolean?): Map<String, Int> {
        val result = experienceDao.count(dslContext, projectIds, expired ?: false)
        return result?.map {
            val count = it[0] as Int
            val projectId = it[1] as String
            projectId to count
        }?.toMap() ?: mapOf()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceService::class.java)
    }
}