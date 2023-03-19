package com.tencent.devops.auth.resources

import com.tencent.devops.auth.api.user.UserAuthPermissionResource
import com.tencent.devops.auth.pojo.dto.PermissionBatchValidateDTO
import com.tencent.devops.auth.service.iam.PermissionCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineViewResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAuthPermissionResourceImpl @Autowired constructor(
    val permissionService: PermissionService,
    val rbacCacheService: PermissionCacheService,
    val client: Client
) : UserAuthPermissionResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserAuthPermissionResourceImpl::class.java)
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        permissionBatchValidateDTO: PermissionBatchValidateDTO
    ): Result<Map<String, Boolean>> {
        logger.info("batch validate user resource permission|$userId|$projectCode|$permissionBatchValidateDTO")
        val watcher = Watcher("batchValidateUserResourcePermission|$userId|$projectCode")
        try {
            val projectActionList = mutableSetOf<String>()
            val resourceActionList = mutableSetOf<String>()

            permissionBatchValidateDTO.actionList.forEach { action ->
                val actionInfo = rbacCacheService.getActionInfo(action)
                val iamRelatedResourceType = actionInfo.relatedResourceType
                if (iamRelatedResourceType == AuthResourceType.PROJECT.value) {
                    projectActionList.add(action)
                } else {
                    resourceActionList.add(action)
                }
            }

            val actionCheckPermissionMap = mutableMapOf<String, Boolean>()
            // 验证项目下的权限
            if (projectActionList.isNotEmpty()) {
                watcher.start("batchValidateProjectAction")
                actionCheckPermissionMap.putAll(
                    validateProjectPermission(
                        userId = userId,
                        actions = projectActionList.toList(),
                        projectCode = projectCode
                    )
                )
            }
            // 验证具体资源权限
            if (resourceActionList.isNotEmpty()) {
                watcher.start("batchValidateResourceAction")
                actionCheckPermissionMap.putAll(
                    validateResourcePermission(
                        userId = userId,
                        projectCode = projectCode,
                        actions = resourceActionList.toList(),
                        resourceType = permissionBatchValidateDTO.resourceType,
                        resourceCode = permissionBatchValidateDTO.resourceCode
                    )
                )
            }
            return Result(actionCheckPermissionMap)
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }

    private fun validateProjectPermission(
        userId: String,
        actions: List<String>,
        projectCode: String
    ): Map<String, Boolean> {
        return permissionService.batchValidateUserResourcePermission(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resourceCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value
        )
    }

    private fun validateResourcePermission(
        userId: String,
        projectCode: String,
        actions: List<String>,
        resourceType: String,
        resourceCode: String
    ): Map<String, Boolean> {
        val parents = mutableListOf<AuthResourceInstance>()
        val projectInstance = AuthResourceInstance(
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        )
        parents.add(projectInstance)
        // 流水线需要添加流水线组父类
        if (resourceType == AuthResourceType.PIPELINE_DEFAULT.value) {
            client.get(UserPipelineViewResource::class).listViewIdsByPipelineId(
                userId = userId,
                projectId = projectCode,
                pipelineId = resourceCode
            ).data?.forEach { viewId ->
                parents.add(
                    AuthResourceInstance(
                        resourceType = AuthResourceType.PIPELINE_GROUP.value,
                        resourceCode = HashUtil.encodeLongId(viewId),
                        parents = listOf(projectInstance)
                    )
                )
            }
        }
        val resourceInstance = AuthResourceInstance(
            resourceType = resourceType,
            resourceCode = resourceCode,
            parents = parents
        )
        return permissionService.batchValidateUserResourcePermissionByInstance(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resource = resourceInstance
        )
    }
}
