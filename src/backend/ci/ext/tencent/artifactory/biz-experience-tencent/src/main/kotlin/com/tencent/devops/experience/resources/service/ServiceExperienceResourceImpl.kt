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

package com.tencent.devops.experience.resources.service

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.service.ServiceExperienceResource
import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.pojo.AppExperienceDetail
import com.tencent.devops.experience.pojo.AppExperienceInstallPackage
import com.tencent.devops.experience.pojo.AppExperienceSummary
import com.tencent.devops.experience.pojo.DownloadUrl
import com.tencent.devops.experience.pojo.Experience
import com.tencent.devops.experience.pojo.ExperienceChangeLog
import com.tencent.devops.experience.pojo.ExperienceInfoForBuild
import com.tencent.devops.experience.pojo.ExperienceJumpInfo
import com.tencent.devops.experience.pojo.ExperienceLastParams
import com.tencent.devops.experience.pojo.ExperienceList
import com.tencent.devops.experience.pojo.ExperienceServiceCreate
import com.tencent.devops.experience.pojo.ExperienceUpdate
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.enums.Source
import com.tencent.devops.experience.pojo.outer.OuterSelectorVO
import com.tencent.devops.experience.service.ExperienceAppService
import com.tencent.devops.experience.service.ExperienceBaseService
import com.tencent.devops.experience.service.ExperienceDownloadService
import com.tencent.devops.experience.service.ExperienceService
import com.tencent.devops.experience.service.GroupService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@SuppressWarnings("ThrowsCount")
class ServiceExperienceResourceImpl @Autowired constructor(
    private val experienceService: ExperienceService,
    private val experienceBaseService: ExperienceBaseService,
    private val experienceDownloadService: ExperienceDownloadService,
    private val experienceAppService: ExperienceAppService,
    private val groupService: GroupService,
    private val client: Client
) : ServiceExperienceResource {

    override fun create(userId: String, projectId: String, experience: ExperienceServiceCreate): Result<String> {
        checkParam(userId, projectId)
        val experienceCreateResp = experienceService.serviceCreate(userId, projectId, experience, Source.OPENAPI)
        return Result(experienceCreateResp.experienceHashId)
    }

    override fun count(projectIds: Set<String>?, expired: Boolean?): Result<Map<String, Int>> {
        return Result(experienceService.count(projectIds ?: setOf(), expired))
    }

    @AuditEntry(actionId = ActionId.EXPERIENCE_TASK_VIEW)
    override fun get(userId: String, projectId: String, experienceHashId: String): Result<Experience> {
        checkParam(userId, projectId)
        return Result(experienceService.get(userId, experienceHashId, false))
    }

    override fun check(userId: String, experienceHashId: String, organization: String?): Result<Boolean> {
        return Result(
            experienceBaseService.userCanExperience(
                userId,
                HashUtil.decodeIdToLong(experienceHashId),
                organization == ExperienceConstant.ORGANIZATION_OUTER
            )
        )
    }

    override fun jumpInfo(projectId: String, bundleIdentifier: String, platform: String): Result<ExperienceJumpInfo> {
        return Result(experienceDownloadService.jumpInfo(projectId, bundleIdentifier, platform))
    }

    @AuditEntry(actionId = ActionId.EXPERIENCE_TASK_EDIT)
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

    override fun listForBuild(
        userId: String?,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Result<List<ExperienceInfoForBuild>> {
        return Result(experienceService.listForBuild(userId, projectId, pipelineId, buildId))
    }

    @AuditEntry(actionId = ActionId.EXPERIENCE_TASK_DELETE)
    override fun offline(userId: String?, projectId: String, experienceHashId: String): Result<Boolean> {
        checkParam(userId, projectId, experienceHashId)
        experienceService.updateOnline(
            userId ?: experienceService.getCreatorById(experienceHashId),
            projectId,
            experienceHashId,
            false
        )
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.EXPERIENCE_TASK_DELETE)
    override fun online(userId: String?, projectId: String, experienceHashId: String): Result<Boolean> {
        checkParam(userId, projectId, experienceHashId)
        experienceService.updateOnline(
            userId ?: experienceService.getCreatorById(experienceHashId),
            projectId,
            experienceHashId,
            true
        )
        return Result(true)
    }

    override fun listV3(userId: String, platform: Int, organization: String?): Result<ExperienceList> {
        return Result(experienceAppService.listV3(userId, platform, organization))
    }

    override fun detail(
        userId: String,
        platform: Int,
        appVersion: String?,
        organization: String?,
        experienceHashId: String,
        forceNew: Boolean
    ): Result<AppExperienceDetail> {
        checkParam(userId, "", experienceHashId)
        val result = experienceAppService.detail(userId, experienceHashId, platform, appVersion, organization, forceNew)
        return Result(result)
    }

    override fun changeLog(
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
        checkParam(userId, "", experienceHashId)
        val result = experienceAppService.changeLog(
            userId = userId,
            experienceHashId = experienceHashId,
            page = page,
            pageSize = pageSize,
            organization = organization,
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
        return Result(result)
    }

    override fun downloadUrl(userId: String, organization: String?, experienceHashId: String): Result<DownloadUrl> {
        checkParam(userId, "", experienceHashId)
        val result = experienceAppService.downloadUrl(userId, experienceHashId, organization)
        return Result(result)
    }

    override fun history(userId: String, appVersion: String?, projectId: String): Result<List<AppExperienceSummary>> {
        checkParam(userId, projectId)
        val result = experienceAppService.history(userId, appVersion, projectId)
        return Result(result)
    }

    override fun projectGroupAndUsers(userId: String, projectId: String): Result<List<ProjectGroupAndUsers>> {
        checkParam(userId, projectId)
        val result = groupService.getProjectGroupAndUsers(userId, projectId)
        return Result(result)
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

    override fun outerList(userId: String, projectId: String): Result<List<OuterSelectorVO>> {
        return Result(emptyList())
    }

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

    private fun checkParam(userId: String?, projectId: String, experienceHashId: String = "default") {
        if (userId != null && userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (experienceHashId.isBlank()) {
            throw ParamBlankException("Invalid experienceHashId")
        }
        // TODO 暂时不校验 , 否则外部用户无法下载构件
//        if (userId != null && client.get(ServiceProjectResource::class)
//                .verifyUserProjectPermission(projectCode = projectId, userId = userId).data != true
//        ) {
//            throw ErrorCodeException(
//                defaultMessage = "用户没有项目权限",
//                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION
//            )
//        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ServiceExperienceResourceImpl::class.java)
    }
}
