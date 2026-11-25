package com.tencent.devops.auth.rbac.service

import com.tencent.devops.auth.provider.rbac.service.SensitiveGroupPermission
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthResourceType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SensitiveGroupPermissionTest {

    @Test
    fun resourceTypesKeepRiskCopyOrder() {
        val types = SensitiveGroupPermission.resourceTypesOf(
            listOf(ActionId.ENV_NODE_USE, ActionId.CREDENTIAL_USE, "pipeline_execute", ActionId.REPERTORY_USE)
        )
        Assertions.assertEquals(
            listOf(
                AuthResourceType.CODE_REPERTORY.value,
                AuthResourceType.TICKET_CREDENTIAL.value,
                AuthResourceType.ENVIRONMENT_ENV_NODE.value
            ),
            types
        )
    }

    @Test
    fun emptyActionsReturnEmptyTypes() {
        Assertions.assertTrue(SensitiveGroupPermission.resourceTypesOf(emptyList()).isEmpty())
    }

    @Test
    fun adminGroupOnlyMatchesProjectManager() {
        Assertions.assertTrue(
            SensitiveGroupPermission.isProjectAdminGroup(AuthResourceType.PROJECT.value, "manager")
        )
        Assertions.assertFalse(
            SensitiveGroupPermission.isProjectAdminGroup("pipeline", "manager")
        )
        Assertions.assertFalse(
            SensitiveGroupPermission.isProjectAdminGroup(AuthResourceType.PROJECT.value, "developer")
        )
        Assertions.assertFalse(
            SensitiveGroupPermission.isProjectAdminGroup(AuthResourceType.PROJECT.value, null)
        )
    }
}
