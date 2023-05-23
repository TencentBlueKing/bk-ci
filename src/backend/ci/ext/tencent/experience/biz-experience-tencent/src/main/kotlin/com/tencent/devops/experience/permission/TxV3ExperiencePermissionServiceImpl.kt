package com.tencent.devops.experience.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.TActionUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.dao.GroupDao
import com.tencent.devops.experience.service.ExperiencePermissionService
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class TxV3ExperiencePermissionServiceImpl @Autowired constructor(
    val client: Client,
    val dslContext: DSLContext,
    val experienceDao: ExperienceDao,
    val groupDao: GroupDao,
    val tokenService: ClientTokenService,
    val authResourceApiStr: AuthResourceApiStr
) : ExperiencePermissionService {

    override fun validateTaskPermission(
        user: String,
        projectId: String,
        experienceId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (authPermission == AuthPermission.VIEW)
            return
        val checkPermission = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(experienceId),
            relationResourceType = null,
            action = TActionUtils.buildAction(authPermission, AuthResourceType.EXPERIENCE_TASK),
            resourceType = TActionUtils.extResourceType(AuthResourceType.EXPERIENCE_TASK)
        ).data ?: false
        if (!checkPermission) {
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

    override fun createTaskResource(user: String, projectId: String, experienceId: Long, experienceName: String) {
        authResourceApiStr.createResource(
            user = user,
            serviceCode = null,
            resourceType = TActionUtils.extResourceType(AuthResourceType.EXPERIENCE_TASK),
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(experienceId),
            resourceName = experienceName
        )
    }

    override fun filterExperience(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val actions = TActionUtils.buildActionList(authPermissions, AuthResourceType.EXPERIENCE_TASK)
        val instanceMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectId,
            action = actions,
            resourceType = TActionUtils.extResourceType(AuthResourceType.EXPERIENCE_TASK)
        ).data ?: emptyMap()
        val resultMap = mutableMapOf<AuthPermission, List<Long>>()
        instanceMap.forEach { key, value ->
            if (value.contains("*")) {
                val ids = experienceDao.list(dslContext, projectId, null, null).map { it.id }
                resultMap[key] = ids
            } else {
                val instanceList = value.map { HashUtil.decodeIdToLong(it) }
                resultMap[key] = instanceList
            }
        }
        return resultMap
    }

    override fun filterCanListExperience(
        user: String,
        projectId: String,
        experienceRecordList: List<TExperienceRecord>
    ): List<TExperienceRecord> = experienceRecordList

    override fun validateCreateGroupPermission(user: String, projectId: String): Boolean = true

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (authPermission == AuthPermission.VIEW)
            return
        val checkPermission = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            relationResourceType = null,
            action = TActionUtils.buildAction(authPermission, AuthResourceType.EXPERIENCE_GROUP),
            resourceType = TActionUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP)
        ).data ?: false
        if (!checkPermission) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createGroupResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        authResourceApiStr.createResource(
            user = userId,
            serviceCode = null,
            resourceType = TActionUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP),
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun modifyGroupResource(projectId: String, groupId: Long, groupName: String) {
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
        val actions = TActionUtils.buildActionList(authPermissions, AuthResourceType.EXPERIENCE_GROUP)
        val instanceMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectId,
            action = actions,
            resourceType = TActionUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP)
        ).data ?: emptyMap()
        val resultMap = mutableMapOf<AuthPermission, List<Long>>()
        instanceMap.forEach { key, value ->
            if (value.contains("*")) {
                val ids = groupDao.list(
                    dslContext = dslContext,
                    projectId = projectId,
                    offset = 0,
                    limit = 1000
                ).map { it.id }
                resultMap[key] = ids
            } else {
                val instanceList = value.map { HashUtil.decodeIdToLong(it) }
                resultMap[key] = instanceList
            }
        }
        return resultMap
    }

    override fun filterCanListGroup(
        user: String,
        projectId: String,
        groupRecordIds: List<Long>
    ): List<Long> = groupRecordIds
}
