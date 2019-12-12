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
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.element.ReportArchiveElement
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.task.AbstractTask
import com.tencent.devops.common.ci.task.CodeCCScanClientTask
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.element.SendRTXNotifyElement
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.environment.api.thirdPartyAgent.ServicePreBuildAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.log.api.UserLogResource
import com.tencent.devops.log.model.pojo.LogLine
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.model.prebuild.tables.records.TPrebuildProjectRecord
import com.tencent.devops.plugin.api.UserCodeccResource
import com.tencent.devops.prebuild.dao.PrebuildPersonalMachineDao
import com.tencent.devops.prebuild.dao.PrebuildProjectDao
import com.tencent.devops.prebuild.pojo.UserProject
import com.tencent.devops.prebuild.pojo.HistoryResponse
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.prebuild.pojo.PreProject
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class PreBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val prebuildProjectDao: PrebuildProjectDao,
    private val prebuildPersonalMachineDao: PrebuildPersonalMachineDao,
    private val preBuildConfig: PreBuildConfig
) {
    private val channelCode = ChannelCode.BS

    companion object {
        private val logger = LoggerFactory.getLogger(PreBuildService::class.java)
    }

    private fun getUserProjectId(userId: String): String {
        return "_$userId"
    }

    fun projectNameExist(userId: String, prebuildProjId: String) =
        null != prebuildProjectDao.get(dslContext, prebuildProjId, userId)

    fun startBuild(
        userId: String,
        preProjectId: String,
        workspace: String,
        yamlStr: String,
        yaml: CIBuildYaml,
        agentId: ThirdPartyAgentStaticInfo
    ): BuildId {
        val userProject = getUserProjectId(userId)
        val pipeline = getPipelineByName(userId, preProjectId)
        val model = createPipelineModel(userId, preProjectId, workspace, yaml, agentId)
        val pipelineId = if (null == pipeline) {
            client.get(ServicePipelineResource::class).create(userId, userProject, model, channelCode).data!!.id
        } else {
            client.get(ServicePipelineResource::class).edit(userId, userProject, pipeline.pipelineId, model, channelCode)
            pipeline.pipelineId
        }
        prebuildProjectDao.createOrUpdate(dslContext, preProjectId, userProject, userId, yamlStr.trim(), pipelineId, workspace)

        logger.info("pipelineId: $pipelineId")

        // 启动构建
        val buildId = client.get(ServiceBuildResource::class).manualStartup(userId, userProject, pipelineId, mapOf(), channelCode).data!!.id
        return BuildId(buildId)
    }

    private fun getPipelineByName(userId: String, preProjectId: String): Pipeline? {
        try {
            val pipelineList = client.get(ServicePipelineResource::class).list(userId, getUserProjectId(userId), 1, 1000).data!!.records
            pipelineList.forEach {
                if (it.pipelineName == preProjectId) {
                    return it
                }
            }
        } catch (e: Throwable) {
            logger.error("List pipeline failed, exception:", e)
        }

        return null
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

    private fun createPipelineModel(userId: String, preProjectId: String, workspace: String, prebuild: CIBuildYaml, agentInfo: ThirdPartyAgentStaticInfo): Model {
        val stageList = mutableListOf<Stage>()

        // 第一个stage，触发类
        val manualTriggerElement = ManualTriggerElement("手动触发", "T-1-1-1")
        val triggerContainer = TriggerContainer("0", "构建触发", listOf(manualTriggerElement))
        val stage1 = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(stage1)

        // 后面的stage
        prebuild.stages!!.forEachIndexed { stageIndex, stage ->
            val containerList = mutableListOf<Container>()
            stage.stage.forEachIndexed { jobIndex, job ->
                val elementList = mutableListOf<Element>()
                job.job.steps.forEach {
                    val element = it.covertToElement(getCiBuildConf(preBuildConfig))
                    elementList.add(element)
                    if (element is MarketBuildAtomElement) {
                        logger.info("install market atom: ${element.getAtomCode()}")
//                        installMarketAtom(getUserProjectId(userId), userId, element.getAtomCode())
                    }

                    addAssociateElement(it, elementList, userId, preProjectId)
                }
                val dispatchType = ThirdPartyAgentIDDispatchType(
                        displayName = agentInfo.agentId,
                        workspace = workspace,
                        agentType = AgentType.ID
                )

                val vmContainer = VMBuildContainer(
                        id = null,
                        name = job.job.name ?: "stage${stageIndex + 2}-${jobIndex + 1}",
                        elements = elementList,
                        status = null,
                        startEpoch = null,
                        systemElapsed = null,
                        elementElapsed = null,
                        baseOS = VMBaseOS.valueOf(agentInfo.os),
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
                        dispatchType = dispatchType
                )
                containerList.add(vmContainer)
            }
            stageList.add(Stage(containerList, "stage-${stageIndex + 3}"))
        }
        return Model(preProjectId, "", stageList, emptyList(), false, userId)
    }

    private fun addAssociateElement(it: AbstractTask, elementList: MutableList<Element>, userId: String, preProjectId: String) {
        if (it.getTaskType() == CodeCCScanClientTask.taskType) { // 如果yaml里面写的是codecc检查任务，则需要增加一个归档报告的插件
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
                false, null, null, null, null).data!!
        val cleanLogs = mutableListOf<LogLine>()
        cleanLogs.addAll(originLog.logs.filterNot { it.message.contains("soda_fold") })
        return QueryLogs(originLog.buildId, originLog.finished, cleanLogs, originLog.timeUsed, originLog.status)
    }

    fun getAfterLogs(userId: String, preProjectId: String, buildId: String, start: Long): QueryLogs {
        val prebuildProjRecord = getPreProjectInfo(preProjectId, userId)
        val originLog = client.get(UserLogResource::class).getAfterLogs(userId, prebuildProjRecord.projectId, prebuildProjRecord.pipelineId, buildId, start, false, null, null, null, null).data!!
        val cleanLogs = mutableListOf<LogLine>()
        cleanLogs.addAll(originLog.logs.filterNot { it.message.contains("soda_fold") })
        return QueryLogs(originLog.buildId, originLog.finished, cleanLogs, originLog.timeUsed, originLog.status)
    }

    private fun getPreProjectInfo(preProjectId: String, userId: String): TPrebuildProjectRecord {
        val preProjectRecord = prebuildProjectDao.get(dslContext, preProjectId, userId)
            ?: throw NotFoundException("当前工程未初始化，请初始化工程，工程名： $preProjectId")
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
        val preProjectRecord = prebuildProjectDao.get(dslContext, preProjectId, userId) ?: return emptyList()
        val historyList = client.get(ServiceBuildResource::class).getHistoryBuild(userId, preProjectRecord.projectId, preProjectRecord.pipelineId, page, pageSize, channelCode).data!!.records

        val result = mutableListOf<HistoryResponse>()
        historyList.forEach {
            result.add(HistoryResponse(it.id, it.buildNum, it.startTime, it.endTime, it.status))
        }
        return result
    }

    fun getBuildLink(userId: String, preProjectId: String, buildId: String): String {
        val preProjectRecord = getPreProjectInfo(preProjectId, userId)
        return HomeHostUtil.innerServerHost() + "/console/pipeline/${preProjectRecord.projectId}/${preProjectRecord.pipelineId}/detail/$buildId"
    }

    fun getOrCreatePreAgent(userId: String, os: OS, ip: String, hostName: String): ThirdPartyAgentStaticInfo {
        val machine = prebuildPersonalMachineDao.get(dslContext, userId, hostName)
        if (null == machine) {
            prebuildPersonalMachineDao.create(dslContext, userId, hostName, ip, "")
            logger.info("Insert host success, ip:$ip, hostName: $hostName")
        } else {
            logger.info("Host has already exists, ip: ${machine.ip}, hostName: ${machine.hostName}")
        }

        val agent = getAgent(userId, os, ip, hostName)
        if (null != agent) {
            logger.info("Agent has already exists. agentId: ${agent.agentId}")
            return agent
        }

        val createResult = client.get(ServicePreBuildAgentResource::class)
                .createPrebuildAgent(userId, getUserProjectId(userId), os, null, ip)
        if (createResult.isNotOk()) {
            logger.error("create prebuild agent failed")
            throw OperationException("create prebuild agent failed")
        }
        logger.info("create prebuild agent success")
        return createResult.data!!
    }

    fun getAgent(userId: String, os: OS, ip: String, hostName: String): ThirdPartyAgentStaticInfo? {
        val listPreAgentResult =
                client.get(ServicePreBuildAgentResource::class).listPreBuildAgent(userId, getUserProjectId(userId), os)
        if (listPreAgentResult.isNotOk()) {
            logger.error("list prebuild agent failed")
            throw OperationException("list prebuild agent failed")
        }
        val preAgents = listPreAgentResult.data!!

        // 优先按hostname查，查不到再按IP地址查
        preAgents.forEach {
            if (it.hostName == hostName) {
                logger.info("Get user personal vm, hostName: $hostName")
                if (it.ip != ip) { // IP 有变更
                    prebuildPersonalMachineDao.updateIp(dslContext, userId, hostName, ip)
                }

                return it
            }
        }
        preAgents.forEach {
            if (it.ip == ip) {
                logger.info("Get user personal vm, ip: $ip")
                if (it.hostName != hostName) { // hostname 有变更
                    prebuildPersonalMachineDao.updateHostname(dslContext, userId, hostName, ip)
                }
                return it
            }
        }

        return null
    }

    private fun getCiBuildConf(buildConf: PreBuildConfig): CiBuildConfig {
        return CiBuildConfig(
                buildConf.codeCCSofwareClientImage,
                buildConf.codeCCSofwarePath,
                buildConf.registryHost,
                buildConf.registryUserName,
                buildConf.registryPassword,
                buildConf.registryImage,
                buildConf.cpu,
                buildConf.memory,
                buildConf.disk,
                buildConf.volume,
                buildConf.activeDeadlineSeconds,
                buildConf.devCloudAppId,
                buildConf.devCloudToken,
                buildConf.devCloudUrl
        )
    }
}
