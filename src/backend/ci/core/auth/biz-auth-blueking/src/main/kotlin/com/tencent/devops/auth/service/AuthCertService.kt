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
class AuthCertService @Autowired constructor(
    val client: Client
) {

    fun getCert(projectId: String, offset: Int, limit: Int): ListInstanceResponseDTO? {
        val certInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .listCert(projectId, offset, limit).data
        val result = ListInstanceInfo()
        if (certInfos?.records == null) {
            logger.info("$projectId 项目下无凭证")
            return result.buildListInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        certInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.certId
            entity.displayName = it.certId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${certInfos?.count}")
        return result.buildListInstanceResult(entityInfo, certInfos.count)
    }

    fun getCertInfo(ids: List<Any>?): FetchInstanceInfoResponseDTO? {
        val certInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .getCertInfos(ids!!.toSet() as Set<String>).data
        val result = FetchInstanceInfo()
        if (certInfos == null || certInfos.isEmpty()) {
            logger.info("$ids 无凭证")
            return result.buildFetchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        certInfos?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.certId
            entity.displayName = it.certId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${certInfos.size.toLong()}")
        return result.buildFetchInstanceResult(entityInfo)
    }

    fun searchCert(projectId: String, keyword: String, limit: Int, offset: Int): SearchInstanceInfo {
        val certInfos =
                client.get(ServiceAuthCallbackResource::class)
                        .searchCertById(
                                projectId = projectId,
                                offset = offset,
                                limit = limit,
                                certId = keyword).data
        val result = SearchInstanceInfo()
        if (certInfos?.records == null) {
            logger.info("$projectId 项目下无证书")
            return result.buildSearchInstanceFailResult()
        }
        val entityInfo = mutableListOf<InstanceInfoDTO>()
        certInfos?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.certId
            entity.displayName = it.certId
            entityInfo.add(entity)
        }
        logger.info("entityInfo $entityInfo, count ${certInfos?.count}")
        return result.buildSearchInstanceResult(entityInfo, certInfos.count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}