package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.CallbackMethodEnum
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO
import com.tencent.devops.auth.pojo.AuthConstants
import com.tencent.devops.auth.pojo.FetchInstanceInfo
import com.tencent.devops.auth.pojo.ListInstanceInfo
import com.tencent.devops.auth.pojo.SearchInstanceInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceAuthProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthProjectService @Autowired constructor(
    val client: Client
) {

    fun getProjectList(page: PageInfoDTO?, method: CallbackMethodEnum, token: String): ListInstanceResponseDTO {
        logger.info("getProjectList method $method, page $page token $token")
        var offset = 0
        var limit = AuthConstants.MAX_LIMIT
        if (page != null) {
            offset = page.offset.toInt()
            limit = page.limit.toInt()
        }
        val projectRecords =
                client.get(ServiceAuthProjectResource::class).list(limit, offset).data
        logger.info("projectRecords $projectRecords")
        val count = projectRecords?.count ?: 0L
        val projectInfo = mutableListOf<InstanceInfoDTO>()
        projectRecords?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.projectName
            projectInfo.add(entity)
        }
        logger.info("projectInfo $projectInfo")
        val result = ListInstanceInfo()
        return result.buildListInstanceResult(projectInfo, count)
    }

    fun getProjectInfo(idList: List<String>, attrs: List<String>): FetchInstanceInfoResponseDTO {
        logger.info("getProjectInfo ids[$idList] attrs[$attrs]")
        val ids = idList.toSet()
        val projectInfo = client.get(ServiceAuthProjectResource::class).getByIds(ids).data
        logger.info("projectRecords $projectInfo")
        val entityList = mutableListOf<InstanceInfoDTO>()
        projectInfo?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.projectName
            entityList.add(entity)
        }
        logger.info("entityInfo $entityList")
        val result = FetchInstanceInfo()
        return result.buildFetchInstanceResult(entityList)
    }

    fun searchProjectInstances(keyword: String, page: PageInfoDTO?): SearchInstanceResponseDTO {
        logger.info("searchInstance keyword[$keyword] page[$page]")
        val projectRecords = client.get(ServiceAuthProjectResource::class).searchByName(keyword, page!!.limit.toInt(), page!!.offset.toInt()).data
        logger.info("projectRecords $projectRecords")
        val count = projectRecords?.count ?: 0L
        val projectInfo = mutableListOf<InstanceInfoDTO>()
        projectRecords?.records?.map {
            val entity = InstanceInfoDTO()
            entity.id = it.englishName
            entity.displayName = it.projectName
            projectInfo.add(entity)
        }
        logger.info("projectInfo $projectInfo")
        val result = SearchInstanceInfo()
        return result.buildSearchInstanceResult(projectInfo, count)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}