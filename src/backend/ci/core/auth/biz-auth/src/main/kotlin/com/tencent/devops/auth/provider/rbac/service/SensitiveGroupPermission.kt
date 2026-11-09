package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType

/**
 * 添加成员时需要风险确认的敏感权限。
 * 判定：使用类操作，且资源范围是项目下任意实例。
 */
object SensitiveGroupPermission {
    private val useActionToResourceType = linkedMapOf(
        ActionId.REPERTORY_USE to AuthResourceType.CODE_REPERTORY.value,
        ActionId.CREDENTIAL_USE to AuthResourceType.TICKET_CREDENTIAL.value,
        ActionId.CERT_USE to AuthResourceType.TICKET_CERT.value,
        ActionId.ENV_NODE_USE to AuthResourceType.ENVIRONMENT_ENV_NODE.value
    )

    val useActions: Set<String> = useActionToResourceType.keys

    fun resourceTypesOf(actions: Collection<String>): List<String> {
        if (actions.isEmpty()) {
            return emptyList()
        }
        return useActionToResourceType.filterKeys { it in actions }.values.toList()
    }

    fun isProjectAdminGroup(resourceType: String, groupCode: String?): Boolean {
        return resourceType == AuthResourceType.PROJECT.value &&
            groupCode == DefaultGroupType.MANAGER.value
    }
}
