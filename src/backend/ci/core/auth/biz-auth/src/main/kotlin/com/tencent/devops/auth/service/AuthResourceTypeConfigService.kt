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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceTypeDao
import com.tencent.devops.auth.pojo.request.ActionCreateRequest
import com.tencent.devops.auth.pojo.request.ProjectGroupConfigUpdateRequest
import com.tencent.devops.auth.pojo.request.ResourceGroupConfigCreateRequest
import com.tencent.devops.auth.pojo.request.ResourceTypeCreateRequest
import com.tencent.devops.auth.pojo.vo.ActionVO
import com.tencent.devops.auth.pojo.vo.ResourceGroupConfigVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeVO
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.model.auth.tables.records.TAuthActionRecord
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupConfigRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthResourceTypeConfigService @Autowired constructor(
    private val dslContext: DSLContext,
    private val authResourceTypeDao: AuthResourceTypeDao,
    private val authActionDao: AuthActionDao,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AuthResourceTypeConfigService::class.java)
        private const val SYSTEM_USER = "system"
    }

    // ==================== 资源类型管理 ====================

    fun listResourceTypes(): List<ResourceTypeVO> {
        return authResourceTypeDao.list(dslContext).map { record ->
            ResourceTypeVO(
                id = record.id,
                resourceType = record.resourceType,
                name = record.name,
                englishName = record.englishName,
                desc = record.desc,
                englishDesc = record.englishDesc,
                parent = record.parent,
                system = record.system,
                createTime = record.createTime,
                updateTime = record.updateTime
            )
        }
    }

    fun getResourceType(resourceType: String): ResourceTypeVO? {
        return authResourceTypeDao.get(dslContext, resourceType)?.let { record ->
            ResourceTypeVO(
                id = record.id,
                resourceType = record.resourceType,
                name = record.name,
                englishName = record.englishName,
                desc = record.desc,
                englishDesc = record.englishDesc,
                parent = record.parent,
                system = record.system,
                createTime = record.createTime,
                updateTime = record.updateTime
            )
        }
    }

    fun createResourceType(request: ResourceTypeCreateRequest): Int {
        // 检查是否已存在
        val existing = authResourceTypeDao.get(dslContext, request.resourceType)
        if (existing != null) {
            throw OperationException("资源类型 ${request.resourceType} 已存在")
        }

        val id = authResourceTypeDao.create(
            dslContext = dslContext,
            resourceType = request.resourceType,
            name = request.name,
            englishName = request.englishName,
            desc = request.desc,
            englishDesc = request.englishDesc,
            parent = request.parent,
            system = request.system,
            createUser = SYSTEM_USER
        )
        logger.info("Created resource type: ${request.resourceType} with ID: $id")
        return id
    }

    fun deleteResourceType(resourceType: String): Boolean {
        val result = authResourceTypeDao.delete(dslContext, resourceType)
        if (result) {
            logger.info("Deleted resource type: $resourceType")
        }
        return result
    }

    // ==================== 操作管理 ====================

    fun listActions(resourceType: String?): List<ActionVO> {
        val records = if (resourceType.isNullOrBlank()) {
            authActionDao.listAll(dslContext)
        } else {
            authActionDao.list(dslContext, resourceType)
        }
        return records.map { record ->
            ActionVO(
                action = record.action,
                resourceType = record.resourceType,
                relatedResourceType = record.relatedResourceType,
                actionName = record.actionName,
                englishName = record.englishName,
                actionType = record.actionType,
                createTime = record.createTime,
                updateTime = record.updateTime
            )
        }
    }

    fun getAction(action: String): ActionVO? {
        return authActionDao.get(dslContext, action)?.let { record ->
            ActionVO(
                action = record.action,
                resourceType = record.resourceType,
                relatedResourceType = record.relatedResourceType,
                actionName = record.actionName,
                englishName = record.englishName,
                actionType = record.actionType,
                createTime = record.createTime,
                updateTime = record.updateTime
            )
        }
    }

    fun createAction(request: ActionCreateRequest): Boolean {
        // 检查是否已存在
        val existing = authActionDao.get(dslContext, request.action)
        if (existing != null) {
            throw OperationException("操作 ${request.action} 已存在")
        }

        val result = authActionDao.create(
            dslContext = dslContext,
            action = request.action,
            resourceType = request.resourceType,
            relatedResourceType = request.relatedResourceType,
            actionName = request.actionName,
            englishName = request.englishName,
            actionType = request.actionType,
            createUser = SYSTEM_USER
        )
        logger.info("Created action: ${request.action}")
        return result
    }

    fun batchCreateActions(requests: List<ActionCreateRequest>): Int {
        val records = requests.map { request ->
            TAuthActionRecord().apply {
                action = request.action
                resourceType = request.resourceType
                relatedResourceType = request.relatedResourceType
                actionName = request.actionName
                englishName = request.englishName
                actionType = request.actionType
                createUser = SYSTEM_USER
            }
        }
        val count = authActionDao.batchCreate(dslContext, records)
        logger.info("Batch created $count actions")
        return count
    }

    fun deleteAction(action: String): Boolean {
        val result = authActionDao.delete(dslContext, action)
        if (result) {
            logger.info("Deleted action: $action")
        }
        return result
    }

    // ==================== 用户组配置管理 ====================

    fun listGroupConfigs(resourceType: String?): List<ResourceGroupConfigVO> {
        val records = if (resourceType.isNullOrBlank()) {
            authResourceGroupConfigDao.listAll(dslContext)
        } else {
            authResourceGroupConfigDao.get(dslContext, resourceType)
        }
        return records.map { record ->
            ResourceGroupConfigVO(
                id = record.id,
                resourceType = record.resourceType,
                groupCode = record.groupCode,
                groupName = record.groupName,
                description = record.description,
                createMode = record.createMode,
                groupType = record.groupType,
                actions = record.actions,
                authorizationScopes = record.authorizationScopes,
                createTime = record.createTime,
                updateTime = record.updateTime
            )
        }
    }

    fun getGroupConfig(id: Long): ResourceGroupConfigVO? {
        return authResourceGroupConfigDao.getById(dslContext, id)?.let { record ->
            ResourceGroupConfigVO(
                id = record.id,
                resourceType = record.resourceType,
                groupCode = record.groupCode,
                groupName = record.groupName,
                description = record.description,
                createMode = record.createMode,
                groupType = record.groupType,
                actions = record.actions,
                authorizationScopes = record.authorizationScopes,
                createTime = record.createTime,
                updateTime = record.updateTime
            )
        }
    }

    fun createGroupConfig(request: ResourceGroupConfigCreateRequest): Long {
        // 检查是否已存在
        val existing = authResourceGroupConfigDao.getByGroupCode(
            dslContext, request.resourceType, request.groupCode
        )
        if (existing != null) {
            throw OperationException(
                "用户组配置 ${request.resourceType}:${request.groupCode} 已存在"
            )
        }

        // 构建 actions JSON
        val actionsJson = objectMapper.writeValueAsString(request.actions)

        // 如果没有提供 authorizationScopes，自动生成
        val authorizationScopes = request.authorizationScopes
            ?: buildDefaultAuthorizationScopes(request.resourceType, request.actions)

        val id = authResourceGroupConfigDao.create(
            dslContext = dslContext,
            resourceType = request.resourceType,
            groupCode = request.groupCode,
            groupName = request.groupName,
            description = request.description,
            createMode = request.createMode,
            groupType = request.groupType,
            actions = actionsJson,
            authorizationScopes = authorizationScopes
        )
        logger.info("Created group config: ${request.resourceType}:${request.groupCode} with ID: $id")
        return id
    }

    fun batchCreateGroupConfigs(requests: List<ResourceGroupConfigCreateRequest>): Int {
        val records = requests.map { request ->
            val actionsJson = objectMapper.writeValueAsString(request.actions)
            val authorizationScopes = request.authorizationScopes
                ?: buildDefaultAuthorizationScopes(request.resourceType, request.actions)

            TAuthResourceGroupConfigRecord().apply {
                resourceType = request.resourceType
                groupCode = request.groupCode
                groupName = request.groupName
                description = request.description
                createMode = request.createMode
                groupType = request.groupType
                actions = actionsJson
                this.authorizationScopes = authorizationScopes
            }
        }
        val count = authResourceGroupConfigDao.batchCreate(dslContext, records)
        logger.info("Batch created $count group configs")
        return count
    }

    /**
     * 追加新的资源类型权限块到用户组配置
     * 适用场景：为用户组添加一个全新资源类型的权限
     */
    fun appendActionsToGroupConfig(id: Long, resourceType: String, actions: List<String>): Boolean {
        val record = authResourceGroupConfigDao.getById(dslContext, id)
            ?: throw OperationException("用户组配置 ID: $id 不存在")

        // 解析现有的 authorizationScopes
        val currentScopes = parseAuthorizationScopes(record.authorizationScopes)

        // 构建新的资源权限块
        val newScope = buildAuthorizationScopeBlock(resourceType, actions)

        // 追加到现有 scopes
        val updatedScopes = currentScopes.toMutableList()
        updatedScopes.add(newScope)

        val updatedScopesJson = objectMapper.writeValueAsString(updatedScopes)
        val result = authResourceGroupConfigDao.updateAuthorizationScopes(
            dslContext, id, updatedScopesJson
        )

        if (result) {
            logger.info("Appended actions to group config ID: $id, resourceType: $resourceType, actions: $actions")
        }
        return result
    }

    /**
     * 在已有的资源类型权限块中追加 actions
     * 适用场景：某个资源类型的权限块已存在，只需要在其 actions 数组中追加新的 action
     *
     * @param id 用户组配置 ID
     * @param targetResourceType 目标资源类型（用于定位已存在的权限块）
     * @param actions 要追加的 action 列表
     * @return 是否成功
     */
    fun appendActionsToExistingScope(id: Long, targetResourceType: String, actions: List<String>): Boolean {
        val record = authResourceGroupConfigDao.getById(dslContext, id)
            ?: throw OperationException("用户组配置 ID: $id 不存在")

        // 解析现有的 authorizationScopes
        val currentScopes = parseAuthorizationScopes(record.authorizationScopes)
        if (currentScopes.isEmpty()) {
            throw OperationException("用户组配置 ID: $id 的 authorizationScopes 为空")
        }

        // 查找目标资源类型的权限块
        var found = false
        val updatedScopes = currentScopes.map { scope ->
            val resources = scope["resources"] as? List<*>
            val resourceType = resources?.firstOrNull()?.let { res ->
                (res as? Map<*, *>)?.get("type") as? String
            }

            if (resourceType == targetResourceType) {
                found = true
                // 在这个 scope 的 actions 中追加新的 actions
                appendActionsToScope(scope, actions)
            } else {
                scope
            }
        }

        if (!found) {
            throw OperationException(
                "用户组配置 ID: $id 中未找到资源类型 $targetResourceType 的权限块"
            )
        }

        val updatedScopesJson = objectMapper.writeValueAsString(updatedScopes)
        val result = authResourceGroupConfigDao.updateAuthorizationScopes(
            dslContext, id, updatedScopesJson
        )

        if (result) {
            logger.info(
                "Appended actions to existing scope in group config ID: $id, " +
                    "targetResourceType: $targetResourceType, actions: $actions"
            )
        }
        return result
    }

    /**
     * 智能追加 actions 到用户组配置
     * 如果目标资源类型的权限块已存在，则追加到该块的 actions 中
     * 如果不存在，则创建新的权限块
     *
     * @param id 用户组配置 ID
     * @param resourceType 资源类型
     * @param actions 要追加的 action 列表
     * @return 是否成功
     */
    fun smartAppendActions(id: Long, resourceType: String, actions: List<String>): Boolean {
        val record = authResourceGroupConfigDao.getById(dslContext, id)
            ?: throw OperationException("用户组配置 ID: $id 不存在")

        // 解析现有的 authorizationScopes
        val currentScopes = parseAuthorizationScopes(record.authorizationScopes)

        // 查找是否已存在该资源类型的权限块
        var existingScopeIndex = -1
        for ((index, scope) in currentScopes.withIndex()) {
            val resources = scope["resources"] as? List<*>
            val scopeResourceType = resources?.firstOrNull()?.let { res ->
                (res as? Map<*, *>)?.get("type") as? String
            }
            if (scopeResourceType == resourceType) {
                existingScopeIndex = index
                break
            }
        }

        val updatedScopes = if (existingScopeIndex >= 0) {
            // 已存在，追加到现有块的 actions 中
            currentScopes.mapIndexed { index, scope ->
                if (index == existingScopeIndex) {
                    appendActionsToScope(scope, actions)
                } else {
                    scope
                }
            }
        } else {
            // 不存在，创建新的权限块
            val newScope = buildAuthorizationScopeBlock(resourceType, actions)
            currentScopes.toMutableList().apply { add(newScope) }
        }

        val updatedScopesJson = objectMapper.writeValueAsString(updatedScopes)
        val result = authResourceGroupConfigDao.updateAuthorizationScopes(
            dslContext, id, updatedScopesJson
        )

        if (result) {
            val mode = if (existingScopeIndex >= 0) "appended to existing" else "created new"
            logger.info(
                "Smart append actions ($mode) to group config ID: $id, " +
                    "resourceType: $resourceType, actions: $actions"
            )
        }
        return result
    }

    /**
     * 在 scope 的 actions 数组中追加新的 actions（去重）
     */
    @Suppress("UNCHECKED_CAST")
    private fun appendActionsToScope(scope: Map<String, Any>, newActions: List<String>): Map<String, Any> {
        val mutableScope = scope.toMutableMap()
        val currentActions = (scope["actions"] as? List<Map<String, Any>>) ?: emptyList()

        // 获取现有的 action id 集合
        val existingActionIds = currentActions.mapNotNull { it["id"] as? String }.toMutableSet()

        // 追加新的 actions（去重）
        val updatedActions = currentActions.toMutableList()
        newActions.forEach { actionId ->
            if (actionId !in existingActionIds) {
                updatedActions.add(mapOf("id" to actionId))
                existingActionIds.add(actionId)
            }
        }

        mutableScope["actions"] = updatedActions
        return mutableScope
    }

    fun batchAppendActionsToProjectGroups(requests: List<ProjectGroupConfigUpdateRequest>): Int {
        var count = 0
        requests.forEach { request ->
            try {
                if (appendActionsToGroupConfig(
                        request.groupConfigId,
                        request.resourceType,
                        request.actions
                    )
                ) {
                    count++
                }
            } catch (e: Exception) {
                logger.error("Failed to append actions to group config ID: ${request.groupConfigId}", e)
            }
        }
        logger.info("Batch appended actions to $count project groups")
        return count
    }

    /**
     * 批量智能追加 actions
     * 如果目标资源类型的权限块已存在，则追加到该块的 actions 中
     * 如果不存在，则创建新的权限块
     */
    fun batchSmartAppendActions(requests: List<ProjectGroupConfigUpdateRequest>): Int {
        var count = 0
        requests.forEach { request ->
            try {
                if (smartAppendActions(
                        request.groupConfigId,
                        request.resourceType,
                        request.actions
                    )
                ) {
                    count++
                }
            } catch (e: Exception) {
                logger.error("Failed to smart append actions to group config ID: ${request.groupConfigId}", e)
            }
        }
        logger.info("Batch smart appended actions to $count project groups")
        return count
    }

    fun deleteGroupConfig(id: Long): Boolean {
        val result = authResourceGroupConfigDao.delete(dslContext, id)
        if (result) {
            logger.info("Deleted group config ID: $id")
        }
        return result
    }

    // ==================== 便捷方法：一键创建完整资源类型配置 ====================

    fun createFullResourceTypeConfig(
        resourceTypeRequest: ResourceTypeCreateRequest,
        actionRequests: List<ActionCreateRequest>,
        groupConfigRequests: List<ResourceGroupConfigCreateRequest>
    ): Boolean {
        try {
            // 1. 创建资源类型
            createResourceType(resourceTypeRequest)

            // 2. 批量创建操作
            batchCreateActions(actionRequests)

            // 3. 批量创建用户组配置
            batchCreateGroupConfigs(groupConfigRequests)

            logger.info("Successfully created full resource type config for: ${resourceTypeRequest.resourceType}")
            return true
        } catch (e: Exception) {
            logger.error("Failed to create full resource type config for: ${resourceTypeRequest.resourceType}", e)
            throw e
        }
    }

    // ==================== 私有辅助方法 ====================

    private fun buildDefaultAuthorizationScopes(resourceType: String, actions: List<String>): String {
        val scopes = mutableListOf<Map<String, Any>>()

        // 添加 project_visit 基础权限
        scopes.add(
            mapOf(
                "system" to "#system#",
                "actions" to listOf(mapOf("id" to "project_visit")),
                "resources" to listOf(
                    mapOf(
                        "system" to "#system#",
                        "type" to "project",
                        "paths" to listOf(
                            listOf(
                                mapOf(
                                    "system" to "#system#",
                                    "type" to "project",
                                    "id" to "#projectId#",
                                    "name" to "#projectName#"
                                )
                            )
                        )
                    )
                )
            )
        )

        // 添加资源操作权限
        scopes.add(buildAuthorizationScopeBlock(resourceType, actions))

        return objectMapper.writeValueAsString(scopes)
    }

    private fun buildAuthorizationScopeBlock(resourceType: String, actions: List<String>): Map<String, Any> {
        return mapOf(
            "system" to "#system#",
            "actions" to actions.map { mapOf("id" to it) },
            "resources" to listOf(
                mapOf(
                    "system" to "#system#",
                    "type" to resourceType,
                    "paths" to listOf(
                        listOf(
                            mapOf(
                                "system" to "#system#",
                                "type" to "project",
                                "id" to "#projectId#",
                                "name" to "#projectName#"
                            )
                        )
                    )
                )
            )
        )
    }

    private fun parseAuthorizationScopes(scopesJson: String?): List<Map<String, Any>> {
        if (scopesJson.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            objectMapper.readValue(
                scopesJson,
                object : TypeReference<List<Map<String, Any>>>() {}
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse authorization scopes: $scopesJson", e)
            emptyList()
        }
    }
}
