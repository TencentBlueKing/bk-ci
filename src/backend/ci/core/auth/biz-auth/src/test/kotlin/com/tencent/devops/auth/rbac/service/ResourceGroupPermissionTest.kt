package com.tencent.devops.auth.rbac.service

import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ResourceGroupPermissionTest {
    @Test
    fun testResourceGroupPermissionDTO() {
        val oldResourceGroupPermissionDTO = ResourceGroupPermissionDTO(
            projectCode = "fayetest",
            resourceType = "pipeline",
            resourceCode = "p-f5e14babde7c442a84cba6b838163d6b",
            iamResourceCode = "9683",
            groupCode = "viewer",
            iamGroupId = 42078,
            action = "pipeline_list",
            actionRelatedResourceType = "pipeline",
            relatedResourceType = "pipeline",
            relatedResourceCode = "p-f5e14babde7c442a84cba6b838163d6b",
            relatedIamResourceCode = "9683"
        )
        val newResourceGroupPermissionDTO = ResourceGroupPermissionDTO(
            projectCode = "fayetest",
            resourceType = "pipeline",
            resourceCode = "p-f5e14babde7c442a84cba6b838163d6b",
            iamResourceCode = "9683",
            groupCode = "viewer",
            iamGroupId = 42078,
            action = "pipeline_list",
            actionRelatedResourceType = "pipeline",
            relatedResourceType = "pipeline",
            relatedResourceCode = "p-f5e14babde7c442a84cba6b838163d6b",
            relatedIamResourceCode = "9683"
        )
        Assertions.assertTrue(oldResourceGroupPermissionDTO == newResourceGroupPermissionDTO)
    }
}
