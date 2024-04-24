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
import com.tencent.devops.project.pojo.user.UserDeptDetail
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
            if (!fixOrganizationByManager(englishName = englishName, tProjectRecord = tProjectRecord)) {
                fixOrganizationByNormal(tProjectRecord = tProjectRecord)
            }
        }
    }

    @Suppress("MaxLineLength")
    private fun fixOrganizationByManager(
        englishName: String,
        tProjectRecord: TProjectRecord
    ): Boolean {
        val managers = client.get(ServiceProjectAuthResource::class).getProjectUsers(
            token = tokenService.getSystemToken(),
            projectCode = englishName,
            group = BkAuthGroup.MANAGER
        ).data ?: return false

        val managerDeptInfos = mutableListOf<UserDeptDetail>()
        managers.forEach forEach@{ manager ->
            val userDeptDetail = try {
                tofService.getUserDeptDetail(manager)
            } catch (ex: Exception) {
                logger.info("$englishName ${ex.message}")
                return@forEach
            }
            managerDeptInfos.add(userDeptDetail)
        }
        if (managerDeptInfos.isEmpty())
            return false

        val managerDeptIds = managerDeptInfos.map { it.deptId }
        val managerCenterIds = managerDeptInfos.map { it.centerId }

        val isManagerDepartmentSame = managerDeptIds.distinct().size == 1
        val isManagerCenterSame = managerCenterIds.distinct().size == 1

        if (isManagerDepartmentSame) {
            val managerDeptInfo = managerDeptInfos.first()
            val centerId = if (isManagerCenterSame) managerDeptInfo.centerId else null
            val centerName = if (isManagerCenterSame) managerDeptInfo.centerName else null
            logger.info("The manager's department is the same: $englishName|$isManagerDepartmentSame|$isManagerCenterSame")
            if (tProjectRecord.deptId?.toString() != managerDeptInfo.deptId ||
                tProjectRecord.centerId?.toString() != centerId) {
                logger.info("fix organization by manager: $englishName|$isManagerDepartmentSame|$isManagerCenterSame")
                projectDao.updateOrganizationByEnglishName(
                    dslContext = dslContext,
                    englishName = englishName,
                    ProjectOrganizationInfo(
                        bgId = managerDeptInfo.bgId.toLong(),
                        bgName = managerDeptInfo.bgName,
                        businessLineId = managerDeptInfo.businessLineId?.toLong(),
                        businessLineName = managerDeptInfo.businessLineName,
                        deptId = managerDeptInfo.deptId.toLong(),
                        deptName = managerDeptInfo.deptName,
                        centerId = centerId?.toLong(),
                        centerName = centerName
                    )
                )
            }
        }
        return isManagerDepartmentSame
    }

    private fun fixOrganizationByNormal(tProjectRecord: TProjectRecord) {
        val rightProjectOrganization = getRightProjectOrganization(tProjectRecord)
        if (rightProjectOrganization.needFix) {
            with(tProjectRecord) {
                logger.info(
                    "organization information before project changes:$englishName|$bgId|$bgName|$businessLineId|" +
                        "$businessLineName|$deptId|$deptName|$centerId|$centerName"
                )
                logger.info("fix organization by normal: $englishName|$rightProjectOrganization")
                projectDao.updateOrganizationByEnglishName(
                    dslContext = dslContext,
                    englishName = englishName,
                    projectOrganizationInfo = rightProjectOrganization
                )
            }
        }
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

    fun updateProjectOrganization(
        englishName: String,
        organization: ProjectOrganizationInfo
    ): Boolean {
        projectDao.updateOrganizationByEnglishName(
            dslContext = dslContext,
            englishName = englishName,
            projectOrganizationInfo = organization
        )
        return true
    }

    /**
     * 组织架构修正
     * 异常情况：原先数据库中存储的bg，部门，中心是以组织架构的层级（1/2/3级）进行存储，存在问题。正确的方式是要以tof获取的组织的type进行存储。
     * 修复方案：
     * 1、首先查询数据库项目组织架构信息。
     * 2、若centerId不为0并且centerName不为空。
     *    2.1 根据centerId查询tof部门信息接口。
     *    2.2 若原先数据库中存储的中心字段确实是小组或者中心类型，则不需要修复数据。
     *    2.3 若原先数据库中存储的中心字段是部门类型，则将原先数据库中部门字段的值挪到业务线字段位置，中心字段的值挪到部门字段位置。
     * 3、若centerId为0或centerName为空。
     *    3.1 若原先数据库中存储的部门字段的值是小组或者中心类型，则将原先数据库中部门字段的值挪到中心字段位置。
     *    3.2 若原先数据库中存储的部门字段值确实是部门类型，则不需要修复数据。
     *    3.3 若原先数据库中存储的部门字段值是业务线类型，则将原先数据库中部门字段的值挪到业务线字段位置。
     * 4、其他可能的情况不做处理。
     */

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
                        deptName = deptName,
                        needFix = false
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
                        businessLineId = businessLineId,
                        businessLineName = businessLineName,
                        centerId = null,
                        centerName = null,
                        deptId = deptId,
                        deptName = deptName,
                        needFix = false
                    )
                }
                OrganizationType.isBusinessLine(
                    tofService.getDeptInfo(id = deptId.toInt()).typeId.toInt()
                ) -> {
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
                else -> {
                    ProjectOrganizationInfo(
                        bgId = bgId,
                        bgName = bgName,
                        businessLineId = businessLineId,
                        businessLineName = businessLineName,
                        centerId = centerId,
                        centerName = centerName,
                        deptId = deptId,
                        deptName = deptName,
                        needFix = false
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
