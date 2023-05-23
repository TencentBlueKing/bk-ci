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

package com.tencent.devops.experience.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.GroupDao
import com.tencent.devops.experience.service.ExperiencePermissionService
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class StreamExperiencePermissionServiceImpl @Autowired constructor(
    val client: Client,
    val tokenService: ClientTokenService,
    val experienceDao: ExperienceDao,
    val groupDao: GroupDao,
    val dslContext: DSLContext
) : ExperiencePermissionService {
    override fun validateTaskPermission(
        user: String,
        projectId: String,
        experienceId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        logger.info("StreamExperiencePermissionServiceImpl user:$user projectId: $projectId ")
        if (authPermission == AuthPermission.VIEW)
            return
        val permissionCheck = checkTaskPermission(user, projectId, authPermission)
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validateCreateTaskPermission(
        user: String,
        projectId: String
    ): Boolean = true

    override fun validateDeleteExperience(
        experienceId: Long,
        userId: String,
        projectId: String,
        message: String
    ) {
        validateTaskPermission(
            user = userId,
            projectId = projectId,
            experienceId = experienceId,
            authPermission = AuthPermission.EDIT,
            message = message
        )
    }

    override fun createTaskResource(
        user: String,
        projectId: String,
        experienceId: Long,
        experienceName: String
    ) {
        return
    }

    override fun filterExperience(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val experienceIds = experienceDao.list(dslContext, projectId, null, null).map { it.id }
        val experienceMap = mutableMapOf<AuthPermission, List<Long>>()
        authPermissions.forEach {
            if (checkTaskPermission(user, projectId, it)) {
                experienceMap[it] = experienceIds
            } else {
                experienceMap[it] = emptyList()
            }
        }
        return experienceMap
    }

    override fun filterCanListExperience(
        user: String,
        projectId: String,
        experienceRecordList: List<TExperienceRecord>
    ): List<TExperienceRecord> {
        return experienceRecordList
    }

    override fun validateCreateGroupPermission(user: String, projectId: String): Boolean = true

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        logger.info("validateGroupPermission user:$userId projectId: $projectId $authPermission ")
        if (authPermission == AuthPermission.VIEW)
            return
        val permissionCheck = checkGroupPermission(userId, projectId, authPermission)
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createGroupResource(
        userId: String,
        projectId: String,
        groupId: Long,
        groupName: String
    ) {
        return
    }

    override fun modifyGroupResource(
        projectId: String,
        groupId: Long,
        groupName: String
    ) {
        return
    }

    override fun deleteGroupResource(projectId: String, groupId: Long) {
        return
    }

    override fun filterGroup(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val groupIds = groupDao.list(
            dslContext = dslContext,
            projectId = projectId,
            offset = 0,
            limit = 1000).map { it.id }
        val experienceMap = mutableMapOf<AuthPermission, List<Long>>()
        authPermissions.forEach {
            if (checkGroupPermission(user, projectId, it)) {
                experienceMap[it] = groupIds
            } else {
                experienceMap[it] = emptyList()
            }
        }
        return experienceMap
    }

    override fun filterCanListGroup(
        user: String,
        projectId: String,
        groupRecordIds: List<Long>
    ): List<Long> = groupRecordIds

    private fun checkGroupPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            userId = userId,
            token = tokenService.getSystemToken(null) ?: "",
            action = authPermission.value,
            projectCode = projectId,
            resourceCode = "",
            resourceType = AuthResourceType.EXPERIENCE_GROUP_NEW.value
        ).data ?: false
    }

    private fun checkTaskPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            userId = userId,
            token = tokenService.getSystemToken(null) ?: "",
            action = authPermission.value,
            projectCode = projectId,
            resourceCode = "",
            resourceType = AuthResourceType.EXPERIENCE_TASK_NEW.value
        ).data ?: false
    }

    companion object {
        val logger = LoggerFactory.getLogger(StreamExperiencePermissionServiceImpl::class.java)
    }
}
