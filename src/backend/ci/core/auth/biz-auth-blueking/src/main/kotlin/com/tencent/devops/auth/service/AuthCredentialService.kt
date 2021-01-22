package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.devops.auth.pojo.FetchInstanceInfo
import com.tencent.devops.auth.pojo.ListInstanceInfo
import com.tencent.devops.auth.pojo.SearchInstanceInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.ticket.api.ServiceAuthCallbackResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthCredentialService @Autowired constructor(
    val client: Client
) {

    fun getCredential(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val credentialInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .listCredential(projectId, offset, limit).data
        val result = ListInstanceInfo()
        if (credentialInfos?.records == null) {
            logger.info("$projectId 项目下无凭证")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        credentialInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.credentialId
            entity.displayName = it.credentialId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${credentialInfos?.count}")
        return result.buildListInstanceResult(entityInfo, credentialInfos.count)
    }

    fun getCredentialInfo(ids: List<Any>?): FetchInstanceInfoResponseDTO? {
        val credentialInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .getCredentialInfos(ids!!.toSet() as Set<String>).data
        val result = FetchInstanceInfo()
        if (credentialInfos == null || credentialInfos.isEmpty()) {
            logger.info("$ids 无凭证")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        credentialInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.credentialId
            entity.displayName = it.credentialId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${credentialInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchCredential(projectId: String, keyword: String, limit: Int, offset: Int): SearchInstanceInfo {
        val credentialInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .searchCredentialById(
                                projectId = projectId,
                                offset = offset,
                                limit = limit,
                                credentialId = keyword).data
        val result = SearchInstanceInfo()
        if (credentialInfos?.records == null) {
            logger.info("$projectId 项目下无证书")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        credentialInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.credentialId
            entity.displayName = it.credentialId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${credentialInfos?.count}")
        return result.buildSearchInstanceResult(entityInfo, credentialInfos.count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}