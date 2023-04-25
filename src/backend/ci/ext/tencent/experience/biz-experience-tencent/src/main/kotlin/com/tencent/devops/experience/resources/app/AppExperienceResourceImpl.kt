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

package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceResource
import com.tencent.devops.experience.filter.annotions.AllowOuter
import com.tencent.devops.experience.pojo.AppExperience
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceInstallPackage
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.ExperienceCreate
import com.tencent.devops.experience.pojo.ExperienceLastParams
import com.tencent.devops.experience.pojo.ExperienceList
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.outer.OuterSelectorVO
import com.tencent.devops.experience.service.ExperienceAppService
import com.tencent.devops.experience.service.ExperienceOuterService
import com.tencent.devops.experience.service.ExperienceService
import com.tencent.devops.experience.service.GroupService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppExperienceResourceImpl @Autowired constructor(
    private val experienceAppService: ExperienceAppService,
    private val experienceService: ExperienceService,
    private val groupService: GroupService,
    private val experienceOuterService: ExperienceOuterService
) : AppExperienceResource {
    override fun list(userId: String, page: Int?, pageSize: Int?): Result<List<AppExperience>> {
        checkParam(userId)
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10000
        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
        val result = experienceAppService.list(userId, offset, pageSizeNotNull, true)
        return Result(result.records)
    }

    @AllowOuter
    override fun listV2(
        userId: String,
        platform: Int,
        organization: String?,
        page: Int,
        pageSize: Int
    ): Result<Pagination<AppExperience>> {
        checkParam(userId)
        val offset = if (pageSize == -1) 0 else (page - 1) * pageSize
        val result = experienceAppService.list(userId, offset, pageSize, false, platform, organization)
        return Result(result)
    }

    @AllowOuter
    override fun listV3(userId: String, platform: Int, organization: String?): Result<ExperienceList> {
        logger.debug("listV3 , userId:$userId , platform:$platform , organization:$organization")
        val privateExperiences = experienceAppService.list(userId, 0, 100, true, platform, organization).records
        val publicExperiences = if (null == organization) {
            experienceAppService.publicExperiences(userId, platform, 0, 100)
        } else {
            emptyList()
        }
        val redPointCount = privateExperiences.count { it.redPointEnabled } +
                publicExperiences.count { it.redPointEnabled }
        return Result(ExperienceList(privateExperiences, publicExperiences, redPointCount))
    }

    @AllowOuter
    override fun detail(
        userId: String,
        platform: Int,
        appVersion: String?,
        organization: String?,
        experienceHashId: String,
        forceNew: Boolean
    ): Result<AppExperienceDetail> {
        checkParam(userId, experienceHashId)
        val result = experienceAppService.detail(userId, experienceHashId, platform, appVersion, organization, forceNew)
        return Result(result)
    }

    @AllowOuter
    override fun changeLog(
        userId: String,
        organization: String?,
        experienceHashId: String,
        page: Int,
        pageSize: Int,
        showAll: Boolean?
    ): Result<Pagination<ExperienceChangeLog>> {
        checkParam(userId, experienceHashId)
        val result = experienceAppService.changeLog(userId, experienceHashId, page, pageSize, organization, showAll)
        return Result(result)
    }

    @AllowOuter
    override fun downloadUrl(userId: String, organization: String?, experienceHashId: String): Result<DownloadUrl> {
        checkParam(userId, experienceHashId)
        val result = experienceAppService.downloadUrl(userId, experienceHashId, organization)
        return Result(result)
    }

    override fun history(userId: String, appVersion: String?, projectId: String): Result<List<AppExperienceSummary>> {
        checkParam(userId)
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val result = experienceAppService.history(userId, appVersion, projectId)
        return Result(result)
    }

    override fun projectGroupAndUsers(userId: String, projectId: String): Result<List<ProjectGroupAndUsers>> {
        checkParam(userId)
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val result = groupService.getProjectGroupAndUsers(userId, projectId)
        return Result(result)
    }

    override fun create(userId: String, projectId: String, experience: ExperienceCreate): Result<Boolean> {
        checkParam(userId, projectId)
        experienceService.create(userId, projectId, experience)
        return Result(true)
    }

    override fun outerList(userId: String, projectId: String): Result<List<OuterSelectorVO>> {
        return Result(experienceOuterService.outerList(projectId).map { OuterSelectorVO(it) })
    }

    @AllowOuter
    override fun installPackages(
        userId: String,
        platform: Int,
        appVersion: String?,
        organization: String?,
        experienceHashId: String
    ): Result<Pagination<AppExperienceInstallPackage>> {
        return Result(
            experienceAppService.installPackages(
                userId = userId,
                platform = platform,
                appVersion = appVersion,
                organization = organization,
                experienceHashId = experienceHashId
            )
        )
    }

    override fun lastParams(
        userId: String,
        name: String,
        projectId: String,
        bundleIdentifier: String
    ): Result<ExperienceLastParams> {
        val lastParams = experienceService.lastParams(userId, name, projectId, bundleIdentifier)
        return if (null == lastParams) {
            Result(ExperienceLastParams(false, null))
        } else {
            Result(ExperienceLastParams(true, lastParams))
        }
    }

    fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }

    fun checkParam(userId: String, experienceHashId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (experienceHashId.isBlank()) {
            throw ParamBlankException("Invalid experienceHashId")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AppExperienceResourceImpl::class.java)
    }
}
