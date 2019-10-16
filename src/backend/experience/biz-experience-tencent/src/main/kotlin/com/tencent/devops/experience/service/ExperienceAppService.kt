package com.tencent.devops.experience.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.api.BkAuthServiceCode
import com.tencent.devops.common.auth.api.BSCCProjectApi
import com.tencent.devops.common.auth.api.pojo.BkAuthProject
import com.tencent.devops.common.auth.code.BSExperienceAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.pojo.AppExperience
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.enums.Platform
import com.tencent.devops.experience.pojo.enums.Source
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class ExperienceAppService(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val bsCCProjectApi: BSCCProjectApi,
    private val bsAuthProjectApi: BSAuthProjectApi,
    private val experienceDao: ExperienceDao,
    private val groupService: GroupService,
    private val experienceService: ExperienceService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val experienceServiceCode: BSExperienceAuthServiceCode,
    private val client: Client
) {
    fun list(userId: String, offset: Int, limit: Int): List<AppExperience> {
        val expireTime = DateUtil.today()

        val projectIdList = mutableListOf<String>()
        // 用户所在项目
        val userProjectIdList = bsAuthProjectApi.getUserProjects(experienceServiceCode, userId, null)
        projectIdList.addAll(userProjectIdList)
        // 用户所在的内部用户（inner user）体验的项目列表
        val innerUserProjectIdList = experienceDao.getProjectIdByInnerUser(dslContext, userId, expireTime, true)?.map { it.value1() }
        if (innerUserProjectIdList != null) {
            projectIdList.addAll(innerUserProjectIdList)
        }

        // 用户所在的体验组的项目列表
        val groupUserProjectIdList = experienceDao.getProjectIdByGroupUser(dslContext, userId)?.map { it.value1() }

        if (groupUserProjectIdList != null) {
            projectIdList.addAll(groupUserProjectIdList)
        }

        val experienceIdList = experienceDao.listIDGroupByProjectIdAndBundleIdentifier(dslContext, projectIdList.distinct().toSet(), expireTime, true)
        val experienceList = experienceDao.list(dslContext, experienceIdList.map { it.value1() }.toSet())

        val groupIdSet = mutableSetOf<String>()
        experienceList.forEach {
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            groupIdSet.addAll(experienceGroups)
        }
        val groupMap = groupService.serviceGet(groupIdSet)

        val userCanExperienceList = mutableListOf<TExperienceRecord>()
        experienceList.forEach {
            val userSet = mutableSetOf<String>()
            val innerUsers = objectMapper.readValue<Set<String>>(it.innerUsers)
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            userSet.addAll(innerUsers)
            experienceGroups.forEach {
                if (groupMap.containsKey(it)) {
                    userSet.addAll(groupMap[it]!!.innerUsers)
                }
            }

            if (userSet.contains(userId) || userId == it.creator) {
                userCanExperienceList.add(it)
            }
        }

        val subUserCanExperienceList = if (limit == -1) {
            userCanExperienceList
        } else {
            if (offset >= userCanExperienceList.size) {
                emptyList<TExperienceRecord>()
            } else {
                val toIndex = if ((offset + limit) >= userCanExperienceList.size) userCanExperienceList.size else offset + limit
                userCanExperienceList.subList(offset, toIndex)
            }
        }

        if (subUserCanExperienceList.isEmpty()) {
            return emptyList()
        }
        val projectIds = subUserCanExperienceList.map { it.projectId }.toSet()
        val projectMap = mutableMapOf<String, BkAuthProject>()
        bsCCProjectApi.getProjectListAsOuter(projectIds).forEach {
            projectMap[it.projectCode] = it
        }

        return subUserCanExperienceList.map {
            val projectId = it.projectId
            val logoUrl = projectMap[projectId]!!.logoAddr
            val projectName = projectMap[projectId]!!.projectName
            AppExperience(
                    HashUtil.encodeLongId(it.id),
                    Platform.valueOf(it.platform),
                    Source.valueOf(it.source),
                    logoUrl,
                    projectName,
                    it.version,
                    it.bundleIdentifier
            )
        }
    }

    fun detail(userId: String, experienceHashId: String): AppExperienceDetail {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        val experience = experienceDao.get(dslContext, experienceId)
        val projectId = experience.projectId
        val bundleIdentifier = experience.bundleIdentifier

        val isExpired = DateUtil.isExpired(experience.endDate)
        val canExperience = experienceService.userCanExperience(userId, experienceId)

        val bkAuthProject = bsCCProjectApi.getProjectListAsOuter(setOf(projectId)).first()
        val logoUrl = bkAuthProject.logoAddr
        val projectName = bkAuthProject.projectName
        val version = experience.version
        val shareUrl = "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=experienceDetail&experienceId=$experienceHashId"

        val path = experience.artifactoryPath
        val artifactoryType = ArtifactoryType.valueOf(experience.artifactoryType)
        val fileDetail = client.get(ServiceArtifactoryResource::class).show(projectId, artifactoryType, path).data!!

        val experienceList = experienceDao.listByBundleIdentifier(dslContext, projectId, bundleIdentifier)
        val changeLog = experienceList.map {
            ExperienceChangeLog(
                    HashUtil.encodeLongId(it.id),
                    it.version,
                    it.creator,
                    it.createTime.timestamp(),
                    it.remark ?: ""
            )
        }
        return AppExperienceDetail(
                experienceHashId,
                fileDetail.size,
                logoUrl,
                shareUrl,
                projectName,
                Platform.valueOf(experience.platform),
                version,
                isExpired,
                canExperience,
                experience.online,
                changeLog
        )
    }

    fun downloadUrl(userId: String, experienceHashId: String): DownloadUrl {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        return experienceDownloadService.serviceGetExternalDownloadUrl(userId, experienceId)
    }

    fun downloadPlistUrl(userId: String, experienceHashId: String): DownloadUrl {
        val experienceId = HashUtil.decodeIdToLong(experienceHashId)
        return experienceDownloadService.serviceGetExternalPlistUrl(userId, experienceId)
    }

    fun history(userId: String, projectId: String): List<AppExperienceSummary> {
        val expireTime = DateUtil.today()
        val experienceList = experienceDao.list(dslContext, projectId, null, null)

        val bkAuthProjectList = bsCCProjectApi.getProjectListAsOuter(setOf(projectId))
        if (bkAuthProjectList.isEmpty()) {
            throw RuntimeException("ProjectId $projectId cannot find in CC")
        }
        val logoUrl = bkAuthProjectList.first().logoAddr

        val groupIdSet = mutableSetOf<String>()
        experienceList.forEach {
            val experienceGroups = objectMapper.readValue<Set<String>>(it.experienceGroups)
            groupIdSet.addAll(experienceGroups)
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

            AppExperienceSummary(
                    HashUtil.encodeLongId(it.id),
                    it.name,
                    Platform.valueOf(it.platform),
                    it.version,
                    it.remark ?: "",
                    it.endDate.timestamp(),
                    Source.valueOf(it.source),
                    logoUrl,
                    it.creator,
                    isExpired,
                    canExperience,
                    it.online
            )
        }
    }
}
