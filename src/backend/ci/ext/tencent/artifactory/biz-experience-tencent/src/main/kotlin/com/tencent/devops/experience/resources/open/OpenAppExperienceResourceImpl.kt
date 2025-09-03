package com.tencent.devops.experience.resources.open

import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.experience.api.app.AppExperienceResource
import com.tencent.devops.experience.api.open.OpenAppExperienceResource
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceInstallPackage
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.ExperienceLastParams
import com.tencent.devops.experience.pojo.ExperienceList
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.outer.OuterSelectorVO
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpenAppExperienceResourceImpl @Autowired constructor(
    private val appExperienceResource: AppExperienceResource
) : OpenAppExperienceResource {

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun listV3(
        token: String,
        userId: String,
        platform: Int,
        organization: String?
    ): Result<ExperienceList> {
        return appExperienceResource.listV3(userId, platform, organization)
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun detail(
        token: String,
        userId: String,
        platform: Int,
        appVersion: String?,
        organization: String?,
        experienceHashId: String,
        forceNew: Boolean
    ): Result<AppExperienceDetail> {
        return appExperienceResource.detail(
            userId = userId,
            platform = platform,
            appVersion = appVersion,
            organization = organization,
            experienceHashId = experienceHashId,
            forceNew = forceNew
        )
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun changeLog(
        token: String,
        userId: String,
        organization: String?,
        experienceHashId: String,
        page: Int,
        pageSize: Int,
        showAll: Boolean?,
        name: String?,
        version: String?,
        remark: String?,
        createDateBegin: Long?,
        createDateEnd: Long?,
        endDateBegin: Long?,
        endDateEnd: Long?,
        creator: String?
    ): Result<Pagination<ExperienceChangeLog>> {
        return appExperienceResource.changeLog(
            userId = userId,
            organization = organization,
            experienceHashId = experienceHashId,
            page = page,
            pageSize = pageSize,
            showAll = showAll,
            name = name,
            version = version,
            remark = remark,
            createDateBegin = createDateBegin,
            createDateEnd = createDateEnd,
            endDateBegin = endDateBegin,
            endDateEnd = endDateEnd,
            creator = creator
        )
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun downloadUrl(
        token: String,
        userId: String,
        organization: String?,
        experienceHashId: String
    ): Result<DownloadUrl> {
        return appExperienceResource.downloadUrl(userId, organization, experienceHashId)
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun history(
        token: String,
        userId: String,
        appVersion: String?,
        projectId: String
    ): Result<List<AppExperienceSummary>> {
        return appExperienceResource.history(userId, appVersion, projectId)
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun projectGroupAndUsers(
        token: String,
        userId: String,
        projectId: String
    ): Result<List<ProjectGroupAndUsers>> {
        return appExperienceResource.projectGroupAndUsers(userId, projectId)
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun lastParams(
        token: String,
        userId: String,
        name: String,
        projectId: String,
        bundleIdentifier: String
    ): Result<ExperienceLastParams> {
        return appExperienceResource.lastParams(userId, name, projectId, bundleIdentifier)
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun outerList(
        token: String,
        userId: String,
        projectId: String
    ): Result<List<OuterSelectorVO>> {
        return appExperienceResource.outerList(userId, projectId)
    }

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun installPackages(
        token: String,
        userId: String,
        platform: Int,
        appVersion: String?,
        organization: String?,
        experienceHashId: String
    ): Result<Pagination<AppExperienceInstallPackage>> {
        return appExperienceResource.installPackages(
            userId = userId,
            platform = platform,
            appVersion = appVersion,
            organization = organization,
            experienceHashId = experienceHashId
        )
    }
}