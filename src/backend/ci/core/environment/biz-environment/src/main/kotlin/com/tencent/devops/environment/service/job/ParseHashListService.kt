package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.constant.T_ENV_ENV_ID
import com.tencent.devops.environment.constant.T_NODE_CLOUD_AREA_ID
import com.tencent.devops.environment.constant.T_NODE_HOST_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_ID
import com.tencent.devops.environment.constant.T_NODE_NODE_IP
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.job.req.ExecuteTarget
import com.tencent.devops.environment.pojo.job.req.Host
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("ParseHashListService")
class ParseHashListService @Autowired constructor(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao
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
        return if (!hostListByHostId.isNullOrEmpty()) {
            listOf(hostListByHostId, hostListFromEnvHash, hostListFromNodeHash).flatten()
        } else if (!hostListByIp.isNullOrEmpty()) {
            listOf(hostListByIp, hostListFromEnvHash, hostListFromNodeHash).flatten()
        } else {
            listOf(hostListFromEnvHash, hostListFromNodeHash).flatten()
        }
    }

    fun getHostFromEnvList(projectId: String, envHashIdList: List<String>?/*环境hashId列表*/): List<Host>? {
        return envHashIdList.takeIf { !it.isNullOrEmpty() }.run {
            val envRecord = nodeDao.getEnvsByEnvHashIdList(
                dslContext, projectId, envHashIdList!!
            )
            val envIdList = envRecord.map { it[T_ENV_ENV_ID] as Long }

            val envNodeRecord = nodeDao.getNodeIdsByEnvIdList(
                dslContext, projectId, envIdList
            )
            val nodeIdList = envNodeRecord.map { it[T_NODE_NODE_ID] as Long }

            val nodeRecord = nodeDao.getNodesByNodeIdList(
                dslContext, projectId, nodeIdList
            )
            val nodeHostList = nodeRecord.map {
                Host(
                    bkHostId = it[T_NODE_HOST_ID] as? Long,
                    bkCloudId = it[T_NODE_CLOUD_AREA_ID] as Long,
                    ip = it[T_NODE_NODE_IP] as String
                )
            }
            if (logger.isDebugEnabled) logger.debug("[getHostFromEnvList] nodeHostList: $nodeHostList")
            nodeHostList
        }
    }

    fun getHostFromNodeList(projectId: String, nodeHashIdList: List<String>?/*节点hashId列表*/): List<Host>? {
        return nodeHashIdList.takeIf { !it.isNullOrEmpty() }.run {
            val nodeRecord = nodeDao.getNodesByNodeHashIdList(
                dslContext, projectId, nodeHashIdList!!
            )
            if (logger.isDebugEnabled) logger.debug("[getHostFromNodeList] nodeRecord: $nodeRecord")
            val hostList = nodeRecord.map {
                Host(
                    bkHostId = it[T_NODE_HOST_ID] as? Long,
                    bkCloudId = it[T_NODE_CLOUD_AREA_ID] as Long,
                    ip = it[T_NODE_NODE_IP] as String
                )
            }
            if (logger.isDebugEnabled) logger.debug("[getHostFromNodeList] hostList: $hostList")
            hostList
        }
    }
}