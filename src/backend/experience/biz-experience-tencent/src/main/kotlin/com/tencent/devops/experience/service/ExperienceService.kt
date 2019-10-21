package com.tencent.devops.experience.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.api.service.ServicePipelineResource
import com.tencent.devops.artifactory.pojo.enums.Permission
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_APP_VERSION
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_BUILD_NO
import com.tencent.devops.common.archive.constant.ARCHIVE_PROPS_PIPELINE_ID
import com.tencent.devops.common.archive.shorturl.ShortUrlApi
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.code.BSExperienceAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.TokenDao
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
import com.tencent.devops.experience.util.WechatGroupUtil
import com.tencent.devops.experience.util.WechatUtil
import com.tencent.devops.notify.api.service.ServiceNotifyResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.Pattern
import javax.ws.rs.NotFoundException

@Service
class ExperienceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val experienceDao: ExperienceDao,
    private val tokenDao: TokenDao,
    private val groupService: GroupService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val wechatWorkService: WechatWorkService,
    private val client: Client,
    private val objectMapper: ObjectMapper,
    private val bsAuthPermissionApi: BSAuthPermissionApi,
    private val bsAuthResourceApi: BSAuthResourceApi,
    private val shortUrlApi: ShortUrlApi,
    private val experienceServiceCode: BSExperienceAuthServiceCode
    
) {
    private val taskResourceType = AuthResourceType.EXPERIENCE_TASK
    private val regex = Pattern.compile(",|;")

    fun hasArtifactoryPermission(userId: String, projectId: String, path: String, artifactoryType: ArtifactoryType): Boolean {
        val type = com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(artifactoryType.name)
        if (!client.get(ServiceArtifactoryResource::class).check(projectId, type, path).data!!) {
            throw NotFoundException("文件($path)不存在")
        }

        val properties = client.get(ServiceArtifactoryResource::class).properties(projectId, type, path).data!!
        val propertyMap = mutableMapOf<String, String>()
        properties.forEach {
            propertyMap[it.key] = it.value
        }
        if (!propertyMap.containsKey(ARCHIVE_PROPS_PIPELINE_ID)) {
            throw RuntimeException("元数据pipelineId不存在")
        }
        val pipelineId = propertyMap[ARCHIVE_PROPS_PIPELINE_ID]!!
        return client.get(ServicePipelineResource::class).hasPermission(userId, projectId, pipelineId, Permission.EXECUTE).data!!
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
            experienceGroups.forEach {
                groupIdSet.add(it)
            }
        }
        val groupMap = groupService.serviceGet(groupIdSet)

        return experienceList.map {
            val userSet = mutableSetOf<String>()
            val innerUsers = objectMapper.readValue<Set<String>>(it.innerUsers)
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            userSet.addAll(innerUsers)
            experienceGroups.forEach {
                if (groupMap.containsKey(it)) {
                    userSet.addAll(groupMap[it]!!.innerUsers)
                }
            }

            val isExpired = DateUtil.isExpired(it.endDate, expireTime)
            val canExperience = userSet.contains(userId) || userId == it.creator
            val canEdit = experiencePermissionListMap[AuthPermission.EDIT]!!.contains(it.id)
            ExperienceSummaryWithPermission(
                    HashUtil.encodeLongId(it.id),
                    it.name,
                    Platform.valueOf(it.platform),
                    it.version,
                    it.remark ?: "",
                    it.endDate.timestamp(),
                    Source.valueOf(it.source),
                    it.creator,
                    isExpired,
                    it.online,
                    ExperiencePermission(canExperience, canEdit)
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
                experienceRecord.name,
                experienceRecord.artifactoryPath,
                ArtifactoryType.valueOf(experienceRecord.artifactoryType),
                Platform.valueOf(experienceRecord.platform),
                experienceRecord.version,
                experienceRecord.remark ?: "",
                experienceRecord.createTime.timestamp(),
                experienceRecord.endDate.timestamp(),
                groupList,
                objectMapper.readValue(experienceRecord.innerUsers),
                experienceRecord.outerUsers,
                objectMapper.readValue(experienceRecord.notifyTypes),
                experienceRecord.enableWechatGroups ?: true,
                experienceRecord.wechatGroups ?: "",
                experienceRecord.creator,
                isExpired,
                canExperience,
                experienceRecord.online,
                url
        )
    }

    fun create(userId: String, projectId: String, experience: ExperienceCreate) {
        experience.experienceGroups.forEach {
            if (!groupService.serviceCheck(it)) {
                throw NotFoundException("体验组($it)不存在")
            }
        }
        if (!hasArtifactoryPermission(userId, projectId, experience.path, experience.artifactoryType)) {
            throw PermissionForbiddenException("用户在项目($projectId)下没有流水线执行权限")
        }

        val platform = if (experience.path.endsWith(".ipa")) Platform.IOS else Platform.ANDROID
        val artifactorySha1 = makeSha1(experience.artifactoryType, experience.path)
        val source = Source.WEB

        val path = experience.path
        val artifactoryType = com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(experience.artifactoryType.name)
        val properties = client.get(ServiceArtifactoryResource::class).properties(projectId, artifactoryType, path).data!!
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
        val appBundleIdentifier = propertyMap[ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER]!!
        val appVersion = propertyMap[ARCHIVE_PROPS_APP_VERSION]!!

        val experienceId = experienceDao.create(dslContext,
                projectId,
                experience.name,
                platform.name,
                experience.path,
                experience.artifactoryType.name,
                artifactorySha1,
                appBundleIdentifier,
                appVersion,
                experience.remark,
                LocalDateTime.ofInstant(Instant.ofEpochSecond(experience.expireDate), ZoneId.systemDefault()),
                objectMapper.writeValueAsString(experience.experienceGroups),
                objectMapper.writeValueAsString(experience.innerUsers),
                experience.outerUsers,
                objectMapper.writeValueAsString(experience.notifyTypes),
                experience.enableWechatGroups,
                experience.wechatGroups ?: "",
                true,
                source.name,
                userId,
                userId
        )
        createTaskResource(userId, projectId, experienceId, "${experience.name}（${experience.version}）")
        sendNotification(experienceId)
    }

    fun edit(userId: String, projectId: String, experienceHashId: String, experience: ExperienceUpdate) {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        validateTaskPermission(userId, projectId, experienceId, AuthPermission.EDIT, "用户在项目($projectId)下没有体验($experienceHashId)的编辑权限")
        if (experienceDao.getOrNull(dslContext, experienceId) == null) {
            throw NotFoundException("体验($experienceHashId)不存在")
        }
        experience.experienceGroups.forEach {
            if (!groupService.serviceCheck(it)) {
                throw NotFoundException("体验组($it)不存在")
            }
        }

        experienceDao.update(dslContext,
                experienceId,
                experience.name,
                experience.remark,
                LocalDateTime.ofInstant(Instant.ofEpochSecond(experience.expireDate), ZoneId.systemDefault()),
                objectMapper.writeValueAsString(experience.experienceGroups),
                objectMapper.writeValueAsString(experience.innerUsers),
                experience.outerUsers,
                objectMapper.writeValueAsString(experience.notifyTypes),
                experience.enableWechatGroups,
                experience.wechatGroups ?: "",
                userId
        )
        sendNotification(experienceId)
    }

    fun updateOnline(userId: String, projectId: String, experienceHashId: String, online: Boolean) {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        validateTaskPermission(userId, projectId, experienceId, AuthPermission.EDIT, "用户在项目($projectId)下没有体验($experienceHashId)的编辑权限")
        if (experienceDao.getOrNull(dslContext, experienceId) == null) {
            throw NotFoundException("体验($experienceHashId)不存在")
        }
        experienceDao.updateOnline(dslContext, experienceId, online)
    }

    fun externalUrl(userId: String, projectId: String, experienceHashId: String): String {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id
        if (!userCanExperience(userId, experienceId)) {
            throw PermissionForbiddenException("用户($userId)不在体验用户名单中")
        }
        return shortUrlApi.getShortUrl("${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=experienceDetail&experienceId=$experienceHashId", 24*3600*3)
    }

    fun downloadUrl(userId: String, projectId: String, experienceHashId: String): String {
        val experienceRecord = experienceDao.get(dslContext, HashUtil.decodeIdToLong(experienceHashId))
        val experienceId = experienceRecord.id
        if (!userCanExperience(userId, experienceId)) {
            throw PermissionForbiddenException("用户($userId)不在体验用户名单中")
        }
        return experienceDownloadService.serviceGetInnerDownloadUrl(userId, experienceId)
    }

    fun serviceCreate(userId: String, projectId: String, experience: ExperienceServiceCreate) {
        val path = experience.path
        val artifactoryType = com.tencent.devops.artifactory.pojo.enums.ArtifactoryType.valueOf(experience.artifactoryType.name)
        if (!client.get(ServiceArtifactoryResource::class).check(projectId, artifactoryType, path).data!!) {
            throw RuntimeException("文件($path)不存在")
        }

        val platform = if (path.endsWith(".ipa")) Platform.IOS else Platform.ANDROID
        val artifactorySha1 = makeSha1(experience.artifactoryType, path)
        val source = Source.PIPELINE

        val properties = client.get(ServiceArtifactoryResource::class).properties(projectId, artifactoryType, path).data!!
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
        if (!propertyMap.containsKey(ARCHIVE_PROPS_BUILD_NO)) {
            throw RuntimeException("元数据buildNo不存在")
        }
        val name = path.split("/").last()
        val appBundleIdentifier = propertyMap[ARCHIVE_PROPS_APP_BUNDLE_IDENTIFIER]!!
        val appVersion = propertyMap[ARCHIVE_PROPS_APP_VERSION]!!
        val buildNo = propertyMap[ARCHIVE_PROPS_BUILD_NO]!!
        val remark = "构建号#$buildNo"

        val experienceId = experienceDao.create(dslContext,
                projectId,
                name,
                platform.name,
                experience.path,
                experience.artifactoryType.name,
                artifactorySha1,
                appBundleIdentifier,
                appVersion,
                remark,
                LocalDateTime.ofInstant(Instant.ofEpochSecond(experience.expireDate), ZoneId.systemDefault()),
                objectMapper.writeValueAsString(experience.experienceGroups),
                objectMapper.writeValueAsString(experience.innerUsers),
                experience.outerUsers,
                objectMapper.writeValueAsString(experience.notifyTypes),
                experience.enableWechatGroups,
                experience.wechatGroups,
                true,
                source.name,
                userId,
                userId
        )
        createTaskResource(userId, projectId, experienceId, "$name（$appVersion）")
        sendNotification(experienceId)
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
            val message = EmailUtil.makeMessage(userId, projectName, name, version, innerUrl, receivers.toSet())
            client.get(ServiceNotifyResource::class).sendEmailNotify(message)
        }

        receivers.forEach {
            if (notifyTypeList.contains(NotifyType.RTX)) {
                val message = RtxUtil.makeMessage(projectName, name, version, innerUrl, outerUrl, setOf(it))
                client.get(ServiceNotifyResource::class).sendRtxNotify(message)
            }
            if (notifyTypeList.contains(NotifyType.WECHAT)) {
                val message = WechatUtil.makeMessage(projectName, name, version, innerUrl, outerUrl, setOf(it))
                client.get(ServiceNotifyResource::class).sendWechatNotify(message)
            }
        }
        if (experienceRecord.enableWechatGroups && !experienceRecord.wechatGroups.isNullOrBlank()) {
            val wechatGroupList = regex.split(experienceRecord.wechatGroups)
            wechatGroupList.forEach {
                val message = WechatGroupUtil.makeRichtextMessage(projectName, name, version, innerUrl, outerUrl, it)
                wechatWorkService.sendRichText(message)
            }
        }
    }

    private fun validateTaskPermission(user: String, projectId: String, experienceId: Long, authPermission: AuthPermission, message: String) {
        if (!bsAuthPermissionApi.validateUserResourcePermission(user, experienceServiceCode, taskResourceType, projectId, HashUtil.encodeLongId(experienceId), authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    private fun createTaskResource(user: String, projectId: String, experienceId: Long, experienceName: String) {
        bsAuthResourceApi.createResource(user, experienceServiceCode, taskResourceType, projectId, HashUtil.encodeLongId(experienceId), experienceName)
    }

    private fun filterExperience(user: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bsAuthPermissionApi.getUserResourcesByPermissions(user, experienceServiceCode, taskResourceType, projectId, authPermissions, null)
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { key, value ->
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
        return shortUrlApi.getShortUrl("${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=experienceDetail&experienceId=$experienceHashId", 24*3600*30)
    }

    fun userCanExperience(userId: String, experienceId: Long): Boolean {
        val experienceRecord = experienceDao.get(dslContext, experienceId)
        val experienceGroups = objectMapper.readValue<Set<String>>(experienceRecord.experienceGroups)
        val innerUsers = objectMapper.readValue<Set<String>>(experienceRecord.innerUsers)

        val allUsers = mutableSetOf<String>()
        allUsers.addAll(innerUsers)
        val groupUserMap = groupService.serviceGet(experienceGroups)
        groupUserMap.forEach { _, value ->
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