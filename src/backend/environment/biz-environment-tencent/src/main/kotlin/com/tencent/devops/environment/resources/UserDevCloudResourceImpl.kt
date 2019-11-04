/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.resources

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserDevCloudResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_NODE_NO_CREATE_PERMISSSION
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.DevCloudImageParam
import com.tencent.devops.environment.pojo.DevCloudModel
import com.tencent.devops.environment.pojo.DevCloudVmParam
import com.tencent.devops.common.environment.agent.pojo.devcloud.TaskAction
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
        if (!environmentPermissionService.checkNodePermission(userId, projectId, AuthPermission.CREATE)) {
            throw ErrorCodeException(ERROR_NODE_NO_CREATE_PERMISSSION, "没有创建节点的权限")
        }
        devCloudService.addDevCloudVm(userId, projectId, devCloudVmParam)
        return Result(true)
    }

    override fun startDevCloudVm(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, AuthPermission.USE)
        devCloudService.operateDevCloudVm(userId, projectId, nodeHashId, containerName, TaskAction.START)
        return Result(true)
    }

    override fun stopDevCloudVm(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, AuthPermission.USE)
        devCloudService.operateDevCloudVm(userId, projectId, nodeHashId, containerName, TaskAction.STOP)
        return Result(true)
    }

    override fun deleteDevCloudVm(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, AuthPermission.DELETE)
        devCloudService.operateDevCloudVm(userId, projectId, nodeHashId, containerName, TaskAction.DELETE)
        return Result(true)
    }

    override fun createImage(
        userId: String,
        projectId: String,
        nodeHashId: String,
        devCloudImage: DevCloudImageParam
    ): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, AuthPermission.USE)
        devCloudService.buildImage(userId, projectId, nodeHashId, containerName, devCloudImage)
        return Result(true)
    }

    override fun createImageResultConfirm(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        val containerName = getContainerName(nodeHashId, projectId, userId, AuthPermission.USE)
        devCloudService.createImageResultConfirm(userId, projectId, nodeHashId, containerName)
        return Result(true)
    }

    override fun getDevCloudVm(userId: String, projectId: String, nodeHashId: String): Result<Map<String, Any>> {
        val containerName = getContainerName(nodeHashId, projectId, userId, AuthPermission.USE)
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
        permission: AuthPermission
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