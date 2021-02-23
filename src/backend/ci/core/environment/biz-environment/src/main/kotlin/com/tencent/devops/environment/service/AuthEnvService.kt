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
class AuthEnvService @Autowired constructor(
    private val envService: EnvService
) {
    fun getEnv(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val envInfos =
            envService.listEnvironmentByLimit(projectId, offset, limit)
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
        val envInfos = envService.listRawEnvByHashIdsAllType(hashId as List<String>)
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
        val envInfos = envService.searchByName(
            projectId = projectId,
            offset = offset,
            limit = limit,
            envName = keyword)
        val result = SearchInstanceInfo()
        if (envInfos?.records == null) {
            logger.info("$projectId 项目下无环境")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        envInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.envHashId
            entity.displayName = it.name
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${envInfos?.count}")
        return result.buildSearchInstanceResult(entityInfo, envInfos.count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
