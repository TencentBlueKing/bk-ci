package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.auth.api.ServiceGroupResource
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.code.ExtAuthConstants
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v3.ApigwAuthResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthResourceV3Impl @Autowired constructor(
    val client: Client
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
            var groupType = ""
            var groupName = ""
            if (!BkAuthGroup.contains(it.groupCode)) {
                groupType = ExtAuthConstants.CUSTOM_GROUP
                groupName = it.displayName ?: ""
            } else {
                groupType = it.groupType
                groupName = it.groupName
            }
            ciGroupInfos.add(
                GroupDTO(
                    groupName = groupName,
                    groupCode = it.groupCode,
                    groupType = groupType,
                    relationId = it.relationId,
                    displayName = it.displayName
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

    companion object {
        val logger = LoggerFactory.getLogger(ApigwAuthResourceV3Impl::class.java)
    }
}
