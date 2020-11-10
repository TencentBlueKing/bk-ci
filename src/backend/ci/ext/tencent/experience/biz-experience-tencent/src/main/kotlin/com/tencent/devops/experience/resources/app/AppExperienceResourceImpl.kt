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

package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceResource
import com.tencent.devops.experience.pojo.AppExperience
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceCreate
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.service.ExperienceAppService
import com.tencent.devops.experience.service.ExperienceService
import com.tencent.devops.experience.service.GroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppExperienceResourceImpl @Autowired constructor(
    private val experienceAppService: ExperienceAppService,
    private val experienceService: ExperienceService,
    private val groupService: GroupService
) : AppExperienceResource {
    override fun list(userId: String, page: Int?, pageSize: Int?): Result<List<AppExperience>> {
        checkParam(userId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
        val result = experienceAppService.list(userId, offset, pageSizeNotNull, true)
        return Result(result)
    }

    override fun listV2(userId: String, page: Int?, pageSize: Int?): Result<List<AppExperience>> {
        checkParam(userId)
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: -1
        val offset = if (pageSizeNotNull == -1) 0 else (pageNotNull - 1) * pageSizeNotNull
        val result = experienceAppService.list(userId, offset, pageSizeNotNull, false)
        return Result(result)
    }

    override fun detail(userId: String, platform: Int?, experienceHashId: String): Result<AppExperienceDetail> {
        checkParam(userId, experienceHashId)
        val result = experienceAppService.detail(userId, experienceHashId, platform)
        return Result(result)
    }

    override fun downloadUrl(userId: String, experienceHashId: String): Result<DownloadUrl> {
        checkParam(userId, experienceHashId)
        val result = experienceAppService.downloadUrl(userId, experienceHashId)
        return Result(result)
    }

    override fun history(userId: String, projectId: String): Result<List<AppExperienceSummary>> {
        checkParam(userId)
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val result = experienceAppService.history(userId, projectId)
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
}