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

package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.service.ServiceExperienceResource
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceInstallPackage
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.ExperienceLastParams
import com.tencent.devops.experience.pojo.ExperienceList
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.outer.OuterSelectorVO
import com.tencent.devops.openapi.api.apigw.v4.ApigwExperienceResourceV4
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwExperienceResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwExperienceResourceV4 {

    override fun listV3(
        appCode: String?,
        apigwType: String?,
        userId: String,
        platform: Int,
        organization: String?
    ): Result<ExperienceList> {
        return client.get(ServiceExperienceResource::class).listV3(userId, platform, organization)
    }

    override fun detail(
        appCode: String?,
        apigwType: String?,
        userId: String,
        platform: Int,
        appVersion: String?,
        organization: String?,
        experienceHashId: String,
        forceNew: Boolean
    ): Result<AppExperienceDetail> {
        return client.get(ServiceExperienceResource::class).detail(
            userId = userId,
            platform = platform,
            appVersion = appVersion,
            organization = organization,
            experienceHashId = experienceHashId,
            forceNew = forceNew
        )
    }

    override fun changeLog(
        appCode: String?,
        apigwType: String?,
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
        return client.get(ServiceExperienceResource::class).changeLog(
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

    override fun downloadUrl(
        appCode: String?,
        apigwType: String?,
        userId: String,
        organization: String?,
        experienceHashId: String
    ): Result<DownloadUrl> {
        return client.get(ServiceExperienceResource::class).downloadUrl(userId, organization, experienceHashId)
    }

    override fun history(
        appCode: String?,
        apigwType: String?,
        userId: String,
        appVersion: String?,
        projectId: String
    ): Result<List<AppExperienceSummary>> {
        return client.get(ServiceExperienceResource::class).history(userId, appVersion, projectId)
    }

    override fun projectGroupAndUsers(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<List<ProjectGroupAndUsers>> {
        return client.get(ServiceExperienceResource::class).projectGroupAndUsers(userId, projectId)
    }

    override fun lastParams(
        appCode: String?,
        apigwType: String?,
        userId: String,
        name: String,
        projectId: String,
        bundleIdentifier: String
    ): Result<ExperienceLastParams> {
        return client.get(ServiceExperienceResource::class).lastParams(userId, name, projectId, bundleIdentifier)
    }

    override fun outerList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<List<OuterSelectorVO>> {
        return client.get(ServiceExperienceResource::class).outerList(userId, projectId)
    }

    override fun installPackages(
        appCode: String?,
        apigwType: String?,
        userId: String,
        platform: Int,
        appVersion: String?,
        organization: String?,
        experienceHashId: String
    ): Result<Pagination<AppExperienceInstallPackage>> {
        return client.get(ServiceExperienceResource::class).installPackages(
            userId = userId,
            platform = platform,
            appVersion = appVersion,
            organization = organization,
            experienceHashId = experienceHashId
        )
    }
}
