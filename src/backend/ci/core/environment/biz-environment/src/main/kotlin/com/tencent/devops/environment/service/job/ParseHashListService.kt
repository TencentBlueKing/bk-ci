package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.job.Host
import com.tencent.devops.environment.service.job.api.ApigwJobCloudApi
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

    fun getHostFromEnvList(projectId: String, envHashIdList: List<String>?/*环境hashId列表*/): List<Host> {
        if (!envHashIdList.isNullOrEmpty()) {
//            val envIdList = mutableListOf<Long>()
            val envRecord = nodeDao.getEnvsFromEnvHashList(
                dslContext, projectId, envHashIdList
            )
            val envIdList = envRecord.map {
//                envIdList.add(it.envId)
                it.envId
            }

//            val nodeIdList = mutableListOf<Long>()
            val envNodeRecord = nodeDao.getNodeIdsFromEnvIdList(
                dslContext, projectId, envIdList
            )
            val nodeIdList = envNodeRecord.map {
//                nodeIdList.add(it.nodeId)
                it.nodeId
            }

//            val nodeHostList = mutableListOf<Host>()
            val nodeRecord = nodeDao.getNodesFromNodeIdList(
                dslContext, projectId, nodeIdList
            )
            val nodeHostList = nodeRecord.map {
                Host(
                    bkHostId = it.hostId,
                    bkCloudId = it.cloudAreaId,
                    ip = it.nodeIp
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
//            val hostList = mutableListOf<Host>()
            if (logger.isDebugEnabled) logger.debug(
                "[getHostFromNodeList] projectId: $projectId，" +
                    "nodeHashIdList：$nodeHashIdList"
            )
            val nodeRecord = nodeDao.getNodesFromNodeHashList(
                dslContext, projectId, nodeHashIdList
            )
            if (logger.isDebugEnabled) logger.debug("[getHostFromNodeList] nodeRecord: $nodeRecord")
//            nodeRecord.map {
//                if (logger.isDebugEnabled) logger.debug(
//                    "[getHostFromNodeList] it.hostId: ${it.hostId}, " +
//                        "it.cloudAreaId: ${it.cloudAreaId}, " +
//                        "it.nodeIp: ${it.nodeIp}"
//                )
//                hostList.add(
//                    Host(
//                        bkHostId = it.hostId,
//                        bkCloudId = it.cloudAreaId,
//                        ip = it.nodeIp
//                    )
//                )
//            }
            val hostList = nodeRecord.map {
                Host(
                    bkHostId = it.hostId,
                    bkCloudId = it.cloudAreaId,
                    ip = it.nodeIp
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