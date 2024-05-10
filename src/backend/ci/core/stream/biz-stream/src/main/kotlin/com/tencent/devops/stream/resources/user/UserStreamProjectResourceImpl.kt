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

package com.tencent.devops.stream.resources.user

import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.stream.api.user.UserStreamProjectResource
import com.tencent.devops.stream.permission.StreamPermissionService
import com.tencent.devops.stream.pojo.StreamProjectCIInfo
import com.tencent.devops.stream.pojo.enums.StreamProjectType
import com.tencent.devops.stream.pojo.enums.StreamProjectsOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import com.tencent.devops.stream.service.StreamBasicSettingService
import com.tencent.devops.stream.service.StreamProjectService
import com.tencent.devops.stream.util.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserStreamProjectResourceImpl @Autowired constructor(
    private val client: Client,
    private val streamProjectService: StreamProjectService,
    private val permissionService: StreamPermissionService,
    private val streamBasicSettingService: StreamBasicSettingService
) : UserStreamProjectResource {
    override fun getProjects(
        userId: String,
        type: StreamProjectType?,
        search: String?,
        page: Int?,
        pageSize: Int?,
        orderBy: StreamProjectsOrder?,
        sort: StreamSortAscOrDesc?
    ): Pagination<StreamProjectCIInfo> {
        return streamProjectService.getProjectList(
            userId = userId,
            type = type,
            search = search,
            page = page,
            pageSize = pageSize,
            orderBy = orderBy,
            sort = sort
        )
    }

    override fun getProjectsHistory(userId: String, size: Long?): Result<List<StreamProjectCIInfo>> {
        val fixPageSize = size?.coerceAtMost(maxPageSize)?.coerceAtLeast(defaultPageSize) ?: defaultPageSize
        return Result(streamProjectService.getUserProjectHistory(userId, size = fixPageSize) ?: emptyList())
    }

    override fun getProjectInfo(userId: String, projectId: String): Result<ProjectVO?> {
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.VIEW
        )
        val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data ?: run {
            with(client.get(ServiceUserResource::class).getDetailFromCache(userId).data ?: return@run null) {
                /*特殊处理，如果没有项目信息则用用户信息进行填充*/
                ProjectVO(
                    id = 0,
                    projectId = projectId,
                    projectName = projectId,
                    projectCode = projectId,
                    projectType = null,
                    approvalStatus = null,
                    approvalTime = null,
                    approver = null,
                    ccAppId = null,
                    ccAppName = null,
                    createdAt = null,
                    creator = null,
                    dataId = null,
                    deployType = null,
                    bgId = bgId,
                    bgName = bgName,
                    centerId = centerId,
                    centerName = centerName,
                    deptId = deptId,
                    deptName = deptName,
                    businessLineId = businessLineId,
                    businessLineName = businessLineName,
                    description = null,
                    englishName = projectId,
                    extra = null,
                    offlined = null,
                    secrecy = null,
                    helmChartEnabled = null,
                    kind = null,
                    logoAddr = null,
                    remark = null,
                    updatedAt = null,
                    updator = null,
                    useBk = null,
                    enabled = null,
                    gray = false,
                    hybridCcAppId = null,
                    enableExternal = null,
                    enableIdc = null,
                    pipelineLimit = null,
                    hybrid_cc_app_id = null,
                    project_id = null,
                    project_name = null,
                    project_code = null,
                    cc_app_id = null,
                    cc_app_name = null,
                    routerTag = null,
                    relationId = null,
                    properties = null,
                    subjectScopes = null,
                    authSecrecy = null,
                    tipsStatus = null,
                    approvalMsg = null,
                    managePermission = null,
                    showUserManageIcon = null,
                    channelCode = null,
                    productId = null,
                    canView = null,
                    pipelineTemplateInstallPerm = null
                )
            }
        }
        return Result(projectInfo)
    }

    override fun updateProjectOrganization(
        userId: String,
        projectId: String,
        productId: Int,
        productName: String,
        organization: ProjectOrganizationInfo
    ): Result<Boolean> {
        permissionService.checkStreamPermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.EDIT
        )
        kotlin.runCatching {
            client.get(ServiceProjectResource::class).updateProjectProductId(projectId, productName)
            client.get(ServiceProjectResource::class).updateOrganizationByEnglishName(projectId, organization)
        }.onFailure {
            if (it is RemoteServiceException && it.httpStatus == HTTP_404) {
                streamBasicSettingService.initStreamConf(
                    userId = userId,
                    projectId = projectId,
                    gitProjectId = GitCommonUtils.getGitProjectId(projectId),
                    enabled = false
                )
                client.get(ServiceProjectResource::class).updateProjectProductId(projectId, productName)
                client.get(ServiceProjectResource::class).updateOrganizationByEnglishName(projectId, organization)
            }
        }
        return Result(true)
    }

    private val maxPageSize = 10L
    private val defaultPageSize = 4L
}
