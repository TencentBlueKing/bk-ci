package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.auth.api.ServiceGroupResource
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwAuthResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthResourceV3Impl @Autowired constructor(
    val client: Client,
    val tokenCheckService: ClientTokenService
) : ApigwAuthResourceV3 {
    override fun batchCreateGroup(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectCode: String,
        groupInfos: List<GroupDTO>
    ): Result<Boolean> {
        logger.info("batchCreateGroup $userId $projectCode $groupInfos")
        val ciGroupInfos = mutableListOf<GroupDTO>()
        groupInfos.forEach {
            var groupType = it.groupType
            var groupName = ""
            var displayName = ""
            if (!BkAuthGroup.contains(it.groupCode)) {
                groupType = false
                groupName = it.groupName ?: ""
                displayName = it.displayName ?: ""
            } else {
                groupType = true
                groupName = DefaultGroupType.get(it.groupCode).displayName
                displayName = DefaultGroupType.get(it.groupCode).displayName
            }
            ciGroupInfos.add(
                GroupDTO(
                    groupName = groupName,
                    groupCode = it.groupCode,
                    groupType = groupType,
                    relationId = it.relationId,
                    displayName = displayName
                )
            )
        }

        client.get(ServiceGroupResource::class).batchCreateGroup(
            userId,
            projectCode,
            ciGroupInfos
        )
        return Result(true)
    }

    override fun validateUserResourcePermission(
        appCode: String?,
        apigwType: String?,
        userId: String,
        action: String,
        projectId: String,
        resourceCode: String,
        resourceType: String
    ): Result<Boolean> {
        return Result(client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenCheckService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectId,
            resourceCode = resourceCode,
            resourceType = resourceType,
            relationResourceType = null,
            action = action
        ).data ?: false
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwAuthResourceV3Impl::class.java)
    }
}
