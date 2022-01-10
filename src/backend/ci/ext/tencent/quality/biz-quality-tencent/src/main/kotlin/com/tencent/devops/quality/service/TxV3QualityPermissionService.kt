package com.tencent.devops.quality.service

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.TActionUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.quality.dao.QualityNotifyGroupDao
import com.tencent.devops.quality.dao.v2.QualityRuleDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class TxV3QualityPermissionService @Autowired constructor(
    val client: Client,
    val dslContext: DSLContext,
    val ruleDao: QualityRuleDao,
    val groupDao: QualityNotifyGroupDao,
    val tokenService: ClientTokenService,
    val authResourceApiStr: AuthResourceApiStr
) : QualityPermissionService {
    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        val permissionCheck = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            action = TActionUtils.buildAction(authPermission, AuthResourceType.QUALITY_GROUP),
            relationResourceType = null,
            resourceType = TActionUtils.extResourceType(AuthResourceType.QUALITY_GROUP)
        ).data ?: false
        if (!permissionCheck) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createGroupResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        authResourceApiStr.createResource(
            user = userId,
            serviceCode = null,
            resourceType = TActionUtils.extResourceType(AuthResourceType.QUALITY_GROUP),
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
        val actions = mutableListOf<String>()
        authPermissions.forEach {
            actions.add(TActionUtils.buildAction(it, AuthResourceType.QUALITY_GROUP))
        }
        val instancesMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            projectCode = projectId,
            resourceType = TActionUtils.extResourceType(AuthResourceType.QUALITY_GROUP),
            action = actions
        ).data ?: emptyMap<AuthPermission, List<Long>>()
        val resultMap = mutableMapOf<AuthPermission, List<Long>>()
        instancesMap.forEach { (key, value) ->
            val instanceLongIds = mutableListOf<Long>()
            if (value.contains("*")) {
                val count = groupDao.count(dslContext, projectId)
                groupDao.list(dslContext, projectId, 0, count.toInt())?.map { instanceLongIds.add(it.id) }
            } else {
                value.forEach {
                    instanceLongIds.add(HashUtil.decodeIdToLong(it.toString()))
                }
            }
            resultMap[key] = instanceLongIds
        }
        return resultMap
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            action = TActionUtils.buildAction(authPermission, AuthResourceType.QUALITY_RULE),
            resourceCode = TActionUtils.extResourceType(AuthResourceType.QUALITY_RULE)
        ).data ?: false
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!validateRulePermission(userId, projectId, authPermission)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        ruleId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        val checkPermission = client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(ruleId),
            action = TActionUtils.buildAction(authPermission, AuthResourceType.QUALITY_RULE),
            resourceType = TActionUtils.extResourceType(AuthResourceType.QUALITY_RULE),
            relationResourceType = null
        ).data ?: false
        if (!checkPermission) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun createRuleResource(userId: String, projectId: String, ruleId: Long, ruleName: String) {
        authResourceApiStr.createResource(
            user = userId,
            serviceCode = null,
            resourceType = TActionUtils.extResourceType(AuthResourceType.QUALITY_RULE),
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(ruleId),
            resourceName = ruleName
        )
    }

    override fun modifyRuleResource(projectId: String, ruleId: Long, ruleName: String) {
        return
    }

    override fun deleteRuleResource(projectId: String, ruleId: Long) {
        return
    }

    override fun filterRules(
        userId: String,
        projectId: String,
        bkAuthPermissionSet: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val actions = mutableListOf<String>()
        bkAuthPermissionSet.forEach {
            actions.add(TActionUtils.buildAction(it, AuthResourceType.QUALITY_RULE))
        }
        val instancesMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceType = TActionUtils.extResourceType(AuthResourceType.QUALITY_RULE),
            action = actions
        ).data ?: emptyMap<AuthPermission, List<Long>>()
        val resultMap = mutableMapOf<AuthPermission, List<Long>>()
        instancesMap.forEach { (key, value) ->
            val instanceLongIds = mutableListOf<Long>()
            if (value.contains("*")) {
                ruleDao.list(dslContext, projectId)?.map { instanceLongIds.add(it.id) }
            } else {
                value.forEach {
                    instanceLongIds.add(HashUtil.decodeIdToLong(it.toString()))
                }
            }
            resultMap[key] = instanceLongIds
        }
        return resultMap
    }
}
