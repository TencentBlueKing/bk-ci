package com.tencent.devops.quality.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.quality.pojo.Group
import com.tencent.devops.quality.pojo.GroupCreate
import com.tencent.devops.quality.dao.GroupDao
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
import javax.ws.rs.NotFoundException
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
        val groupPermissionListMap = filterGroup(userId, projectId, setOf(AuthPermission.EDIT, AuthPermission.DELETE))

        val count = groupDao.count(dslContext, projectId)
        val finalLimit = if (limit == -1) count.toInt() else limit
        val list = groupDao.list(dslContext, projectId, offset, finalLimit).map {
            val canEdit = groupPermissionListMap[AuthPermission.EDIT]!!.contains(it.id)
            val canDelete = groupPermissionListMap[AuthPermission.DELETE]!!.contains(it.id)
            GroupSummaryWithPermission(
                    HashUtil.encodeLongId(it.id),
                    it.name,
                    it.innerUsersCount,
                    it.outerUsersCount,
                    objectMapper.readValue(it.innerUsers),
                    it.outerUsers,
                    it.creator,
                    it.remark ?: "",
                    GroupPermission(canEdit, canDelete)
            )
        }
        return Pair(count, list)
    }

    fun getProjectGroupAndUsers(userId: String, projectId: String): List<ProjectGroupAndUsers> {
        val groupAndUsersList = bkAuthProjectApi.getProjectGroupAndUserList(serviceCode, projectId)
        return groupAndUsersList.map {
            ProjectGroupAndUsers(
                    it.displayName,
                    it.roleName,
                    it.userIdList.toSet()
            )
        }
    }

    fun create(userId: String, projectId: String, group: GroupCreate) {
        if (groupDao.has(dslContext, projectId, group.name)) {
            throw CustomException(Response.Status.BAD_REQUEST, "用户组(${group.name})已存在")
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        val groupId = groupDao.create(dslContext,
                projectId,
                group.name,
                objectMapper.writeValueAsString(group.innerUsers),
                innerUsersCount,
                group.outerUsers,
                outerUsersCount,
                group.remark,
                userId,
                userId)
        createResource(userId, projectId, groupId, group.name)
    }

    fun get(userId: String, projectId: String, groupHashId: String): Group {
        return serviceGet(groupHashId)
    }

    fun serviceGet(groupHashId: String): Group {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        val groupRecord = groupDao.get(dslContext, groupId)
        return Group(
                groupHashId,
                groupRecord.name,
                objectMapper.readValue(groupRecord.innerUsers),
                groupRecord.outerUsers,
                groupRecord.remark ?: ""
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
        return GroupUsers(innerUsersSet, outerUsersSet)
    }

    fun edit(userId: String, projectId: String, groupHashId: String, group: GroupUpdate) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        validatePermission(userId, projectId, groupId, AuthPermission.EDIT, "用户在项目($projectId)没有用户组($groupHashId)的编辑权限")
        if (groupDao.getOrNull(dslContext, groupId) == null) {
            throw NotFoundException("用户组($groupHashId)不存在")
        }
        if (groupDao.has(dslContext, projectId, group.name, groupId)) {
            throw CustomException(Response.Status.BAD_REQUEST, "用户组(${group.name})已存在")
        }

        val outerUsers = regex.split(group.outerUsers)
        val outerUsersCount = outerUsers.filter { it.isNotBlank() && it.isNotEmpty() }.size
        val innerUsersCount = group.innerUsers.size

        groupDao.update(dslContext,
                groupId,
                group.name,
                objectMapper.writeValueAsString(group.innerUsers),
                innerUsersCount,
                group.outerUsers,
                outerUsersCount,
                group.remark,
                userId)
        modifyResource(projectId, groupId, group.name)
    }

    fun delete(userId: String, projectId: String, groupHashId: String) {
        val groupId = HashUtil.decodeIdToLong(groupHashId)
        validatePermission(userId, projectId, groupId, AuthPermission.DELETE, "用户在项目($projectId)没有用户组($groupHashId)的删除权限")

        deleteResource(projectId, groupId)
        groupDao.delete(dslContext, groupId)
    }

    private fun validatePermission(userId: String, projectId: String, groupId: Long, authPermission: AuthPermission, message: String) {
        if (!bkAuthPermissionApi.validateUserResourcePermission(userId, serviceCode, resourceType, projectId, HashUtil.encodeLongId(groupId), authPermission)) {
            logger.error(message)
            throw PermissionForbiddenException(message)
        }
    }

    private fun createResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        bkAuthResourceApi.createResource(userId, serviceCode, resourceType, projectId, HashUtil.encodeLongId(groupId), groupName)
    }

    private fun modifyResource(projectId: String, groupId: Long, groupName: String) {
        bkAuthResourceApi.modifyResource(serviceCode, resourceType, projectId, HashUtil.encodeLongId(groupId), groupName)
    }

    private fun deleteResource(projectId: String, groupId: Long) {
        bkAuthResourceApi.deleteResource(serviceCode, resourceType, projectId, HashUtil.encodeLongId(groupId))
    }

    private fun filterGroup(user: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bkAuthPermissionApi.getUserResourcesByPermissions(user, serviceCode, resourceType, projectId, authPermissions, null)
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { key, value ->
            map[key] = value.map { HashUtil.decodeIdToLong(it) }
        }
        return map
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GroupService::class.java)
    }
}