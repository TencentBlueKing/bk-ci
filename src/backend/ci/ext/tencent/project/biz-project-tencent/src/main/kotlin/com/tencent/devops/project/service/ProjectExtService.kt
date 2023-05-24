package com.tencent.devops.project.service

import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.MigrateProjectInfo
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException

@Service
class ProjectExtService @Autowired constructor(
    private val projectDao: ProjectDao,
    private val projectService: ProjectService,
    private val client: Client,
    private val dslContext: DSLContext
) {
    fun getMigrateProjectInfo(): List<MigrateProjectInfo> {
        var offset = 0
        val limit = 100
        val migrateProjectInfoList = mutableListOf<MigrateProjectInfo>()
        do {
            val migrateProjects = projectDao.listMigrateProjects(
                dslContext = dslContext,
                limit = limit,
                offset = offset
            )
            migrateProjects.forEach {
                migrateProjectInfoList.add(
                    MigrateProjectInfo(
                        englishName = it.englishName,
                        projectName = it.projectName,
                        authSystemType = projectService.buildRouterTag(it.routerTag),
                        creator = it.creator,
                        creatorNotExist = client.get(ServiceDeptResource::class)
                            .getUserInfo("admin", it.creator).data == null
                    )
                )
            }
            offset += limit
        } while (migrateProjects.size == limit)
        return migrateProjectInfoList
    }

    fun updateProjectCreator(projectUpdateCreatorDtoList: List<ProjectUpdateCreatorDTO>): Boolean {
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
}
