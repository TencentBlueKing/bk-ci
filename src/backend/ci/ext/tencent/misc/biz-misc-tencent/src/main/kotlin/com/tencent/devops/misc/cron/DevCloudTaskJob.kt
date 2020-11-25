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

package com.tencent.devops.misc.cron

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.environment.agent.pojo.devcloud.Action
import com.tencent.devops.common.environment.agent.pojo.devcloud.Container
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudContainer
import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudImage
import com.tencent.devops.common.environment.agent.pojo.devcloud.ImageParams
import com.tencent.devops.common.environment.agent.pojo.devcloud.TaskAction
import com.tencent.devops.common.environment.agent.pojo.devcloud.TaskStatus
import com.tencent.devops.environment.pojo.enums.NodeStatus
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.common.environment.agent.client.DevCloudClient
import com.tencent.devops.common.environment.agent.pojo.devcloud.Registry
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.misc.dao.EnvironmentEnvNodeDao
import com.tencent.devops.misc.dao.EnvironmentNodeDao
import com.tencent.devops.misc.dao.devcloud.DevCloudTaskDao
import com.tencent.devops.misc.utils.NodeAuthUtils
import com.tencent.devops.model.environment.tables.records.TDevCloudTaskRecord
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import javax.ws.rs.NotFoundException

@Component
class DevCloudTaskJob @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val devCloudTaskDao: DevCloudTaskDao,
    private val nodeDao: EnvironmentNodeDao,
    private val envNodeDao: EnvironmentEnvNodeDao,
    private val nodeAuthUtils: NodeAuthUtils,
    private val devCloudClient: DevCloudClient,
    private val redisOperation: RedisOperation,
    private val gray: Gray
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudTaskJob::class.java)
        private const val jobLockKey = "env_cron_dev_cloud_task"
        private const val jobExecuteKey = "env_cron_dev_cloud_task_execute"
        private const val delay = 10L
    }

    @Value("\${notify.templateCode.devcloud:#{null}}")
    private lateinit var templateCode: String

    private val executor = Executors.newFixedThreadPool(10)

    @Scheduled(initialDelay = 50000, fixedDelay = delay * 1000)
    fun run() {
        logger.info("DevCloudTaskJob")
        val redisLock = RedisLock(
            redisOperation,
            jobLockKey(), delay
        )
        try {
            val lockSuccess = redisLock.tryLock()
            if (lockSuccess) {
                logger.info("Lock success")
                val key = jobExecuteKey()
                val lock = redisOperation.get(key)
                if (lock == null) {
                    redisOperation.set(key = key, value = "LOCKED", expiredInSecond = delay)
                    logger.info("DevCloudTaskJob start")
                    executeTask()
                } else {
                    logger.info("Lock failed, ignore")
                }
            }
        } catch (e: Throwable) {
            logger.error("DevCloudTaskJob exception", e)
        } finally {
            redisLock.unlock()
        }
    }

    private fun jobLockKey(): String {
        return if (gray.isGray()) {
            "gray_$jobLockKey"
        } else {
            jobLockKey
        }
    }

    private fun jobExecuteKey(): String {
        return if (gray.isGray()) {
            "gray_$jobExecuteKey"
        } else {
            jobExecuteKey
        }
    }

    private fun executeTask() {
        devCloudTaskDao.getWaitingTask(dslContext)?.forEach { task ->
            logger.info("task: $task")
            if (!gray.isGrayMatchProject(task.projectId, redisOperation)) {
                logger.info("The project[${task.projectId}] is not match the gray type[${gray.isGray()}], ignore")
                return@forEach
            }
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, TaskStatus.RUNNING)
            executor.execute {
                when (task.action) {
                    TaskAction.CREATE.name -> createDevCloudVm(task)
                    TaskAction.START.name -> startDevCloudVm(task)
                    TaskAction.STOP.name -> stopDevCloudVm(task)
                    TaskAction.RECREATE.name -> recreateDevCloudVm(task)
                    TaskAction.DELETE.name -> deleteDevCloudVm(task)
                    TaskAction.BUILD_IMAGE.name -> buildImage(task)
                }
            }
        }
    }

    private fun deleteDevCloudVm(task: TDevCloudTaskRecord) {
        val nodeToDelete = nodeDao.get(dslContext, task.projectId, task.nodeLongId)
        val devCloudTaskId: String
        try {
            devCloudTaskId = devCloudClient.operateContainer(task.operator, task.containerName, Action.DELETE)
        } catch (e: Throwable) {
            logger.error("Delete container exception", e)
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, TaskStatus.FAILED, e.message!!)
            return
        }

        val createResult = waitTaskFinish(task.operator, devCloudTaskId)
        if (createResult.first == TaskStatus.SUCCEEDED) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                nodeDao.batchDeleteNode(context, task.projectId, listOf(nodeToDelete!!.nodeId))
                envNodeDao.deleteByNodeIds(context, listOf(nodeToDelete.nodeId))
                nodeAuthUtils.deleteResource(task.projectId, nodeToDelete.nodeId)
                devCloudTaskDao.updateTaskStatus(context, task.taskId, TaskStatus.SUCCEEDED) // 刷新状态为成功
            }
            val (title, content) = createDeleteEmailBody(true, nodeToDelete!!, "")
            sendEmail(task.operator, task.projectId, title, content)
        } else {
            // 删除失败，记录日志，告警，通知
            logger.error("start dev cloud vm failed, status:${createResult.first}, msg: ${createResult.second}")
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, createResult.first, createResult.second)
            val (title, content) = createDeleteEmailBody(false, nodeToDelete!!, createResult.second)
            sendEmail(task.operator, task.projectId, title, content)
        }
    }

    private fun recreateDevCloudVm(task: TDevCloudTaskRecord) {
        logger.info("Not implement")
    }

    private fun stopDevCloudVm(task: TDevCloudTaskRecord) {
        val devCloudTaskId: String
        try {
            devCloudTaskId = devCloudClient.operateContainer(task.operator, task.containerName, Action.STOP)
        } catch (e: Throwable) {
            logger.error("stop container exception", e)
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, TaskStatus.FAILED, e.message!!)
            return
        }

        val createResult = waitTaskFinish(task.operator, devCloudTaskId)
        if (createResult.first == TaskStatus.SUCCEEDED) {
            nodeDao.updateNodeStatus(dslContext, task.nodeLongId, NodeStatus.STOPPED)
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, createResult.first) // 刷新状态为成功
        } else {
            // 启动失败，记录日志，告警，通知
            logger.error("start dev cloud vm failed, status:${createResult.first}, msg: ${createResult.second}")
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, createResult.first, createResult.second)
        }
    }

    private fun startDevCloudVm(task: TDevCloudTaskRecord) {
        nodeDao.updateNodeStatus(dslContext, task.nodeLongId, NodeStatus.STARTING)
        val devCloudTaskId: String
        try {
            devCloudTaskId = devCloudClient.operateContainer(task.operator, task.containerName, Action.START)
        } catch (e: Throwable) {
            logger.error("stop container exception", e)
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, TaskStatus.FAILED, e.message!!)
            return
        }
        val createResult = waitTaskFinish(task.operator, devCloudTaskId)
        if (createResult.first == TaskStatus.SUCCEEDED) {
            nodeDao.updateNodeStatus(dslContext, task.nodeLongId, NodeStatus.RUNNING)
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, createResult.first) // 刷新状态为成功
        } else {
            // 启动失败，记录日志，告警，通知
            logger.error("start dev cloud vm failed, status:${createResult.first}, msg: ${createResult.second}")
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, createResult.first, createResult.second)
        }
    }

    private fun createDevCloudVm(task: TDevCloudTaskRecord) {
        val errorMsg = mutableMapOf<Long, String>()

        // 得到本次任务创建的所有node
        val allDevCloudNodes = nodeDao.listDevCloudNodesByTaskId(dslContext, task.taskId)

        // dev类型的容器，需要一个一个创建
        for (i in 0 until task.replica) {
            val devCloudTaskId: String
            try {
                devCloudTaskId = devCloudClient.createContainer(
                    staffName = task.operator,
                    devCloudContainer = DevCloudContainer(
                        name = task.containerName,
                        type = task.containerType,
                        image = task.image,
                        registry = Registry(task.registryHost, task.registryUser, task.registryPwd),
                        cpu = task.cpu,
                        memory = task.memory,
                        disk = task.disk,
                        replica = 1,
                        ports = emptyList(),
                        password = task.password,
                        params = null
                    )
                )
            } catch (e: Throwable) {
                logger.error("createContainer exception", e)
                errorMsg[allDevCloudNodes[i].nodeId] = e.message!!
                allDevCloudNodes[i].nodeStatus = NodeStatus.DELETED.name
                // 创建不成功的节点需要删除
                nodeDao.updateNodeStatus(dslContext, allDevCloudNodes[i].nodeId, NodeStatus.DELETED)
                continue
            }
            logger.info("Create vm[$i] request success, devCloudTaskId: $devCloudTaskId")
            devCloudTaskDao.updateDevCloudTaskId(
                dslContext = dslContext,
                taskId = task.taskId,
                devCloudTaskId = devCloudTaskId
            )

            val createResult = waitTaskFinish(userId = task.operator, taskId = devCloudTaskId)
            if (createResult.first == TaskStatus.SUCCEEDED) {
                // 得到本次任务的实例的信息
                val containerName = createResult.second
                val containerInstanceInfo = devCloudClient.getContainerInstance(task.operator, containerName)
                val actionCode = containerInstanceInfo.optInt("actionCode")
                if (actionCode != 200) {
                    val actionMessage = containerInstanceInfo.optString("actionMessage")
                    logger.error("Get container instance failed, msg: $actionMessage")
                    errorMsg[allDevCloudNodes[i].nodeId] = actionMessage
                    // 创建不成功的节点需要删除
                    nodeDao.updateNodeStatus(dslContext, allDevCloudNodes[i].nodeId, NodeStatus.DELETED)
                    continue
                }

                val item = containerInstanceInfo.optJSONArray("data")[0] as JSONObject
                allDevCloudNodes[i].nodeIp = item.optString("ip")
                allDevCloudNodes[i].nodeStringId = item.optString("name")
                allDevCloudNodes[i].nodeName = containerName
                allDevCloudNodes[i].displayName = containerName
                if ("running" == item.optString("status")) {
                    allDevCloudNodes[i].nodeStatus = NodeStatus.RUNNING.name
                } else {
                    allDevCloudNodes[i].nodeStatus = NodeStatus.ABNORMAL.name
                }
                nodeDao.updateNode(dslContext, allDevCloudNodes[i])
                nodeAuthUtils.createNodeResource(
                    user = task.operator,
                    projectId = task.projectId,
                    nodeId = allDevCloudNodes[i].nodeId,
                    nodeStringId = allDevCloudNodes[i].nodeStringId,
                    nodeIp = allDevCloudNodes[i].nodeIp
                )
            } else {
                // 创建失败，记录日志，告警，通知
                logger.error("create dev cloud vm failed, status:${createResult.first}, msg: ${createResult.second}")
                errorMsg[allDevCloudNodes[i].nodeId] = createResult.second
                allDevCloudNodes[i].nodeStatus = NodeStatus.DELETED.name
                nodeDao.updateNode(dslContext, allDevCloudNodes[i])
            }
        }

        if (errorMsg.isEmpty()) {
            // 全部成功
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, TaskStatus.SUCCEEDED)
            val (title, content) = createAddVmEmailBody(
                successNum = task.replica,
                failedNum = 0,
                devCloudNodes = allDevCloudNodes,
                password = task.password,
                errMsg = emptyMap()
            )
            sendEmail(userId = task.operator, projectId = task.projectId, titleStr = title, contentStr = content)
        } else {
            // 有失败或全部失败
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, TaskStatus.FAILED)
            nodeDao.deleteDevCloudNodesByTaskId(dslContext, task.taskId)
            val (title, content) = createAddVmEmailBody(
                successNum = task.replica,
                failedNum = errorMsg.size,
                devCloudNodes = allDevCloudNodes,
                password = task.password,
                errMsg = errorMsg
            )
            sendEmail(userId = task.operator, projectId = task.projectId, titleStr = title, contentStr = content)
        }
    }

    private fun buildImage(task: TDevCloudTaskRecord) {
        val devCloudTaskId: String
        val imageNameTag = task.image.split(":")
        val imageName = imageNameTag[0]
        val imageTag = imageNameTag[1]
        try {
            val devCloudImage = DevCloudImage(
                imageName,
                imageTag,
                task.description,
                "rw",
                listOf(task.operator),
                ImageParams(
                    Container(task.containerName),
                    Registry(task.registryHost, task.registryUser, task.registryPwd)
                )
            )
            devCloudTaskId = devCloudClient.createImage(task.operator, devCloudImage)
        } catch (e: Throwable) {
            logger.error("create image exception", e)
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, TaskStatus.FAILED, e.message!!)
            return
        }

        val createResult = waitTaskFinish(task.operator, devCloudTaskId)
        if (createResult.first == TaskStatus.SUCCEEDED) {
            nodeDao.updateNodeStatus(dslContext, task.nodeLongId, NodeStatus.BUILD_IMAGE_SUCCESS)
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, createResult.first) // 刷新状态为成功
            val properties = mapOf(
                "devops.creator" to task.operator,
                "devops.desc" to task.description
            )
            client.get(ServiceArtifactoryResource::class).setProperties(task.projectId, imageName, imageTag, properties)
            val (title, content) = createBuildImageEmailBody(true, task.image, task.description, "")
            sendEmail(task.operator, task.projectId, title, content)
            // 将devcloud制作的镜像拷贝一份到docker构建镜像中
            copyImageToDockerHost(task.operator, task.projectId, imageName, imageTag)
        } else {
            // 启动失败，记录日志，告警，通知
            logger.error("create image failed, status:${createResult.first}, msg: ${createResult.second}")
            nodeDao.updateNodeStatus(dslContext, task.nodeLongId, NodeStatus.BUILD_IMAGE_FAILED)
            devCloudTaskDao.updateTaskStatus(dslContext, task.taskId, createResult.first, createResult.second)
            val (title, content) = createBuildImageEmailBody(false, task.image, task.description, createResult.second)
            sendEmail(task.operator, task.projectId, title, content)
        }
    }

    private fun copyImageToDockerHost(operator: String, projectId: String, imageName: String, imageTag: String) {
        try {
            client.get(ServiceImageResource::class).setBuildImage(
                userId = operator,
                projectId = projectId,
                imageRepo = imageName,
                imageTag = imageTag
            )
            logger.info("copy image to build image success")
        } catch (e: Throwable) {
            logger.error("copy image to build image failed, msg: ${e.message}")
        }
    }

    private fun waitTaskFinish(userId: String, taskId: String): Pair<TaskStatus, String> {
        logger.info("waiting for dev cloud task finish")
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 30 * 60 * 1000) {
                logger.error("dev cloud task timeout")
                return Pair(TaskStatus.TIMEOUT, "")
            }
            Thread.sleep(5 * 1000)
            val (isFinish, success, msg) = getTaskResult(userId, taskId)
            return when {
                !isFinish -> continue@loop
                !success -> {
                    logger.error("execute job failed, msg: $msg")
                    Pair(TaskStatus.FAILED, msg)
                }
                else -> Pair(TaskStatus.SUCCEEDED, msg)
            }
        }
    }

    private fun getTaskResult(userId: String, taskId: String): TaskResult {
        try {
            val taskStatus = devCloudClient.getTasks(userId, taskId)
            val actionCode = taskStatus.optString("actionCode")
            return if ("200" != actionCode) {
                // 创建失败
                val msg = taskStatus.optString("actionMessage")
                logger.error("Execute  task failed, actionCode is $actionCode, msg: $msg")
                TaskResult(true, false, msg)
            } else {
                val status = taskStatus.optJSONObject("data").optString("status")
                when (status) {
                    "succeeded" -> {
                        val containerName = taskStatus.optJSONObject("data").optString("name")
                        logger.info("Task success, containerName: $containerName")
                        TaskResult(true, true, containerName)
                    }
                    "failed" -> {
                        val resultDisplay = taskStatus.optJSONObject("data").optString("result")
                        logger.error("Task failed")
                        TaskResult(true, false, resultDisplay)
                    }
                    else -> TaskResult(false, false, "")
                }
            }
        } catch (e: Exception) {
            logger.error("Get dev cloud task error", e)
            return TaskResult(true, false, "创建失败，异常信息:${e.message}")
        }
    }

    private fun createAddVmEmailBody(
        successNum: Int,
        failedNum: Int,
        devCloudNodes: List<TNodeRecord>,
        password: String,
        errMsg: Map<Long, String>
    ): Pair<String, String> {
        val sb = StringBuilder("申请Linux机器完成！其中：成功（${successNum}台）,失败（${failedNum}台）")
            .append(
                """
                <tr class="email-information">
                    <td class="table-info">
                        <table cellpadding="0" cellspacing="0" width="100%" style="font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;">
                            <tr class="table-title">
                                <td style="padding-top: 36px; padding-bottom: 14px; color: #707070;">详情：</td>
                            </tr>
                            <tr>
                                <td>
                                    <table cellpadding="0" cellspacing="0" width="100%" style="font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;">
                                        <thead style="background: #f6f8f8;">
                                            <tr style="color: #333C48;">
                                                <th width="20%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">名称</th>
                                                <th width="20%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">IP</th>
                                                <th width="20%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;">状态</th>
                                                <th width="20%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;">密码</th>
                                                <th width="20%" style=" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;">描述</th>
                                            </tr>
                                        </thead>
                                        <tbody style="color: #707070;">
                """
            )
        devCloudNodes.forEach { node ->
            sb.append(
                """
                                            <tr>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${node.nodeName}</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${node.nodeIp}</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${NodeStatus.parseByName(
                    node.nodeStatus
                ).statusName}</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">$password</td>
                                                <td style=" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;">${errMsg[node.nodeId]
                    ?: ""}</td>
                                            </tr>
                    """
            )
        }
        sb.append(
            """
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            """
        )

        return Pair("申请Linux机器完成！", sb.toString())
    }

    private fun createDeleteEmailBody(success: Boolean, node: TNodeRecord, msg: String): Pair<String, String> {
        val sb = StringBuilder()
        if (success) {
            sb.append("销毁Linux机器成功！<br/>")
                .append("<br/>名称：${node.nodeName}<br/>")
                .append("IP: ${node.nodeIp}<br/>")
        } else {
            sb.append("销毁Linux机器失败！<br/>\"")
                .append("失败信息：$msg")
        }
        return Pair("销毁Linux机器完成", sb.toString())
    }

    private fun createBuildImageEmailBody(
        success: Boolean,
        imageNameTag: String,
        desc: String,
        msg: String
    ): Pair<String, String> {
        val sb = StringBuilder()
        if (success) {
            sb.append("制作镜像成功！<br/>")
                .append("<br/>镜像名称：$imageNameTag<br/>")
                .append("描述: $desc<br/>")
        } else {
            sb.append("制作镜像失败！<br/>\"")
                .append("<br/>镜像名称：$imageNameTag<br/>")
                .append("描述: $desc<br/>")
                .append("失败信息：$msg")
        }
        return Pair("制作镜像完成", sb.toString())
    }

    private fun sendEmail(userId: String, projectId: String, titleStr: String, contentStr: String) {
        try {
            val host = HomeHostUtil.innerServerHost()
            val url = "$host/console/environment/$projectId/nodeList"
            val content = "$contentStr <br/><br/>详情可以点击：<a target='_blank' href=\"$url\">查看详情</a> "

            val bkAuthProject = client.get(ServiceProjectResource::class).get(projectId).data
                ?: throw NotFoundException("Fail to find the project info of project($projectId)")
            logger.info("Send email, projectName: ${bkAuthProject.projectName}")
            val templateParams = mapOf(
                "templateTitle" to titleStr,
                "templateContent" to content,
                "projectName" to (bkAuthProject.projectName)
            )

            val sendNotifyMessageTemplateRequest = SendNotifyMessageTemplateRequest(
                templateCode = templateCode,
                receivers = mutableSetOf(userId),
                notifyType = mutableSetOf(NotifyType.EMAIL.name),
                titleParams = mapOf(),
                bodyParams = templateParams
            )
            val sendNotifyResult = client.get(ServiceNotifyMessageTemplateResource::class)
                .sendNotifyMessageByTemplate(sendNotifyMessageTemplateRequest)
            logger.info("[$projectId]|DevCloudTaskJob|sendNotifyMessageTemplateRequest=$sendNotifyMessageTemplateRequest|result=$sendNotifyResult")
        } catch (e: Throwable) {
            logger.error("Send email exception: ", e)
        }
    }

    data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)
}