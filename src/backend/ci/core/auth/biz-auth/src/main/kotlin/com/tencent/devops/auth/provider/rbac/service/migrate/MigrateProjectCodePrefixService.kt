/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.auth.provider.rbac.service.migrate

import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.model.auth.tables.TAuthResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

/**
 * 迁移项目ID前缀服务
 * 一次性迁移脚本：为所有项目ID添加tencent.前缀，重建IAM权限数据
 */
@Service
@Suppress("LongParameterList", "TooManyFunctions")
class MigrateProjectCodePrefixService @Autowired constructor(
    private val dslContext: DSLContext,
    private val authResourceDao: AuthResourceDao,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val permissionResourceService: PermissionResourceService,
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val permissionResourceMemberService: PermissionResourceMemberService
) {

    companion object {
        private val logger =
            LoggerFactory.getLogger(MigrateProjectCodePrefixService::class.java)
        private const val PROJECT_CODE_PREFIX = "tencent."
    }

    /**
     * 执行迁移：为所有项目ID添加tencent.前缀
     *
     * 流程：
     * 1. 获取所有不含.的distinct项目ID（过滤测试数据）
     * 2. 对每个项目：
     *    a. 创建新项目资源（tencent.前缀）及默认组
     *    b. 迁移项目级别的自定义组（groupCode=custom，同时调IAM+写DB）
     *    c. 创建非项目资源及默认组
     *    d. 迁移组成员（同时调IAM+写DB，保留原过期时间）
     *    e. 删除旧数据
     */
    fun migrateProjectCodePrefix(): Boolean {
        logger.info("Start migrating project code prefix")
        val projectCodes = listDistinctProjectCodes()
        logger.info("Found ${projectCodes.size} projects to migrate: $projectCodes")
        projectCodes.forEach { oldProjectCode ->
            try {
                migrateProject(oldProjectCode)
            } catch (e: Exception) {
                logger.error(
                    "Failed to migrate project: $oldProjectCode", e
                )
            }
        }
        logger.info("Finish migrating project code prefix")
        return true
    }

    /**
     * 获取所有不含.的distinct项目ID
     */
    private fun listDistinctProjectCodes(): List<String> {
        val t = TAuthResource.T_AUTH_RESOURCE
        return dslContext.selectDistinct(t.PROJECT_CODE)
            .from(t)
            .where(t.PROJECT_CODE.notLike("%.%"))
            .fetch(t.PROJECT_CODE)
    }

    /**
     * 迁移单个项目
     */
    @Suppress("LongMethod")
    private fun migrateProject(oldProjectCode: String) {
        val newProjectCode = "$PROJECT_CODE_PREFIX$oldProjectCode"
        logger.info(
            "Migrating project: $oldProjectCode -> $newProjectCode"
        )

        // Step 1: 创建新项目资源及默认组
        createNewProjectResource(oldProjectCode, newProjectCode)

        // Step 2: 创建非项目资源及默认组
        createNonProjectResources(oldProjectCode, newProjectCode)

        // Step 3: 迁移组成员（含默认组和自定义组）
        migrateGroupMembers(oldProjectCode, newProjectCode)

        // Step 4: 删除旧项目数据
        deleteOldProjectData(oldProjectCode)

        logger.info(
            "Successfully migrated project: " +
                "$oldProjectCode -> $newProjectCode"
        )
    }

    /**
     * Step 1: 创建新项目资源及默认组
     */
    private fun createNewProjectResource(
        oldProjectCode: String,
        newProjectCode: String
    ) {
        val oldProjectResource = authResourceDao.get(
            dslContext = dslContext,
            projectCode = oldProjectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = oldProjectCode
        )
        if (oldProjectResource == null) {
            logger.warn(
                "Project resource not found: $oldProjectCode, skip"
            )
            return
        }
        logger.info(
            "Creating new project resource: $newProjectCode (name=${oldProjectResource.resourceName})"
        )
        try {
            permissionResourceService.resourceCreateRelation(
                userId = oldProjectResource.createUser,
                projectCode = newProjectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = newProjectCode,
                resourceName = oldProjectResource.resourceName,
                tenantId = null,
                async = false
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to create project resource: $newProjectCode", e
            )
            throw e
        }
    }

    /**
     * Step 2: 迁移项目级别的自定义组（groupCode=custom）
     * 通过 PermissionResourceGroupService.createGroup 在新项目下创建同名自定义组
     * 该方法同时调用 IAM 创建组 + 写入数据库
     */
    private fun migrateProjectCustomGroups(
        oldProjectCode: String,
        newProjectCode: String
    ) {
        val oldProjectGroups = authResourceGroupDao.getByResourceCode(
            dslContext = dslContext,
            projectCode = oldProjectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = oldProjectCode
        )
        val customGroups = oldProjectGroups.filter {
            it.groupCode == DefaultGroupType.CUSTOM.value
        }
        if (customGroups.isEmpty()) {
            logger.info(
                "No custom groups found for project $oldProjectCode"
            )
            return
        }
        logger.info(
            "Found ${customGroups.size} custom groups " +
                "in project $oldProjectCode"
        )
        customGroups.forEach { oldGroup ->
            try {
                logger.info(
                    "Creating custom group: " +
                        "name=${oldGroup.groupName}, " +
                        "code=${oldGroup.groupCode}"
                )
                permissionResourceGroupService.createGroup(
                    projectId = newProjectCode,
                    groupAddDTO = GroupAddDTO(
                        groupName = oldGroup.groupName,
                        groupDesc = oldGroup.description ?: oldGroup.groupName
                    )
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to create custom group: " +
                        "${oldGroup.groupName} " +
                        "in project $newProjectCode",
                    e
                )
            }
        }
    }

    /**
     * Step 3: 创建非项目资源及默认组
     */
    private fun createNonProjectResources(
        oldProjectCode: String,
        newProjectCode: String
    ) {
        // 查询旧项目下所有非项目类型资源
        val offset = 0
        val limit = 10000
        val resources = authResourceDao.list(
            dslContext = dslContext,
            projectCode = oldProjectCode,
            resourceName = null,
            resourceType = null,
            offset = offset,
            limit = limit
        ).filter {
            it.resourceType != AuthResourceType.PROJECT.value
        }
        logger.info(
            "Found ${resources.size} non-project resources " +
                "in project $oldProjectCode"
        )
        resources.forEach { resource ->
            try {
                logger.info(
                    "Creating resource: type=${resource.resourceType}," +
                        " code=${resource.resourceCode}," +
                        " name=${resource.resourceName}"
                )
                permissionResourceService.resourceCreateRelation(
                    userId = resource.createUser,
                    projectCode = newProjectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode,
                    resourceName = resource.resourceName,
                    tenantId = null,
                    async = false
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to create resource: " +
                        "${resource.resourceType}/" +
                        "${resource.resourceCode} " +
                        "in project $newProjectCode",
                    e
                )
            }
        }
    }

    /**
     * Step 4: 迁移组成员
     * 一次性获取旧项目下所有成员和新项目下所有组，
     * 通过 iamGroupId 建立旧组->新组映射，
     * 然后通过 addGroupMember 将成员迁移过去（同时调IAM+写DB，保留原过期时间）
     */
    private fun migrateGroupMembers(
        oldProjectCode: String,
        newProjectCode: String
    ) {
        // 一次性获取旧项目下所有成员
        val allOldMembers = authResourceGroupMemberDao.listResourceGroupMember(
            dslContext = dslContext,
            projectCode = oldProjectCode
        )
        if (allOldMembers.isEmpty()) {
            logger.info(
                "No members found for project $oldProjectCode"
            )
            return
        }
        // 按 iamGroupId 分组
        val membersByIamGroupId = allOldMembers.groupBy { it.iamGroupId }

        // 一次性获取新项目下所有组，建立映射
        val newGroupMap = buildNewGroupMap(
            oldProjectCode = oldProjectCode,
            newProjectCode = newProjectCode
        )

        logger.info(
            "Migrating members for project $oldProjectCode: " +
                "${allOldMembers.size} members in " +
                "${membersByIamGroupId.size} groups"
        )

        membersByIamGroupId.forEach { (oldIamGroupId, members) ->
            val newGroup = newGroupMap[oldIamGroupId]
            if (newGroup == null) {
                logger.warn(
                    "New group not found for " +
                        "oldIamGroupId=$oldIamGroupId " +
                        "in project $newProjectCode, " +
                        "skip ${members.size} members"
                )
                return@forEach
            }
            logger.info(
                "Migrating ${members.size} members: " +
                    "oldIamGroupId=$oldIamGroupId -> " +
                    "newIamGroupId=${newGroup.relationId}"
            )
            members.forEach { member ->
                try {
                    permissionResourceMemberService.addGroupMember(
                        projectCode = newProjectCode,
                        memberId = member.memberId,
                        memberType = member.memberType,
                        expiredAt = member.expiredTime.timestamp(),
                        iamGroupId = newGroup.relationId
                    )
                } catch (e: Exception) {
                    logger.error(
                        "Failed to add member " +
                            "${member.memberId} to group " +
                            "${newGroup.relationId} " +
                            "in project $newProjectCode",
                        e
                    )
                }
            }
        }
    }

    /**
     * 构建旧 iamGroupId -> 新组 的映射
     * 一次性获取旧项目和新项目下所有组，
     * 默认组通过 (resourceType, resourceCode, groupCode) 匹配，
     * 自定义组通过 (resourceType, resourceCode, groupName) 匹配。
     */
    private fun buildNewGroupMap(
        oldProjectCode: String,
        newProjectCode: String
    ): Map<Int, AuthResourceGroup> {
        // 获取旧项目所有组
        val allOldGroups = listAllGroupsByProject(oldProjectCode)
        // 获取新项目所有组
        val allNewGroups = listAllGroupsByProject(newProjectCode)

        // 新项目组按匹配 key 建立索引
        // 默认组 key: resourceType:resourceCode:groupCode
        // 自定义组 key: resourceType:resourceCode:custom:groupName
        val newGroupIndex = allNewGroups.associateBy { group ->
            buildGroupMatchKey(newProjectCode, group)
        }

        val result = mutableMapOf<Int, AuthResourceGroup>()
        allOldGroups.forEach { oldGroup ->
            val matchKey = buildGroupMatchKey(
                oldProjectCode, oldGroup, newProjectCode
            )
            val newGroup = newGroupIndex[matchKey]
            if (newGroup != null) {
                result[oldGroup.relationId] = newGroup
            }
        }
        return result
    }

    /**
     * 获取项目下所有组（包含项目资源和非项目资源的组）
     */
    private fun listAllGroupsByProject(
        projectCode: String
    ): List<AuthResourceGroup> {
        val resources = authResourceDao.list(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceName = null,
            resourceType = null,
            offset = 0,
            limit = 10000
        )
        val allGroups = mutableListOf<AuthResourceGroup>()
        resources.forEach { resource ->
            allGroups.addAll(
                authResourceGroupDao.getByResourceCode(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode
                )
            )
        }
        return allGroups
    }

    /**
     * 构建组匹配 key
     * @param projectCode 组所属的项目ID
     * @param group 组信息
     * @param targetProjectCode 目标项目ID（用于替换项目资源的 resourceCode）
     */
    private fun buildGroupMatchKey(
        projectCode: String,
        group: AuthResourceGroup,
        targetProjectCode: String? = null
    ): String {
        val resourceCode =
            if (group.resourceType == AuthResourceType.PROJECT.value) {
                targetProjectCode ?: projectCode
            } else {
                group.resourceCode
            }
        val isCustomGroup =
            group.groupCode == DefaultGroupType.CUSTOM.value
        return if (isCustomGroup) {
            "${group.resourceType}:$resourceCode:" +
                "custom:${group.groupName}"
        } else {
            "${group.resourceType}:$resourceCode:" +
                group.groupCode
        }
    }

    /**
     * Step 5: 删除旧项目的所有数据
     */
    private fun deleteOldProjectData(oldProjectCode: String) {
        logger.info("Deleting old project data: $oldProjectCode")

        // 先获取所有旧资源用于逐个删除
        val resources = authResourceDao.list(
            dslContext = dslContext,
            projectCode = oldProjectCode,
            resourceName = null,
            resourceType = null,
            offset = 0,
            limit = 10000
        )

        resources.forEach { resource ->
            try {
                // 删除成员
                authResourceGroupMemberDao.deleteByResource(
                    dslContext = dslContext,
                    projectCode = oldProjectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode
                )
                // 删除组
                authResourceGroupDao.delete(
                    dslContext = dslContext,
                    projectCode = oldProjectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode
                )
                // 删除资源
                authResourceDao.delete(
                    dslContext = dslContext,
                    projectCode = oldProjectCode,
                    resourceType = resource.resourceType,
                    resourceCode = resource.resourceCode
                )
            } catch (e: Exception) {
                logger.error(
                    "Failed to delete old resource: " +
                        "${resource.resourceType}/" +
                        "${resource.resourceCode} " +
                        "in project $oldProjectCode",
                    e
                )
            }
        }
        logger.info(
            "Deleted old project data: $oldProjectCode, " +
                "total ${resources.size} resources"
        )
    }
}
