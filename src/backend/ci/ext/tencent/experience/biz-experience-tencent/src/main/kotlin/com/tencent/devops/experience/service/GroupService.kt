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
package com.tencent.devops.experience.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.ExperienceAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.ExperienceGroupDao
import com.tencent.devops.experience.dao.ExperienceGroupInnerDao
import com.tencent.devops.experience.dao.ExperienceGroupOuterDao
import com.tencent.devops.experience.dao.GroupDao
import com.tencent.devops.experience.pojo.Group
import com.tencent.devops.experience.pojo.GroupCreate
import com.tencent.devops.experience.pojo.GroupPermission
import com.tencent.devops.experience.pojo.GroupSummaryWithPermission
import com.tencent.devops.experience.pojo.GroupUpdate
import com.tencent.devops.experience.pojo.GroupUsers
import com.tencent.devops.experience.pojo.NotifyType
import com.tencent.devops.experience.pojo.ProjectGroupAndUsers
import com.tencent.devops.experience.pojo.enums.ProjectGroup
import com.tencent.devops.experience.util.DateUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class GroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val groupDao: GroupDao,
    private val bsAuthProjectApi: AuthProjectApi,
    private val experienceServiceCode: ExperienceAuthServiceCode,
    private val experienceGroupInnerDao: ExperienceGroupInnerDao,
    private val experienceGroupOuterDao: ExperienceGroupOuterDao,
    private val experienceGroupDao: ExperienceGroupDao,
    private val experienceDao: ExperienceDao,
    private val objectMapper: ObjectMapper,
    private val experienceBaseService: ExperienceBaseService,
    private val experiencePermissionService: ExperiencePermissionService,
    private val experienceService: ExperienceService
) {

    private val resourceType = AuthResourceType.EXPERIENCE_GROUP

    fun list(
        userId: String,
        projectId: String,
        offset: Int,
        limit: Int,
        returnPublic: Boolean
    ): Pair<Long, List<GroupSummaryWithPermission>> {
        // todo 增加List类型的experience。对于rbac才返回有列表权限的版本体验，其他的直接返回。
        val groupPermissionListMap = experiencePermissionService.filterGroup(
            user = userId,
            projectId = projectId,
            authPermissions = setOf(AuthPermission.EDIT, AuthPermission.DELETE)
        )

        val addPublicElement = offset == 0 && returnPublic
        val count = groupDao.count(dslContext, projectId)
        val finalLimit = if (limit == -1) count.toInt() else limit
        val groups = groupDao.list(
            dslContext,
            projectId,
            offset,
            finalLimit
        )
        val groupIds = groups.map { it.id }.toSet()

        val groupIdToInnerUserIds = experienceBaseService.getGroupIdToInnerUserIds(groupIds)
        val groupIdToOuters = experienceBaseService.getGroupIdToOuters(groupIds)

        val list = groups.map {
            val canEdit = groupPermissionListMap[AuthPermission.EDIT]?.contains(it.id) ?: false
            val canDelete = groupPermissionListMap[AuthPermission.DELETE]?.contains(it.id) ?: false
            GroupSummaryWithPermission(
                groupHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                innerUsersCount = groupIdToInnerUserIds[it.id]?.size ?: 0,
                outerUsersCount = groupIdToOuters[it.id]?.size ?: 0,
                innerUsers = groupIdToInnerUserIds[it.id] ?: emptySet(),
                outerUsers = groupIdToOuters[it.id] ?: emptySet(),
                creator = it.creator,
                remark = it.remark ?: "",
                permissions = GroupPermission(canEdit, canDelete)
            )
        }

        if (addPublicElement) {
            list.add(
                index = 0,
                element = GroupSummaryWithPermission(
                    groupHashId = HashUtil.encodeLongId(ExperienceConstant.PUBLIC_GROUP),
                    name = ExperienceConstant.PUBLIC_NAME,
                    innerUsersCount = 1,
                    outerUsersCount = 0,
                    innerUsers = ExperienceConstant.PUBLIC_INNER_USERS,
                    outerUsers = emptySet(),
                    creator = "admin",
                    remark = "",
                    permissions = GroupPermission(canEdit = false, canDelete = false)
                )
            )
        }

        return Pair(count, list)
    }

    fun getProjectUsers(userId: String, projectId: String, projectGroup: ProjectGroup?): List<String> {
        val bkAuthGroup = if (projectGroup == null) null else BkAuthGroup.valueOf(projectGroup.name)
        return bsAuthProjectApi.getProjectUsers(experienceServiceCode, projectId, bkAuthGroup)
    }

    fun getProjectGroupAndUsers(userId: String, projectId: String): List<ProjectGroupAndUsers> {
        val groupAndUsersList = bsAuthProjectApi.getProjectGroupAndUserList(experienceServiceCode, projectId)
        return groupAndUsersList.map {
            ProjectGroupAndUsers(
                groupName = MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${it.roleName}",
                    defaultMessage = it.displayName
                ),
                groupId = it.roleName,
                groupRoleId = it.roleId,
                users = it.userIdList.toSet()
            )
        }
    }

    fun create(projectId: String, userId: String, group: GroupCreate): String {
        if (groupDao.has(dslContext, projectId, group.name)) {
            throw ErrorCodeException(
                defaultMessage = "体验组(${group.name})已存在",
                errorCode = ExperienceMessageCode.EXP_GROUP_IS_EXISTS,
                params = arrayOf(group.name)
            )
        }

        val innerUsersCount = group.innerUsers.size

        val groupId = groupDao.create(
            dslContext = dslContext,
            projectId = projectId,
            name = group.name,
            innerUsers = "[]",
            innerUsersCount = innerUsersCount,
            remark = group.remark,
            creator = userId,
            updator = userId
        )

        // 增加权限
        group.innerUsers.forEach {
            experienceGroupInnerDao.create(dslContext, groupId, it)
        }
        group.outerUsers.forEach {
            experienceGroupOuterDao.create(dslContext, groupId, it)
        }

        experiencePermissionService.createGroupResource(
            userId = userId,
            projectId = projectId,
            groupId = groupId,
            groupName = group.name
        )

        return HashUtil.encodeLongId(groupId)
    }

    fun get(userId: String, projectId: String, groupHashId: String): Group {
        return serviceGet(groupHashId)
    }

    fun serviceGet(groupHashId: String): Group {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        val groupRecord = groupDao.get(dslContext, groupId)
        val userIds = experienceGroupInnerDao.listByGroupIds(dslContext, setOf(groupId)).map { it.userId }.toSet()
        val outers = experienceGroupOuterDao.listByGroupIds(dslContext, setOf(groupId)).map { it.outer }.toSet()
        return Group(
            groupHashId = groupHashId,
            name = groupRecord.name,
            innerUsers = userIds,
            outerUsers = outers,
            remark = groupRecord.remark ?: ""
        )
    }

    fun getUsers(userId: String, projectId: String, groupHashId: String): GroupUsers {
        return serviceGetUsers(groupHashId)
    }

    fun serviceGetUsers(groupHashId: String): GroupUsers {
        val groupId = HashUtil.decodeIdToLong(groupHashId)

        val groupRecord = groupDao.get(dslContext, groupId)
        val innerUsers = experienceGroupInnerDao.listByGroupIds(dslContext, setOf(groupId)).map { it.userId }.toSet()
        val outerUsers = experienceGroupOuterDao.listByGroupIds(dslContext, setOf(groupId)).map { it.outer }.toSet()
        return GroupUsers(innerUsers, outerUsers)
    }

    fun edit(userId: String, projectId: String, groupHashId: String, group: GroupUpdate) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        experiencePermissionService.validateGroupPermission(
            userId = userId,
            projectId = projectId,
            groupId = groupId,
            authPermission = AuthPermission.EDIT,
            message = "用户在项目($projectId)没有体验组($groupHashId)的编辑权限"
        )
        if (groupDao.getOrNull(dslContext, groupId) == null) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                defaultMessage = "体验组($groupHashId)不存在",
                errorCode = ExperienceMessageCode.EXP_GROUP_NOT_EXISTS,
                params = arrayOf(groupHashId)
            )
        }
        if (groupDao.has(dslContext, projectId, group.name, groupId)) {
            throw ErrorCodeException(
                defaultMessage = "体验组(${group.name})已存在",
                errorCode = ExperienceMessageCode.EXP_GROUP_IS_EXISTS,
                params = arrayOf(group.name)
            )
        }

        val innerUsersCount = group.innerUsers.size

        groupDao.update(
            dslContext = dslContext,
            id = groupId,
            name = group.name,
            innerUsers = "[]",
            innerUsersCount = innerUsersCount,
            remark = group.remark,
            updator = userId
        )
        // 新增内部人员
        val oldInnerUsers = experienceGroupInnerDao.listByGroupIds(dslContext, setOf(groupId)).map { it.userId }.toSet()
        val latestInnerUsers = group.innerUsers
        val newAddInnerUsers = latestInnerUsers.subtract(oldInnerUsers).toMutableSet()
        // 新增外部人员
        val oldOuterUsers = experienceGroupOuterDao.listByGroupIds(dslContext, setOf(groupId)).map { it.outer }.toSet()
        val latestOldOuterUsers = group.outerUsers
        val newAddOuterUsers = latestOldOuterUsers.subtract(oldOuterUsers).toMutableSet()
        // 向新增人员发送最新版本体验信息
        if (newAddOuterUsers.isNotEmpty()) {
            sendNotificationToNewAddUser(
                newAddUsers = newAddOuterUsers,
                userType = NEW_ADD_OUTER_USERS,
                groupId = groupId
            )
        }
        if (newAddInnerUsers.isNotEmpty()) {
            sendNotificationToNewAddUser(
                newAddUsers = newAddInnerUsers,
                userType = NEW_ADD_INNER_USERS,
                groupId = groupId
            )
        }

        // 修改权限
        experienceGroupInnerDao.deleteByGroupId(dslContext, groupId)
        group.innerUsers.forEach {
            experienceGroupInnerDao.create(dslContext, groupId, it)
        }
        experienceGroupOuterDao.deleteByGroupId(dslContext, groupId)
        group.outerUsers.forEach {
            experienceGroupOuterDao.create(dslContext, groupId, it)
        }

        experiencePermissionService.modifyGroupResource(
            projectId = projectId,
            groupId = groupId,
            groupName = group.name
        )
    }

    private fun sendNotificationToNewAddUser(
        newAddUsers: MutableSet<String>,
        userType: String,
        groupId: Long
    ) {
        val experienceIds = mutableSetOf<Long>()
        val groupIds = mutableSetOf<Long>()
        groupIds.add(groupId)
        experienceIds.addAll(
            experienceGroupDao.listRecordIdByGroupIds(dslContext, groupIds)
                .map { it.value1() }.toSet()
        )
        experienceIds.forEach continuing@{ experienceId ->
            val experienceRecord = experienceDao.get(dslContext, experienceId)
            if (DateUtil.isExpired(experienceRecord.endDate) || !experienceRecord.online) {
                return@continuing
            }
            when (userType) {
                NEW_ADD_OUTER_USERS -> {
                    experienceService.sendMessageToOuterReceivers(
                        outerReceivers = newAddUsers,
                        experienceRecord = experienceRecord
                    )
                }

                NEW_ADD_INNER_USERS -> {
                    val notifyTypeList = objectMapper.readValue<Set<NotifyType>>(experienceRecord.notifyTypes)
                    val pcUrl = experienceService.getPcUrl(experienceRecord.projectId, experienceId)
                    val appUrl = experienceService.getShortExternalUrl(experienceId)
                    val projectName =
                        client.get(ServiceProjectResource::class).get(experienceRecord.projectId).data!!.projectName
                    experienceService.sendMessageToInnerReceivers(
                        notifyTypeList = notifyTypeList,
                        projectName = projectName,
                        innerReceivers = newAddUsers,
                        experienceRecord = experienceRecord,
                        pcUrl = pcUrl,
                        appUrl = appUrl
                    )
                }
            }
        }
    }

    fun delete(userId: String, projectId: String, groupHashId: String) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        experiencePermissionService.validateGroupPermission(
            userId = userId,
            projectId = projectId,
            groupId = groupId,
            authPermission = AuthPermission.DELETE,
            message = "用户在项目($projectId)没有体验组($groupHashId)的删除权限"
        )

        experiencePermissionService.deleteGroupResource(projectId, groupId)
        groupDao.delete(dslContext, groupId)
        experienceGroupDao.deleteByGroupId(dslContext, groupId)
        experienceGroupInnerDao.deleteByGroupId(dslContext, groupId)
        experienceGroupOuterDao.deleteByGroupId(dslContext, groupId)
    }

    companion object {
        const val NEW_ADD_OUTER_USERS = "new_add_outer_users"
        const val NEW_ADD_INNER_USERS = "new_add_inner_users"
    }
}
