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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.agent.AgentArchType
import com.tencent.devops.common.api.pojo.agent.UpgradeItem
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.environment.agent.AgentGrayUtils
import com.tencent.devops.common.environment.agent.AgentUpgradeType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.model.AgentProps
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentUpgradeByVersionInfo
import com.tencent.devops.environment.utils.FileMD5CacheUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Suppress("ComplexMethod", "LongMethod")
@Service
class UpgradeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val redisOperation: RedisOperation,
    private val downloadAgentInstallService: DownloadAgentInstallService,
    private val agentGrayUtils: AgentGrayUtils,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UpgradeService::class.java)
    }

    private val stringCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build<String, String>()

    fun getRedisValueWithCache(redisKey: String): String {
        var value = stringCache.getIfPresent(redisKey)
        if (value != null) {
            return value
        }
        synchronized(this) {
            value = stringCache.getIfPresent(redisKey)
            if (value != null) {
                return value!!
            }
            logger.info("refresh $redisKey from redis")
            value = redisOperation.get(key = redisKey, isDistinguishCluster = true) ?: ""
            stringCache.put(redisKey, value ?: "")
        }
        return value!!
    }

    fun setUpgrade(agentVersion: String) =
        redisOperation.set(
            key = agentGrayUtils.getAgentVersionKey(),
            value = agentVersion,
            expired = false,
            isDistinguishCluster = true
        )

    fun setMasterVersion(masterVersion: String) =
        redisOperation.set(
            key = agentGrayUtils.getAgentMasterVersionKey(),
            value = masterVersion,
            expired = false,
            isDistinguishCluster = true
        )

    fun getWorkerVersion() = getRedisValueWithCache(agentGrayUtils.getAgentVersionKey())

    fun getAgentVersion() = getRedisValueWithCache(agentGrayUtils.getAgentMasterVersionKey())

    fun getJdkVersion(os: String?, arch: String?): String? {
        if (os.isNullOrBlank()) {
            return null
        }

        val osE = when (os) {
            OS.WINDOWS.name -> return getRedisValueWithCache(
                //  win目前只有一个架构随便填即可
                agentGrayUtils.getJdkVersionKey(OS.WINDOWS, AgentArchType.AMD64)
            )

            OS.MACOS.name -> OS.MACOS
            else -> OS.LINUX
        }
        // 这里的arch需要和go的编译脚本中的GOARCH统一，因为上报是根据go runtime上报的
        return when (arch) {
            AgentArchType.ARM64.arch -> getRedisValueWithCache(
                agentGrayUtils.getJdkVersionKey(osE, AgentArchType.ARM64)
            )

            AgentArchType.MIPS64.arch -> getRedisValueWithCache(
                agentGrayUtils.getJdkVersionKey(osE, AgentArchType.MIPS64)
            )

            AgentArchType.AMD64.arch -> getRedisValueWithCache(
                agentGrayUtils.getJdkVersionKey(osE, AgentArchType.AMD64)
            )

            else -> null
        }
    }

    fun getDockerInitFileMd5(): String {
        // 目前仅支持linux且amd和arm脚本上没有区别
        return getRedisValueWithCache(agentGrayUtils.getDockerInitFileMd5Key())
    }

    fun getGatewayMapping(): Map<String, String> {
        val mappingConfig = getRedisValueWithCache("environment.thirdparty.gateway.mapping")
        return objectMapper.readValue(mappingConfig)
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

        val currentVersion = getWorkerVersion()
        if (currentVersion.isBlank()) {
            logger.warn("The current agent version is not exist")
            return AgentResult(AgentStatus.IMPORT_OK, false)
        }
        val currentMasterVersion = getAgentVersion()
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
            agentGrayUtils.checkLockUpgrade(agentId, null) -> false
            agentGrayUtils.checkForceUpgrade(agentId, null) -> true
            else -> agentNeedUpgrade && agentGrayUtils.getCanUpgradeAgents().contains(HashUtil.decodeIdToLong(agentId))
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
                status, UpgradeItem(
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

        val currentWorkerVersion = getWorkerVersion()
        val currentGoAgentVersion = getAgentVersion()
        val currentJdkVersion = getJdkVersion(os, props?.arch)
        val currentDockerInitFileMd5 = getDockerInitFileMd5()

        val canUpgrade = agentGrayUtils.getCanUpgradeAgents().contains(HashUtil.decodeIdToLong(agentId))

        val workerVersion = when {
            currentWorkerVersion.isBlank() -> {
                logger.warn("The current agent version is not exist")
                false
            }

            agentGrayUtils.checkLockUpgrade(agentId, AgentUpgradeType.WORKER) -> false
            agentGrayUtils.checkForceUpgrade(agentId, AgentUpgradeType.WORKER) -> true
            else -> canUpgrade && (info.workerVersion.isNullOrBlank() || (currentWorkerVersion != info.workerVersion))
        }

        val goAgentVersion = when {
            currentGoAgentVersion.isBlank() -> {
                logger.warn("The current agent master version is not exist")
                false
            }

            agentGrayUtils.checkLockUpgrade(agentId, AgentUpgradeType.GO_AGENT) -> false
            agentGrayUtils.checkForceUpgrade(agentId, AgentUpgradeType.GO_AGENT) -> true
            else -> canUpgrade &&
                    (info.goAgentVersion.isNullOrBlank() || (currentGoAgentVersion != info.goAgentVersion))
        }

        val jdkVersion = when {
            currentJdkVersion.isNullOrBlank() -> {
                logger.warn("project: $projectId|agent: $agentId|os: $os|arch: ${props?.arch}|current jdk is null")
                false
            }

            agentGrayUtils.checkLockUpgrade(agentId, AgentUpgradeType.JDK) -> false
            agentGrayUtils.checkForceUpgrade(agentId, AgentUpgradeType.JDK) -> true
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
                    "project: $projectId|agent: $agentId|os: $os|arch: ${props?.arch}|current docker init md5 is null"
                )
                false
            }

            agentGrayUtils.checkLockUpgrade(agentId, AgentUpgradeType.DOCKER_INIT_FILE) -> false
            agentGrayUtils.checkForceUpgrade(agentId, AgentUpgradeType.DOCKER_INIT_FILE) -> true
            else -> canUpgrade && info.dockerInitFileInfo?.fileMd5 != currentDockerInitFileMd5
        }

        return AgentResult(
            AgentStatus.IMPORT_OK, UpgradeItem(
                agent = goAgentVersion,
                worker = workerVersion,
                jdk = jdkVersion,
                dockerInitFile = dockerInitFile
            )
        )
    }

    fun downloadUpgradeFile(
        projectId: String,
        agentId: String,
        secretKey: String,
        file: String,
        md5: String?
    ): Response {
        val (status, _) = checkAgent(projectId, agentId, secretKey)
        if (status == AgentStatus.DELETE) {
            logger.warn("The agent($agentId)'s status($status) is DELETE")
            return Response.status(Response.Status.NOT_FOUND).build()
        }

        val upgradeFile = getUpgradeFile(file)
        val fileName = upgradeFile.name

        var modify = true
        val existMD5 = FileMD5CacheUtils.getFileMD5(upgradeFile)
        if (!upgradeFile.exists()) {
            logger.warn("The upgrade of agent($agentId) file(${upgradeFile.absolutePath}) is not exist")
            modify = false
        } else {
            if (md5 != null) {
                if (existMD5 == md5) {
                    modify = false
                }
            }
        }
        return if (modify) {
            logger.info("upgrade file($file) changed, server file md5: $existMD5")
            Response.ok(upgradeFile.inputStream(), MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("content-disposition", "attachment; filename = $fileName")
                .header("X-Checksum-Md5", existMD5)
                .build()
        } else {
            Response.status(Response.Status.NOT_MODIFIED).build()
        }
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

        val props = parseAgentProps(agentRecord.agentProps)

        return Triple(AgentStatus.fromStatus(agentRecord.status), props, agentRecord.os)
    }

    fun parseAgentProps(props: String?): AgentProps? {
        return if (props.isNullOrBlank()) {
            null
        } else {
            try {
                JsonUtil.to(props, AgentProps::class.java)
            } catch (e: Exception) {
                // 兼容老数据格式不对的情况
                null
            }
        }
    }

    /**
     * 校验这个agent所属的项目是否可以进行升级或者其他属性
     * @return true 可以升级 false 不能进行升级
     */
    private fun checkProjectUpgrade(
        projectId: String
    ): Boolean {
        // 校验不升级项目，这些项目不参与Agent升级
        if (projectId in agentGrayUtils.getNotUpgradeProjects()) {
            return false
        }

        // 校验优先升级，这些项目在升级时，其他项目不能进行升级
        val priorityProjects = agentGrayUtils.getPriorityUpgradeProjects()
        if (priorityProjects.isEmpty()) {
            return true
        }

        return projectId in priorityProjects
    }

    private fun getUpgradeFile(file: String) = downloadAgentInstallService.getUpgradeFile(file)
}
