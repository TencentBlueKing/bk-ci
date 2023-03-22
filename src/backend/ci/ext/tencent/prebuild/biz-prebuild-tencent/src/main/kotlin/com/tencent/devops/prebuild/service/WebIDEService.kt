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

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.api.thirdPartyAgent.ServicePreBuildAgentResource
import com.tencent.devops.environment.api.thirdPartyAgent.ServiceThirdPartyAgentResource
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.PreBuildCode
import com.tencent.devops.prebuild.PreBuildCode.BK_MANUAL_TRIGGER
import com.tencent.devops.prebuild.PreBuildCode.BK_TBUILD_ENVIRONMENT_LINUX
import com.tencent.devops.prebuild.dao.WebIDEOpenDirDao
import com.tencent.devops.prebuild.dao.WebIDEStatusDao
import com.tencent.devops.prebuild.pojo.DevcloudUserRes
import com.tencent.devops.prebuild.pojo.IDEInfo
import com.tencent.devops.prebuild.pojo.UserResItem
import com.tencent.devops.prebuild.pojo.ide.IdeDirInfo
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.user.UserPipelineResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.concurrent.Executors

@Service
class WebIDEService @Autowired constructor(
    private val dslContext: DSLContext,
    private val webIDEStatusDao: WebIDEStatusDao,
    private val webIDEOpenDirDao: WebIDEOpenDirDao,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WebIDEService::class.java)
        private val executorService = Executors.newFixedThreadPool(30)
    }

    private val logger = LoggerFactory.getLogger(javaClass)!!

    fun getUserProject(userId: String, accessToken: String): ProjectVO? {
        val projectInfo = client.get(ServiceTxProjectResource::class).getPreUserProject(userId, accessToken)
        return projectInfo!!.data
    }

    fun getUserIDEInfo(userId: String, projectId: String): List<IDEInfo> {
        // 1. get devcloud ip info
        val devcloudInfo = getUserDevCloudInfo(userId)

        // 2. get database info
        val ideList = mutableListOf<IDEInfo>()
        val infoList = webIDEStatusDao.get(dslContext, userId)
        infoList.forEach {
            if (devcloudInfo.containsKey(it.ip)) {
                devcloudInfo.remove(it.ip)
                val ideUrl = "http://dev.devgw.devops.oa.com/webide/$userId/${it.ip}/"
                val info = IDEInfo(it.ideStatus, it.agentStatus, it.ip, ideUrl, it.ideVersion, it.serverType, it.serverCreateTime)
                ideList.add(info)
            } else {
                webIDEStatusDao.del(dslContext, userId, it.ip)
                logger.info("delete user devcloud server, userID:%s, ip:%s", userId, it.ip)
            }
        }

        // 将新申请的服务器信息添加到数据库
        if (devcloudInfo.size > 0) {
            devcloudInfo.forEach {
                val date = SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(it.value.createdAt)
                val ideUrl = "http://dev.devgw.devops.oa.com/webide/$userId/${it.value.ip}/"
                val info = IDEInfo(0, 0, it.value.ip, ideUrl, "0", it.value.res_type, date.time)
                ideList.add(info)
                addNewInfo(userId, info)
            }
        }

        // 更新服务器agent的状态
        updateAgentStatus(userId, projectId, ideList)
        // 更新ide状态
        updateIdeStatus(ideList)
        // 按照服务器创建时间的降序排序（新的在前面）
        ideList.sortByDescending { it.serverCreateTime }
        return ideList
    }

    private fun updateIdeStatus(ideList: List<IDEInfo>) {
/*        ideList.forEach {
            logger.info("try testing url: ${it.ideURL}")
            val request = Request.Builder()
                    .url(it.ideURL)
                    .get()
                    .build()
            val client = OkHttpClient.Builder().build().newCall(request)
            val response = client.execute()
            val responseContent = response.body()!!.string()
            logger.info("response succ: ${response.isSuccessful}")
            logger.info("response code: ${response.code()}")
            logger.info("response: $responseContent")
        }*/
    }

    private fun updateAgentStatus(userID: String, projectID: String, ideList: List<IDEInfo>) {
        val nodeInfoList = client.get(ServiceNodeResource::class).listNodeByNodeType(projectID, NodeType.THIRDPARTY)
        if (nodeInfoList.isNotOk()) {
            logger.error("list user third party node failed")
            throw OperationException("list user third party node failed")
        }
        logger.info("try to get agent by userid:$userID, projectId:$projectID")

        val nodeInfo = nodeInfoList.data!!
        logger.info("nodeinfolist size:${nodeInfo.size}")
        nodeInfo.forEach {
            val serverInfo = ideList.find { s -> s.ip == it.ip }
            if (serverInfo != null) {
                if (it.agentStatus == true) {
                    serverInfo?.agentInstanceStatus = 1
                    logger.info("set agent status to 1, ${it.ip}")
                } else {
                    serverInfo?.agentInstanceStatus = 0
                    logger.info("set agent status to 0, ${it.ip}")
                }
            }
        }
    }

    private fun getAgentInfo(userId: String, projectId: String, ip: String): ThirdPartyAgentInfo {
        // val nodeInfoList = client.get(ServiceNodeResource::class).listNodeByNodeType(projectId, NodeType.THIRDPARTY)
        val nodeInfoList = client.get(ServiceThirdPartyAgentResource::class).listAgents(userId, projectId, OS.LINUX)
        if (nodeInfoList.isNotOk()) {
            logger.error("list user third party node failed")
            throw OperationException("list user third party node failed")
        }
        logger.info("try to get agent by userid:$userId, projectId:$projectId")
        val nodeInfo = nodeInfoList.data!!
        logger.info("nodeList size is ${nodeInfo.size}")
        val agentInfo = nodeInfo.find { it.ip == ip }
        if (agentInfo == null) {
            logger.error("can not find specific agent by projectId:$projectId and ip:$ip")
            throw OperationException("can not find specific agent by projectId:$projectId and ip:$ip")
        }

        logger.info("succ get agent info by ip:$ip, nodeID:${agentInfo.agentId}")
        return agentInfo
    }

    private fun updateWebIDEStatus(userID: String, ideList: List<IDEInfo>) {
        ideList.forEach {
            val ideURL = "http://devops.oa.com/webide/$userID/${it.ip}"
        }
    }

    fun getAgentInstallLink(userId: String, projectId: String, operationSystem: String, zoneName: String?, initIp: String?): ThirdPartyAgentStaticInfo {
        val agent = client.get(ServicePreBuildAgentResource::class)
                .createPrebuildAgent(userId, projectId, OS.valueOf(operationSystem), zoneName, initIp)

        // client.get(UserThirdPartyAgentResource::class).generateLink(userId, projectId, OS.valueOf(operationSystem), zoneName)

        if (agent.isNotOk()) {
            logger.error("create agent link failed, userId:$userId, projectId:$projectId, opsys:$operationSystem, zoneName:$zoneName")
            throw OperationException("create agent link failed")
        }
        logger.info("create agent link success:${agent.data!!.link}")
        return agent.data!!
    }

    private fun getUserDevCloudInfo(userID: String): HashMap<String, UserResItem> {
        val url = "http://oss.esb.oa.com/devops-dev/devcloud/api/v1/resource/list?username=$userID&page=1&size=100"
        val infoMap = HashMap<String, UserResItem>()
        logger.info(url)
        val request = Request.Builder()
                .url(url)
                .headers(makeDevCloudAPIHeaders("10004", "Eeav59x*xFki46B0").toHeaders())
                .get()
                .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body!!.string()
            if (!response.isSuccessful) {
                logger.info("response code: ${response.code}")
                logger.info("response: $responseContent")
                throw RuntimeException("Fail to start docker")
            }
            logger.info("devcloud content: $responseContent")
            val devCloudUserRes = jacksonObjectMapper().readValue<DevcloudUserRes>(responseContent)
            if (devCloudUserRes.actionCode == 200) {
                devCloudUserRes.data.items.forEach {
                    infoMap[it.ip] = it
                    logger.info("Get from devcloud.oa.com, ip:${it.ip}")
                }
            }
        }
        return infoMap
    }

    private fun makeDevCloudAPIHeaders(appId: String, token: String): Map<String, String> {
        val headerBuilder = mutableMapOf<String, String>()
        headerBuilder["APPID"] = appId
        val random = RandomStringUtils.randomAlphabetic(8)
        headerBuilder["RANDOM"] = random
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        headerBuilder["TIMESTP"] = timestamp
        val encKey = DigestUtils.md5Hex("$token$timestamp$random")
        headerBuilder["ENCKEY"] = encKey
        headerBuilder["TIMESTAMP"] = timestamp
        return headerBuilder
    }

    private fun addNewInfo(userId: String, newItem: IDEInfo) {
        webIDEStatusDao.create(dslContext,
                userId,
                newItem.ip,
                newItem.serverType,
                newItem.agentInstanceStatus,
                newItem.ideInstanceStatus,
                newItem.ideVersion,
                newItem.serverCreateTime,
                "")
    }

    fun setupAgent(userId: String, projectId: String, ip: String): BuildId {
        // 创建手动触发流水线
        val agentId = getAgentInfo(userId, projectId, ip).agentId
        var pipelineId = isPipelineExist(userId, projectId, ip)
        if (pipelineId == "") {
            pipelineId = createAgentPipeline(userId, projectId, agentId, ip)
            logger.info("pipeline does not exist, create succ: $pipelineId")
            webIDEStatusDao.updatePipelineId(dslContext, userId, ip, pipelineId)
        }
        val buildId = client.get(ServiceBuildResource::class).manualStartup(userId, projectId, pipelineId, mapOf(), ChannelCode.BS).data!!.id
        logger.info("succ create build, id:$buildId")
        return BuildId(buildId)
    }

    private fun isPipelineExist(userId: String, projectId: String, ip: String): String {
        val info = webIDEStatusDao.getByIp(dslContext, userId, ip)
        logger.info("get pipeline id ${info.pipelineId} by userId: $userId and ip: $ip")
        val exist = client.get(UserPipelineResource::class).pipelineExist(userId, projectId, "BKVSCode").data
        logger.info("pipeline ${info.pipelineId} exist: $exist")
        if (exist == true) {
            return info.pipelineId
        } else {
            return ""
        }
    }

    private fun makeScriptElement(userId: String, id: String): LinuxScriptElement {
        var sb = StringBuilder()
        sb.appendln("mkdir -p /bkvscode")
        sb.appendln("cd /bkvscode")
        sb.appendln("killall bkvscode")
        sb.appendln("sleep 2")
        sb.appendln("curl -o bkvscode.tgz 'http://dev.gw.devops.oa.com/webide/bkvscode/bkvscode.tgz'")
        sb.appendln("tar xzvf bkvscode.tgz")
        sb.appendln("BUILD_ID=dontKillMe nohup ./bkvscode --owner=$userId --id=$id --port=58998 --dev > myout.file 2>&1 & disown")

        val linuxScriptElement = LinuxScriptElement(
                name = "Script Task",
                id = "",
                status = "",
                scriptType = BuildScriptType.SHELL,
                script = sb.toString(),
                continueNoneZero = true
        )
        return linuxScriptElement
    }

    private fun createAgentPipeline(userId: String, projectId: String, agentId: String, agentIp: String): String {
        val stageList = mutableListOf<Stage>()

        val manualTriggerElement = ManualTriggerElement(
            MessageUtil.getMessageByLocale(
                messageCode = BK_MANUAL_TRIGGER,
                language = I18nUtil.getLanguage(userId)
            ), "T-1-1-1")
        val params: List<BuildFormProperty> = emptyList()
        val triggerContainer = TriggerContainer(
                "0",
            MessageUtil.getMessageByLocale(
                messageCode = PreBuildCode.BK_BUILD_TRIGGER,
                language = I18nUtil.getLanguage(userId)
            ),
                listOf(manualTriggerElement),
                null,
                null,
                null,
                null,
                params)
        val stage1 = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(stage1)

        val model = createPipelineModel(userId, projectId, agentId, agentIp)
        val pipeLineId = client.get(ServicePipelineResource::class).create(userId, projectId, model, ChannelCode.BS).data!!.id
        logger.info("pipelineId: $pipeLineId")
        return pipeLineId
    }

    private fun createPipelineModel(userId: String, projectId: String, agentId: String, agentIp: String): Model {
        val stageList = mutableListOf<Stage>()
        val elementList = mutableListOf<Element>()
        elementList.add(makeScriptElement(userId, agentIp))

        val manualTriggerElement = ManualTriggerElement(
            MessageUtil.getMessageByLocale(
                messageCode = BK_MANUAL_TRIGGER,
                language = I18nUtil.getLanguage(userId)
        ), "T-1-1-1")
        val triggerContainer = TriggerContainer("0",
            MessageUtil.getMessageByLocale(
                messageCode = PreBuildCode.BK_BUILD_TRIGGER,
                language = I18nUtil.getLanguage(userId)
            ), listOf(manualTriggerElement))
        val stage1 = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(stage1)

        val vmContainer = VMBuildContainer(
                id = "1",
                name = MessageUtil.getMessageByLocale(
                    messageCode = BK_TBUILD_ENVIRONMENT_LINUX,
                    language = I18nUtil.getLanguage(userId)
                ),
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
                thirdPartyAgentId = agentId,
                thirdPartyAgentEnvId = null,
                thirdPartyWorkspace = null,
                dockerBuildVersion = null,
                tstackAgentId = null,
                dispatchType = ThirdPartyAgentIDDispatchType(agentId, null, AgentType.ID, null)
        )
        val stage2 = Stage(listOf(vmContainer), "stage-2")
        stageList.add(stage2)
        return Model("BKVSCode", "", stageList, emptyList(), false, userId)
    }

    fun heartBeat(userId: String, ip: String): Boolean {
        webIDEStatusDao.updateIDEHeartBeat(dslContext, userId, ip)
        return true
    }

    fun lastOpenDir(userId: String, ip: String): IdeDirInfo {
        val openDirInfoRecord = webIDEOpenDirDao.get(dslContext, userId, ip)
        if (openDirInfoRecord == null) {
            return IdeDirInfo("", ip)
        } else {
            return IdeDirInfo(openDirInfoRecord!!.path, openDirInfoRecord!!.ip)
        }
    }

    fun updateLastOpenDir(userId: String, ip: String, path: String): Boolean {
        webIDEOpenDirDao.update(dslContext, userId, ip, path)
        return true
    }
}
