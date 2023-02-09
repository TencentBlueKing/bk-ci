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

package com.tencent.devops.auth.service.rbac

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.dto.manager.V2ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.common.Constants
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionProjectService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.utils.AuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class RbacPermissionProjectServiceImpl @Autowired constructor(
    val client: Client,
    val iamManagerService: V2ManagerService,
    val deptService: DeptService,
    val policyService: PolicyService
) : PermissionProjectService {
    private val rbacProjectIdCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, String>()

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        val allGroupAndUser = getProjectGroupAndUserList(projectCode)
        return if (group == null) {
            val allMembers = mutableSetOf<String>()
            allGroupAndUser.map { allMembers.addAll(it.userIdList) }
            allMembers.toList()
        } else {
            allGroupAndUser.forEach {
                if (it.roleName == group.groupName) {
                    it.userIdList
                }
            }
            emptyList()
        }
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        // 1. 转换projectCode为iam侧分级管理员Id
        val iamProjectId = getProjectId(projectCode)
        // 2. 获取项目下的所有用户组
        val groupInfos = getPermissionRole(iamProjectId)
        logger.info(
            "[IAM-RBAC] getProjectGroupAndUserList: projectCode = $projectCode |" +
                " iamProjectId = $iamProjectId | groupInfos: $groupInfos"
        )
        val result = mutableListOf<BkAuthGroupAndUserList>()
        groupInfos.forEach {
            val pageInfoDTO = PageInfoDTO()
            pageInfoDTO.limit = 0L
            pageInfoDTO.offset = 1000L
            val groupMemberInfos = iamManagerService.getRoleGroupMemberV2(it.id.toInt(), pageInfoDTO).results
            logger.info("[IAM-RBAC] $projectCode $iamProjectId ,role ${it.name} | users $groupMemberInfos")
            val members = mutableListOf<String>()
            groupMemberInfos.forEach { memberInfo ->
                // 如果为组织需要获取组织对应的用户
                if (memberInfo.type == ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)) {
                    logger.info("[IAM] $projectCode $iamProjectId ,role ${it.id} | dept ${memberInfo.id}")
                    val deptUsers = deptService.getDeptUser(memberInfo.id.toInt(), null)
                    if (deptUsers != null) {
                        members.addAll(deptUsers)
                    }
                } else {
                    members.add(memberInfo.id)
                }
                // todo 不可以这么简单的处理，迁移的项目最好不要加项目
                val groupName = it.name.substring(it.name.indexOf("-") + 1)
                val groupAndUser = BkAuthGroupAndUserList(
                    displayName = groupName,
                    roleId = it.id.toInt(),
                    roleName = groupName,
                    userIdList = members,
                    // TODO: 待iam完成group lable后补齐
                    type = ""
                )
                result.add(groupAndUser)
            }
        }
        return result
    }

    override fun getUserProjects(userId: String): List<String> {
        // todo rbac 会拉取用户有 PROJECT_VIEW项目，需要数据迁移的时候，将all_action去掉，进行迁移。
        val actionDTOs = mutableListOf<ActionDTO>()
        val viewAction = Constants.PROJECT_VIEW
        val viewActionDto = ActionDTO()
        viewActionDto.id = viewAction
        actionDTOs.add(viewActionDto)
        val actionPolicyDTOs = policyService.batchGetPolicyByActionList(userId, actionDTOs, null) ?: return emptyList()
        logger.info("[IAM] getUserProjects: actionPolicyDTOs = $actionPolicyDTOs")
        val projectCodes = mutableSetOf<String>()
        actionPolicyDTOs.forEach {
            projectCodes.addAll(AuthUtils.getProjects(it.condition))
        }
        return projectCodes.toList()
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        return false
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        return false
    }

    override fun createProjectUser(userId: String, projectCode: String, roleCode: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        TODO("Not yet implemented")
    }

    private fun getProjectId(projectCode: String): Int {
        val iamProjectId = if (rbacProjectIdCache.getIfPresent(projectCode) != null) {
            rbacProjectIdCache.getIfPresent(projectCode)!!
        } else {
            val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data

            if (projectInfo != null && !projectInfo.relationId.isNullOrEmpty()) {
                rbacProjectIdCache.put(projectCode, projectInfo.relationId!!)
            }
            projectInfo?.relationId
        }
        if (iamProjectId.isNullOrEmpty()) {
            logger.warn("[IAM] $projectCode iamProject is empty")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
                defaultMessage = MessageCodeUtil.getCodeLanMessage(
                    messageCode = AuthMessageCode.RELATED_RESOURCE_EMPTY
                )
            )
        }
        return iamProjectId.toInt()
    }

    private fun getPermissionRole(iamGradeManagerId: Int): List<V2ManagerRoleGroupInfo> {
        val v2PageInfoDTO = V2PageInfoDTO()
        v2PageInfoDTO.pageSize = 1000
        v2PageInfoDTO.page = 1
        val groupInfos = iamManagerService.getGradeManagerRoleGroupV2(iamGradeManagerId.toString(), null, v2PageInfoDTO)
            ?: return emptyList()
        return groupInfos.results
    }

    private fun getUserByIam(group: BkAuthGroup, projectCode: String): List<String> {
        // 获取iam的分级管理员id
        val iamProjectId = getProjectId(projectCode)
        val pageInfoDTO = PageInfoDTO()
        pageInfoDTO.limit = 0L
        pageInfoDTO.offset = 1000L
        val groupMemberInfos = iamManagerService.getRoleGroupMemberV2(iamProjectId, pageInfoDTO).results
        val users = mutableListOf<String>()
        groupMemberInfos.forEach {
            if (it.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER)) {
                users.add(it.id)
            }
        }
        return users
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbsPermissionProjectService::class.java)
    }
}
