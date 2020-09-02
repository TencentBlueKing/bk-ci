package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.devops.auth.pojo.PermissionUrlDTO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.pojo.Action
import com.tencent.devops.common.auth.pojo.IamPermissionUrlReq
import com.tencent.devops.common.auth.pojo.RelatedResourceTypes
import com.tencent.devops.common.auth.service.IamEsbService
import com.tencent.devops.common.auth.utlis.ActionUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class IamService @Autowired constructor(
    val iamEsbService: IamEsbService,
    val iamConfiguration: IamConfiguration
) {
    fun getPermissionUrl(permissionUrlDTO: List<PermissionUrlDTO>): Result<String?> {
        logger.info("get permissionUrl permissionUrlDTO: $permissionUrlDTO")
        val actions = mutableListOf<Action>()
        permissionUrlDTO.map {
            val resourceType = ActionUtils.buildAction(it.resourceId, it.actionId)
            val instanceList = it.instanceId
            val relatedResourceTypes = mutableListOf<RelatedResourceTypes>()
            relatedResourceTypes.add(RelatedResourceTypes(
                    system = iamConfiguration.systemId,
                    type = it.resourceId.value,
                    instances = listOf(instanceList)))

            actions.add(
                    Action(
                            id = resourceType,
                            related_resource_types = relatedResourceTypes
                    )
            )
        }
        val iamEsbReq = IamPermissionUrlReq(
                system = iamConfiguration.systemId,
                actions = actions,
                bk_app_code = "",
                bk_app_secret = "",
                bk_username = "admin"
        )
        logger.info("get permissionUrl iamEsbReq: $iamEsbReq")
        return Result(iamEsbService.getPermissionUrl(iamEsbReq))
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}