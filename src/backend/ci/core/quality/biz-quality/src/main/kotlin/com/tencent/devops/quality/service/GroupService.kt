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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.quality.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.quality.constant.QualityMessageCode
import com.tencent.devops.quality.dao.GroupDao
import com.tencent.devops.quality.pojo.Group
import com.tencent.devops.quality.pojo.GroupCreate
import com.tencent.devops.quality.pojo.GroupPermission
import com.tencent.devops.quality.pojo.GroupSummaryWithPermission
import com.tencent.devops.quality.pojo.GroupUpdate
import com.tencent.devops.quality.pojo.GroupUsers
import com.tencent.devops.quality.pojo.ProjectGroupAndUsers
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import javax.ws.rs.core.Response

@Service
class GroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val groupDao: GroupDao,
    private val bkAuthProjectApi: AuthProjectApi,
    private val bkAuthPermissionApi: AuthPermissionApi,
    private val bkAuthResourceApi: AuthResourceApi,
    private val serviceCode: QualityAuthServiceCode
) {

    private val resourceType = AuthResourceType.QUALITY_GROUP
    private val regex = Pattern.compile("[,;]")

    fun list(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<GroupSummaryWithPermission>> {
        val groupPermissionListMap = filterGroup(
            user = userId,
            projectId = projectId,
            authPermissions = setOf(AuthPermission.EDIT, AuthPermission.DELETE)
        )

        val count = groupDao.count(dslContext, projectId)
        val finalLimit = if (limit == -1) count.toInt() else limit
        val list = groupDao.list(dslContext, projectId, offset, finalLimit).map {
            val canEdit = groupPermissionListMap[AuthPermission.EDIT]!!.contains(it.id)
            val canDelete = groupPermissionListMap[AuthPermission.DELETE]!!.contains(it.id)
            GroupSummaryWithPermission(
                groupHashId = HashUtil.encodeLongId(it.id),
                name = it.name,
                innerUsersCount = it.innerUsersCount,
                outerUsersCount = it.outerUsersCount,
                innerUsers = objectMapper.readValue(it.innerUsers),
                outerUsers = it.outerUsers,
                creator = it.creator,
                remark = it.remark ?: "",
                permissions = GroupPermission(canEdit, canDelete)
            )
        }
        return Pair(count, list)
    }

    fun getProjectGroupAndUsers(userId: String, projectId: String): List<ProjectGroupAndUsers> {
        val groupAndUsersList = bkAuthProjectApi.getProjectGroupAndUserList(serviceCode, projectId)
        return groupAndUsersList.map {
            ProjectGroupAndUsers(
                groupName = MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${CommonMessageCode.MSG_CODE_ROLE_PREFIX}${it.roleName}",
                    defaultMessage = it.displayName
                ),
                groupId = it.roleName,
                users = it.userIdList.toSet()
            )
        }
    }

    fun create(userId: String, projectId: String, group: GroupCreate) {
        if (groupDao.has(dslContext, projectId, group.name)) {
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = QualityMessageCode.USER_GROUP_IS_EXISTS,
                defaultMessage = "用户组(${group.name})已存在",
                params = arrayOf(group.name)
            )
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        val groupId = groupDao.create(
            dslContext = dslContext,
            projectId = projectId,
            name = group.name,
            innerUsers = objectMapper.writeValueAsString(group.innerUsers),
            innerUsersCount = innerUsersCount,
            outerUsers = group.outerUsers,
            outerUsersCount = outerUsersCount,
            remark = group.remark,
            creator = userId,
            updator = userId
        )
        createResource(userId, projectId, groupId, group.name)
    }

    fun get(userId: String, projectId: String, groupHashId: String): Group {
        return serviceGet(groupHashId)
    }

    fun serviceGet(groupHashId: String): Group {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        val groupRecord = groupDao.get(dslContext, groupId)
        return Group(
            groupHashId = groupHashId,
            name = groupRecord.name,
            innerUsers = objectMapper.readValue(groupRecord.innerUsers),
            outerUsers = groupRecord.outerUsers,
            remark = groupRecord.remark ?: ""
        )
    }

    fun getUsers(userId: String, projectId: String, groupHashId: String): GroupUsers {
        return serviceGetUsers(groupHashId)
    }

    fun serviceGetUsers(groupHashId: String): GroupUsers {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        val groupRecord = groupDao.get(dslContext, groupId)
        val innerUsers = objectMapper.readValue<Set<String>>(groupRecord.innerUsers)
        val outerUsers = regex.split(groupRecord.outerUsers).toSet()
        return GroupUsers(innerUsers, outerUsers)
    }

    fun serviceGetUsers(groupHashIdList: List<String>): GroupUsers {
        val groupIdList = groupHashIdList.map { HashUtil.decodeIdToLong(it) }
        val result = groupDao.list(dslContext, groupIdList.toSet())
        val innerUsersSet = mutableSetOf<String>()
        val outerUsersSet = mutableSetOf<String>()

        result.forEach {
            val innerUsers = objectMapper.readValue<Set<String>>(it.innerUsers)
            val outerUsers = regex.split(it.outerUsers).toSet()
            innerUsersSet.addAll(innerUsers)
            outerUsersSet.addAll(outerUsers)
        }
        return GroupUsers(innerUsers = innerUsersSet, outerUsers = outerUsersSet)
    }

    fun edit(userId: String, projectId: String, groupHashId: String, group: GroupUpdate) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        validatePermission(
            userId = userId,
            projectId = projectId,
            groupId = groupId,
            authPermission = AuthPermission.EDIT,
            message = "用户没有用户组的编辑权限"
        )
        if (groupDao.getOrNull(dslContext, groupId) == null) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = QualityMessageCode.USER_GROUP_NOT_EXISTS,
                defaultMessage = "用户组($groupHashId)不存在",
                params = arrayOf(groupHashId)
            )
        }
        if (groupDao.has(dslContext, projectId, group.name, groupId)) {
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = QualityMessageCode.USER_GROUP_IS_EXISTS,
                defaultMessage = "用户组(${group.name})已存在",
                params = arrayOf(group.name)
            )
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        groupDao.update(
            dslContext = dslContext,
            id = groupId,
            name = group.name,
            innerUsers = objectMapper.writeValueAsString(group.innerUsers),
            innerUsersCount = innerUsersCount,
            outerUsers = group.outerUsers,
            outerUsersCount = outerUsersCount,
            remark = group.remark,
            updator = userId
        )
        modifyResource(projectId = projectId, groupId = groupId, groupName = group.name)
    }

    fun delete(userId: String, projectId: String, groupHashId: String) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        validatePermission(
            userId = userId,
            projectId = projectId,
            groupId = groupId,
            authPermission = AuthPermission.DELETE,
            message = "用户没有用户组的删除权限"
        )

        deleteResource(projectId = projectId, groupId = groupId)
        groupDao.delete(dslContext, groupId)
    }

    private fun validatePermission(userId: String, projectId: String, groupId: Long, authPermission: AuthPermission, message: String) {
        if (!bkAuthPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = serviceCode,
                resourceType = resourceType,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(groupId),
                permission = authPermission
            )) {
            logger.error(message)
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                defaultMessage = authPermission.alias
            )
            throw ErrorCodeException(
                errorCode = QualityMessageCode.NEED_USER_GROUP_X_PERMISSION,
                defaultMessage = message,
                params = arrayOf(permissionMsg))
        }
    }

    private fun createResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        bkAuthResourceApi.createResource(
            user = userId,
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    private fun modifyResource(projectId: String, groupId: Long, groupName: String) {
        bkAuthResourceApi.modifyResource(
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    private fun deleteResource(projectId: String, groupId: Long) {
        bkAuthResourceApi.deleteResource(
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId)
        )
    }

    private fun filterGroup(user: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bkAuthPermissionApi.getUserResourcesByPermissions(
            user = user,
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = null
        )
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { (key, value) ->
            map[key] = value.map { HashUtil.decodeIdToLong(it) }
        }
        return map
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GroupService::class.java)
    }
}