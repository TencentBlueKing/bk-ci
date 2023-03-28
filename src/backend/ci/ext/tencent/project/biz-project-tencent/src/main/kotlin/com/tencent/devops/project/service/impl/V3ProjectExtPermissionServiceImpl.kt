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

package com.tencent.devops.project.service.impl

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.api.ServiceRoleMemberResource
import com.tencent.devops.auth.api.ServiceRoleResource
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.api.v3.TxV3AuthPermissionApi
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.constant.ProjectMessageCode.ASSOCIATED_SYSTEM_NOT_BOUND
import com.tencent.devops.project.constant.ProjectMessageCode.NUMBER_AUTHORIZED_USERS_EXCEEDS_LIMIT
import com.tencent.devops.project.constant.ProjectMessageCode.QUERY_USER_INFO_FAIL
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.service.ProjectExtPermissionService
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V3ProjectExtPermissionServiceImpl @Autowired constructor(
    val client: Client,
    val tokenService: ClientTokenService,
    val projectDao: ProjectDao,
    val dslContext: DSLContext,
    val tofService: TOFService
) : ProjectExtPermissionService {
    override fun verifyUserProjectPermission(
        accessToken: String,
        projectCode: String,
        userId: String
    ): Boolean {
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectCode
        ).data ?: false
    }

    override fun createUser2Project(
        createUser: String,
        userIds: List<String>,
        projectCode: String,
        roleId: Int?,
        roleName: String?,
        checkManager: Boolean
    ): Boolean {
        val projectInfo = projectDao.getByEnglishName(dslContext, projectCode) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            defaultMessage = MessageUtil.getCodeLanMessage(messageCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()))
        )
        val projectRelationId = projectInfo.relationId

        if (projectRelationId.isNullOrEmpty()) {
            logger.warn("create V3 project user, not binding $projectCode iamV3")
            throw ErrorCodeException(
                errorCode = ASSOCIATED_SYSTEM_NOT_BOUND
            )
        }
        logger.info("getProject role $projectCode $projectRelationId")

        // 应用态checkManager为false，且操作人为“”,需替换为项目的修改人(修改人肯定有权限)
        val currencyCreateUser = if (!checkManager || createUser.isBlank()) {
            projectInfo.creator
        } else {
            createUser
        }

        // 获取目标用户组在iam内用户组id
        val groupInfos = client.get(ServiceRoleResource::class).getProjectRoles(
            userId = currencyCreateUser,
            projectId = projectRelationId.toInt()).data
        if (groupInfos == null) {
            logger.warn("$projectCode $projectRelationId group is empty")
            return false
        }

        var managerFlag = false
        val groupMap = mutableMapOf<String, Int>()
        groupInfos.map {
            groupMap[it.code] = it.id
            if (it.code == DefaultGroupType.MANAGER.value) {
                groupMap["ci_manager"] = it.id
                managerFlag = true
            }
            logger.info("project role ${it.code} ${it.name} ${it.id}")
        }
        var relationGroupId: Int? = null
        if (!roleName.isNullOrEmpty()) {
            relationGroupId = groupMap[roleName!!]
        }
        val memberList = mutableListOf<RoleMemberDTO>()
        userIds.map {
            // 校验用户是否为真实用户
            try {
                tofService.getStaffInfo(it)
            } catch (ope: OperationException) {
                logger.warn("getStaffInfo fail $it $projectCode")
                throw ope
            } catch (e: Exception) {
                logger.warn("getStaffInfo fail, userId[$it]", e)
                throw OperationException(MessageUtil.getCodeLanMessage(
                    messageCode = QUERY_USER_INFO_FAIL,
                    defaultMessage = e.message,
                    params = arrayOf(it),
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                ))
            }
            memberList.add(
                RoleMemberDTO(
                    id = it,
                    type = ManagerScopesEnum.USER
                )
            )
        }

        if (relationGroupId == null) {
            logger.warn("create group user fail, $projectCode $roleName not find relationGroup")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
                defaultMessage = MessageUtil.getCodeLanMessage(
                    messageCode = AuthMessageCode.RELATED_RESOURCE_EMPTY,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()))
            )
        }

        logger.info("add project $projectCode group $relationGroupId $userIds $checkManager")
        // 添加用户到用户组
        client.get(ServiceRoleMemberResource::class).createRoleMember(
            userId = currencyCreateUser,
            projectId = projectRelationId.toInt(),
            roleId = relationGroupId,
            managerGroup = managerFlag,
            members = memberList,
            checkGradeManager = checkManager
        )
        return true
    }

    override fun grantInstancePermission(
        userId: String,
        projectId: String,
        action: String,
        resourceType: String,
        resourceCode: String,
        userList: List<String>
    ): Boolean {
        logger.info("grantInstancePermission $userId|$projectId|$action|$resourceType|$resourceCode|$userList")
        // 此处做保护,防止用户一次加太多用户
        if (userList.size > TxV3AuthPermissionApi.GRANT_USER_MAX_SIZE) {
            logger.warn("grant instance user too long $projectId|$resourceCode|$resourceType|$userList")
            throw ParamBlankException(
                MessageUtil.getMessageByLocale(
                    messageCode = NUMBER_AUTHORIZED_USERS_EXCEEDS_LIMIT,
                    language = I18nUtil.getLanguage(userId)
                ) + ":${TxV3AuthPermissionApi.GRANT_USER_MAX_SIZE}")
        }
        userList.forEach {
            val grantInstanceDTO = GrantInstanceDTO(
                resourceType = resourceType,
                resourceCode = resourceCode,
                permission = action,
                createUser = it,
                resourceName = null
            )
            client.get(ServicePermissionAuthResource::class).grantInstancePermission(
                userId = userId,
                projectCode = projectId,
                token = tokenService.getSystemToken(null)!!,
                grantInstance = grantInstanceDTO
            ).data
        }
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(V3ProjectExtPermissionServiceImpl::class.java)
    }
}
