package com.tencent.devops.project.service

import com.tencent.devops.project.api.pojo.ProjectOrganization
import com.tencent.devops.project.dao.ProjectDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectTxInfoService @Autowired constructor(
    val projectDao: ProjectDao,
    val dslContext: DSLContext
) {
    fun getProjectOrganizations(projectCode: String): ProjectOrganization? {
        val projectInfo = projectDao.getByEnglishName(dslContext, projectCode) ?: return null
        return ProjectOrganization(
                projectId = projectInfo.id.toString(),
                projectEnglishName = projectInfo.englishName,
                projectName = projectInfo.projectName,
                projectType = projectInfo.projectType,
                bgId = projectInfo.bgId,
                bgName = projectInfo.bgName,
                centerId = projectInfo.centerId,
                centerName = projectInfo.centerName,
                deptId = projectInfo.deptId,
                deptName = projectInfo.deptName
        )
    }
}