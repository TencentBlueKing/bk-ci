/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
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

package com.tencent.devops.environment.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.environment.agent.client.DevCloudClient
import com.tencent.devops.common.environment.agent.pojo.devcloud.ContainerType
import com.tencent.devops.common.environment.agent.pojo.devcloud.TaskAction
import com.tencent.devops.common.environment.agent.pojo.devcloud.TaskStatus
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.dao.StaticData
import com.tencent.devops.environment.dao.devcloud.DevCloudTaskDao
import com.tencent.devops.environment.pojo.DevCloudImageParam
import com.tencent.devops.environment.pojo.DevCloudModel
import com.tencent.devops.environment.pojo.DevCloudVmParam
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Random

@Service
class DevCloudService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    private val projectConfigDao: ProjectConfigDao,
    private val devCloudTaskDao: DevCloudTaskDao,
    private val devCloudClient: DevCloudClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    @Value("\${registry.host}")
    val registryHost: String? = null

    @Value("\${registry.userName}")
    val registryUser: String? = null

    @Value("\${registry.password}")
    val registryPwd: String? = null

    fun listDevCloudModel(): List<DevCloudModel> {
        return StaticData.getDevCloudModelList()
    }

    fun addDevCloudVm(userId: String, projectId: String, devCloudVmParam: DevCloudVmParam) {
        val projectConfig = projectConfigDao.get(dslContext, projectId, userId)
        if (!projectConfig.devCloudEnalbed) {
            logger.error("projectConfig.devCloudEnalbed is disabled")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_PROJECT_FEATURE_NOT_ACTIVED,
                params = arrayOf(projectId)
            )
        }
        val usedCount = nodeDao.countDevCloudVm(dslContext, projectId)
        val limit = projectConfig.devCloudQuota
        if (devCloudVmParam.instanceCount > limit - usedCount) {
            logger.error("projectConfig.devCloudQuota exhausted, max: $limit, used: $usedCount")
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_QUOTA_LIMIT,
                params = arrayOf(limit.toString(), usedCount.toString())
            )
        }
        val devCloudModel = StaticData.getDevCloudModelList().filter { it.moduleId == devCloudVmParam.modelId }
        val now = LocalDateTime.now()
        val toAddNodeList = mutableListOf<TNodeRecord>()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val taskId = devCloudTaskDao.insertTask(
                dslContext = context,
                projectId = projectId,
                operator = userId,
                action = TaskAction.CREATE,
                status = TaskStatus.WAITING,
                registryHost = registryHost!!,
                registryUser = registryUser,
                registryPwd = registryPwd,
                containerName = UUIDUtil.generate(),
                containerType = ContainerType.DEV,
                image = devCloudVmParam.imageId,
                cpu = devCloudModel[0].cpu,
                memory = devCloudModel[0].memory,
                disk = devCloudModel[0].disk,
                replica = devCloudVmParam.instanceCount,
                password = generatePwd(),
                nodeId = null,
                description = null
            )

            for (i in 0 until devCloudVmParam.instanceCount) {
                toAddNodeList.add(
                    TNodeRecord(
                        null,
                        "-",
                        projectId,
                        "-",
                        "-",
                        NodeStatus.CREATING.name,
                        NodeType.DEVCLOUD.name,
                        null,
                        null,
                        userId,
                        now,
                        null,
                        "Linux",
                        userId,
                        userId,
                        false,
                        "",
                        devCloudVmParam.imageId,
                        taskId,
                        now,
                        userId
                    )
                )
            }
            nodeDao.batchAddNode(context, toAddNodeList)
        }
    }

    fun operateDevCloudVm(
        userId: String,
        projectId: String,
        nodeHashId: String,
        containerName: String,
        action: TaskAction
    ) {
        val nodeLongId = HashUtil.decodeIdToLong(nodeHashId)
        val node = nodeDao.get(dslContext, projectId, nodeLongId)
            ?: throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS)
        if (action == TaskAction.DELETE) {
            if (node.nodeStatus == NodeStatus.DELETED.name ||
                node.nodeStatus == NodeStatus.CREATING.name ||
                node.nodeStatus == NodeStatus.STARTING.name ||
                node.nodeStatus == NodeStatus.STOPPING.name ||
                node.nodeStatus == NodeStatus.RESTARTING.name ||
                node.nodeStatus == NodeStatus.DELETING.name ||
                node.nodeStatus == NodeStatus.BUILDING_IMAGE.name
            ) {
                logger.info("dev cloud vm status is ${node.nodeStatus}, can not delete")
                throw ErrorCodeException(
                    errorCode = EnvironmentMessageCode.ERROR_VM_CAN_NOT_DESTROY,
                    params = arrayOf(NodeStatus.getStatusName(node.nodeStatus))
                )
            }
        }

        logger.info("insert into dev cloud task, containerName: $containerName, createEnv is ${action.name}")

        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            devCloudTaskDao.insertTask(
                dslContext = context,
                projectId = projectId,
                operator = userId,
                action = action,
                status = TaskStatus.WAITING,
                registryHost = null,
                registryUser = null,
                registryPwd = null,
                containerName = containerName,
                containerType = null,
                image = null,
                cpu = null,
                memory = null,
                disk = null,
                replica = null,
                password = null,
                nodeId = nodeLongId,
                description = null
            )
            val status = when (action) {
                TaskAction.CREATE -> NodeStatus.CREATING
                TaskAction.START -> NodeStatus.STARTING
                TaskAction.STOP -> NodeStatus.STOPPING
                TaskAction.RECREATE -> NodeStatus.RESTARTING
                TaskAction.DELETE -> NodeStatus.DELETING
                TaskAction.BUILD_IMAGE -> NodeStatus.BUILDING_IMAGE
                else -> NodeStatus.UNKNOWN
            }
            nodeDao.updateNodeStatus(context, nodeLongId, status)
        }
    }

    fun buildImage(
        userId: String,
        projectId: String,
        nodeHashId: String,
        containerName: String,
        devCloudImage: DevCloudImageParam
    ) {
        val nodeLongId = HashUtil.decodeIdToLong(nodeHashId)
        val node = nodeDao.get(dslContext, projectId, nodeLongId)
            ?: throw ErrorCodeException(errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS)
        if (node.nodeStatus != NodeStatus.RUNNING.name && node.nodeStatus != NodeStatus.NORMAL.name) {
            logger.info("dev cloud vm status is ${node.nodeStatus}, can not build image")
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_VM_CAN_NOT_IMAGED,
                params = arrayOf(NodeStatus.getStatusName(node.nodeStatus))
            )
        }
        logger.info("insert into dev cloud task, containerName: $containerName, createEnv is buildImage")
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            devCloudTaskDao.insertTask(
                dslContext = context,
                projectId = projectId,
                operator = userId,
                action = TaskAction.BUILD_IMAGE,
                status = TaskStatus.WAITING,
                registryHost = registryHost!!,
                registryUser = registryUser,
                registryPwd = registryPwd,
                containerName = containerName,
                containerType = null,
                image = "devcloud/$projectId/${devCloudImage.name.removePrefix("/")}:${devCloudImage.tag}",
                cpu = null,
                memory = null,
                disk = null,
                replica = null,
                password = null,
                nodeId = nodeLongId,
                description = devCloudImage.description
            )
            nodeDao.updateNodeStatus(context, nodeLongId, NodeStatus.BUILDING_IMAGE)
        }
    }

    fun createImageResultConfirm(userId: String, projectId: String, nodeHashId: String, containerName: String) {
        val nodeLongId = HashUtil.decodeIdToLong(nodeHashId)
        val node = nodeDao.get(dslContext, projectId, nodeLongId)
        if (null != node && (node.nodeStatus == NodeStatus.BUILD_IMAGE_FAILED.name || node.nodeStatus == NodeStatus.BUILD_IMAGE_SUCCESS.name)) {
            logger.info("Update node status to normal")
            nodeDao.updateNodeStatus(dslContext, nodeLongId, NodeStatus.NORMAL)
        }
    }

    fun getDevCloudVm(userId: String, containerName: String): JSONObject {
        return devCloudClient.getContainerInstance(userId, containerName)
    }

    private fun generatePwd(): String {
        val secretSeed =
            arrayOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz", "0123456789", "[()~!@#%&-+=_")

        val random = Random()
        val buf = StringBuffer()
        for (i in 0 until 15) {
            val num = random.nextInt(secretSeed[i / 4].length)
            buf.append(secretSeed[i / 4][num])
        }
        return buf.toString()
    }
}