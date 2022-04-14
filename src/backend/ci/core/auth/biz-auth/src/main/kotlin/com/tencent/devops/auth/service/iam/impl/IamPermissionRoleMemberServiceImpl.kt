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

package com.tencent.devops.auth.service.iam.impl

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleMemberDTO
import com.tencent.bk.sdk.iam.exception.IamException
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.GroupMember
import com.tencent.devops.auth.pojo.MemberInfo
import com.tencent.devops.auth.pojo.dto.GroupMemberDTO
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.auth.pojo.dto.UserGroupInfoDTO
import com.tencent.devops.auth.pojo.enum.ExpiredStatus
import com.tencent.devops.auth.pojo.enum.UserType
import com.tencent.devops.auth.pojo.vo.ProjectMembersVO
import com.tencent.devops.auth.service.AuthGroupMemberService
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.ci.impl.AbsPermissionRoleMemberImpl
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.utils.IamGroupUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

open class IamPermissionRoleMemberServiceImpl @Autowired constructor(
    private val permissionGradeService: PermissionGradeService,
    private val groupService: AuthGroupService,
    private val iamCacheService: IamCacheService,
    private val groupMemberService: AuthGroupMemberService,
    open val iamManagerService: ManagerService,
) : AbsPermissionRoleMemberImpl(
    permissionGradeService, groupService, iamCacheService, groupMemberService
) {
    private val projectMemberCache = CacheBuilder.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, ProjectMembersVO>()

    override fun createRoleMember(
        userId: String,
        projectId: String,
        roleId: Int,
        members: List<RoleMemberDTO>,
        managerGroup: Boolean,
        checkAGradeManager: Boolean?,
        expiredDay: Long
    ) {
        super.createRoleMember(userId, projectId, roleId, members, managerGroup, checkAGradeManager, expiredDay)
        createMemberExt(
            projectId = projectId,
            roleId = roleId,
            members = members,
            managerGroup = managerGroup,
            expiredDay = expiredDay
        )
        return
    }

    override fun deleteRoleMember(executeUserId: String, projectId: String, roleId: Int, deleteUserId: String, type: ManagerScopesEnum, managerGroup: Boolean) {
        super.deleteRoleMember(executeUserId, projectId, roleId, deleteUserId, type, managerGroup)

        // 删除iam相关人员信息
        val iamId = groupService.getRelationId(roleId)
        if (iamId == null) {
            logger.warn("$roleId can not find relationId")
            throw ParamBlankException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.CAN_NOT_FIND_RELATION))
        }
        val iamProjectId = iamCacheService.getProjectIamRelationId(projectId)

        iamManagerService.deleteRoleGroupMember(iamId.toInt(), ManagerScopesEnum.getType(type), deleteUserId)
        // 如果是删除用户,且用户是管理员需同步删除该用户分分级管理员权限
        if (managerGroup && type == ManagerScopesEnum.USER) {
            iamManagerService.deleteGradeManagerRoleMember(deleteUserId, iamProjectId)
        }
    }

    override fun getRoleMember(projectId: String, roleId: Int, page: Int?, pageSiz: Int?): GroupMemberDTO {
        val iamRoleId = groupService.getRelationId(roleId)
        if (iamRoleId == null) {
            AbsPermissionRoleMemberImpl.logger.warn("$roleId can not find relationId")
            throw ParamBlankException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.CAN_NOT_FIND_RELATION))
        }
        val pageInfoDTO = PageInfoDTO()
        val pageInfo = PageUtil.convertPageSizeToSQLLimit(page, pageSiz ?: 1000)
        pageInfoDTO.limit = pageInfo.limit.toLong()
        pageInfoDTO.offset = pageInfo.offset.toLong()
        val iamGroupInfos = iamManagerService.getRoleGroupMember(iamRoleId.toInt(), pageInfoDTO)
        val groupMember = mutableListOf<GroupMember>()
        iamGroupInfos.results.forEach {
            groupMember += GroupMember(
                id = it.id,
                type = it.type,
                expiredAt = it.expiredAt,
                expiredStatus = ExpiredStatus.buildExpiredStatus(
                    expiredTime = it.expiredAt,
                    expiredSize = TimeUnit.DAYS.toMillis(EXPIRED_TIMEOUT_SIZE.toLong())
                ),
                // TODO: iam未返回此字段
                createTime = 0
            )
        }
        return GroupMemberDTO(
            count = iamGroupInfos.count,
            result = groupMember
        )
    }

    override fun getUserGroups(projectId: String, userId: String): List<UserGroupInfoDTO>? {
        AbsPermissionRoleMemberImpl.logger.info("getUserGroup: $projectId $userId")
        val iamProjectId = iamCacheService.getProjectIamRelationId(projectId)
        val groupInfos = iamManagerService.getUserGroup(iamProjectId, userId)
        val userGroups = mutableListOf<UserGroupInfoDTO>()
        groupInfos.forEach {
            userGroups.add(
                UserGroupInfoDTO(
                    groupName = IamGroupUtils.buildCIGroup(it.name),
                    groupDesc = it.description,
                    groupId = it.id.toString(),
                    groupType = true, // iam的场景用不到此字段
                    // TODO: 待iam接口提供此字段
                    expiredAt = System.currentTimeMillis(),
                    // TODO: 待iam接口提供此字段
                    expiredStatus = ExpiredStatus.buildExpiredStatus(0, TimeUnit.DAYS.toMillis(EXPIRED_TIMEOUT_SIZE.toLong()))
                )
            )
        }
        AbsPermissionRoleMemberImpl.logger.info("getUserGroup: $projectId $iamProjectId $userId $userGroups")
        return userGroups
    }

    override fun getProjectAllMember(projectId: String, page: Int?, pageSiz: Int?): ProjectMembersVO? {
        if (projectMemberCache.getIfPresent(projectId) != null) {
            AbsPermissionRoleMemberImpl.logger.info("getProjectAllMember $projectId get by cache")
            return projectMemberCache.getIfPresent(projectId)!!
        }
        val iamProjectId = iamCacheService.getProjectIamRelationId(projectId)
        val pageInfoDTO = PageInfoDTO()
        val pageInfo = PageUtil.convertPageSizeToSQLLimit(page ?: 0, pageSiz ?: 2000)
        pageInfoDTO.limit = pageInfo.limit.toLong()
        pageInfoDTO.offset = pageInfo.offset.toLong()
        // 获取项目下的用户组
        val groupInfos = iamManagerService.getGradeManagerRoleGroup(iamProjectId, pageInfoDTO)
        if (groupInfos == null || groupInfos.count == 0) {
            return null
        }

        val memberInfos = mutableMapOf<String, MemberInfo>()

        groupInfos.results.forEach { group ->
            // 获取用户组下用户名单
            val membersInfos = iamManagerService.getRoleGroupMember(group.id, pageInfoDTO).results
            membersInfos.forEach { member ->
                if (memberInfos.containsKey(member.id)) {
                    val memberInfo = memberInfos[member.id]
                    // 追加用户加入的用户组
                    memberInfo?.groups?.add(group.name)
                } else {
                    // 添加用户加入的用户组
                    memberInfos[member.id] = MemberInfo(
                        id = member.id,
                        type = member.type,
                        name = member.name,
                        groups = setOf(IamGroupUtils.buildCIGroup(group.name)) as MutableSet<String>
                    )
                }
            }
        }
        val count = memberInfos.size
        val result = ProjectMembersVO(
            count = count,
            results = memberInfos
        )
        projectMemberCache.put(projectId, result)
        return result
    }

    // 添加人员信息到iam系统
    private fun createMemberExt(
        projectId: String,
        roleId: Int,
        members: List<RoleMemberDTO>,
        managerGroup: Boolean,
        expiredDay: Long
    ) {
        val iamId = groupService.getRelationId(roleId)
        if (iamId == null) {
            logger.warn("$roleId can not find relationId")
            throw ParamBlankException(MessageCodeUtil.getCodeLanMessage(AuthMessageCode.CAN_NOT_FIND_RELATION))
        }
        val iamProjectId = iamCacheService.getProjectIamRelationId(projectId)

        val roleMembers = mutableListOf<ManagerMember>()
        val userIds = mutableListOf<String>()
        members.forEach {
            if (it.type == UserType.USER) {
                checkUser(it.id)
                userIds.add(it.id)
                roleMembers.add(ManagerMember(ManagerScopesEnum.USER.name, it.id))
            } else if (it.type == UserType.DEPARTMENT) {
                roleMembers.add(ManagerMember(ManagerScopesEnum.DEPARTMENT.name, it.id))
            }
        }
        val expiredTime = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(expiredDay)
        val managerMemberGroupDTO = ManagerMemberGroupDTO.builder().expiredAt(expiredTime).members(roleMembers).build()
        try {
            iamManagerService.createRoleGroupMember(iamId!!.toInt(), managerMemberGroupDTO)
        } catch (iamEx: IamException) {
            logger.warn("create group user fail. code: ${iamEx.errorCode}| msg: ${iamEx.errorMsg}")
            throw OperationException(
                MessageCodeUtil.getCodeMessage(
                    messageCode = AuthMessageCode.IAM_SYSTEM_ERROR,
                    params = arrayOf(iamEx.errorMsg)
                ).toString()
            )
        } catch (e: Exception) {
            logger.warn("create group user fail. code: $e")
            throw OperationException(
                MessageCodeUtil.getCodeMessage(
                    messageCode = AuthMessageCode.IAM_SYSTEM_ERROR,
                    params = arrayOf(e.message ?: "unknown")
                ).toString()
            )
        }

        // 添加用户到管理员需要同步添加用户到分级管理员
        if (managerGroup) {
            if (userIds.isNotEmpty()) {
                val gradeMembers = ManagerRoleMemberDTO.builder().members(userIds).build()
                iamManagerService.batchCreateGradeManagerRoleMember(gradeMembers, iamProjectId)
            }
        }
    }

    override fun checkUser(userId: String) {
        TODO("Not yet implemented")
    }

    companion object {
        val logger = LoggerFactory.getLogger(IamPermissionRoleMemberServiceImpl::class.java)
        private const val EXPIRED_TIMEOUT_SIZE = 5
    }
}
