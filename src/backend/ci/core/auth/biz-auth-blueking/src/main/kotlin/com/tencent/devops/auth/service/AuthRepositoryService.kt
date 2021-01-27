package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.auth.pojo.FetchInstanceInfo
import com.tencent.devops.auth.pojo.ListInstanceInfo
import com.tencent.devops.auth.pojo.SearchInstanceInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceAuthRepositoryResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthRepositoryService @Autowired constructor(
    val client: Client
) {

    fun getRepository(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val repositoryInfos =
                client.get(ServiceAuthRepositoryResource::class)
                        .listByProjects(projectId, offset, limit).data
        val result = ListInstanceInfo()
        if (repositoryInfos?.records == null) {
            logger.info("$projectId 项目下无代码库")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        repositoryInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.repositoryHashId
            entity.displayName = it.aliasName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${repositoryInfos?.count}")
        return result.buildListInstanceResult(entityInfo, repositoryInfos.count)
    }

    fun getRepositoryInfo(hashId: List<Any>?): FetchInstanceInfoResponseDTO? {
        val repositoryInfos =
                client.get(ServiceAuthRepositoryResource::class)
                        .getInfos(hashId as List<String>).data
        val result = FetchInstanceInfo()
        if (repositoryInfos == null || repositoryInfos.isEmpty()) {
            logger.info("$hashId 未匹配到代码库")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        repositoryInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.repositoryHashId
            entity.displayName = it.aliasName
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${repositoryInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchRepositoryInstances(projectId: String, keyword: String, limit: Int, offset: Int): SearchInstanceInfo {
        logger.info("searchInstance keyword[$keyword] projectId[$projectId], limit[$limit] , offset[$offset]")
        val repositoryRecords = client.get(ServiceAuthRepositoryResource::class).searchByName(
                projectId = projectId,
                limit = limit,
                offset = offset,
                aliasName = keyword
        ).data
        logger.info("repositoryRecords $repositoryRecords")
        val count = repositoryRecords?.count ?: 0L
        val repositorytInfo = mutableListOf<InstanceInfoDTO>()
        repositoryRecords?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.repositoryHashId
            entity.displayName = it.aliasName
            repositorytInfo.add(entity)
        }
        logger.info("repositorytInfo $repositorytInfo")
        val result = SearchInstanceInfo()
        return result.buildSearchInstanceResult(repositorytInfo, count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}