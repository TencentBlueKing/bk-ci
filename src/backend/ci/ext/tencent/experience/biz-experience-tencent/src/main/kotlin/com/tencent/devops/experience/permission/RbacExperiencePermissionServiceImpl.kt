package com.tencent.devops.experience.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.service.ExperiencePermissionService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class RbacExperiencePermissionServiceImpl @Autowired constructor(
    val client: Client,
    val dslContext: DSLContext,
    val experienceDao: ExperienceDao,
    val tokenService: ClientTokenService,
) : ExperiencePermissionService {
    override fun validateTaskPermission(
        user: String,
        projectId: String,
        experienceId: Long, authPermission: AuthPermission, message: String
    ) {
        val checkPermission = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(experienceId),
            relationResourceType = null,
            action = RbacAuthUtils.buildAction(authPermission, AuthResourceType.EXPERIENCE_TASK),
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_TASK)
        ).data ?: false
        if (!checkPermission) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createTaskResource(
        user: String,
        projectId: String,
        experienceId: Long, experienceName: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = user,
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_TASK),
            resourceCode = HashUtil.encodeLongId(experienceId),
            resourceName = experienceName
        )
    }

    override fun filterExperience(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val actions = RbacAuthUtils.buildActionList(authPermissions, AuthResourceType.EXPERIENCE_TASK)
        val instanceMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectId,
            action = actions,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_TASK)
        ).data ?: emptyMap()
        return RbacAuthUtils.buildResultMap(instanceMap)
    }

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        val checkPermission = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            relationResourceType = null,
            action = RbacAuthUtils.buildAction(authPermission, AuthResourceType.EXPERIENCE_GROUP),
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP)
        ).data ?: false
        if (!checkPermission) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createGroupResource(
        userId: String,
        projectId: String,
        groupId: Long,
        groupName: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP),
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun modifyGroupResource(
        projectId: String,
        groupId: Long,
        groupName: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP),
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun deleteGroupResource(
        projectId: String,
        groupId: Long
    ) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP),
            resourceCode = HashUtil.encodeLongId(groupId)
        )
    }

    override fun filterGroup(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val actions = RbacAuthUtils.buildActionList(authPermissions, AuthResourceType.EXPERIENCE_GROUP)
        val instanceMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectId,
            action = actions,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP)
        ).data ?: emptyMap()
        return RbacAuthUtils.buildResultMap(instanceMap)
    }
}
