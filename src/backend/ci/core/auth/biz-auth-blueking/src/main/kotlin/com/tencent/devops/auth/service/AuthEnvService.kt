package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.auth.pojo.FetchInstanceInfo
import com.tencent.devops.auth.pojo.ListInstanceInfo
import com.tencent.devops.auth.pojo.SearchInstanceInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.RemoteEnvResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthEnvService @Autowired constructor(
    val client: Client
) {
    fun getEnv(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val envInfos =
                client.get(RemoteEnvResource::class)
                        .listEnvForAuth(projectId, offset, limit).data
        val result = ListInstanceInfo()
        if (envInfos?.records == null) {
            logger.info("$projectId 项目下无环境")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${envInfos?.count}")
        return result.buildListInstanceResult(entityInfo, envInfos.count)
    }

    fun getEnvInfo(hashId: List<Any>?): FetchInstanceInfoResponseDTO? {
        val envInfos =
                client.get(RemoteEnvResource::class)
                        .getEnvInfos(hashId as List<String>).data
        val result = FetchInstanceInfo()
        if (envInfos == null || envInfos.isEmpty()) {
            logger.info("$hashId 下无环境")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${envInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchEnv(projectId: String, keyword: String, limit: Int, offset: Int): SearchInstanceInfo {
        val envInfos =
                client.get(RemoteEnvResource::class)
                        .searchByName(
                                projectId = projectId,
                                offset = offset,
                                limit = limit,
                                envName = keyword).data
        val result = SearchInstanceInfo()
        if (envInfos?.records == null) {
            AuthPipelineService.logger.info("$projectId 项目下无环境")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        AuthPipelineService.logger.info("entityInfo $entityInfo, count ${envInfos?.count}")
        return result.buildSearchInstanceResult(entityInfo, envInfos.count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}