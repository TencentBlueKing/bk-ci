package com.tencent.devops.experience.permission

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.BSAuthPermissionApi
import com.tencent.devops.common.auth.api.BSAuthResourceApi
import com.tencent.devops.common.auth.code.BSExperienceAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.experience.constant.ExperienceMessageCode
import com.tencent.devops.experience.service.ExperiencePermissionService
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

class TxExperiencePermissionServiceImpl @Autowired constructor(
    private val bsAuthPermissionApi: BSAuthPermissionApi,
    private val bsAuthResourceApi: BSAuthResourceApi,
    private val experienceServiceCode: BSExperienceAuthServiceCode
) : ExperiencePermissionService {

    private val taskResourceType = AuthResourceType.EXPERIENCE_TASK
    private val groupResourceType = AuthResourceType.EXPERIENCE_GROUP

    override fun validateTaskPermission(
        user: String,
        projectId: String,
        experienceId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (authPermission == AuthPermission.VIEW)
            return
        if (!bsAuthPermissionApi.validateUserResourcePermission(
                user = user,
                serviceCode = experienceServiceCode,
                resourceType = taskResourceType,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(experienceId),
                permission = authPermission
            )
        ) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                defaultMessage = authPermission.alias
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ExperienceMessageCode.USER_NEED_EXP_X_PERMISSION,
                defaultMessage = message,
                params = arrayOf(permissionMsg)
            )
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
        bsAuthResourceApi.createResource(
            user = user,
            serviceCode = experienceServiceCode,
            resourceType = taskResourceType,
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
        val permissionResourceMap = bsAuthPermissionApi.getUserResourcesByPermissions(
            user = user,
            serviceCode = experienceServiceCode,
            resourceType = taskResourceType,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = null
        )
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { (key, value) ->
            map[key] = value.map { HashUtil.decodeIdToLong(it) }
        }
        return map
    }

    override fun filterCanListExperience(
        user: String,
        projectId: String,
        experienceRecordList: List<TExperienceRecord>
    ): List<TExperienceRecord> {
        return experienceRecordList
    }

    override fun validateCreateGroupPermission(
        user: String,
        projectId: String
    ): Boolean = true

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!bsAuthPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = experienceServiceCode,
                resourceType = groupResourceType,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(groupId),
                permission = authPermission
            )
        ) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                defaultMessage = authPermission.alias
            )
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ExperienceMessageCode.USER_NEED_EXP_GROUP_X_PERMISSION,
                defaultMessage = message,
                params = arrayOf(permissionMsg)
            )
        }
    }

    override fun createGroupResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        bsAuthResourceApi.createResource(
            user = userId,
            serviceCode = experienceServiceCode,
            resourceType = groupResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun modifyGroupResource(projectId: String, groupId: Long, groupName: String) {
        bsAuthResourceApi.modifyResource(
            serviceCode = experienceServiceCode,
            resourceType = groupResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun deleteGroupResource(projectId: String, groupId: Long) {
        bsAuthResourceApi.deleteResource(
            serviceCode = experienceServiceCode,
            resourceType = groupResourceType,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId)
        )
    }

    override fun filterGroup(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = bsAuthPermissionApi.getUserResourcesByPermissions(
            user = user,
            serviceCode = experienceServiceCode,
            resourceType = groupResourceType,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = null
        )
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { (key, value) ->
            map[key] = value.map { HashUtil.decodeIdToLong(it) }
        }
        return map
    }

    override fun filterCanListGroup(
        user: String,
        projectId: String,
        groupRecordIds: List<Long>
    ): List<Long> = groupRecordIds
}
