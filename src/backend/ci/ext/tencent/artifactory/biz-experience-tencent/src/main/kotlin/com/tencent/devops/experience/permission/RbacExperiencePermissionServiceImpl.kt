package com.tencent.devops.experience.permission

import com.tencent.bk.sdk.iam.util.AuthCacheUtil
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.AuthCacheKeyUtil
import com.tencent.devops.common.auth.rbac.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.service.ExperiencePermissionService
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class RbacExperiencePermissionServiceImpl @Autowired constructor(
    val client: Client,
    val dslContext: DSLContext,
    val experienceDao: ExperienceDao,
    val tokenService: ClientTokenService
) : ExperiencePermissionService {
    override fun validateTaskPermission(
        user: String,
        projectId: String,
        experienceId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        val action = RbacAuthUtils.buildAction(authPermission, AuthResourceType.EXPERIENCE_TASK)
        val resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_TASK)
        val cacheKey = AuthCacheKeyUtil.getCacheKey(
            userId = user,
            resourceType = resourceType,
            action = action,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(experienceId)
        )
        val checkPermission = AuthCacheUtil.cachePermission(cacheKey) {
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                token = tokenService.getSystemToken()!!,
                userId = user,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(experienceId),
                relationResourceType = null,
                action = action,
                resourceType = resourceType
            ).data ?: false
        }
        if (!checkPermission) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validateCreateTaskPermission(user: String, projectId: String): Boolean {
        val action = RbacAuthUtils.buildAction(AuthPermission.CREATE, AuthResourceType.EXPERIENCE_TASK)
        val resourceType = AuthResourceType.PROJECT.value
        val cacheKey = AuthCacheKeyUtil.getCacheKey(
            userId = user,
            resourceType = resourceType,
            action = action,
            projectCode = projectId,
            resourceCode = projectId
        )
        return AuthCacheUtil.cachePermission(cacheKey) {
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
                token = tokenService.getSystemToken()!!,
                userId = user,
                projectCode = projectId,
                action = action,
                resourceCode = resourceType
            ).data ?: false
        }
    }

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
            authPermission = AuthPermission.DELETE,
            message = message
        )
    }

    override fun createTaskResource(
        user: String,
        projectId: String,
        experienceId: Long,
        experienceName: String
    ) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = user,
            token = tokenService.getSystemToken()!!,
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
            token = tokenService.getSystemToken()!!,
            userId = user,
            projectCode = projectId,
            action = actions,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_TASK)
        ).data ?: emptyMap()
        return RbacAuthUtils.buildResultMap(instanceMap)
    }

    override fun filterCanListExperience(
        user: String,
        projectId: String,
        experienceRecordList: List<TExperienceRecord>
    ): List<TExperienceRecord> {
        val canListExperienceIds = filterExperience(user, projectId, setOf(AuthPermission.LIST))[AuthPermission.LIST]
        if (canListExperienceIds!!.isEmpty())
            return emptyList()
        val canListExperience = experienceRecordList.filter {
            canListExperienceIds.contains(it.id)
        }
        return canListExperience.ifEmpty { emptyList() }
    }

    override fun validateCreateGroupPermission(
        user: String,
        projectId: String
    ): Boolean {
        val action = RbacAuthUtils.buildAction(AuthPermission.CREATE, AuthResourceType.EXPERIENCE_GROUP)
        val resourceType = AuthResourceType.PROJECT.value
        val cacheKey = AuthCacheKeyUtil.getCacheKey(
            userId = user,
            resourceType = resourceType,
            action = action,
            projectCode = projectId,
            resourceCode = projectId
        )
        return AuthCacheUtil.cachePermission(cacheKey) {
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
                token = tokenService.getSystemToken()!!,
                userId = user,
                projectCode = projectId,
                action = action,
                resourceCode = resourceType
            ).data ?: false
        }
    }

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        val action = RbacAuthUtils.buildAction(authPermission, AuthResourceType.EXPERIENCE_GROUP)
        val resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP)
        val cacheKey = AuthCacheKeyUtil.getCacheKey(
            userId = userId,
            resourceType = resourceType,
            action = action,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId)
        )
        val checkPermission = AuthCacheUtil.cachePermission(cacheKey) {
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                token = tokenService.getSystemToken()!!,
                userId = userId,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(groupId),
                relationResourceType = null,
                action = action,
                resourceType = resourceType
            ).data ?: false
        }
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
            token = tokenService.getSystemToken()!!,
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
            token = tokenService.getSystemToken()!!,
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
            token = tokenService.getSystemToken()!!,
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
            token = tokenService.getSystemToken()!!,
            userId = user,
            projectCode = projectId,
            action = actions,
            resourceType = RbacAuthUtils.extResourceType(AuthResourceType.EXPERIENCE_GROUP)
        ).data ?: emptyMap()
        return RbacAuthUtils.buildResultMap(instanceMap)
    }

    override fun filterCanListGroup(
        user: String,
        projectId: String,
        groupRecordIds: List<Long>
    ): List<Long> {
        val canListGroupIds = filterGroup(
            user,
            projectId,
            setOf(AuthPermission.LIST)
        )[AuthPermission.LIST]
        if (canListGroupIds!!.isEmpty())
            return emptyList()
        val canListGroup = groupRecordIds.filter {
            canListGroupIds.contains(it)
        }
        return canListGroup.ifEmpty { emptyList() }
    }
}
