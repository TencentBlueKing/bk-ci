package com.tencent.devops.ticket.service

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
class AuthCredentialService @Autowired constructor(
    private val credentialService: CredentialService
) {

    fun getCredential(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val credentialInfos = credentialService.serviceList(projectId, offset, limit)
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
        val credentialInfos = credentialService.getCredentialByIds(null, ids!!.toSet() as Set<String>)
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
        val credentialInfos = credentialService.searchByCredentialId(
                                projectId = projectId,
                                offset = offset,
                                limit = limit,
                                credentialId = keyword)
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
