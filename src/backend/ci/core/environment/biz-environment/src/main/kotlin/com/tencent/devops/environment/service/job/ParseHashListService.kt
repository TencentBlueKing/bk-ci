package com.tencent.devops.environment.service.job

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
        val hostListFromEnvHash: List<Host> = getHostFromEnvList(projectId, executeTarget.envHashIdList)
        val hostListFromNodeHash: List<Host> = getHostFromNodeList(projectId, executeTarget.nodeHashIdList)
        return if (null != executeTarget.hostList) {
            listOf(
                executeTarget.hostList!!, hostListFromEnvHash, hostListFromNodeHash
            ).flatten()
        } else {
            listOf(hostListFromEnvHash, hostListFromNodeHash).flatten()
        }
    }

    fun getHostFromEnvList(projectId: String, envHashIdList: List<String>?/*环境hashId列表*/): List<Host> {
        if (!envHashIdList.isNullOrEmpty()) {
            val envRecord = nodeDao.getEnvsFromEnvHashList(
                dslContext, projectId, envHashIdList
            )
            val envIdList = envRecord.map { it.value1() }

            val envNodeRecord = nodeDao.getNodeIdsFromEnvIdList(
                dslContext, projectId, envIdList
            )
            val nodeIdList = envNodeRecord.map { it.value1() }

            val nodeRecord = nodeDao.getNodesFromNodeIdList(
                dslContext, projectId, nodeIdList
            )
            val nodeHostList = nodeRecord.map {
                Host(
                    bkHostId = it.value2(),
                    bkCloudId = it.value3(),
                    ip = it.value1()
                )
            }
            if (logger.isDebugEnabled) logger.debug("[getHostFromEnvList] nodeHostList: $nodeHostList")
            return nodeHostList
        } else {
            logger.warn("[getHostFromEnvList] envHashIdList is null or empty.")
            return emptyList()
        }
    }

    fun getHostFromNodeList(projectId: String, nodeHashIdList: List<String>?/*节点hashId列表*/): List<Host> {
        if (!nodeHashIdList.isNullOrEmpty()) {
            val nodeRecord = nodeDao.getNodesFromNodeHashList(
                dslContext, projectId, nodeHashIdList
            )
            if (logger.isDebugEnabled) logger.debug("[getHostFromNodeList] nodeRecord: $nodeRecord")
            val hostList = nodeRecord.map {
                Host(
                    bkHostId = it.value2(),
                    bkCloudId = it.value3(),
                    ip = it.value1()
                )
            }
            if (logger.isDebugEnabled) logger.debug("[getHostFromNodeList] hostList: $hostList")
            return hostList
        } else {
            logger.warn("[getHostFromNodeList] nodeHashIdList is null or empty.")
            return emptyList()
        }
    }
}