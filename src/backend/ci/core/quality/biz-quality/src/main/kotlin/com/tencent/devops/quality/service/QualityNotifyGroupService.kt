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

package com.tencent.devops.quality.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.quality.constant.QualityMessageCode
import com.tencent.devops.quality.constant.QualityMessageCode.NEED_USER_GROUP_X_PERMISSION
import com.tencent.devops.quality.constant.QualityMessageCode.USER_DOES_NOT_HAVE_PERMISSION_TO_CREATE_QUALITY
import com.tencent.devops.quality.dao.QualityNotifyGroupDao
import com.tencent.devops.quality.pojo.Group
import com.tencent.devops.quality.pojo.GroupCreate
import com.tencent.devops.quality.pojo.GroupPermission
import com.tencent.devops.quality.pojo.GroupSummaryWithPermission
import com.tencent.devops.quality.pojo.GroupUpdate
import com.tencent.devops.quality.pojo.GroupUsers
import com.tencent.devops.quality.pojo.ProjectGroupAndUsers
import org.apache.commons.lang3.math.NumberUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.regex.Pattern
import javax.ws.rs.core.Response

@Service
class QualityNotifyGroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val qualityNotifyGroupDao: QualityNotifyGroupDao,
    private val bkAuthProjectApi: AuthProjectApi,
    private val serviceCode: QualityAuthServiceCode,
    private val qualityPermissionService: QualityPermissionService
) {

    private val resourceType = AuthResourceType.QUALITY_GROUP
    private val regex = Pattern.compile("[,;]")

    fun list(userId: String, projectId: String, offset: Int, limit: Int): Pair<Long, List<GroupSummaryWithPermission>> {
        val allGroupIds = qualityNotifyGroupDao.listIds(
            dslContext = dslContext,
            projectId = projectId
        ).map { it.value1() }
        val hasListPermissionGroupIds = qualityPermissionService.filterListPermissionGroups(
            userId = userId,
            projectId = projectId,
            allGroupIds = allGroupIds
        )
        if (hasListPermissionGroupIds.isEmpty())
            return Pair(0, listOf())
        val count = hasListPermissionGroupIds.size
        val groupPermissionListMap = qualityPermissionService.filterGroup(
            user = userId,
            projectId = projectId,
            authPermissions = setOf(AuthPermission.EDIT, AuthPermission.DELETE)
        )

        val finalLimit = if (limit == -1) count else limit
        val ruleRecordList = qualityNotifyGroupDao.listByIds(
            dslContext = dslContext,
            projectId = projectId,
            groupIds = hasListPermissionGroupIds,
            offset = offset,
            limit = finalLimit
        )
        val list = ruleRecordList.map {
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
        return Pair(count.toLong(), list)
    }

    fun getProjectGroupAndUsers(userId: String, projectId: String): List<ProjectGroupAndUsers> {
        val groupAndUsersList = bkAuthProjectApi.getProjectGroupAndUserList(serviceCode, projectId)
        return groupAndUsersList.map {
            ProjectGroupAndUsers(
                groupName = it.displayName,
                groupId = it.roleName,
                users = it.userIdList.toSet()
            )
        }
    }

    fun create(userId: String, projectId: String, group: GroupCreate) {
        qualityPermissionService.validateGroupPermission(
            userId = userId,
            projectId = projectId,
            authPermission = AuthPermission.CREATE,
            message = I18nUtil.getCodeLanMessage(USER_DOES_NOT_HAVE_PERMISSION_TO_CREATE_QUALITY)
        )
        if (qualityNotifyGroupDao.has(dslContext, projectId, group.name)) {
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = QualityMessageCode.USER_GROUP_IS_EXISTS,
                params = arrayOf(group.name)
            )
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        val groupId = qualityNotifyGroupDao.create(
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
        qualityPermissionService.createGroupResource(userId, projectId, groupId, group.name)
    }

    fun get(userId: String, projectId: String, groupHashId: String): Group {
        return serviceGet(groupHashId)
    }

    fun serviceGet(groupHashId: String): Group {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        val groupRecord = qualityNotifyGroupDao.get(dslContext, groupId)
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
        val groupRecord = qualityNotifyGroupDao.get(dslContext, groupId)
        val innerUsers = objectMapper.readValue<Set<String>>(groupRecord.innerUsers)
        val outerUsers = regex.split(groupRecord.outerUsers).toSet()
        return GroupUsers(innerUsers, outerUsers)
    }

    fun serviceGetUsers(groupIdList: List<String>): GroupUsers {
        val queryGroupIdList = mutableListOf<Long>()

        groupIdList.forEach {
            if (NumberUtils.isDigits(it)) {
                queryGroupIdList.add(it.toLong())
            }
        }

        val result = qualityNotifyGroupDao.list(dslContext, queryGroupIdList)
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
        val language = I18nUtil.getLanguage(userId)
        val authPermission = AuthPermission.EDIT
        qualityPermissionService.validateGroupPermission(
            userId = userId,
            projectId = projectId,
            groupId = groupId,
            authPermission = authPermission,
            message = MessageUtil.getMessageByLocale(
                NEED_USER_GROUP_X_PERMISSION,
                language,
                arrayOf(authPermission.getI18n(I18nUtil.getLanguage(userId)))
            )
        )
        if (qualityNotifyGroupDao.getOrNull(dslContext, groupId) == null) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = QualityMessageCode.USER_GROUP_NOT_EXISTS,
                params = arrayOf(groupHashId)
            )
        }
        if (qualityNotifyGroupDao.has(dslContext, projectId, group.name, groupId)) {
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = QualityMessageCode.USER_GROUP_IS_EXISTS,
                params = arrayOf(group.name)
            )
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        qualityNotifyGroupDao.update(
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
        qualityPermissionService.modifyGroupResource(projectId = projectId, groupId = groupId, groupName = group.name)
    }

    fun delete(userId: String, projectId: String, groupHashId: String) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        val language = I18nUtil.getLanguage(userId)
        val authPermission = AuthPermission.DELETE
        qualityPermissionService.validateGroupPermission(
            userId = userId,
            projectId = projectId,
            groupId = groupId,
            authPermission = authPermission,
            message = MessageUtil.getMessageByLocale(
                NEED_USER_GROUP_X_PERMISSION,
                language,
                arrayOf(authPermission.getI18n(I18nUtil.getLanguage(userId)))
            )
        )

        qualityPermissionService.deleteGroupResource(projectId = projectId, groupId = groupId)
        qualityNotifyGroupDao.delete(dslContext, groupId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(QualityNotifyGroupService::class.java)
    }
}
