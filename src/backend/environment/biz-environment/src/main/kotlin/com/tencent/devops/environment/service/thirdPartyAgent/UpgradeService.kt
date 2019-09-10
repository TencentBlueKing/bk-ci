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

package com.tencent.devops.environment.service.thirdPartyAgent

import com.tencent.devops.common.api.enums.AgentStatus
import com.tencent.devops.common.api.pojo.AgentResult
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.SecurityUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.environment.dao.thirdPartyAgent.ThirdPartyAgentDao
import com.tencent.devops.environment.utils.FileMD5CacheUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Service
class UpgradeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val thirdPartyAgentDao: ThirdPartyAgentDao,
    private val redisOperation: RedisOperation,
    private val downloadAgentInstallService: DownloadAgentInstallService,
    private val gray: Gray
) {
    companion object {
        private val logger = LoggerFactory.getLogger(UpgradeService::class.java)
        private const val CURRENT_AGENT_MASTER_VERSION = "environment.thirdparty.agent.master.version"
        private const val GRAY_CURRENT_AGENT_MASTERT_VERSION = "environment.thirdparty.agent.gray.master.version"

        private const val CURRENT_AGENT_VERSION = "environment.thirdparty.agent.verison"
        private const val GRAY_CURRENT_AGENT_VERSION = "environment.thirdparty.agent.gray.version"

        private const val PARALLEL_UPGRADE_COUNT = "environment.thirdparty.agent.parallel.upgrade.count"
        private const val DEFAULT_PARALLEL_UPGRADE_COUNT = 50
    }

    fun setWorkerVersion(agentVersion: String) =
        redisOperation.set(getAgentVersionKey(), agentVersion, TimeUnit.DAYS.toSeconds(120))

    fun setMasterVersion(masterVersion: String) =
        redisOperation.set(getAgentMasterVersionKey(), masterVersion, TimeUnit.DAYS.toSeconds(120))

    fun getAgentVersion() = redisOperation.get(getAgentVersionKey()) ?: "null"

    fun getAgentMasterVersion() = redisOperation.get(getAgentMasterVersionKey()) ?: "null"

    fun checkUpgrade(
        projectId: String,
        agentId: String,
        secretKey: String,
        agentVersion: String?,
        masterVersion: String?
    ): AgentResult<Boolean> {
        logger.info(
            "Checking if the agent($agentId) of masterVersion($masterVersion) |" +
                " version($agentVersion) of project($projectId) can upgrade"
        )

        val status = checkAgent(projectId, agentId, secretKey)

        if (status != AgentStatus.IMPORT_OK) {
            logger.warn("The agent($agentId) status($status) is not OK")
            return AgentResult(status, false)
        }

        val currentVersion = redisOperation.get(getAgentVersionKey())
        if (currentVersion.isNullOrBlank()) {
            logger.warn("The current agent($agentId) version is not exist in redis")
            return AgentResult(AgentStatus.IMPORT_OK, false)
        }
        val currentMasterVersion = redisOperation.get(getAgentMasterVersionKey())
        if (currentMasterVersion.isNullOrBlank()) {
            logger.warn("The current agent($agentId) master version is not exist in redis")
            return AgentResult(AgentStatus.IMPORT_OK, false)
        }

        val agentNeedUpgrade = when {
            agentVersion.isNullOrBlank() -> true
            masterVersion.isNullOrBlank() -> (currentVersion != agentVersion)
            else -> (currentVersion != agentVersion) || (currentMasterVersion != masterVersion)
        }
        // 此处未控制并发
        if (agentNeedUpgrade) {
            logger.info(
                "The agent($agentId) can upgrade from $masterVersion|$agentVersion " +
                    "to $currentMasterVersion|$currentVersion"
            )
        }
        return AgentResult(AgentStatus.IMPORT_OK, agentNeedUpgrade)
    }

    fun downloadUpgradeFile(
        projectId: String,
        agentId: String,
        secretKey: String,
        file: String,
        md5: String?
    ): Response {
        logger.info("Trying the download file($file - $md5) of agent($agentId)")
        val status = checkAgent(projectId, agentId, secretKey)
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
                if (existMD5 != md5) {
                    logger.info("The current upgrade md5($md5) of agent($agentId) is not match to the server one($existMD5)")
                } else {
                    logger.info("The current upgrade of agent($agentId) is match the one in customer device")
                    modify = false
                }
            }
        }
        return if (modify) {
            Response.ok(StreamingOutput { output ->
                output.write(upgradeFile.readBytes())
                output.flush()
            }, MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .header("content-disposition", "attachment; filename = $fileName")
                .header("X-Checksum-Md5", existMD5)
                .build()
        } else {
            Response.status(Response.Status.NOT_MODIFIED).build()
        }
    }

    fun setMaxParallelUpgradeCount(count: Int) {
        redisOperation.set(PARALLEL_UPGRADE_COUNT, count.toString())
    }

    fun getMaxParallelUpgradeCount(): Int {
        return redisOperation.get(PARALLEL_UPGRADE_COUNT)?.toInt() ?: DEFAULT_PARALLEL_UPGRADE_COUNT
    }

    private fun checkAgent(
        projectId: String,
        agentId: String,
        secretKey: String
    ): AgentStatus {
        val id = HashUtil.decodeIdToLong(agentId)
        val agentRecord = thirdPartyAgentDao.getAgent(dslContext, id, projectId)
            ?: return AgentStatus.DELETE

        val key = SecurityUtil.decrypt(agentRecord.secretKey)
        if (key != secretKey) {
            logger.warn("The agent($agentId)($id) of project($projectId)'s secret($secretKey) is not match the expect one($key)")
            return AgentStatus.DELETE
        }

        return AgentStatus.fromStatus(agentRecord.status)
    }

    private fun getUpgradeFile(file: String) = downloadAgentInstallService.getUpgradeFile(file)

    private fun getAgentMasterVersionKey(): String {
        return if (gray.isGray()) {
            GRAY_CURRENT_AGENT_MASTERT_VERSION
        } else {
            CURRENT_AGENT_MASTER_VERSION
        }
    }

    private fun getAgentVersionKey() =
        if (gray.isGray()) {
            GRAY_CURRENT_AGENT_VERSION
        } else {
            CURRENT_AGENT_VERSION
        }
}