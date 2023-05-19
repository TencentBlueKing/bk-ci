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

package com.tencent.devops.auth.service.iam.impl

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleMemberDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.ManagerGroupMemberVo
import com.tencent.bk.sdk.iam.exception.IamException
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.constant.AuthMessageCode.CAN_NOT_FIND_RELATION
import com.tencent.devops.auth.pojo.MemberInfo
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.auth.pojo.vo.ProjectMembersVO
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class AbsPermissionRoleMemberImpl @Autowired constructor(
    open val iamManagerService: ManagerService,
    private val permissionGradeService: PermissionGradeService,
    private val groupService: AuthGroupService
) : PermissionRoleMemberService {

    private val projectMemberCache = CacheBuilder.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, ProjectMembersVO>()

    override fun createRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        members: List<RoleMemberDTO>,
        managerGroup: Boolean,
        checkAGradeManager: Boolean?
    ) {
        val iamId = groupService.getRelationId(roleId)
        if (iamId == null) {
            logger.warn("$roleId can not find iam relationId")
            throw ParamBlankException(I18nUtil.getCodeLanMessage(messageCode = CAN_NOT_FIND_RELATION))
        }

        // 页面操作需要校验分级管理员,服务间调用无需校验
        if (checkAGradeManager!!) {
            permissionGradeService.checkGradeManagerUser(projectId = projectId, userId = userId)
        }
        val roleMembers = mutableListOf<ManagerMember>()
        val userIds = mutableListOf<String>()
        members.forEach {
            if (it.type == ManagerScopesEnum.USER) {
                checkUser(it.id)
                userIds.add(it.id)
            }
            roleMembers.add(ManagerMember(ManagerScopesEnum.getType(it.type), it.id))
        }
        val expiredTime = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(expiredAt)
        val managerMemberGroupDTO = ManagerMemberGroupDTO.builder().expiredAt(expiredTime).members(roleMembers).build()
        try {
            iamManagerService.createRoleGroupMember(iamId!!.toInt(), managerMemberGroupDTO)
        } catch (iamEx: IamException) {
            logger.warn("create group user fail. code: ${iamEx.errorCode} | msg: ${iamEx.errorMsg}")
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = AuthMessageCode.IAM_SYSTEM_ERROR,
                    params = arrayOf(iamEx.errorMsg)
                )
            )
        } catch (e: Exception) {
            logger.warn("create group user fail. code: $e")
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = AuthMessageCode.IAM_SYSTEM_ERROR,
                    params = arrayOf(e.message ?: "unknown")
                )
            )
        }

        // 添加用户到管理员需要同步添加用户到分级管理员
        if (managerGroup) {
            if (userIds.isNotEmpty()) {
                val gradeMembers = ManagerRoleMemberDTO.builder().members(userIds).build()
                iamManagerService.batchCreateGradeManagerRoleMember(gradeMembers, projectId)
            }
        }
    }

    override fun deleteRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        id: String,
        type: ManagerScopesEnum,
        managerGroup: Boolean
    ) {
        val iamId = groupService.getRelationId(roleId)
        if (iamId == null) {
            logger.warn("$roleId can not find iam relationId")
            throw ParamBlankException(
                I18nUtil.getCodeLanMessage(CAN_NOT_FIND_RELATION)
            )
        }
        permissionGradeService.checkGradeManagerUser(userId, projectId)

        iamManagerService.deleteRoleGroupMember(iamId.toInt(), ManagerScopesEnum.getType(type), id)
        // 如果是删除用户,且用户是管理员需同步删除该用户分分级管理员权限
        if (managerGroup && type == ManagerScopesEnum.USER) {
            iamManagerService.deleteGradeManagerRoleMember(id, projectId)
        }
    }

    override fun getRoleMember(
        projectId: Int,
        roleId: Int,
        page: Int?,
        pageSiz: Int?
    ): ManagerGroupMemberVo {
        val iamId = groupService.getRelationId(roleId)
        if (iamId == null) {
            logger.warn("$roleId can not find iam relationId")
            throw ParamBlankException(
                I18nUtil.getCodeLanMessage(
                    messageCode = CAN_NOT_FIND_RELATION
                )
            )
        }
        val pageInfoDTO = PageInfoDTO()
        val pageInfo = PageUtil.convertPageSizeToSQLLimit(page, pageSiz ?: 1000)
        pageInfoDTO.limit = pageInfo.limit.toLong()
        pageInfoDTO.offset = pageInfo.offset.toLong()
        return iamManagerService.getRoleGroupMember(iamId.toInt(), pageInfoDTO)
    }

    override fun getProjectAllMember(projectId: Int, page: Int?, pageSiz: Int?): ProjectMembersVO? {
        if (projectMemberCache.getIfPresent(projectId.toString()) != null) {
            logger.info("getProjectAllMember: projectId = $projectId")
            return projectMemberCache.getIfPresent(projectId.toString())!!
        }

        val pageInfoDTO = PageInfoDTO()
        val pageInfo = PageUtil.convertPageSizeToSQLLimit(page ?: 0, pageSiz ?: 2000)
        pageInfoDTO.limit = pageInfo.limit.toLong()
        pageInfoDTO.offset = pageInfo.offset.toLong()
        // 获取项目下的用户组
        val groupInfos = iamManagerService.getGradeManagerRoleGroup(projectId, pageInfoDTO)
        if (groupInfos == null || groupInfos.count == 0) {
            return null
        }

        val members = mutableSetOf<MemberInfo>()
        groupInfos.results.forEach { group ->
            val membersInfos = iamManagerService.getRoleGroupMember(group.id, pageInfoDTO).results
            membersInfos.forEach { member ->
                members.add(MemberInfo(
                    id = member.id,
                    name = member.name,
                    type = member.type
                ))
            }
        }
        val count = members.size
        val result = ProjectMembersVO(
            count = count,
            results = members
        )
        projectMemberCache.put(projectId.toString(), result)
        return result
    }

    override fun getUserGroups(projectId: Int, userId: String): List<ManagerRoleGroupInfo>? {
        logger.info("getUserGroup: projectId = $projectId | userId = $userId")
        val groupInfos = iamManagerService.getUserGroup(projectId, userId)
        logger.info("getUserGroup: projectId = $projectId | userId = $userId | groupInfos = $groupInfos")
        return groupInfos
    }

    abstract fun checkUser(userId: String)

    companion object {
        val logger = LoggerFactory.getLogger(AbsPermissionRoleMemberImpl::class.java)
        const val expiredAt = 365L
    }
}
