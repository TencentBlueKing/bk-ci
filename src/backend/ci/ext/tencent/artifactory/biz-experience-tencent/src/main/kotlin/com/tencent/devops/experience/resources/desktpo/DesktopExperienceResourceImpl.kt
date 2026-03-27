package com.tencent.devops.experience.resources.desktpo

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.desktop.DesktopExperienceResource
import com.tencent.devops.experience.filter.annotions.AllowOuter
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceInstallPackage
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.ExperienceList
import com.tencent.devops.experience.service.ExperienceAppService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class DesktopExperienceResourceImpl @Autowired constructor(
    private val experienceAppService: ExperienceAppService
) : DesktopExperienceResource {

    @AllowOuter
    override fun list(
        userId: String,
        organization: String?
    ): Result<ExperienceList> {
        checkUserId(userId)
        return Result(experienceAppService.listForDesktop(userId, DEFAULT_PLATFORM, organization))
    }

    @AllowOuter
    override fun detail(
        userId: String,
        experienceHashId: String,
        organization: String?
    ): Result<AppExperienceDetail> {
        checkParam(userId, experienceHashId)
        val result = experienceAppService.detail(
            userId = userId,
            experienceHashId = experienceHashId,
            platform = DEFAULT_PLATFORM,
            appVersion = DEFAULT_APP_VERSION,
            organization = organization,
            forceNew = true,
            enablePublicAccess = true
        )
        return Result(result)
    }

    @AllowOuter
    override fun changeLog(
        userId: String,
        experienceHashId: String,
        page: Int,
        pageSize: Int,
        version: String?,
        organization: String?,
        createDateBegin: Long?,
        createDateEnd: Long?,
        endDateBegin: Long?,
        endDateEnd: Long?,
    ): Result<Pagination<ExperienceChangeLog>> {
        checkParam(userId, experienceHashId)
        val result = experienceAppService.changeLog(
            userId = userId,
            experienceHashId = experienceHashId,
            page = page,
            pageSize = pageSize,
            organization = organization,
            showAll = null,
            name = null,
            version = version,
            remark = null,
            createDateBegin = createDateBegin,
            createDateEnd = createDateEnd,
            endDateBegin = endDateBegin,
            endDateEnd = endDateEnd,
            creator = null,
            enablePublicAccess = true
        )
        return Result(result)
    }

    @AllowOuter
    override fun installPackages(
        userId: String,
        experienceHashId: String,
        organization: String?
    ): Result<Pagination<AppExperienceInstallPackage>> {
        checkParam(userId, experienceHashId)
        return Result(
            experienceAppService.installPackages(
                userId = userId,
                platform = DEFAULT_PLATFORM,
                appVersion = DEFAULT_APP_VERSION,
                organization = organization,
                experienceHashId = experienceHashId,
                enablePublicAccess = true
            )
        )
    }

    @AllowOuter
    override fun downloadUrl(
        userId: String,
        experienceHashId: String,
        organization: String?
    ): Result<DownloadUrl> {
        checkParam(userId, experienceHashId)
        val result = experienceAppService.downloadUrl(
            userId = userId,
            experienceHashId = experienceHashId,
            organization = organization,
            enablePublicAccess = true
        )
        return Result(result)
    }

    private fun checkUserId(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    private fun checkParam(userId: String, experienceHashId: String) {
        checkUserId(userId)
        if (experienceHashId.isBlank()) {
            throw ParamBlankException("Invalid experienceHashId")
        }
    }

    companion object {
        // 桌面端默认平台为WIN
        private const val DEFAULT_PLATFORM = 4
        private const val DEFAULT_APP_VERSION = "2.0.0"
    }
}
