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

package com.tencent.devops.experience.resources.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.user.UserExperienceResource
import com.tencent.devops.experience.pojo.Experience
import com.tencent.devops.experience.pojo.ExperienceCount
import com.tencent.devops.experience.pojo.ExperienceCreate
import com.tencent.devops.experience.pojo.ExperienceSummaryWithPermission
import com.tencent.devops.experience.pojo.ExperienceUpdate
import com.tencent.devops.experience.pojo.ExperienceUserCount
import com.tencent.devops.experience.pojo.Url
import com.tencent.devops.experience.pojo.enums.ArtifactoryType
import com.tencent.devops.experience.pojo.outer.OuterSelectorVO
import com.tencent.devops.experience.service.ExperienceDownloadService
import com.tencent.devops.experience.service.ExperienceOuterService
import com.tencent.devops.experience.service.ExperienceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@SuppressWarnings("TooManyFunctions")
class UserExperienceResourceImpl @Autowired constructor(
    private val experienceService: ExperienceService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val experienceOuterService: ExperienceOuterService
) : UserExperienceResource {
    override fun hasArtifactoryPermission(
        userId: String,
        projectId: String,
        path: String,
        artifactoryType: ArtifactoryType
    ): Result<Boolean> {
        checkParam(userId, projectId)
        return Result(experienceService.hasArtifactoryPermission(userId, projectId, path, artifactoryType))
    }

    override fun list(
        userId: String,
        projectId: String,
        expired: Boolean?
    ): Result<List<ExperienceSummaryWithPermission>> {
        checkParam(userId, projectId)
        return Result(experienceService.list(userId, projectId, expired))
    }

    override fun get(userId: String, projectId: String, experienceHashId: String): Result<Experience> {
        checkParam(userId, projectId, experienceHashId)
        return Result(experienceService.get(userId, experienceHashId))
    }

    override fun create(userId: String, projectId: String, experience: ExperienceCreate): Result<Boolean> {
        checkParam(userId, projectId)
        experienceService.create(userId, projectId, experience)
        return Result(true)
    }

    override fun edit(
        userId: String,
        projectId: String,
        experienceHashId: String,
        experience: ExperienceUpdate
    ): Result<Boolean> {
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
        val result = experienceDownloadService.downloadCount(experienceHashId)
        return Result(result)
    }

    override fun downloadUserCount(
        userId: String,
        projectId: String,
        experienceHashId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ExperienceUserCount>> {
        checkParam(userId, projectId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) -1 else (pageNotNull - 1) * pageSizeNotNull
        val result =
            experienceDownloadService.downloadUserCount(userId, projectId, experienceHashId, pageNotNull, offset)
        return Result(Page(pageNotNull, pageSizeNotNull, result.first, result.second))
    }

    override fun externalUrl(userId: String, projectId: String, experienceHashId: String): Result<Url> {
        checkParam(userId, projectId)
        val url = experienceService.externalUrl(userId, experienceHashId)
        return Result(Url(url))
    }

    override fun downloadUrl(userId: String, projectId: String, experienceHashId: String): Result<Url> {
        checkParam(userId, projectId)
        val url = experienceService.downloadUrl(userId, experienceHashId)
        return Result(Url(url))
    }

    override fun outerList(userId: String, projectId: String): Result<List<OuterSelectorVO>> {
        return Result(experienceOuterService.outerList(projectId).map { OuterSelectorVO(it) })
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
