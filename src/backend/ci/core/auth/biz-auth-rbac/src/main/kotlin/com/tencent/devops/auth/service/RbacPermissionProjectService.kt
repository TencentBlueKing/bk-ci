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
 *
 */

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.common.Constants
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import org.slf4j.LoggerFactory

class RbacPermissionProjectService(
    private val authHelper: AuthHelper,
    private val authResourceService: AuthResourceService,
    private val iamV2ManagerService: V2ManagerService,
    private val iamConfiguration: IamConfiguration
) : PermissionProjectService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionProjectService::class.java)
    }

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        TODO("Not yet implemented")
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        TODO("Not yet implemented")
    }

    override fun getUserProjects(userId: String): List<String> {
        val projectList = authHelper.getInstanceList(
            userId,
            Constants.PROJECT_VIEW,
            RbacAuthUtils.extResourceType(AuthResourceType.PROJECT)
        )
        logger.info("get user projects:$projectList")
        return projectList
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        val managerPermission = checkProjectManager(userId, projectCode)
        // 有管理员权限或者若为校验管理员权限,直接返回是否时管理员成员
        if (managerPermission || (group != null && group == BkAuthGroup.MANAGER)) {
            return managerPermission
        }
        val instanceDTO = InstanceDTO()
        instanceDTO.system = iamConfiguration.systemId
        instanceDTO.id = projectCode
        instanceDTO.type = AuthResourceType.PROJECT.value
        return authHelper.isAllowed(userId, Constants.PROJECT_VIEW, instanceDTO)
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        val projectInfo = authResourceService.get(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        )
        val gradeManagerDetail = iamV2ManagerService.getGradeManagerDetail(projectInfo.relationId)
        return gradeManagerDetail.members.contains(userId)
    }

    override fun createProjectUser(userId: String, projectCode: String, roleCode: String): Boolean {
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return emptyList()
    }
}
