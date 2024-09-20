package com.tencent.devops.auth.dao

import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.model.auth.tables.TAuthResourceGroupPermission
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupPermissionRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
@Suppress("LongParameterList")
class AuthResourceGroupPermissionDao {
    fun batchCreate(
        dslContext: DSLContext,
        records: List<ResourceGroupPermissionDTO>
    ) {
        with(TAuthResourceGroupPermission.T_AUTH_RESOURCE_GROUP_PERMISSION) {
            records.forEach {
                dslContext.insertInto(
                    this,
                    PROJECT_CODE,
                    RESOURCE_TYPE,
                    RESOURCE_CODE,
                    IAM_RESOURCE_CODE,
                    GROUP_CODE,
                    IAM_GROUP_ID,
                    ACTION,
                    ACTION_RELATED_RESOURCE_TYPE,
                    RELATED_RESOURCE_TYPE,
                    RELATED_RESOURCE_CODE,
                    RELATED_IAM_RESOURCE_CODE
                ).values(
                    it.projectCode,
                    it.resourceType,
                    it.resourceCode,
                    it.iamResourceCode,
                    it.groupCode,
                    it.iamGroupId,
                    it.action,
                    it.actionRelatedResourceType,
                    it.relatedResourceType,
                    it.relatedResourceCode,
                    it.relatedIamResourceCode
                ).onDuplicateKeyIgnore()
                    .execute()
            }
        }
    }

    fun batchDeleteByIds(
        dslContext: DSLContext,
        projectCode: String,
        ids: List<Long>
    ) {
        with(TAuthResourceGroupPermission.T_AUTH_RESOURCE_GROUP_PERMISSION) {
            dslContext.deleteFrom(this)
                .where(ID.`in`(ids))
                .and(PROJECT_CODE.eq(projectCode))
                .execute()
        }
    }

    fun listByGroupId(
        dslContext: DSLContext,
        projectCode: String,
        iamGroupId: Int
    ): List<ResourceGroupPermissionDTO> {
        return with(TAuthResourceGroupPermission.T_AUTH_RESOURCE_GROUP_PERMISSION) {
            dslContext.selectFrom(this)
                .where(PROJECT_CODE.eq(projectCode))
                .and(IAM_GROUP_ID.eq(iamGroupId))
                .fetch().map { it.convert() }
        }
    }

    fun TAuthResourceGroupPermissionRecord.convert(): ResourceGroupPermissionDTO {
        return ResourceGroupPermissionDTO(
            id = id,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            iamResourceCode = iamResourceCode,
            groupCode = groupCode,
            iamGroupId = iamGroupId,
            action = action,
            actionRelatedResourceType = actionRelatedResourceType,
            relatedResourceType = resourceType,
            relatedResourceCode = relatedResourceCode,
            relatedIamResourceCode = iamResourceCode
        )
    }
}
