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

package com.tencent.devops.auth.service

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.dao.AuthGroupMemberDao
import com.tencent.devops.auth.entity.GroupMemberInfo
import com.tencent.devops.auth.pojo.GroupMember
import com.tencent.devops.auth.pojo.MemberInfo
import com.tencent.devops.auth.pojo.dto.GroupMemberDTO
import com.tencent.devops.auth.pojo.dto.UserGroupInfoDTO
import com.tencent.devops.auth.pojo.enum.ExpiredStatus
import com.tencent.devops.auth.pojo.enum.UserType
import com.tencent.devops.auth.pojo.vo.ProjectMembersVO
import com.tencent.devops.auth.service.ci.impl.AbsPermissionRoleMemberImpl
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.auth.utils.IamGroupUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class AuthGroupMemberService @Autowired constructor(
    val dslContext: DSLContext,
    val authGroupMemberDao: AuthGroupMemberDao
){

    private val userProjectCache = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .maximumSize(100)
        .build<String, List<Int>?>()

    fun createGroupMember(
        memberInfo: GroupMemberInfo
    ) {
        authGroupMemberDao.createGroupMember(
            dslContext = dslContext,
            groupMember = memberInfo
        )
    }

    fun batchCreateGroupMember(
        memberInfos: List<GroupMemberInfo>
    ) {
        authGroupMemberDao.batchCreateMember(
            dslContext = dslContext,
            groupMembers = memberInfos
        )
    }

    fun deleteGroupMember(
        groupId: Int,
        userId: String
    ) {
        val count = authGroupMemberDao.deleteGroupMember(
            dslContext = dslContext,
            groupId = groupId,
            userId = userId
        )
    }

    fun getRoleMember(
        roleId: Int,
        projectCode: String
    ): GroupMemberDTO? {
        val groupMembers = authGroupMemberDao.getGroupMemberByGroup(
            dslContext = dslContext,
            groupId = roleId
        ) ?: return null
        val groupMember = mutableListOf<GroupMember>()
        groupMembers.map {
            val expiredAt = DateTimeUtil.convertLocalDateTimeToTimestamp(it.expiredTiem)
            groupMember.add(
                GroupMember(
                    id = it.userId,
                    type = UserType.getUserType(it.userType).type,
                    createTime = DateTimeUtil.convertLocalDateTimeToTimestamp(it.createTime),
                    expiredAt = expiredAt,
                    expiredStatus = ExpiredStatus.buildExpiredStatus(expiredAt, TimeUnit.DAYS.toMillis(5))
                )
            )
        }
        return GroupMemberDTO(
            result = groupMember,
            count = groupMember.size
        )
    }

    fun getUserGroupByProject(
        userId: String,
        projectCode: String
    ): List<UserGroupInfoDTO>? {
        val memberGroupInfos = authGroupMemberDao.getGroupMemberByProject(
            dslContext = dslContext,
            projectCode = projectCode,
            userId = userId
        ) ?: return null
        val groupInfos = mutableListOf<UserGroupInfoDTO>()
        memberGroupInfos.map {
            val expiredAt = DateTimeUtil.convertLocalDateTimeToTimestamp(it.expiredTiem)
            // TODO: 获取用户组信息
            UserGroupInfoDTO(
                groupId = it.groupId.toString(),
                groupDesc = "",
                expiredAt = expiredAt,
                expiredStatus = ExpiredStatus.buildExpiredStatus(expiredAt, TimeUnit.DAYS.toMillis(5)),
                groupName = "",
                groupType = it.groupType
            )
        }

        return groupInfos
    }

    fun getProjectMember(
        projectCode: String
    ): List<String>? {
        val members = mutableListOf<String>()
        authGroupMemberDao.getGroupMemberByProject(
            dslContext = dslContext,
            projectCode = projectCode,
            userId = null
        )?.map {
            members.add(it.userId)
        }
        return members
    }

    fun getProjectMemberMap(
        projectCode: String
    ): Map<String, List<String>> {
        val groupMembers = mutableMapOf<String, List<String>>()
        val memberInfos = authGroupMemberDao.getGroupMemberByProject(
            dslContext = dslContext,
            projectCode = projectCode,
            userId = null
        ) ?: return emptyMap()
        memberInfos.forEach {
            if (groupMembers.containsKey(it.groupId.toString())) {
                val newMembers = mutableListOf<String>()
                val members = groupMembers[it.groupId.toString()]
                newMembers.addAll(members!!)
                newMembers.add(it.userId)
                groupMembers[it.groupId.toString()] = newMembers
            } else {
                groupMembers[it.groupId.toString()] = arrayListOf(it.userId)
            }
        }
        return groupMembers
    }

    /**
     * 优先从缓存里面取
     */
    fun getUserJoinGroup(
        userId: String
    ): List<Int>? {
        if (userProjectCache.getIfPresent(userId) != null) {
            return userProjectCache.getIfPresent(userId)
        }
        val userJoinGroup = authGroupMemberDao.getUserGroup(
            dslContext, userId
        )
        if (userJoinGroup.isNullOrEmpty()) {
            userProjectCache.put(userId, emptyList())
            return null
        }
        val groupIds = mutableListOf<Int>()
        userJoinGroup.map {
            groupIds.add(it.groupId)
        }
        userProjectCache.put(userId, groupIds)
        return groupIds
    }

    fun getProjectMemberList(
        projectCode: String
    ): ProjectMembersVO? {
        val memberInfos = authGroupMemberDao.getGroupMemberByProject(
            dslContext = dslContext,
            projectCode = projectCode,
            userId = null
        ) ?: return null
        val count = memberInfos.size
        // Map<用户名， 用户加入的用户组列表>
        val memberMap = mutableMapOf<String, MemberInfo>()
        memberInfos.map {
            if (memberMap.containsKey(it.userId)) {
                val memberInfo = memberMap[it.id]
                // 追加用户加入的用户组
                memberInfo?.groups?.add(it.groupId.toString())
            } else {
                // 添加用户加入的用户组
                memberMap[it.userId] = MemberInfo(
                    id = it.userId,
                    type = UserType.getUserType(it.userType).type,
                    name = it.userId,
                    groups = setOf(it.groupId) as MutableSet<String>
                )
            }
        }
        return ProjectMembersVO(
            results = memberMap,
            count = count
        )
    }
}