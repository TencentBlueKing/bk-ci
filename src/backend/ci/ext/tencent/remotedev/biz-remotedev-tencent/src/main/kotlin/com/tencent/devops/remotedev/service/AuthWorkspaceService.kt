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

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.auth.api.AuthTokenApi
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.remotedev.dao.WorkspaceGroupDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.common.QueryType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthWorkspaceService @Autowired constructor(
    private val authTokenApi: AuthTokenApi,
    private val dslContext: DSLContext,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val workspaceGroupDao: WorkspaceGroupDao,
    private val workspaceSharedDao: WorkspaceSharedDao
) {

    /**
     * 工作空间回调入口
     */
    fun workspaceInfo(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        authTokenApi.checkToken(token)
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent?.id ?: ""
        return when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> listWorkspace(
                projectId = projectId,
                offset = page.offset.toInt(),
                limit = page.limit.toInt()
            )

            CallbackMethodEnum.FETCH_INSTANCE_INFO -> fetchWorkspaceInfo(
                ids = callBackInfo.filter.idList.map { it.toString() }
            )

            CallbackMethodEnum.SEARCH_INSTANCE -> searchWorkspace(
                projectId = projectId,
                keyword = callBackInfo.filter.keyword ?: "",
                limit = page.limit.toInt(),
                offset = page.offset.toInt()
            )

            else -> null
        }
    }

    /**
     * 工作空间分组回调入口
     */
    fun workspaceGroupInfo(callBackInfo: CallbackRequestDTO, token: String): CallbackBaseResponseDTO? {
        authTokenApi.checkToken(token)
        val method = callBackInfo.method
        val page = callBackInfo.page
        val projectId = callBackInfo.filter.parent?.id ?: ""
        return when (method) {
            CallbackMethodEnum.LIST_INSTANCE -> listWorkspaceGroup(
                projectId = projectId,
                offset = page.offset.toInt(),
                limit = page.limit.toInt()
            )

            CallbackMethodEnum.FETCH_INSTANCE_INFO -> fetchWorkspaceGroupInfo(
                ids = callBackInfo.filter.idList.map { it.toString() }
            )

            CallbackMethodEnum.SEARCH_INSTANCE -> searchWorkspaceGroup(
                projectId = projectId,
                keyword = callBackInfo.filter.keyword ?: "",
                limit = page.limit.toInt(),
                offset = page.offset.toInt()
            )

            else -> null
        }
    }

    private fun listWorkspace(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO {
        val search = WorkspaceSearch(projectId = listOf(projectId))
        val data = workspaceJoinDao.limitFetchProjectWorkspace(
            dslContext = dslContext,
            limit = SQLLimit(offset, limit),
            queryType = QueryType.WEB,
            search = search
        ) ?: emptyList()
        val count = workspaceJoinDao.countProjectWorkspace(dslContext, QueryType.WEB, search)
        val instances = data.map { rec ->
            val entity = InstanceInfoDTO()
            entity.id = rec.workspaceName
            entity.displayName = rec.workspaceName
            entity
        }
        return ListInstanceInfo().buildListInstanceResult(instances, count)
    }

    private fun fetchWorkspaceInfo(ids: List<String>): FetchInstanceInfoResponseDTO {
        val names = ids.toSet()
        val data = workspaceJoinDao.fetchWindowsWorkspaces(
            dslContext = dslContext,
            workspaceNames = names,
            checkField = WorkspaceJoinDao.windowsFullFields
        )
        val owners = mutableMapOf<String, String>()
        workspaceSharedDao.batchFetchWorkspaceSharedInfo(dslContext, names).forEach {
            when (it.type) {
                WorkspaceShared.AssignType.OWNER -> {
                    owners.putIfAbsent(it.workspaceName, it.sharedUser)
                }
                else -> {}
            }
        }

        val instances = data.map { rec ->
            val entity = InstanceInfoDTO()
            entity.id = rec.workspaceName
            entity.displayName = rec.workspaceName
            entity.iamApprover = arrayListOf(owners[rec.workspaceName] ?: rec.createUserId)
            entity
        }
        return FetchInstanceInfo().buildFetchInstanceResult(instances)
    }

    private fun searchWorkspace(
        projectId: String,
        keyword: String,
        limit: Int,
        offset: Int
    ): SearchInstanceInfo {
        // 目前仅按名称模糊
        val search = WorkspaceSearch(
            projectId = listOf(projectId),
            workspaceName = listOf(keyword),
            onFuzzyMatch = true
        )
        val data = workspaceJoinDao.limitFetchProjectWorkspace(
            dslContext = dslContext,
            limit = SQLLimit(offset, limit),
            queryType = QueryType.WEB,
            search = search
        ) ?: emptyList()
        val count = workspaceJoinDao.countProjectWorkspace(dslContext, QueryType.WEB, search)
        val instances = data.map { rec ->
            val entity = InstanceInfoDTO()
            entity.id = rec.workspaceName
            entity.displayName = rec.workspaceName
            entity
        }
        return SearchInstanceInfo().buildSearchInstanceResult(instances, count)
    }

    private fun listWorkspaceGroup(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO {
        val groups = workspaceGroupDao.list(dslContext, projectId)
        val slice = groups.drop(offset).take(limit)
        val instances = slice.map { g ->
            val entity = InstanceInfoDTO()
            entity.id = g.id.toString()
            entity.displayName = g.name
            entity
        }
        return ListInstanceInfo().buildListInstanceResult(instances, groups.size.toLong())
    }

    private fun fetchWorkspaceGroupInfo(ids: List<String>): FetchInstanceInfoResponseDTO {
        val instances = ids.mapNotNull { idStr ->
            val id = idStr.toLongOrNull() ?: return@mapNotNull null
            val g = workspaceGroupDao.get(dslContext, projectId = "", groupId = id)
            // 注意：FETCH_INSTANCE_INFO 的 parentId 可能为空，这里不校验项目，按 id 取
            g ?: return@mapNotNull null
        }.map { g ->
            val entity = InstanceInfoDTO()
            entity.id = g.id.toString()
            entity.displayName = g.name
            entity.iamApprover = arrayListOf(g.createUser)
            entity
        }
        return FetchInstanceInfo().buildFetchInstanceResult(instances)
    }

    private fun searchWorkspaceGroup(projectId: String, keyword: String, limit: Int, offset: Int): SearchInstanceInfo {
        val groups = workspaceGroupDao.list(dslContext, projectId)
        val matched = groups.filter { it.name.contains(keyword, ignoreCase = true) }
        val slice = matched.drop(offset).take(limit)
        val instances = slice.map { g ->
            val entity = InstanceInfoDTO()
            entity.id = g.id.toString()
            entity.displayName = g.name
            entity
        }
        return SearchInstanceInfo().buildSearchInstanceResult(instances, matched.size.toLong())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthWorkspaceService::class.java)
    }
}
