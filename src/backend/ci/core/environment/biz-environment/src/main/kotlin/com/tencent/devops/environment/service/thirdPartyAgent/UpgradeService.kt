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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.environment.agent.AgentUpgradeType
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentUpgradeByVersionInfo
import com.tencent.devops.environment.service.thirdPartyAgent.upgrade.AgentPropsScope
import com.tencent.devops.environment.service.thirdPartyAgent.upgrade.AgentScope
import com.tencent.devops.environment.service.thirdPartyAgent.upgrade.ProjectScope
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ComplexMethod", "LongMethod")
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

        if (!checkProjectUpgrade(projectId)) {
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

        if (!checkProjectUpgrade(projectId)) {
            return AgentResult(
                AgentStatus.IMPORT_OK,
                UpgradeItem(agent = false, worker = false, jdk = false, dockerInitFile = false)
            )
        }

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

        val workerVersion = when {
            currentWorkerVersion.isBlank() -> {
                logger.warn("The current agent version is not exist")
                false
            }

            agentScope.checkLockUpgrade(agentId, AgentUpgradeType.WORKER) -> false
            agentScope.checkForceUpgrade(agentId, AgentUpgradeType.WORKER) -> true
            else -> canUpgrade && (info.workerVersion.isNullOrBlank() || (currentWorkerVersion != info.workerVersion))
        }

        val goAgentVersion = when {
            currentGoAgentVersion.isBlank() -> {
                logger.warn("The current agent master version is not exist")
                false
            }

            agentScope.checkLockUpgrade(agentId, AgentUpgradeType.GO_AGENT) -> false
            agentScope.checkForceUpgrade(agentId, AgentUpgradeType.GO_AGENT) -> true
            else -> canUpgrade &&
                    (info.goAgentVersion.isNullOrBlank() || (currentGoAgentVersion != info.goAgentVersion))
        }

        val jdkVersion = when {
            currentJdkVersion.isNullOrBlank() -> {
                logger.warn("project: $projectId|agent: $agentId|os: $os|arch: ${props?.arch}|current jdk is null")
                false
            }

            agentScope.checkLockUpgrade(agentId, AgentUpgradeType.JDK) -> false
            agentScope.checkForceUpgrade(agentId, AgentUpgradeType.JDK) -> true
            else -> canUpgrade &&
                    (info.jdkVersion.isNullOrEmpty() ||
                            ((info.jdkVersion?.size ?: 0) > 2 &&
                                    currentJdkVersion.trim() != info.jdkVersion?.get(2)?.trim()))
        }

        val dockerInitFile = when {
            info.dockerInitFileInfo == null -> false
            // 目前存在非linux系统的不支持，旧数据或agent不使用docker构建机，所以不校验升级
            info.dockerInitFileInfo?.needUpgrade != true -> false
            currentDockerInitFileMd5.isBlank() -> {
                logger.warn(
                    "project: $projectId|agent: $agentId|os: $os|arch: ${props?.arch}| docker init md5 is null"
                )
                false
            }

            agentScope.checkLockUpgrade(agentId, AgentUpgradeType.DOCKER_INIT_FILE) -> false
            agentScope.checkForceUpgrade(agentId, AgentUpgradeType.DOCKER_INIT_FILE) -> true
            else -> canUpgrade && info.dockerInitFileInfo?.fileMd5 != currentDockerInitFileMd5
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
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId)
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
    private fun checkProjectUpgrade(projectId: String): Boolean {
        // 校验不升级项目，这些项目不参与Agent升级
        if (projectScope.checkDenyUpgradeProject(projectId)) {
            return false
        }

        // 校验是否在优先升级的项目列表中，如果不在里面并且优先升级项目的列表为空也允许Agent升级。
        return projectScope.checkInPriorityUpgradeProjectOrEmpty(projectId)
    }
}
