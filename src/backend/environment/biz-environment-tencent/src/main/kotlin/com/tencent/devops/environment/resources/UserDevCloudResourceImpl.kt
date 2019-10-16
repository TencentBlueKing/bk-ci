package com.tencent.devops.environment.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserDevCloudResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.DevCloudImageParam
import com.tencent.devops.environment.pojo.DevCloudModel
import com.tencent.devops.environment.pojo.DevCloudVmParam
import com.tencent.devops.environment.pojo.devcloud.TaskAction
import com.tencent.devops.environment.service.DevCloudService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserDevCloudResourceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val devCloudService: DevCloudService,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val nodeDao: NodeDao,
    private val objectMapper: ObjectMapper
) : UserDevCloudResource {
    override fun getDevCloudModelList(userId: String, projectId: String): Result<List<DevCloudModel>> {
        return Result(devCloudService.listDevCloudModel())
    }

    override fun addDevCloudVm(userId: String, projectId: String, devCloudVmParam: DevCloudVmParam): Result<Boolean> {
        if (!environmentPermissionService.checkNodePermission(userId, projectId, BkAuthPermission.CREATE)) {
            throw ErrorCodeException(ERROR_NODE_NO_CREATE_PERMISSSION, "没有创建节点的权限")
        }
        devCloudService.addDevCloudVm(userId, projectId, devCloudVmParam)
        return Result(true)
    }

    override fun startDevCloudVm(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, BkAuthPermission.USE)
        devCloudService.operateDevCloudVm(userId, projectId, nodeHashId, containerName, TaskAction.START)
        return Result(true)
    }

    override fun stopDevCloudVm(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, BkAuthPermission.USE)
        devCloudService.operateDevCloudVm(userId, projectId, nodeHashId, containerName, TaskAction.STOP)
        return Result(true)
    }

    override fun deleteDevCloudVm(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, BkAuthPermission.DELETE)
        devCloudService.operateDevCloudVm(userId, projectId, nodeHashId, containerName, TaskAction.DELETE)
        return Result(true)
    }

    override fun createImage(
        userId: String,
        projectId: String,
        nodeHashId: String,
        devCloudImage: DevCloudImageParam
    ): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, BkAuthPermission.USE)
        devCloudService.buildImage(userId, projectId, nodeHashId, containerName, devCloudImage)
        return Result(true)
    }

    override fun createImageResultConfirm(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, BkAuthPermission.USE)
        devCloudService.createImageResultConfirm(userId, projectId, nodeHashId, containerName)
        return Result(true)
    }

    override fun getDevCloudVm(userId: String, projectId: String, nodeHashId: String): Result<Map<String, Any>> {
        val containerName = getContainerName(nodeHashId, projectId, userId, BkAuthPermission.USE)
        return Result(
            objectMapper.readValue<Map<String, Any>>(
                devCloudService.getDevCloudVm(
                    userId,
                    containerName
                ).toString()
            )
        )
    }

    private fun getContainerName(
        nodeHashId: String,
        projectId: String,
        userId: String,
        permission: BkAuthPermission
    ): String {
        val nodeLongId = HashUtil.decodeIdToLong(nodeHashId)
        val existNodeList = nodeDao.listByIds(dslContext, projectId, listOf(nodeLongId))
        if (existNodeList.isEmpty()) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                defaultMessage = "节点不存在 [$nodeHashId]",
                params = arrayOf(nodeHashId)
            )
        }
        return existNodeList[0].nodeName
    }
}