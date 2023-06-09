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

import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.V2ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.CreateSubsetManagerDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.UpdateSubsetManagerDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.enums.AuthGroupCreateMode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class PermissionSubsetManagerService @Autowired constructor(
    private val permissionGroupPoliciesService: PermissionGroupPoliciesService,
    private val iamV2ManagerService: V2ManagerService,
    private val dslContext: DSLContext,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val authResourceNameConverter: AuthResourceNameConverter
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionSubsetManagerService::class.java)
    }

    /**
     * 创建二级管理员
     */
    @SuppressWarnings("LongParameterList", "LongMethod")
    fun createSubsetManager(
        gradeManagerId: String,
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        iamResourceCode: String
    ): Int {
        val managerGroupConfig = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = DefaultGroupType.MANAGER.value
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.DEFAULT_GROUP_CONFIG_NOT_FOUND,
            params = arrayOf(DefaultGroupType.MANAGER.value),
            defaultMessage = "${resourceType}_${DefaultGroupType.MANAGER.value} group config  not exist"
        )
        val name = authResourceNameConverter.generateIamName(
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
        val description = managerGroupConfig.description
        val authorizationScopes = permissionGroupPoliciesService.buildAuthorizationScopes(
            authorizationScopesStr = managerGroupConfig.authorizationScopes,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = iamResourceCode,
            resourceName = resourceName
        )
        // TODO 流水线组一期不创建拥有者
        val syncPerm = resourceType != AuthResourceType.PIPELINE_GROUP.value
        val createSubsetManagerDTO = CreateSubsetManagerDTO.builder()
            .name(name)
            .description(description)
            .members(listOf(userId))
            .authorizationScopes(authorizationScopes)
            .inheritSubjectScope(true)
            .subjectScopes(listOf())
            .syncPerm(syncPerm)
            .groupName(managerGroupConfig.groupName)
            .build()
        return iamV2ManagerService.createSubsetManager(
            gradeManagerId,
            createSubsetManagerDTO
        )
    }

    @SuppressWarnings("LongParameterList")
    fun modifySubsetManager(
        subsetManagerId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        iamResourceCode: String
    ): Boolean {
        val managerGroupConfig = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = resourceType,
            groupCode = DefaultGroupType.MANAGER.value
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_RESOURCE_GROUP_CONFIG_NOT_EXIST,
            params = arrayOf(DefaultGroupType.MANAGER.value),
            defaultMessage = "${resourceType}_${DefaultGroupType.MANAGER.value} group config  not exist"
        )
        val name = authResourceNameConverter.generateIamName(
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName
        )

        val authorizationScopes = permissionGroupPoliciesService.buildAuthorizationScopes(
            authorizationScopesStr = managerGroupConfig.authorizationScopes,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = iamResourceCode,
            resourceName = resourceName
        )
        // TODO 流水线组一期不创建拥有者
        val syncPerm = resourceType != AuthResourceType.PIPELINE_GROUP.value
        val subsetManagerDetail = iamV2ManagerService.getSubsetManagerDetail(subsetManagerId)
        val updateSubsetManagerDTO = UpdateSubsetManagerDTO.builder()
            .name(name)
            .members(subsetManagerDetail.members)
            .description(subsetManagerDetail.description)
            .authorizationScopes(authorizationScopes)
            .inheritSubjectScope(true)
            .subjectScopes(listOf())
            .syncPerm(syncPerm)
            .groupName(managerGroupConfig.groupName)
            .build()
        iamV2ManagerService.updateSubsetManager(
            subsetManagerId,
            updateSubsetManagerDTO
        )
        return true
    }

    fun deleteSubsetManager(subsetManagerId: String) {
        iamV2ManagerService.deleteSubsetManager(subsetManagerId)
    }

    fun listGroup(
        subsetManagerId: String,
        page: Int,
        pageSize: Int
    ): List<V2ManagerRoleGroupInfo> {
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = page
        pageInfoDTO.pageSize = pageSize
        val iamGroupInfoList =
            iamV2ManagerService.getSubsetManagerRoleGroup(subsetManagerId.toInt(), pageInfoDTO)
        return iamGroupInfoList.results
    }

    /**
     * 创建二级管理员默认分组
     *
     * @param createMode true-创建时创建组，false-开启权限管理创建默认组
     */
    @Suppress("LongParameterList", "LongMethod")
    fun createSubsetManagerDefaultGroup(
        subsetManagerId: Int,
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        iamResourceCode: String,
        createMode: AuthGroupCreateMode
    ) {
        // 创建资源时，先同步二级管理员创建时创建的组
        syncSubsetManagerGroup(
            subsetManagerId = subsetManagerId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName,
            iamResourceCode = iamResourceCode
        )
        val resourceGroupConfigs = if (createMode == AuthGroupCreateMode.CREATE) {
            authResourceGroupConfigDao.get(
                dslContext = dslContext,
                resourceType = resourceType,
                createMode = false
            )
        } else {
            authResourceGroupConfigDao.get(
                dslContext = dslContext,
                resourceType = resourceType
            )
        }
        resourceGroupConfigs.filter {
            it.groupCode != DefaultGroupType.MANAGER.value
        }.forEach { groupConfig ->
            val resourceGroupInfo = authResourceGroupDao.get(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                groupCode = groupConfig.groupCode
            )
            // 判断组是否已经存在，如先关闭权限管理再开启权限管理,就不需要再重复创建组
            if (resourceGroupInfo != null) {
                return@forEach
            }
            val name = groupConfig.groupName
            val description = groupConfig.description
            val managerRoleGroup = ManagerRoleGroup(name, description, false)
            val managerRoleGroupDTO = ManagerRoleGroupDTO.builder().groups(listOf(managerRoleGroup)).build()
            val iamGroupId = iamV2ManagerService.batchCreateSubsetRoleGroup(subsetManagerId, managerRoleGroupDTO)
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                iamResourceCode = iamResourceCode,
                groupCode = groupConfig.groupCode,
                groupName = name,
                defaultGroup = true,
                relationId = iamGroupId.toString()
            )
            permissionGroupPoliciesService.grantGroupPermission(
                authorizationScopesStr = groupConfig.authorizationScopes,
                projectCode = projectCode,
                projectName = projectName,
                iamResourceCode = iamResourceCode,
                resourceName = resourceName,
                iamGroupId = iamGroupId
            )
        }
    }

    /**
     * 同步二级管理员创建时自动创建的用户组
     */
    @Suppress("LongParameterList")
    private fun syncSubsetManagerGroup(
        subsetManagerId: Int,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        iamResourceCode: String
    ) {
        val resourceManageGroupInfo = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            groupCode = DefaultGroupType.MANAGER.value
        )
        if (resourceManageGroupInfo != null) {
            return
        }
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = PageUtil.DEFAULT_PAGE
        pageInfoDTO.pageSize = PageUtil.DEFAULT_PAGE_SIZE
        val iamGroupInfoList =
            iamV2ManagerService.getSubsetManagerRoleGroup(subsetManagerId, pageInfoDTO)
        iamGroupInfoList.results.forEach { iamGroupInfo ->
            authResourceGroupDao.create(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                iamResourceCode = iamResourceCode,
                groupCode = DefaultGroupType.MANAGER.value,
                groupName = iamGroupInfo.name,
                defaultGroup = true,
                relationId = iamGroupInfo.id.toString()
            )
        }
    }

    @Suppress("LongParameterList")
    fun modifySubsetManagerDefaultGroup(
        subsetManagerId: Int,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ) {
        val defaultGroupConfigs = authResourceGroupConfigDao.get(
            dslContext = dslContext,
            resourceType = resourceType
        )
        defaultGroupConfigs.forEach { groupConfig ->
            authResourceGroupDao.update(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                resourceName = resourceName,
                groupCode = groupConfig.groupCode,
                groupName = groupConfig.groupName
            )
        }
    }

    fun deleteSubsetManagerDefaultGroup(
        subsetManagerId: Int,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ) {
        authResourceGroupDao.getByResourceCode(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode
        ).filter {
            it.groupCode != DefaultGroupType.MANAGER.value
        }.forEach {
            logger.info("delete subset manage default group|$subsetManagerId|${it.relationId}")
            iamV2ManagerService.deleteRoleGroupV2(it.relationId.toInt())
        }
    }
}
