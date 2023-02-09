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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.prebuild.service

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.ci.NORMAL_JOB
import com.tencent.devops.common.ci.VM_JOB
import com.tencent.devops.common.ci.image.PoolType
import com.tencent.devops.common.ci.task.AbstractTask
import com.tencent.devops.common.ci.task.CodeCCScanInContainerTask
import com.tencent.devops.common.ci.task.MarketBuildInput
import com.tencent.devops.common.ci.task.MarketBuildTask
import com.tencent.devops.common.ci.task.SyncLocalCodeInput
import com.tencent.devops.common.ci.task.SyncLocalCodeTask
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.ci.yaml.Job
import com.tencent.devops.common.ci.yaml.ResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.pojo.LogLine
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.environment.api.thirdPartyAgent.ServicePreBuildAgentResource
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.log.api.ServiceLogResource
import com.tencent.devops.model.prebuild.tables.records.TPrebuildProjectRecord
import com.tencent.devops.prebuild.dao.PreBuildPluginVersionDao
import com.tencent.devops.prebuild.dao.PrebuildPersonalMachineDao
import com.tencent.devops.prebuild.dao.PrebuildProjectDao
import com.tencent.devops.prebuild.pojo.HistoryResponse
import com.tencent.devops.prebuild.pojo.PrePluginVersion
import com.tencent.devops.prebuild.pojo.PreProject
import com.tencent.devops.prebuild.pojo.StartUpReq
import com.tencent.devops.prebuild.pojo.UserProject
import com.tencent.devops.prebuild.pojo.enums.PreBuildPluginType
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Service
class PreBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val prebuildProjectDao: PrebuildProjectDao,
    private val prebuildPersonalMachineDao: PrebuildPersonalMachineDao,
    private val preBuildVersionDao: PreBuildPluginVersionDao,
    private val preBuildConfig: PreBuildConfig
) : CommonPreBuildService(client, dslContext, prebuildProjectDao) {
    companion object {
        private val logger = LoggerFactory.getLogger(PreBuildService::class.java)
    }

    fun projectNameExist(userId: String, prebuildProjId: String) =
        null != prebuildProjectDao.get(dslContext, prebuildProjId, userId)

    fun startBuild(
        userId: String,
        preProjectId: String,
        startUpReq: StartUpReq,
        yaml: CIBuildYaml,
        agentId: ThirdPartyAgentStaticInfo
    ): BuildId {
        val model = createPipelineModel(userId, preProjectId, startUpReq, yaml, agentId)
        val pipelineId = createOrUpdatePipeline(userId, preProjectId, startUpReq, model)
        val projectId = getUserProjectId(userId)

        // 启动构建
        val buildId = client.get(ServiceBuildResource::class)
            .manualStartup(userId, projectId, pipelineId, mapOf(), channelCode).data!!.id
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
        return client.get(ServiceBuildResource::class)
            .manualShutdown(
                userId,
                projectId,
                preProjectRecord.pipelineId,
                buildId,
                channelCode
            ).data!!
    }

    private fun createPipelineModel(
        userId: String,
        preProjectId: String,
        startUpReq: StartUpReq,
        prebuild: CIBuildYaml,
        agentInfo: ThirdPartyAgentStaticInfo
    ): Model {
        val stageList = mutableListOf<Stage>()

        val buildFormProperties = mutableListOf<BuildFormProperty>()
        if (prebuild.variables != null && prebuild.variables!!.isNotEmpty()) {
            prebuild.variables!!.forEach {
                val property = BuildFormProperty(
                    id = it.key,
                    required = false,
                    type = BuildFormPropertyType.STRING,
                    defaultValue = it.value,
                    options = null,
                    desc = null,
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
                buildFormProperties.add(property)
            }
        }

        // 第一个stage，触发类
        val manualTriggerElement = ManualTriggerElement("手动触发", "T-1-1-1")
        val triggerContainer = TriggerContainer(
            id = "0",
            name = "构建触发",
            elements = listOf(manualTriggerElement),
            params = buildFormProperties
        )
        val stage1 = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(stage1)

        // 后面的stage
        prebuild.stages!!.forEachIndexed { stageIndex, stage ->
            val containerList = mutableListOf<Container>()
            stage.stage.forEachIndexed { jobIndex, job ->
                if (job.job.type == null || job.job.type == VM_JOB) {
                    val vmContainer =
                        createVMBuildContainer(job, startUpReq, agentInfo, jobIndex, userId)
                    containerList.add(vmContainer)
                } else if (job.job.type == NORMAL_JOB) {
                    val normalContainer = createNormalContainer(job, userId)
                    containerList.add(normalContainer)
                } else {
                    logger.error("Invalid job type: ${job.job.type}")
                }
            }
            stageList.add(Stage(containerList, "stage-${stageIndex + 3}"))
        }
        return Model(preProjectId, "", stageList, emptyList(), false, userId)
    }

    private fun createNormalContainer(job: Job, userId: String): NormalContainer {
        val elementList = mutableListOf<Element>()
        job.job.steps.forEach {
            val element = it.covertToElement(getCiBuildConf(preBuildConfig))
            elementList.add(element)
            if (element is MarketBuildAtomElement || element is MarketBuildLessAtomElement) {
                logger.info("install market atom: ${element.getAtomCode()}")
                installMarketAtom(userId, element.getAtomCode())
            }
        }

        return NormalContainer(
            containerId = null,
            id = null,
            name = "无编译环境",
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            enableSkip = false,
            conditions = null,
            canRetry = false,
            jobControlOption = null,
            mutexGroup = null
        )
    }

    private fun createVMBuildContainer(
        job: Job,
        startUpReq: StartUpReq,
        agentInfo: ThirdPartyAgentStaticInfo,
        jobIndex: Int,
        userId: String
    ): VMBuildContainer {
        val elementList = mutableListOf<Element>()
        val vmType = job.job.resourceType
        job.job.steps.forEach {
            var step = it
            if (startUpReq.extraParam != null) {
                step = codeCCAtomCheck(step, startUpReq, job)
            }
            // 启动子流水线将代码拉到远程构建机
            if (step is SyncLocalCodeTask) {
                if (vmType != ResourceType.REMOTE) {
                    return@forEach
                }
                step.inputs = SyncLocalCodeInput(
                    step.inputs?.agentId ?: agentInfo.agentId,
                    step.inputs?.workspace ?: startUpReq.workspace,
                    step.inputs?.useDelete ?: true,
                    step.inputs?.syncGitRepository ?: false
                )

                installMarketAtom(userId, "syncCodeToRemote") // 确保同步代码插件安装
            }

            val element = step.covertToElement(getCiBuildConf(preBuildConfig))
            elementList.add(element)
            if (element is MarketBuildAtomElement || element is MarketBuildLessAtomElement) {
                logger.info("install market atom: ${element.getAtomCode()}")
                installMarketAtom(userId, element.getAtomCode())
            }
        }

        val dispatchType = getDispatchType(job, startUpReq, agentInfo)

        val vmBaseOS = if (vmType == ResourceType.REMOTE) {
            when (dispatchType) {
                is ThirdPartyAgentIDDispatchType -> {
                    job.job.pool?.os ?: VMBaseOS.LINUX
                }
                is ThirdPartyAgentEnvDispatchType -> {
                    job.job.pool?.os ?: VMBaseOS.LINUX
                }
                is MacOSDispatchType -> VMBaseOS.MACOS
                else -> VMBaseOS.LINUX
            }
        } else VMBaseOS.valueOf(agentInfo.os)

        return VMBuildContainer(
            id = null,
            name = "Job_${jobIndex + 1} " + (job.job.displayName ?: ""),
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = vmBaseOS,
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = 900,
            buildEnv = job.job.pool?.env,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            dispatchType = dispatchType
        )
    }

    private fun codeCCAtomCheck(
        oldStep: AbstractTask,
        startUpReq: StartUpReq,
        job: Job
    ): AbstractTask {
        var step = oldStep
        val isRunOnDocker = job.job.resourceType == ResourceType.REMOTE &&
                (job.job.pool?.type == PoolType.DockerOnDevCloud ||
                        job.job.pool?.type == PoolType.DockerOnVm)
        if (step is MarketBuildTask && step.inputs.atomCode == CodeCCScanInContainerTask.atomCode) {
            val whitePath = getWhitePath(startUpReq, isRunOnDocker)
            val data = step.inputs.data.toMutableMap()
            val input = (data["input"] as Map<*, *>).toMutableMap()
            if (whitePath.isNotEmpty()) {
                input["path"] = whitePath
            }
            data["input"] = input.toMap()
            step = step.copy(
                inputs = with(step.inputs) {
                    MarketBuildInput(atomCode, name, version, data.toMap())
                }
            )
        } else if (step is CodeCCScanInContainerTask) {
            val whitePath = getWhitePath(startUpReq, isRunOnDocker)
            if (whitePath.isNotEmpty()) {
                step.inputs.path = whitePath
            }
        }
        return step
    }

    fun getDispatchType(
        job: Job,
        startUpReq: StartUpReq,
        agentInfo: ThirdPartyAgentStaticInfo
    ): DispatchType {
        return when (job.job.resourceType) {
            ResourceType.LOCAL, null -> {
                ThirdPartyAgentIDDispatchType(
                    displayName = agentInfo.agentId,
                    workspace = startUpReq.workspace,
                    agentType = AgentType.ID,
                    dockerInfo = null
                )
            }

            ResourceType.REMOTE -> {
                with(job.job.pool) {
                    if (this == null) {
                        logger.error("getDispatchType , remote , pool is null")
                        throw OperationException("当 resourceType = REMOTE, pool参数不能为空")
                    }

                    (this.type ?: PoolType.DockerOnVm).toDispatchType(this)
                }
            }
        }
    }

    fun getBuildDetail(userId: String, preProjectId: String, buildId: String): Result<ModelDetail> {
        val preProjectRecord = getPreProjectInfo(preProjectId, userId)

        return client.get(ServiceBuildResource::class)
            .getBuildDetail(
                userId,
                preProjectRecord.projectId,
                preProjectRecord.pipelineId,
                buildId,
                channelCode
            )
    }

    fun getInitLogs(
        userId: String,
        pipelineId: String,
        buildId: String,
        debugLog: Boolean?
    ): QueryLogs {
        val projectId = getUserProjectId(userId)
        val originLog = client.get(ServiceLogResource::class).getInitLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debugLog,
            tag = null,
            jobId = null,
            executeCount = null
        ).data!!
        val cleanLogs = mutableListOf<LogLine>()
        cleanLogs.addAll(originLog.logs.filterNot {
            it.message.contains("soda_fold")
        }.run {
            if (null == debugLog || debugLog) this else filterNot { it.tag.startsWith("startVM") }
        })
        return QueryLogs(
            originLog.buildId,
            originLog.finished,
            originLog.hasMore,
            cleanLogs,
            originLog.timeUsed,
            originLog.status
        )
    }

    fun getAfterLogs(
        userId: String,
        preProjectId: String,
        buildId: String,
        start: Long,
        debugLog: Boolean?
    ): QueryLogs {
        val prebuildProjRecord = getPreProjectInfo(preProjectId, userId)
        val originLog = client.get(ServiceLogResource::class).getAfterLogs(
            userId = userId,
            projectId = prebuildProjRecord.projectId,
            pipelineId = prebuildProjRecord.pipelineId,
            buildId = buildId,
            start = start,
            debug = debugLog,
            tag = null,
            jobId = null,
            executeCount = null
        ).data!!
        val cleanLogs = mutableListOf<LogLine>()
        cleanLogs.addAll(originLog.logs.filterNot {
            it.message.contains("soda_fold")
        }.run {
            if (null == debugLog || debugLog) this else filterNot { it.tag.startsWith("startVM") }
        })
        return QueryLogs(
            originLog.buildId,
            originLog.finished,
            originLog.hasMore,
            cleanLogs,
            originLog.timeUsed,
            originLog.status
        )
    }

    private fun getPreProjectInfo(preProjectId: String, userId: String): TPrebuildProjectRecord {
        val preProjectRecord = prebuildProjectDao.get(dslContext, preProjectId, userId)
            ?: throw NotFoundException("当前工程未初始化，请初始化工程，工程名： $preProjectId")
        if (userId != preProjectRecord.owner) {
            throw NotFoundException("用户${userId}没有操作权限")
        }
        return preProjectRecord
    }

    fun getOrCreateUserProject(userId: String, accessToken: String): UserProject {
        val projectResult =
            client.get(ServiceTxProjectResource::class).getPreUserProject(userId, accessToken)

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

    fun getHistory(
        userId: String,
        preProjectId: String,
        page: Int?,
        pageSize: Int?
    ): List<HistoryResponse> {
        val preProjectRecord =
            prebuildProjectDao.get(dslContext, preProjectId, userId) ?: return emptyList()
        val historyList = client.get(ServiceBuildResource::class).getHistoryBuild(
            userId,
            preProjectRecord.projectId,
            preProjectRecord.pipelineId,
            page,
            pageSize,
            channelCode
        ).data!!.records

        val result = mutableListOf<HistoryResponse>()
        historyList.forEach {
            result.add(HistoryResponse(it.id, it.buildNum, it.startTime, it.endTime, it.status))
        }
        return result
    }

    fun getBuildLink(userId: String, preProjectId: String, buildId: String): String {
        val preProjectRecord = getPreProjectInfo(preProjectId, userId)
        return HomeHostUtil.innerServerHost() +
                "/console/pipeline/${preProjectRecord.projectId}/${preProjectRecord.pipelineId}/detail/$buildId"
    }

    fun getOrCreatePreAgent(
        userId: String,
        os: OS,
        ip: String,
        hostName: String
    ): ThirdPartyAgentStaticInfo {
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
                logger.debug("Get user personal vm, hostName: $hostName")
                if (it.ip != ip) { // IP 有变更
                    logger.debug("Update ip, ip: $ip")
                    prebuildPersonalMachineDao.updateIp(dslContext, userId, hostName, ip)
                }

                return it
            }
        }
        preAgents.forEach {
            if (it.ip == ip) {
                logger.debug("Get user personal vm, ip: $ip")
                if (it.hostName != hostName) { // hostname 有变更
                    logger.debug("Update hostName, hostName: $hostName")
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

    fun getAgentStatus(userId: String, os: OS, ip: String, hostName: String): AgentStatus {
        val agent = getAgent(userId, os, ip, hostName)
        if (agent?.status == null) {
            logger.info("Agent not exists. need to install.")
            return AgentStatus.IMPORT_EXCEPTION
        }
        return AgentStatus.fromStatus(agent.status!!)
    }

    fun validateCIBuildYaml(yamlStr: String) = CiYamlUtils.validateYaml(yamlStr)

    fun checkYml(yamlStr: String) =
        CiYamlUtils.checkYaml(
            YamlUtil.getObjectMapper().readValue(yamlStr, CIBuildYaml::class.java)
        )

    fun getPluginVersion(userId: String, pluginType: PreBuildPluginType): PrePluginVersion? {
        val record = preBuildVersionDao.getVersion(
            pluginType = pluginType.name,
            dslContext = dslContext
        ) ?: return null

        return PrePluginVersion(
            version = record.version,
            desc = record.desc,
            modifyUser = record.modifyUser,
            updateTime = DateTimeUtil.toDateTime(record.updateTime as LocalDateTime),
            pluginType = PreBuildPluginType.valueOf(record.pluginType)
        )
    }

    fun creatPluginVersion(prePluginVersion: PrePluginVersion): Boolean {
        val record = preBuildVersionDao.getVersion(
            pluginType = prePluginVersion.pluginType.name,
            dslContext = dslContext
        )

        if (record != null) {
            throw RuntimeException("已存在当前插件类型的版本信息，无法新增")
        }

        return with(prePluginVersion) {
            preBuildVersionDao.create(
                version = version,
                modifyUser = modifyUser,
                desc = desc,
                pluginType = pluginType.name,
                dslContext = dslContext
            ) > 0
        }
    }

    fun deletePluginVersion(version: String): Boolean {
        return preBuildVersionDao.delete(version, dslContext) > 0
    }

    fun updatePluginVersion(prePluginVersion: PrePluginVersion): Boolean {
        return with(prePluginVersion) {
            preBuildVersionDao.update(
                version = version,
                modifyUser = modifyUser,
                desc = desc,
                pluginType = pluginType.name,
                dslContext = dslContext
            ) > 0
        }
    }

    fun getPluginVersionList(): List<PrePluginVersion> {
        val records = preBuildVersionDao.list(dslContext) ?: return listOf()
        return records.map {
            PrePluginVersion(
                version = it.version,
                desc = it.desc,
                modifyUser = it.modifyUser,
                updateTime = DateTimeUtil.toDateTime(it.updateTime as LocalDateTime),
                pluginType = PreBuildPluginType.valueOf(it.pluginType)
            )
        }
    }
}
