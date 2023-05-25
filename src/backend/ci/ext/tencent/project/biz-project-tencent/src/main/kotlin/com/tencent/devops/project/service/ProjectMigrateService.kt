package com.tencent.devops.project.service

import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.MigrateProjectInfo
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class ProjectMigrateService @Autowired constructor(
    private val projectDao: ProjectDao,
    private val projectService: ProjectService,
    private val client: Client,
    private val dslContext: DSLContext
) {
    fun getMigrateProjectInfo(): List<MigrateProjectInfo> {
        val limit = 100
        val migrateProjectInfoList = mutableListOf<MigrateProjectInfo>()
        var hasMore = true
        var offset = 0
        logger.info("get migrate projectInfo start..")
        while (hasMore) {
            val migrateProjects = projectDao.listMigrateProjects(
                dslContext = dslContext,
                limit = limit,
                offset = offset
            )
            migrateProjectInfoList.addAll(migrateProjects.map {
                val isCreatorNotExist = client.get(ServiceDeptResource::class)
                    .getUserInfo("admin", it.creator).data == null
                MigrateProjectInfo(
                    englishName = it.englishName,
                    projectName = it.projectName,
                    authSystemType = projectService.buildRouterTag(it.routerTag),
                    creator = it.creator,
                    creatorNotExist = isCreatorNotExist
                )
            })
            hasMore = migrateProjects.size == limit
            offset += limit
        }
        return migrateProjectInfoList
    }

    fun updateProjectCreator(projectUpdateCreatorDtoList: List<ProjectUpdateCreatorDTO>): Boolean {
        logger.info("update project create start | $projectUpdateCreatorDtoList")
        projectUpdateCreatorDtoList.forEach {
            projectDao.getByEnglishName(
                dslContext = dslContext,
                englishName = it.projectCode
            ) ?: throw NotFoundException("project - ${it.projectCode} is not exist!")
            projectDao.updateCreatorByCode(
                dslContext = dslContext,
                projectCode = it.projectCode,
                creator = it.creator
            )
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectMigrateService::class.java)
    }
}
