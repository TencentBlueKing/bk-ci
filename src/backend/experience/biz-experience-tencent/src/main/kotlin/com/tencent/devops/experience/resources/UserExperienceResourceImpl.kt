package com.tencent.devops.experience.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.UserExperienceResource
import com.tencent.devops.experience.pojo.Experience
import com.tencent.devops.experience.pojo.ExperienceCount
import com.tencent.devops.experience.pojo.ExperienceCreate
import com.tencent.devops.experience.pojo.ExperienceSummaryWithPermission
import com.tencent.devops.experience.pojo.ExperienceUpdate
import com.tencent.devops.experience.pojo.ExperienceUserCount
import com.tencent.devops.experience.pojo.Url
import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import com.tencent.devops.experience.service.ExperienceDownloadService
import com.tencent.devops.experience.service.ExperienceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExperienceResourceImpl @Autowired constructor(
    private val experienceService: ExperienceService,
    private val experienceDownloadService: ExperienceDownloadService
) : UserExperienceResource {
    override fun hasArtifactoryPermission(userId: String, projectId: String, path: String, artifactoryType: ArtifactoryType): Result<Boolean> {
        checkParam(userId, projectId)
        return Result(experienceService.hasArtifactoryPermission(userId, projectId, path, artifactoryType))
    }

    override fun list(userId: String, projectId: String, expired: Boolean?): Result<List<ExperienceSummaryWithPermission>> {
        checkParam(userId, projectId)
        return Result(experienceService.list(userId, projectId, expired))
    }

    override fun get(userId: String, projectId: String, experienceHashId: String): Result<Experience> {
        checkParam(userId, projectId, experienceHashId)
        return Result(experienceService.get(userId, projectId, experienceHashId))
    }

    override fun create(userId: String, projectId: String, experience: ExperienceCreate): Result<Boolean> {
        checkParam(userId, projectId)
        experienceService.create(userId, projectId, experience)
        return Result(true)
    }

    override fun edit(userId: String, projectId: String, experienceHashId: String, experience: ExperienceUpdate): Result<Boolean> {
        checkParam(userId, projectId, experienceHashId)
        experienceService.edit(userId, projectId, experienceHashId, experience)
        return Result(true)
    }

    override fun offline(userId: String, projectId: String, experienceHashId: String): Result<Boolean> {
        checkParam(userId, projectId, experienceHashId)
        experienceService.updateOnline(userId, projectId, experienceHashId, false)
        return Result(true)
    }

    override fun downloadCount(userId: String, projectId: String, experienceHashId: String): Result<ExperienceCount> {
        checkParam(userId, projectId)
        val result = experienceDownloadService.downloadCount(userId, projectId, experienceHashId)
        return Result(result)
    }

    override fun downloadUserCount(userId: String, projectId: String, experienceHashId: String, page: Int?, pageSize: Int?): Result<Page<ExperienceUserCount>> {
        checkParam(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) -1 else (pageNotNull - 1) * pageSizeNotNull
        val result = experienceDownloadService.downloadUserCount(userId, projectId, experienceHashId, pageNotNull, offset)
        return Result(Page(pageNotNull, pageSizeNotNull, result.first, result.second))
    }

    override fun externalUrl(userId: String, projectId: String, experienceHashId: String): Result<Url> {
        checkParam(userId, projectId)
        val url = experienceService.externalUrl(userId, projectId, experienceHashId)
        return Result(Url(url))
    }

    override fun downloadUrl(userId: String, projectId: String, experienceHashId: String): Result<Url> {
        checkParam(userId, projectId)
        val url = experienceService.downloadUrl(userId, projectId, experienceHashId)
        return Result(Url(url))
    }

    fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    fun checkParam(userId: String, projectId: String, experienceHashId: String) {
        checkParam(userId, projectId)
        if (experienceHashId.isBlank()) {
            throw ParamBlankException("Invalid experienceHashId")
        }
    }
}