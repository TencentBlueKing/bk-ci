package com.tencent.devops.process.service

import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServiceAuthPipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthPipelineService @Autowired constructor(
    val client: Client
) {
    fun searchPipeline(projectId: String, keyword: String, limit: Int, offset: Int): SearchInstanceInfo {
        val pipelineInfos =
                client.get(ServiceAuthPipelineResource::class)
                        .searchPipelineInstances(projectId, offset, limit, keyword).data
        val result = SearchInstanceInfo()
        if (pipelineInfos?.records == null) {
            logger.info("$projectId 项目下无流水线")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.pipelineId
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos?.count}")
        return result.buildSearchInstanceResult(entityInfo, pipelineInfos.count)
    }

    fun getPipeline(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val pipelineInfos =
                client.get(ServiceAuthPipelineResource::class)
                        .pipelineList(projectId, offset, limit).data
        val result = ListInstanceInfo()
        if (pipelineInfos?.records == null) {
            logger.info("$projectId 项目下无流水线")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.pipelineId
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos?.count}")
        return result.buildListInstanceResult(entityInfo, pipelineInfos.count)
    }

    fun getPipelineInfo(ids: List<Any>?): FetchInstanceInfoResponseDTO? {
        val pipelineInfos =
                client.get(ServiceAuthPipelineResource::class)
                        .pipelineInfos(ids!!.toSet() as Set<String>).data
        val result = FetchInstanceInfo()

        if (pipelineInfos == null || pipelineInfos.isEmpty()) {
            logger.info("$ids 未匹配到启用流水线")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        pipelineInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.pipelineId
            entity.displayName = it.pipelineName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${pipelineInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
