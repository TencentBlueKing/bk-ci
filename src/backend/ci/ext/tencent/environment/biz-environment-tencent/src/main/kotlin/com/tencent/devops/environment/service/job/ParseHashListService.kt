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

package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.constant.T_ENV_ENV_ID
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.dao.job.CmdbEnvDao
import com.tencent.devops.environment.dao.job.CmdbEnvNodeDao
import com.tencent.devops.environment.dao.job.CmdbNodeDao
import com.tencent.devops.environment.pojo.job.jobreq.ExecuteTarget
import com.tencent.devops.environment.pojo.job.jobreq.Host
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("ParseHashListService")
class ParseHashListService @Autowired constructor(
    private val dslContext: DSLContext,
    private val cmdbNodeDao: CmdbNodeDao,
    private val cmdbEnvDao: CmdbEnvDao,
    private val cmdbEnvNodeDao: CmdbEnvNodeDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ParseHashListService::class.java)
    }

    fun getAllHostList(projectId: String, executeTarget: ExecuteTarget): List<Host> {
        val hostListFromEnvHash: List<Host> = getHostFromEnvList(projectId, executeTarget.envHashIdList) ?: emptyList()
        val hostListFromNodeHash: List<Host> = getHostFromNodeList(
            projectId, executeTarget.nodeHashIdList
        ) ?: emptyList()
        val hostListByHostId: List<Host>? = executeTarget.hostIdList?.map { Host(bkHostId = it) }
        val hostListByIp: List<Host>? = executeTarget.ipList?.map { Host(bkCloudId = it.bkCloudId, ip = it.ip) }
        val allHostList = if (!hostListByHostId.isNullOrEmpty()) {
            listOf(hostListByHostId, hostListFromEnvHash, hostListFromNodeHash).flatten()
        } else if (!hostListByIp.isNullOrEmpty()) {
            listOf(hostListByIp, hostListFromEnvHash, hostListFromNodeHash).flatten()
        } else {
            listOf(hostListFromEnvHash, hostListFromNodeHash).flatten()
        }
        logger.info("All Host List: ${allHostList.joinToString(separator = ", ", transform = { it.toString() })}")
        return allHostList
    }

    fun getHostFromEnvList(projectId: String, envHashIdList: List<String>?/*环境hashId列表*/): List<Host>? {
        return if (!envHashIdList.isNullOrEmpty()) {
            val envRecord = cmdbEnvDao.getEnvsByEnvHashIdList(
                dslContext, projectId, envHashIdList
            )
            val envIdList = envRecord.map { it[T_ENV_ENV_ID] as Long }

            val envNodeRecord = cmdbEnvNodeDao.getNodeIdsByEnvIdList(
                dslContext, projectId, envIdList
            )
            val nodeIdList = envNodeRecord.map { it[T_NODE_NODE_ID] as Long }

            val nodeRecord = cmdbNodeDao.getNodesByNodeIdList(
                dslContext, projectId, nodeIdList
            )
            val nodeHostList = nodeRecord.map {
                Host(
                    bkHostId = it[T_NODE_HOST_ID] as? Long,
                    bkCloudId = it[T_NODE_CLOUD_AREA_ID] as? Long,
                    ip = it[T_NODE_NODE_IP] as String
                )
            }
            if (logger.isDebugEnabled)
                logger.debug(
                    "Host from env list: ${nodeHostList.joinToString(separator = ", ", transform = { it.toString() })}"
                )
            nodeHostList
        } else null
    }

    fun getHostFromNodeList(projectId: String, nodeHashIdList: List<String>?/*节点hashId列表*/): List<Host>? {
        return if (!nodeHashIdList.isNullOrEmpty()) {
            val nodeRecord = cmdbNodeDao.getNodesByNodeHashIdList(
                dslContext, projectId, nodeHashIdList
            )
            if (logger.isDebugEnabled) logger.debug("[getHostFromNodeList] nodeRecord: $nodeRecord")
            val hostList = nodeRecord.map {
                val hostId = it[T_NODE_HOST_ID] as? Long
                val bkCloudId = it[T_NODE_CLOUD_AREA_ID] as? Long
                val ip = it[T_NODE_NODE_IP] as String
                if (logger.isDebugEnabled)
                    logger.debug("[getHostFromNodeList]hostId:$hostId, bkCloudId:$bkCloudId, ip:$ip")
                Host(
                    bkHostId = hostId,
                    bkCloudId = bkCloudId,
                    ip = ip
                )
            }
            if (logger.isDebugEnabled)
                logger.debug(
                    "Host from node list: ${hostList.joinToString(separator = ", ", transform = { it.toString() })}"
                )
            hostList
        } else null
    }
}