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

package com.tencent.devops.prebuild.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.ReportArchiveElement
import com.tencent.devops.common.notify.pojo.elements.SendRTXNotifyElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.environment.api.thirdPartyAgent.ServicePreBuildAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.log.api.UserLogResource
import com.tencent.devops.log.model.pojo.LogLine
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.log.model.pojo.enums.LogStatus
import com.tencent.devops.model.prebuild.tables.records.TPrebuildProjectRecord
import com.tencent.devops.plugin.api.UserCodeccResource
import com.tencent.devops.prebuild.dao.PrebuildPersonalVmDao
import com.tencent.devops.prebuild.dao.PrebuildProjectDao
import com.tencent.devops.prebuild.pojo.UserProject
import com.tencent.devops.prebuild.pojo.Prebuild
import com.tencent.devops.prebuild.pojo.PreProjectReq
import com.tencent.devops.prebuild.pojo.InitPreProjectTask
import com.tencent.devops.prebuild.pojo.AbstractTask
import com.tencent.devops.prebuild.pojo.CodeCCScanTask
import com.tencent.devops.prebuild.pojo.HistoryResponse
import com.tencent.devops.prebuild.pojo.enums.TaskStatus
import com.tencent.devops.prebuild.utils.RedisUtils
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.prebuild.pojo.PreProject
import com.tencent.devops.prebuild.pojo.UserNode
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import javax.ws.rs.NotFoundException

@Service
class PreBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val prebuildProjectDao: PrebuildProjectDao,
    private val prebuildPersonalVmDao: PrebuildPersonalVmDao,
    private val devCloudVmService: DevCloudVmService,
    private val redisUtils: RedisUtils,
    private val preBuildConfig: PreBuildConfig
) {
    private val channelCode = ChannelCode.BS

    companion object {
        private val logger = LoggerFactory.getLogger(PreBuildService::class.java)
        private val executorService = Executors.newFixedThreadPool(30)
    }

    private fun getUserProjectId(userId: String): String {
        return "_$userId"
    }

    fun projectNameExist(userId: String, prebuildProjId: String) =
        null != prebuildProjectDao.get(dslContext, prebuildProjId, userId)

    fun manualStartup(
        userId: String,
        accessToken: String,
        preProjectId: String,
        buildYaml: String,
        prebuild: Prebuild
    ): BuildId {
        val userProject = getOrCreateUserProject(userId, accessToken)
        val projectId = userProject.projectCode
        val preProjectRecord = prebuildProjectDao.get(dslContext, preProjectId, userId)
            ?: throw OperationException("prebuild project not exist")
        val workspace = preProjectRecord.workspace
        val agent = getPreAgent(userId, userProject.projectCode)
        val pipelineId = if (!preProjectRecord.pipelineId.isNullOrBlank()) {
            with(preProjectRecord) {
                if (owner != userId) {
                    throw PermissionForbiddenException("用户${userId}没有操作权限")
                }
                if (yaml != buildYaml.trim()) {
                    val model = createPipelineModel(userId, preProjectId, agent.agentId, workspace, prebuild)
                    client.get(ServicePipelineResource::class).edit(userId, userProject.projectCode, pipelineId, model, channelCode)
                    prebuildProjectDao.update(dslContext, preProjectId, userId, buildYaml.trim(), pipelineId)
                }
                pipelineId
            }
        } else {
            val model = createPipelineModel(userId, preProjectId, agent.agentId, workspace, prebuild)
            val pipelineId = client.get(ServicePipelineResource::class).create(userId, projectId, model, channelCode).data!!.id
            prebuildProjectDao.update(dslContext, preProjectId, userId, buildYaml.trim(), pipelineId)
            pipelineId
        }

        logger.info("pipelineId: $pipelineId")
        // 启动构建
        val buildId = client.get(ServiceBuildResource::class).manualStartup(userId, userProject.projectCode, pipelineId, mapOf(), channelCode).data!!.id
        return BuildId(buildId)
    }

    fun shutDown(
        userId: String,
        accessToken: String,
        preProjectId: String,
        buildId: String
    ): Boolean {
        val userProject = getOrCreateUserProject(userId, accessToken)
        val projectId = userProject.projectCode
        val preProjectRecord = prebuildProjectDao.get(dslContext, preProjectId, userId)
                ?: throw OperationException("prebuild project not exist")
        logger.info("Manual shutdown the build, buildId: $buildId")
        return client.get(ServiceBuildResource::class).manualShutdown(userId, projectId, preProjectRecord.pipelineId, buildId, channelCode).data!!
    }

    private fun createPipelineModel(userId: String, preProjectId: String, agentId: String, workspace: String, prebuild: Prebuild): Model {
        val stageList = mutableListOf<Stage>()
        val elementList = mutableListOf<Element>()

        prebuild.steps.forEach {
            val element = it.covertToElement(preBuildConfig)
            elementList.add(element)
            addAssociateElement(it, elementList, userId, preProjectId)
        }
        val manualTriggerElement = ManualTriggerElement("手动触发", "T-1-1-1")
        val triggerContainer = TriggerContainer("0", "构建触发", listOf(manualTriggerElement))
        val stage1 = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(stage1)

        val vmContainer = VMBuildContainer(
            id = "1",
            name = "构建环境-LINUX",
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = VMBaseOS.LINUX,
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = 900,
            buildEnv = null,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            dispatchType = ThirdPartyAgentIDDispatchType(agentId, workspace, AgentType.ID)
        )
        val stage2 = Stage(listOf(vmContainer), "stage-2")
        stageList.add(stage2)
        return Model(preProjectId, "", stageList, emptyList(), false, userId)
    }

    private fun addAssociateElement(it: AbstractTask, elementList: MutableList<Element>, userId: String, preProjectId: String) {
        if (it.getClassType() == CodeCCScanTask.classType) { // 如果yaml里面写的是codecc检查任务，则需要增加一个归档报告的插件
            elementList.add(ReportArchiveElement(
                    "reportArchive",
                    null,
                    null,
                    "/tmp/codecc_$preProjectId/",
                    "index.html",
                    "PreBuild Report",
                    true,
                    setOf(userId),
                    "【\${pipeline.name}】 #\${pipeline.build.num} PreBuild报告已归档"
            ))
            elementList.add(SendRTXNotifyElement(
                    "sendRTXNotify",
                    null,
                    null,
                    setOf(userId),
                    "PreBuild流水线【\${pipeline.name}】 #\${pipeline.build.num} 构建完成通知",
                    "PreBuild流水线【\${pipeline.name}】 #\${pipeline.build.num} 构建完成\n",
                    false,
                    null,
                    true
            ))
        }
    }

    fun getBuildDetail(userId: String, preProjectId: String, buildId: String): Result<ModelDetail> {
        val preProjectRecord = getPreProjectInfo(preProjectId, userId)
        return client.get(ServiceBuildResource::class).getBuildDetail(userId, preProjectRecord.projectId, preProjectRecord.pipelineId, buildId, channelCode)
    }

    fun getInitLogs(userId: String, pipelineId: String, buildId: String): QueryLogs {
        val projectId = getUserProjectId(userId)
        val originLog = client.get(UserLogResource::class).getInitLogs(userId, projectId, pipelineId, buildId,
                false, null, null, null).data!!
        val cleanLogs = mutableListOf<LogLine>()
        cleanLogs.addAll(originLog.logs.filterNot { it.message.contains("soda_fold") })
        return QueryLogs(originLog.buildId, originLog.finished, cleanLogs, originLog.timeUsed, originLog.status)
    }

    fun getAfterLogs(userId: String, preProjectId: String, buildId: String, start: Long): QueryLogs {
        val prebuildProjRecord = getPreProjectInfo(preProjectId, userId)
        val originLog = client.get(UserLogResource::class).getAfterLogs(userId, prebuildProjRecord.projectId, prebuildProjRecord.pipelineId, buildId, start, false, null, null, null).data!!
        val cleanLogs = mutableListOf<LogLine>()
        cleanLogs.addAll(originLog.logs.filterNot { it.message.contains("soda_fold") })
        return QueryLogs(originLog.buildId, originLog.finished, cleanLogs, originLog.timeUsed, originLog.status)
    }

    fun getTaskLogs(userId: String, preProjectId: String, buildId: String): QueryLogs {
        val prebuildProjRecord = getPreProjectInfo(preProjectId, userId)
        val model = client.get(ServicePipelineResource::class).get(userId, prebuildProjRecord.projectId, prebuildProjRecord.pipelineId, channelCode).data!!
        val logs: MutableList<LogLine> = mutableListOf()
        var finished = true
        run outer@{
            model.stages.forEach { s ->
                s.containers.forEach { c ->
                    if (c is VMBuildContainer) {
                        c.elements.forEach { e ->
                            val queryLogs = client.get(UserLogResource::class).getInitLogs(userId, prebuildProjRecord.projectId,
                                prebuildProjRecord.pipelineId, buildId, false, null, e.id, null).data!!
                            if (queryLogs.status == LogStatus.SUCCEED) {
                                logs.addAll(queryLogs.logs.filterNot { it.message.contains("soda_fold") })
                            }
                            if (!queryLogs.finished) {
                                finished = false
                                return@outer
                            }
                        }
                    }
                }
            }
        }
        logs.filter { it.message.contains("Start Element") }
        return QueryLogs(buildId, finished, logs)
    }

    private fun getPreProjectInfo(preProjectId: String, userId: String): TPrebuildProjectRecord {
        val preProjectRecord = prebuildProjectDao.get(dslContext, preProjectId, userId)
            ?: throw NotFoundException("构建项目不存在")
        if (userId != preProjectRecord.owner) {
            throw NotFoundException("用户${userId}没有操作权限")
        }
        return preProjectRecord
    }

    fun getCodeccReport(userId: String, buildId: String) = client.get(UserCodeccResource::class).getCodeccReport(buildId)

    fun getOrCreateUserProject(userId: String, accessToken: String): UserProject {
        val projectResult = client.get(ServiceTxProjectResource::class).getPreUserProject(userId, accessToken)
        if (projectResult.isNotOk()) {
            throw RuntimeException("get user project err")
        }
        val project = projectResult.data!!
        return UserProject(
            project.id,
            project.projectId,
            project.projectName,
            project.projectCode,
            project.creator,
            project.description,
            project.englishName,
            project.updatedAt
        )
    }

    fun listUserNodes(userId: String): List<UserNode> {
        val projectCode = getUserProjectId(userId)
        val listPreAgentResult = client.get(ServicePreBuildAgentResource::class).listPreBuildAgent(userId, projectCode, OS.LINUX)
        if (listPreAgentResult.isNotOk()) {
            throw OperationException("list prebuild agent failed")
        }
        return listPreAgentResult.data!!.map {
            UserNode(
                it.agentId,
                ""
            )
        }
    }

    fun initPreProject(userId: String, accessToken: String, req: PreProjectReq): InitPreProjectTask {
        val userProject = getOrCreateUserProject(userId, accessToken)
        val preProject = prebuildProjectDao.get(dslContext, req.preProjectId, userId)
        if (preProject == null) {
            prebuildProjectDao.create(
                    dslContext,
                    req.preProjectId,
                    userProject.projectCode,
                    userId,
                    "",
                    "",
                    req.workspace
            )
        }
        val taskID = UUIDUtil.generate()
        val task = InitPreProjectTask(
            taskID,
            req.preProjectId,
            userProject.projectCode,
            req.workspace,
            "root",
            "",
            "",
            TaskStatus.RUNNING,
            mutableListOf(),
            userId
        )
        redisUtils.setPreBuildInitTask(taskID, task)
        val taskRunner = SpringContextUtil.getBean(TaskRunner::class.java)
        taskRunner.task = task
        executorService.execute(taskRunner)

        return task
    }

    fun queryInitTask(userId: String, taskId: String): InitPreProjectTask {
        val task = redisUtils.getPreBuildInitTask(taskId) ?: throw OperationException("task not exits")
        if (task.userId != userId) {
            logger.error("task userId: ${task.userId}, request userId: $userId")
            throw OperationException("No permission to access the task")
        }

        task.logs = redisUtils.getPreBuildInitTaskLogs(taskId)
        redisUtils.cleanPreBuildInitTaskLogs(taskId)
        return task
    }

    fun createUserNode(userId: String): UserNode {
        val vm = prebuildPersonalVmDao.get(dslContext, userId)
        return if (null == vm) {
            val (vmIP, pwd, containerName) = devCloudVmService.createVm(userId, "public/tlinux-2.2-base:latest")
            prebuildPersonalVmDao.create(dslContext, userId, vmIP, containerName, pwd)
            UserNode(vmIP, pwd)
        } else {
            UserNode(vm.vmIp, vm.rsyncPwd)
        }
    }

    fun executeCmdInUserNode(userId: String, command: String): Pair<Int, String> {
        val vm = prebuildPersonalVmDao.get(dslContext, userId) ?: return Pair(1, "vm not found")
        return devCloudVmService.executeContainerCommand(userId, vm.vmName, listOf(command))
    }

    fun getPreAgent(userId: String, projectCode: String): ThirdPartyAgentStaticInfo {
        val listPreAgentResult = client.get(ServicePreBuildAgentResource::class).listPreBuildAgent(userId, projectCode, null)
        if (listPreAgentResult.isNotOk()) {
            throw OperationException("list prebuild agent failed")
        }
        val preAgents = listPreAgentResult.data!!
        if (preAgents.isNotEmpty()) {
            return preAgents[0]
        }

        throw OperationException("Agent not exists.")
    }

    fun listPreProject(userId: String): List<PreProject> {
        val projectCode = getUserProjectId(userId)
        return prebuildProjectDao.list(dslContext, userId, projectCode).map {
            PreProject(
                it.prebuildProjectId,
                it.projectId,
                it.workspace,
                it.prebuildProjectId,
                ""
            )
        }
    }

    fun getHistory(userId: String, preProjectId: String, page: Int?, pageSize: Int?): List<HistoryResponse> {
        val preProjectRecord = getPreProjectInfo(preProjectId, userId)
        val historyList = client.get(ServiceBuildResource::class).getHistoryBuild(userId, preProjectRecord.projectId, preProjectRecord.pipelineId, page, pageSize, channelCode).data!!.records

        val result = mutableListOf<HistoryResponse>()
        historyList.forEach {
            result.add(
                HistoryResponse(
                    it.id,
                    it.buildNum,
                    it.startTime,
                    it.endTime,
                    it.status
                )
            )
        }
        return result
    }

    fun getBuildLink(userId: String, preProjectId: String, buildId: String): String {
        val preProjectRecord = getPreProjectInfo(preProjectId, userId)
        return HomeHostUtil.innerServerHost() + "/console/pipeline/${preProjectRecord.projectId}/${preProjectRecord.pipelineId}/detail/$buildId"
    }
}
