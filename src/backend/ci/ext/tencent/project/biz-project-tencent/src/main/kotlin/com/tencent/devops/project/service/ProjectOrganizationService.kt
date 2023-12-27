package com.tencent.devops.project.service

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.enums.OrganizationType
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProjectOrganizationService constructor(
    val client: Client,
    val tokenService: ClientTokenService,
    val tofService: TOFService,
    val projectService: ProjectService,
    val projectDao: ProjectDao,
    val dslContext: DSLContext
) {
    @Suppress("MaxLineLength")
    fun fixProjectOrganization(englishName: String) {
        val projectInfo = projectService.getByEnglishName(englishName) ?: return

        val managers = client.get(ServiceProjectAuthResource::class).getProjectUsers(
            token = tokenService.getSystemToken(),
            projectCode = englishName,
            group = BkAuthGroup.MANAGER
        ).data ?: return

        val deptInfos = managers.map { tofService.getUserDeptDetail(it) }
        val deptIds = deptInfos.map { it.deptId }
        val isManagerDepartmentSame = deptIds.distinct().size == 1

        logger.info("Determine whether project managers have the same organization: $englishName | $isManagerDepartmentSame")

        if (isManagerDepartmentSame) {
            val deptId = deptIds.first()
            val deptName = deptInfos.first().deptName
            val parentDeptInfos = tofService.getParentDeptInfo(groupId = deptId, level = 10)
            val childDeptInfos = tofService.getChildDeptInfos(type = OrganizationType.dept, id = deptId.toInt(), level = 10)

            val bgInfo = parentDeptInfos.firstOrNull { it.typeId.toInt() == OrganizationType.bg.typeId }
            val businessLineInfo = parentDeptInfos.firstOrNull { it.typeId.toInt() == OrganizationType.businessLine.typeId }
            val centerId = if (childDeptInfos.map { it.ID }.contains(projectInfo.centerId)) projectInfo.centerId else null
            val centerName = if (childDeptInfos.map { it.Name }.contains(projectInfo.centerName)) projectInfo.centerName else null

            projectDao.updateOrganizationByEnglishName(
                dslContext = dslContext,
                englishName = englishName,
                ProjectOrganizationInfo(
                    bgId = bgInfo?.id?.toLong(),
                    bgName = bgInfo?.name,
                    businessLineId = businessLineInfo?.id?.toLong(),
                    businessLineName = businessLineInfo?.name,
                    deptId = deptId.toLong(),
                    deptName = deptName,
                    centerId = centerId?.toLong(),
                    centerName = centerName
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectOrganizationService::class.java)
    }
}
