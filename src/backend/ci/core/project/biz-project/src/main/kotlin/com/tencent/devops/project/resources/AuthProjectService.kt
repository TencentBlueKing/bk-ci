package com.tencent.devops.project.resources

import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO
import com.tencent.devops.common.auth.callback.AuthConstants
import com.tencent.devops.common.auth.callback.FetchInstanceInfo
import com.tencent.devops.common.auth.callback.ListInstanceInfo
import com.tencent.devops.common.auth.callback.SearchInstanceInfo
import com.tencent.devops.project.service.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AuthProjectService @Autowired constructor(
    val projectService: ProjectService
) {

    fun getProjectList(page: PageInfoDTO?): ListInstanceResponseDTO {
        logger.info("getProjectList page $page ")
        var offset = 0
        var limit = AuthConstants.MAX_LIMIT
        if (page != null) {
            offset = page.offset.toInt()
            limit = page.limit.toInt()
        }
        val projectRecords = projectService.list(limit, offset)
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

    fun getProjectInfo(idList: List<String>): FetchInstanceInfoResponseDTO {
        logger.info("getProjectInfo ids[$idList]")
        val ids = idList.toSet()
        val projectInfo = projectService.list(ids)
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
        val projectRecords = projectService.searchProjectByProjectName(keyword, page!!.limit.toInt(), page!!.offset.toInt())
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
