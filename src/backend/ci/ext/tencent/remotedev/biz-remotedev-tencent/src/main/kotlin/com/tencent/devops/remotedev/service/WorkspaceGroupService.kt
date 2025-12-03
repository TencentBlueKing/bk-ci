/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceGroupDao
import com.tencent.devops.remotedev.dao.WorkspaceGroupRelationDao
import com.tencent.devops.remotedev.pojo.WorkspaceGroup
import com.tencent.devops.remotedev.pojo.WorkspaceGroupCreate
import com.tencent.devops.remotedev.pojo.WorkspaceGroupItem
import com.tencent.devops.remotedev.pojo.WorkspaceGroupUpdate
import com.tencent.devops.remotedev.service.permission.WorkspaceGroupPermissionService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceGroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val permissionService: PermissionService,
    private val workspaceGroupDao: WorkspaceGroupDao,
    private val workspaceGroupRelationDao: WorkspaceGroupRelationDao,
    private val workspaceDao: WorkspaceDao,
    private val workspaceGroupPermissionService: WorkspaceGroupPermissionService
) {

    fun listGroups(userId: String, projectId: String): List<WorkspaceGroup> {
        if (!permissionService.checkUserVisitPermission(userId, projectId)) {
            return emptyList()
        }
        val groups = workspaceGroupDao.list(dslContext, projectId)
        val counts = workspaceGroupRelationDao.countByGroups(
            dslContext, projectId, groups.map { it.id }
        )
        return groups.map { g -> g.copy(workspaceCount = counts[g.id] ?: 0) }
    }

    fun createGroup(userId: String, req: WorkspaceGroupCreate): Boolean {
        permissionService.checkUserManager(userId, req.projectId)
        if (workspaceGroupDao.countByName(dslContext, req.projectId, req.name) > 0) {
            throw ErrorCodeException(errorCode = "DUPLICATE_GROUP_NAME", params = arrayOf(req.name))
        }

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            val groupId = workspaceGroupDao.create(
                dslContext = transactionContext,
                projectId = req.projectId,
                name = req.name,
                description = req.description,
                userId = userId
            )

            // 注册到权限中心
            workspaceGroupPermissionService.createResource(userId, req.projectId, groupId, req.name)
        }

        return true
    }

    fun updateGroup(userId: String, req: WorkspaceGroupUpdate): Boolean {
        permissionService.checkUserManager(userId, req.projectId)
        if (req.name != null && workspaceGroupDao.countByName(dslContext, req.projectId, req.name!!) > 0) {
            val exist = workspaceGroupDao.get(dslContext, req.projectId, req.groupId)
            if (exist == null || !exist.name.equals(req.name, ignoreCase = true)) {
                throw ErrorCodeException(errorCode = "DUPLICATE_GROUP_NAME", params = arrayOf(req.name!!))
            }
        }

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            workspaceGroupDao.update(
                dslContext = transactionContext,
                projectId = req.projectId,
                groupId = req.groupId,
                name = req.name,
                description = req.description,
                userId = userId
            )

            // 如果更新了名称，需要同步到权限中心
            if (req.name != null) {
                workspaceGroupPermissionService.editResource(req.projectId, req.groupId, req.name!!)
            }
        }

        return true
    }

    fun deleteGroup(userId: String, projectId: String, groupId: Long): Boolean {
        permissionService.checkUserManager(userId, projectId)

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)

            workspaceGroupRelationDao.removeByGroup(transactionContext, projectId, groupId)
            workspaceGroupDao.delete(transactionContext, projectId, groupId)

            // 从权限中心删除资源
            workspaceGroupPermissionService.deleteResource(projectId, groupId)
        }

        return true
    }

    fun addWorkspaces(userId: String, projectId: String, groupId: Long, names: List<String>): Boolean {
        permissionService.checkUserManager(userId, projectId)
        if (names.isEmpty()) return true
        val valid = workspaceDao.fetchProjectWorkspaceName(dslContext, projectId)
        val addList = names.filter { it in valid }
        workspaceGroupRelationDao.batchAdd(dslContext, projectId, groupId, addList, userId)
        return true
    }

    fun removeWorkspace(userId: String, projectId: String, groupId: Long, workspaceName: String): Boolean {
        permissionService.checkUserManager(userId, projectId)
        workspaceGroupRelationDao.remove(dslContext, projectId, groupId, workspaceName)
        return true
    }

    fun listWorkspaces(
        userId: String,
        projectId: String,
        groupId: Long,
        page: Int,
        pageSize: Int
    ): Page<WorkspaceGroupItem> {
        if (!permissionService.checkUserVisitPermission(userId, projectId)) {
            return Page(page = page, pageSize = pageSize, count = 0, records = emptyList())
        }

        // 先获取总数
        val count = workspaceGroupRelationDao.countWorkspaces(dslContext, projectId, groupId)
        if (count == 0L) {
            return Page(page = page, pageSize = pageSize, count = 0, records = emptyList())
        }

        // 在数据库层面进行分页，直接获取工作空间名称和创建时间
        val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val workspaceItems = workspaceGroupRelationDao.listWorkspaceNamesWithPage(
            dslContext = dslContext,
            projectId = projectId,
            groupId = groupId,
            offset = limit.offset,
            limit = limit.limit
        )

        if (workspaceItems.isEmpty()) {
            return Page(page = page, pageSize = pageSize, count = count, records = emptyList())
        }

        // 转换为返回对象
        val records = workspaceItems.map { (workspaceName, createTime) ->
            WorkspaceGroupItem(
                workspaceName = workspaceName,
                createTime = createTime.timestampmilli()
            )
        }

        return Page(page = page, pageSize = pageSize, count = count, records = records)
    }
}
