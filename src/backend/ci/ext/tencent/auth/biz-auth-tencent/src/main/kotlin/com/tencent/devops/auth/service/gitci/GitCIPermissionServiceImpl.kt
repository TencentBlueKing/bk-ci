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

package com.tencent.devops.auth.service.gitci

import com.tencent.devops.auth.service.ManagerService
import com.tencent.devops.auth.service.gitci.entify.GitCIPermissionLevel
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.pojo.git.GitMember
import com.tencent.devops.scm.api.ServiceGitCiResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class GitCIPermissionServiceImpl @Autowired constructor(
    val client: Client,
    val managerService: ManagerService,
    val projectInfoService: GitCiProjectInfoService,
    val projectServiceImpl: GitCIPermissionProjectServiceImpl
) : PermissionService {

    // GitCI权限场景不会出现次调用, 故做默认实现
    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        return true
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        // review管理员校验
        try {
            if (reviewManagerCheck(userId, projectCode, action, resourceType ?: "")) {
                logger.info("$projectCode $userId $action $resourceType is review manager")
                return true
            }
        } catch (e: Exception) {
            // 管理员报错不影响主流程, 有种场景gitCI会在项目未创建得时候调权限校验,通过projectId匹配组织会报空指针
            logger.warn("reviewManager fail $e")
        }
        // 特殊逻辑, 额外校验工蜂ci页面是否展示新增按钮
        if (action == AuthPermission.WEB_CHECK.value) {
            return webCheckAction(projectCode, userId)
        }

        // 查看,下载只需要校验是否为项目成员, 不做developer, oauth相关的校验
        if (isProjectActionList(action)) {
            return if (action == AuthPermission.DOWNLOAD.value) {
                // download需要是项目成员
                val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)
                projectServiceImpl.checkProjectUser(userId, gitProjectId, projectCode)
            } else {
                // list, view项目成员或者开源项目都校验通过
                projectServiceImpl.isProjectUser(userId, projectCode, null)
            }
        }

        val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)

        // 操作类action需要校验用户oauth, 查看类的无需oauth校验
        if (!checkListOrViewAction(action)) {
            val checkOauth = client.get(ServiceOauthResource::class).gitGet(userId).data
            if (checkOauth == null) {
                logger.warn("GitCICertPermissionServiceImpl $userId oauth is required")
                throw OauthForbiddenException("OAUTH is required.")
            }
        }
        logger.info("GitCI validate user:$userId projectId: $projectCode gitProjectId: $gitProjectId")

        // 判断是否为开源项目
        if (projectInfoService.checkProjectPublic(gitProjectId)) {
            // 若为pipeline 且action为list 校验成功
            if (checkExtAction(action, resourceType)) {
                logger.info("$projectCode is public, views action can check success")
                return true
            }
        }

        val gitUserId = projectInfoService.getGitUserByRtx(userId, gitProjectId)
        if (gitUserId.isNullOrEmpty()) {
            logger.warn("$userId is not gitCI user")
            return false
        }

        val checkResult = client.getScm(ServiceGitCiResource::class)
            .checkUserGitAuth(gitUserId, gitProjectId).data ?: false
        if (!checkResult) {
            logger.warn("$projectCode $userId $action $resourceType check permission fail")
        }
        return checkResult
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        return validateUserResourcePermission(userId, action, projectCode, resourceType)
    }

    // GitCI权限场景不会出现次调用, 故做默认实现
    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        return emptyList()
    }

    // GitCI权限场景不会出现次调用, 故做默认实现
    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        return emptyMap()
    }

    private fun checkListOrViewAction(action: String): Boolean {
        val passAction = mutableListOf<String>()
        passAction.add(AuthPermission.LIST.value)
        passAction.add(AuthPermission.VIEW.value)
        passAction.add(AuthPermission.DOWNLOAD.value)
        passAction.add(AuthPermission.USE.value)
        if (passAction.contains(action)) {
            return true
        }
        return false
    }

    private fun isProjectActionList(action: String): Boolean {
        val actionList = mutableListOf<String>()
        actionList.add(AuthPermission.LIST.value)
        actionList.add(AuthPermission.VIEW.value)
        actionList.add(AuthPermission.DOWNLOAD.value)
        if (actionList.contains(action)) {
            return true
        }
        return false
    }

    private fun checkExtAction(action: String?, resourceCode: String?): Boolean {
        if (action.isNullOrEmpty() || resourceCode.isNullOrEmpty()) {
            return false
        }
        if ((action == AuthPermission.VIEW.value || action == AuthPermission.LIST.value) &&
            resourceCode == AuthResourceType.PIPELINE_DEFAULT.value) {
            return true
        }
        return false
    }

    private fun reviewManagerCheck(
        userId: String,
        projectCode: String,
        action: String,
        resourceTypeStr: String
    ): Boolean {
        if (resourceTypeStr.isNullOrEmpty()) {
            return false
        }
        return try {
            val authPermission = AuthPermission.get(action)
            val resourceType = AuthResourceType.get(resourceTypeStr)
            managerService.isManagerPermission(
                userId = userId,
                projectId = projectCode,
                authPermission = authPermission,
                resourceType = resourceType
            )
        } catch (e: Exception) {
            logger.warn("reviewManagerCheck change enum fail $projectCode $action $resourceTypeStr")
            false
        }
    }

    /**
     * 是否公开, 是否有权限 二维组合
     * 公开+develop以上  展示
     * 非公开+develop以上 展示
     * 公开+develop以下成员 不展示
     * 非公开+develop以下成员 不展示
     * 公开+ 非项目成员  不展示
     * 非公开+ 非项目成员  报异常
     */
    private fun webCheckAction(projectCode: String, userId: String): Boolean {
        val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)

        val publicCheck = projectInfoService.checkProjectPublic(gitProjectId)
        var permissionCheck: GitCIPermissionLevel?
        try {
            val gitProjectMembers = client.getScm(ServiceGitCiResource::class).getProjectMembersAll(
                gitProjectId = gitProjectId,
                page = 0,
                pageSize = 100,
                search = userId
            ).data
            logger.info("$projectCode project member $userId $gitProjectMembers")
            if (gitProjectMembers.isNullOrEmpty()) {
                throw PermissionForbiddenException(
                    MessageCodeUtil.getCodeMessage(
                        CommonMessageCode.PERMISSION_DENIED,
                        arrayOf(AuthPermission.WEB_CHECK.value))
                )
            }

            val memberMap = mutableMapOf<String, GitMember>()
            gitProjectMembers.forEach {
                memberMap[it.username] = it
            }

            permissionCheck = if (memberMap.containsKey(userId)) {
                val memberInfo = memberMap[userId]
                if (memberInfo?.accessLevel ?: 0 > 20) {
                    GitCIPermissionLevel.DEVELOP_UP
                } else GitCIPermissionLevel.DEVELOP_DOWN
            } else {
                GitCIPermissionLevel.NO_PERMISSION
            }
        } catch (e: Exception) {
            logger.warn("$userId is not $gitProjectId member")
            permissionCheck = GitCIPermissionLevel.NO_PERMISSION
        }

        logger.info("webCheckAction $projectCode $gitProjectId permission:$permissionCheck public:$publicCheck")
        if (!publicCheck) {
            if (permissionCheck == GitCIPermissionLevel.NO_PERMISSION) {
                throw PermissionForbiddenException(
                    MessageCodeUtil.getCodeMessage(
                        CommonMessageCode.PERMISSION_DENIED,
                        arrayOf(AuthPermission.WEB_CHECK.value)
                    )
                )
            } else return permissionCheck != GitCIPermissionLevel.DEVELOP_DOWN
        }
        return permissionCheck == GitCIPermissionLevel.DEVELOP_UP
    }

    companion object {
        val logger = LoggerFactory.getLogger(GitCIPermissionServiceImpl::class.java)
    }
}
