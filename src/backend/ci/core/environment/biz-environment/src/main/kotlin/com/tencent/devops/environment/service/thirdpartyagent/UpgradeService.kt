/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.environment.pojo.AgentUpgradeType
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.pojo.thirdpartyagent.ThirdPartyAgentUpgradeByVersionInfo
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.AgentPropsScope
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.AgentScope
import com.tencent.devops.environment.service.thirdpartyagent.upgrade.ProjectScope
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ComplexMethod", "LongMethod", "ReturnCount")
@Service
class UpgradeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val agentPropsScope: AgentPropsScope,
    private val agentScope: AgentScope,
    private val projectScope: ProjectScope
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UpgradeService::class.java)
    }

    fun checkUpgrade(
        projectId: String,
        agentId: String,
        secretKey: String,
        agentVersion: String?,
        masterVersion: String?
    ): AgentResult<Boolean> {
        val (status, _) = checkAgent(projectId, agentId, secretKey)
        if (status != AgentStatus.IMPORT_OK) {
            logger.warn("The agent($agentId) status($status) is not OK")
            return AgentResult(status, false)
        }

        if (!checkProjectUpgrade(projectId, AgentUpgradeType.WORKER) &&
            !checkProjectUpgrade(projectId, AgentUpgradeType.GO_AGENT)
        ) {
            return AgentResult(status = AgentStatus.IMPORT_OK, data = false)
        }

        val currentVersion = agentPropsScope.getWorkerVersion()
        if (currentVersion.isBlank()) {
            logger.warn("The current agent version is not exist")
            return AgentResult(AgentStatus.IMPORT_OK, false)
        }
        val currentMasterVersion = agentPropsScope.getAgentVersion()
        if (currentMasterVersion.isBlank()) {
            logger.warn("The current agent master version is not exist")
            return AgentResult(AgentStatus.IMPORT_OK, false)
        }

        val agentNeedUpgrade = when {
            agentVersion.isNullOrBlank() -> true
            masterVersion.isNullOrBlank() -> (currentVersion != agentVersion)
            else -> (currentVersion != agentVersion) || (currentMasterVersion != masterVersion)
        }

        val upgrade = when {
            agentScope.checkLockUpgrade(agentId, null) -> false
            agentScope.checkForceUpgrade(agentId, null) -> true
            else -> agentNeedUpgrade && agentScope.checkCanUpgrade(agentId)
        }

        return AgentResult(AgentStatus.IMPORT_OK, upgrade)
    }

    fun checkUpgradeNew(
        projectId: String,
        agentId: String,
        secretKey: String,
        info: ThirdPartyAgentUpgradeByVersionInfo
    ): AgentResult<UpgradeItem> {
        val (status, props, os) = checkAgent(projectId, agentId, secretKey)
        if (status != AgentStatus.IMPORT_OK) {
            logger.warn("The agent($agentId) status($status) is not OK")
            return AgentResult(
                status = status,
                data = UpgradeItem(
                    agent = false,
                    worker = false,
                    jdk = false,
                    dockerInitFile = false
                )
            )
        }
        return checkUpgradeNew(projectId, agentId, props, os, info)
    }

    // 抽出来方便写单测
    fun checkUpgradeNew(
        projectId: String,
        agentId: String,
        props: AgentProps?,
        os: String?,
        info: ThirdPartyAgentUpgradeByVersionInfo
    ): AgentResult<UpgradeItem> {
        val currentWorkerVersion = agentPropsScope.getWorkerVersion()
        val currentGoAgentVersion = agentPropsScope.getAgentVersion()
        val currentJdkVersion = agentPropsScope.getJdkVersion(os, props?.arch)
        val currentDockerInitFileMd5 = agentPropsScope.getDockerInitFileMd5()

        val canUpgrade = agentScope.checkCanUpgrade(agentId)

        if (logger.isDebugEnabled) {
            logger.debug(
                "$projectId|$agentId|canUpgrade=$canUpgrade" +
                        "|currentWorkerVersion=$currentWorkerVersion,agent_WorkerVersion=${info.workerVersion}" +
                        "|currentGoAgentVersion=$currentGoAgentVersion,agent_GoAgentVersion=${info.goAgentVersion}" +
                        "|currentJdkVersion=$currentJdkVersion,agent_JdkVersion=${info.jdkVersion}" +
                        "|currentDockerInitFileMd5=$currentDockerInitFileMd5," +
                        "agent_DockerInitFileMd5=${info.dockerInitFileInfo?.fileMd5}"
            )
        }

        /**
         * Agent组件的升级规则
         * 1、判断组件版本信息是否配置，未配置不升级
         * 2、判断组件的强制升级或者锁定升级，锁定升级不升级，强制升级判断版本信息是否一致看是否升级
         * 3、判断组件是否符合项目升级标准，不符合则不升级
         * 4、正常判断组件是否可以升级
         */

        val workerCheckFun = fun() = currentWorkerVersion != info.workerVersion
        val workerVersion = when {
            currentWorkerVersion.isBlank() -> false
            agentScope.checkLockUpgrade(agentId, AgentUpgradeType.WORKER) -> false
            agentScope.checkForceUpgrade(agentId, AgentUpgradeType.WORKER) && workerCheckFun() -> true
            !checkProjectUpgrade(projectId, AgentUpgradeType.WORKER) -> false
            else -> canUpgrade && workerCheckFun()
        }

        // 除了worker之外的组件都和agent走一个项目检查
        val agentProjectCheck = checkProjectUpgrade(projectId, AgentUpgradeType.GO_AGENT)

        val goAgentCheckFun = fun() = currentGoAgentVersion != info.goAgentVersion
        val goAgentVersion = when {
            currentGoAgentVersion.isBlank() -> false
            agentScope.checkLockUpgrade(agentId, AgentUpgradeType.GO_AGENT) -> false
            agentScope.checkForceUpgrade(agentId, AgentUpgradeType.GO_AGENT) && goAgentCheckFun() -> true
            !agentProjectCheck -> false
            else -> canUpgrade && goAgentCheckFun()
        }

        val jdkCheckFun = fun() = info.jdkVersion.isNullOrEmpty() ||
                ((info.jdkVersion?.size ?: 0) > 2 && currentJdkVersion?.trim() != info.jdkVersion?.get(2)?.trim())
        val jdkVersion = when {
            currentJdkVersion.isNullOrBlank() -> false
            agentScope.checkLockUpgrade(agentId, AgentUpgradeType.JDK) -> false
            agentScope.checkForceUpgrade(agentId, AgentUpgradeType.JDK) && jdkCheckFun() -> true
            !agentProjectCheck -> false
            else -> canUpgrade && jdkCheckFun()
        }

        val dockerInitFileCheckFun = fun() = info.dockerInitFileInfo?.fileMd5 != currentDockerInitFileMd5
        val dockerInitFile = when {
            currentDockerInitFileMd5.isBlank() -> false
            // 目前存在非linux系统的不支持，旧数据或agent不使用docker构建机，所以不校验升级
            info.dockerInitFileInfo == null -> false
            info.dockerInitFileInfo?.needUpgrade != true -> false
            agentScope.checkLockUpgrade(agentId, AgentUpgradeType.DOCKER_INIT_FILE) -> false
            agentScope.checkForceUpgrade(agentId, AgentUpgradeType.DOCKER_INIT_FILE) && dockerInitFileCheckFun() -> true
            !agentProjectCheck -> false
            else -> canUpgrade && dockerInitFileCheckFun()
        }

        return AgentResult(
            status = AgentStatus.IMPORT_OK,
            data = UpgradeItem(
                agent = goAgentVersion,
                worker = workerVersion,
                jdk = jdkVersion,
                dockerInitFile = dockerInitFile
            )
        )
    }

    private fun checkAgent(
        projectId: String,
        agentId: String,
        secretKey: String
    ): Triple<AgentStatus, AgentProps?, String?> {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id)
            ?: return Triple(AgentStatus.DELETE, null, null)

        val key = SecurityUtil.decrypt(agentRecord.secretKey)
        if (key != secretKey) {
            return Triple(AgentStatus.DELETE, null, null)
        }

        val props = agentPropsScope.parseAgentProps(agentRecord.agentProps)

        return Triple(AgentStatus.fromStatus(agentRecord.status), props, agentRecord.os)
    }

    /**
     * 校验这个agent所属的项目是否可以进行升级或者其他属性
     * @return true 可以升级 false 不能进行升级
     */
    private fun checkProjectUpgrade(projectId: String, type: AgentUpgradeType?): Boolean {
        // 校验不升级项目，这些项目不参与Agent升级
        if (projectScope.checkDenyUpgradeProject(projectId, type)) {
            return false
        }

        // 校验是否在优先升级的项目列表中，如果不在里面并且优先升级项目的列表为空也允许Agent升级。
        return projectScope.checkInPriorityUpgradeProjectOrEmpty(projectId, type)
    }
}
