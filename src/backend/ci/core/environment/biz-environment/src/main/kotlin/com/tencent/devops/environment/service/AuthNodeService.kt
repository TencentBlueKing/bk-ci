package com.tencent.devops.environment.service

import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthNodeService @Autowired constructor(
    private val nodeService: NodeService
) {

    fun getNodeInfo(hashIds: List<Any>?): FetchInstanceInfoResponseDTO? {
        val nodeInfos = nodeService.listRawServerNodeByIds(hashIds as List<String>)
        val result = FetchInstanceInfo()
        if (nodeInfos == null || nodeInfos.isEmpty()) {
            logger.info("$hashIds 无节点")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        nodeInfos.map {
            val entity = InstanceInfoDTO()
            entity.id = it.nodeHashId
            entity.displayName = it.displayName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${nodeInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun getNode(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val nodeInfos = nodeService.listByPage(projectId, offset, limit)
        val result = ListInstanceInfo()
        if (nodeInfos?.records == null) {
            logger.info("$projectId 项目下无节点")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        nodeInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.nodeHashId
            entity.displayName = it.displayName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${nodeInfos?.count}")
        return result.buildListInstanceResult(entityInfo, nodeInfos.count)
    }

    fun searchNode(projectId: String, keyword: String, limit: Int, offset: Int): SearchInstanceInfo {
        val nodeInfos = nodeService.searchByDisplayName(
                                projectId = projectId,
                                offset = offset,
                                limit = limit,
                                displayName = keyword)
        val result = SearchInstanceInfo()
        if (nodeInfos?.records == null) {
            logger.info("$projectId 项目下无节点")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        nodeInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.nodeHashId
            entity.displayName = it.displayName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${nodeInfos?.count}")
        return result.buildSearchInstanceResult(entityInfo, nodeInfos.count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
