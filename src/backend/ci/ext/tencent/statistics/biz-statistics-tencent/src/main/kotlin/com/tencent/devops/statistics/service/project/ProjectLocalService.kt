/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.statistics.service.project

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.statistics.dao.project.ProjectDao
import com.tencent.devops.statistics.jmx.api.project.ProjectJmxApi
import com.tencent.devops.statistics.util.project.ProjectUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectLocalService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val bkAuthProjectApi: AuthProjectApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val jmxApi: ProjectJmxApi
) {
    fun getProjectEnNamesByOrganization(
        userId: String,
        bgId: Long?,
        deptName: String?,
        centerName: String?,
        interfaceName: String? = "ProjectLocalService"
    ): List<String> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = projectDao.listByOrganization(
                dslContext = dslContext,
                bgId = bgId,
                deptName = deptName,
                centerName = centerName
            )?.filter { it.enabled == null || it.enabled }?.map { it.englishName }?.toList() ?: emptyList()
            success = true
            return list
        } finally {
            jmxApi.execute("getProjectEnNamesByOrganization", System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list project EnNames,userName:$userId")
        }
    }

    fun getProjectEnNamesByCenterId(
        userId: String,
        centerId: Long?,
        interfaceName: String? = "ProjectLocalService"
    ): List<String> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = projectDao.listByGroupId(
                dslContext = dslContext,
                bgId = null,
                deptId = null,
                centerId = centerId
            ).filter { it.enabled == null || it.enabled }.map { it.englishName }.toList()
            success = true
            return list
        } finally {
            jmxApi.execute("getProjectEnNamesByOrganization", System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list project EnNames,userName:$userId")
        }
    }

    fun getProjectEnNamesByOrganization(
        userId: String,
        deptId: Long?,
        centerName: String?,
        interfaceName: String? = "ProjectLocalService"
    ): List<String> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = projectDao.listByOrganization(
                dslContext = dslContext,
                deptId = deptId,
                centerName = centerName
            )?.filter { it.enabled == null || it.enabled }?.map { it.englishName }?.toList() ?: emptyList()
            success = true
            return list
        } finally {
            jmxApi.execute("getProjectEnNamesByOrganization", System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list project EnNames,userName:$userId")
        }
    }

    fun getProjectByGroup(userId: String, bgName: String?, deptName: String?, centerName: String?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = ArrayList<ProjectVO>()
            projectDao.listByGroup(dslContext, bgName, deptName, centerName).filter { it.enabled == null || it.enabled }
                .map {
                    list.add(ProjectUtils.packagingBean(it))
                }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects,userName:$userId")
        }
    }

    fun getProjectByOrganizationId(
        userId: String,
        organizationType: String,
        organizationId: Long,
        deptName: String?,
        centerName: String?,
        interfaceName: String? = "ProjectLocalService"
    ): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = ArrayList<ProjectVO>()
            val records = when (organizationType) {
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> {
                    projectDao.listByOrganization(dslContext, organizationId, deptName, centerName)
                }
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> {
                    projectDao.listByOrganization(dslContext, organizationId, centerName)
                }
                AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> {
                    projectDao.listByGroupId(dslContext, null, null, organizationId)
                }
                else -> {
                    null
                }
            }
            records?.filter { it.enabled == null || it.enabled }
                ?.map {
                    list.add(ProjectUtils.packagingBean(it))
                }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects,userName:$userId")
        }
    }

    fun getProjectByGroupId(bgId: Long?, deptId: Long?, centerId: Long?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = ArrayList<ProjectVO>()
            projectDao.listByGroupId(dslContext, bgId, deptId, centerId).filter { it.enabled == null || it.enabled }
                .map {
                    list.add(ProjectUtils.packagingBean(it))
                }
            success = true
            return list
        } finally {
            jmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    fun getProjectRole(
        organizationType: String,
        organizationId: Long,
        projectId: String
    ): List<BKAuthProjectRolesResources> {
        logger.info(
            "[getProjectRole] organizationType[$organizationType]," +
                " organizationId[$organizationId] projectCode[$projectId]"
        )
        val projectList = getProjectListByOrg(organizationType = organizationType, organizationId = organizationId)
        if (projectList.isEmpty()) {
            logger.error(
                "organizationType[$organizationType] :organizationId[$organizationId] " +
                    " not project[$projectId] permission "
            )
            throw OperationException((I18nUtil.getCodeLanMessage(
                messageCode = ProjectMessageCode.ORG_NOT_PROJECT
            )))
        }
        var queryProject: ProjectVO? = null
        projectList.forEach { project ->
            if (project.projectCode == projectId) {
                queryProject = project
                return@forEach
            }
        }
        var roles = mutableListOf<BKAuthProjectRolesResources>()
        if (queryProject != null) {
            roles = bkAuthProjectApi.getProjectRoles(
                bsPipelineAuthServiceCode,
                queryProject!!.englishName,
                queryProject!!.projectId
            ).toMutableList()
        }
        return roles
    }

    private fun getProjectListByOrg(organizationType: String, organizationId: Long): List<ProjectVO> {
        var bgId: Long? = null
        var deptId: Long? = null
        var centerId: Long? = null
        when (organizationType) {
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_BG -> bgId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_DEPARTMENT -> deptId = organizationId
            AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE_CENTER -> centerId = organizationId
            else -> {
                throw OperationException((I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.ORG_TYPE_ERROR
                )))
            }
        }
        return getProjectByGroupId(bgId, deptId, centerId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectLocalService::class.java)
        const val PROJECT_LIST = "project_list"
    }
}
