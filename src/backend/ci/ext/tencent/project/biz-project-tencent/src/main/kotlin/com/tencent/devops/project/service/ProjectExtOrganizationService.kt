package com.tencent.devops.project.service

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.model.project.tables.records.TProjectRecord
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
    val projectDao: ProjectDao,
    val dslContext: DSLContext
) {
    fun fixProjectOrganization(englishName: String) {
        projectDao.getByEnglishName(dslContext, englishName)?.also { tProjectRecord ->
            if (!fixOrganizationByManager(englishName = englishName)) {
                fixOrganizationByNormal(tProjectRecord = tProjectRecord)
            }
        }
    }

    @Suppress("MaxLineLength")
    private fun fixOrganizationByManager(englishName: String): Boolean {
        val managers = client.get(ServiceProjectAuthResource::class).getProjectUsers(
            token = tokenService.getSystemToken(),
            projectCode = englishName,
            group = BkAuthGroup.MANAGER
        ).data ?: return false

        val deptInfos = managers.map { tofService.getUserDeptDetail(it) }
        val deptIds = deptInfos.map { it.deptId }
        val centerIds = deptInfos.map { it.centerId }
        val isManagerDepartmentSame = deptIds.distinct().size == 1
        val isManagerCenterSame = centerIds.distinct().size == 1

        if (isManagerDepartmentSame) {
            logger.info("fix organization by manager: $englishName|$isManagerDepartmentSame|$isManagerCenterSame")
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
        return isManagerDepartmentSame
    }

    private fun fixOrganizationByNormal(tProjectRecord: TProjectRecord) {
        val rightProjectOrganization = getRightProjectOrganization(tProjectRecord)
        logger.info("fix organization by normal: ${tProjectRecord.englishName}|$rightProjectOrganization")
        projectDao.updateOrganizationByEnglishName(
            dslContext = dslContext,
            englishName = tProjectRecord.englishName,
            projectOrganizationInfo = rightProjectOrganization
        )
    }

    fun fixProjectOrganization(englishNames: List<String>): Boolean {
        logger.info("batch fix project organization:$englishNames")
        englishNames.forEach {
            executor.submit {
                fixProjectOrganization(englishName = it)
            }
        }
        return true
    }

    fun fixAllProjectOrganization(channelCode: String): Boolean {
        Thread {
            logger.info("fix all project organization:$channelCode")
            var offset = 0
            val limit = PageUtil.MAX_PAGE_SIZE
            do {
                val projectInfos = projectDao.listByChannel(
                    dslContext = dslContext,
                    limit = limit,
                    offset = offset,
                    channelCodes = listOf(channelCode)
                )
                fixProjectOrganization(englishNames = projectInfos.map { it.englishName })
                offset += limit
            } while (projectInfos.size == limit)
        }.start()
        return true
    }

    fun getRightProjectOrganization(
        tProjectRecord: TProjectRecord
    ): ProjectOrganizationInfo {
        val centerId = tProjectRecord.centerId
        val centerName = tProjectRecord.centerName
        val deptId = tProjectRecord.deptId

        return with(tProjectRecord) {
            when {
                centerId != 0L && !centerName.isNullOrBlank() && OrganizationType.isBelowTheDept(
                    tofService.getDeptInfo(id = centerId.toInt()).typeId.toInt()
                ) -> {
                    ProjectOrganizationInfo(
                        bgId = bgId,
                        bgName = bgName,
                        businessLineId = businessLineId,
                        businessLineName = businessLineName,
                        centerId = centerId,
                        centerName = centerName,
                        deptId = deptId,
                        deptName = deptName
                    )
                }
                centerId != 0L && !centerName.isNullOrBlank() -> {
                    ProjectOrganizationInfo(
                        bgId = bgId,
                        bgName = bgName,
                        businessLineId = deptId,
                        businessLineName = deptName,
                        centerId = null,
                        centerName = null,
                        deptId = centerId,
                        deptName = centerName
                    )
                }
                OrganizationType.isBelowTheDept(
                    tofService.getDeptInfo(id = deptId.toInt()).typeId.toInt()
                ) -> {
                    ProjectOrganizationInfo(
                        bgId = bgId,
                        bgName = bgName,
                        businessLineId = null,
                        businessLineName = null,
                        centerId = deptId,
                        centerName = deptName,
                        deptId = null,
                        deptName = null
                    )
                }
                OrganizationType.isDept(
                    tofService.getDeptInfo(id = deptId.toInt()).typeId.toInt()
                ) -> {
                    ProjectOrganizationInfo(
                        bgId = bgId,
                        bgName = bgName,
                        businessLineId = null,
                        businessLineName = null,
                        centerId = null,
                        centerName = null,
                        deptId = deptId,
                        deptName = deptName
                    )
                }
                else -> {
                    ProjectOrganizationInfo(
                        bgId = bgId,
                        bgName = bgName,
                        businessLineId = deptId,
                        businessLineName = deptName,
                        centerId = null,
                        centerName = null,
                        deptId = null,
                        deptName = null
                    )
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectExtOrganizationService::class.java)
        private val executor = Executors.newFixedThreadPool(5)
    }
}
