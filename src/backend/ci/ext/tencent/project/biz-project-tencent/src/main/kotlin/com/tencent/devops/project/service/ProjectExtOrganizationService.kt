package com.tencent.devops.project.service

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.enums.OrganizationType
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ProjectExtOrganizationService constructor(
    val client: Client,
    val tokenService: ClientTokenService,
    val tofService: TOFService,
    val projectService: ProjectService,
    val projectDao: ProjectDao,
    val dslContext: DSLContext
) {
    @Suppress("MaxLineLength")
    fun fixProjectOrganization(englishName: String) {
        projectService.getByEnglishName(englishName) ?: return

        val managers = client.get(ServiceProjectAuthResource::class).getProjectUsers(
            token = tokenService.getSystemToken(),
            projectCode = englishName,
            group = BkAuthGroup.MANAGER
        ).data ?: return

        val deptInfos = managers.map { tofService.getUserDeptDetail(it) }
        val deptIds = deptInfos.map { it.deptId }
        val centerIds = deptInfos.map { it.centerId }
        val isManagerDepartmentSame = deptIds.distinct().size == 1
        val isManagerCenterSame = centerIds.distinct().size == 1

        logger.info(
            "Determine whether project managers have the same organization: $englishName | " +
                "$isManagerDepartmentSame|$isManagerCenterSame"
        )

        if (isManagerDepartmentSame) {
            val deptId = deptIds.first()
            val deptName = deptInfos.first().deptName
            val parentDeptInfos = tofService.getParentDeptInfo(groupId = deptId, level = 10)

            val bgInfo = parentDeptInfos.firstOrNull { it.typeId.toInt() == OrganizationType.bg.typeId }
            val businessLineInfo = parentDeptInfos.firstOrNull { it.typeId.toInt() == OrganizationType.businessLine.typeId }
            val centerId = if (isManagerCenterSame) centerIds.first() else null
            val centerName = if (isManagerCenterSame) deptInfos.first().centerName else null

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

    fun fixProjectOrganization(englishNames: List<String>): Boolean {
        logger.info("batch fix  project organization:$englishNames")
        englishNames.forEach {
            executor.submit {
                fixProjectOrganization(englishName = it)
            }
        }
        return true
    }

    fun fixAllProjectOrganization(channelCode: String? = ChannelCode.BS.name): Boolean {
        Thread {
            logger.info("fix all project organization:$channelCode")
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            do {
                val projectInfos = projectDao.listByChannel(
                    dslContext = dslContext,
                    limit = limit,
                    offset = offset,
                    channelCodes = listOf(channelCode!!)
                )
                fixProjectOrganization(englishNames = projectInfos.map { it.englishName })
                offset += limit
            } while (projectInfos.size == limit)
        }.start()
        return true
    }


    companion object {
        private val logger = LoggerFactory.getLogger(ProjectExtOrganizationService::class.java)
        private val executor = Executors.newFixedThreadPool(5)
    }
}
