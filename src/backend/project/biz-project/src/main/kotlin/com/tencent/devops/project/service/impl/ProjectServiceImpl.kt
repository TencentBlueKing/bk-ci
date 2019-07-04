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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service.impl

import com.tencent.devops.artifactory.api.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.jmx.api.ProjectJmxApi.Companion.PROJECT_LIST
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.util.ImageUtil
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.util.ArrayList
import java.util.regex.Pattern

/**
 * 蓝鲸权限中心管控的项目服务实现
 */
@Service
class ProjectServiceImpl @Autowired constructor(
    private val projectPermissionService: ProjectPermissionService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val projectJmxApi: ProjectJmxApi,
    private val redisOperation: RedisOperation,
    private val gray: Gray,
    private val client: Client
) : ProjectService {

    override fun validate(validateType: ProjectValidateType, name: String, projectId: String?) {
        if (name.isBlank()) {
            throw OperationException("名称不能为空")
        }
        when (validateType) {
            ProjectValidateType.project_name -> {
                if (name.length < 4 || name.length > 12) {
                    throw OperationException("项目名至多4-12个字符")
                }
                if (projectDao.existByProjectName(dslContext, name, projectId)) {
                    throw OperationException("项目名已经存在")
                }
            }
            ProjectValidateType.english_name -> {
                // 2 ~ 32 个字符+数字，以小写字母开头
                if (name.length < 2 || name.length > 32) {
                    throw OperationException("英文名长度在3-32个字符")
                }
                if (!Pattern.matches(ENGLISH_NAME_PATTERN, name)) {
                    logger.warn("Project English Name($name) is not match")
                    throw OperationException("英文名是字符+数字组成，并以小写字母开头")
                }
                if (projectDao.existByEnglishName(dslContext, name, projectId)) {
                    throw OperationException("英文名已经存在")
                }
            }
        }
    }

    /**
     * 创建项目信息
     */
    override fun create(userId: String, projectCreateInfo: ProjectCreateInfo) {
        validate(ProjectValidateType.project_name, projectCreateInfo.projectName)
        validate(ProjectValidateType.english_name, projectCreateInfo.englishName)

        // 随机生成首字母图片
        val firstChar = projectCreateInfo.englishName.substring(0, 1).toUpperCase()
        val logoFile = ImageUtil.drawImage(
            firstChar,
            Width,
            Height
        )
        try {
            // 保存Logo文件
            val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
            val result =
                CommonUtils.serviceUploadFile(userId, serviceUrlPrefix, logoFile, FileChannelTypeEnum.WEB_SHOW.name)
            if (result.isNotOk()) {
                throw OperationException("${result.status}:${result.message}")
            }
            val logoAddress = result.data!!

            try {
                // 注册项目到权限中心
                projectPermissionService.createResources(
                    userId = userId,
                    projectList = listOf(
                        ResourceRegisterInfo(
                            projectCreateInfo.englishName,
                            projectCreateInfo.projectName
                        )
                    )
                )
            } catch (e: Exception) {
                logger.warn("权限中心创建项目信息： $projectCreateInfo", e)
                throw OperationException("权限中心创建项目失败")
            }

            val projectId = UUIDUtil.generate()
            val userDeptDetail = UserDeptDetail(
                bgName = "",
                bgId = 1,
                centerName = "",
                centerId = 1,
                deptName = "",
                deptId = 1,
                groupId = 0,
                groupName = ""
            )
            try {
                projectDao.create(dslContext, userId, logoAddress, projectCreateInfo, userDeptDetail, projectId)
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectCreateInfo", e)
                throw OperationException("项目名或者英文名重复")
            } catch (ignored: Throwable) {
                logger.warn(
                    "Fail to create the project ($projectCreateInfo)",
                    ignored
                )
                projectPermissionService.deleteResource(projectCode = projectCreateInfo.englishName)

                throw ignored
            }
        } finally {
            if (logoFile.exists()) {
                logoFile.delete()
            }
        }
    }

    override fun getByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return packagingBean(record, grayProjectSet())
    }

    override fun update(userId: String, projectId: String, projectUpdateInfo: ProjectUpdateInfo): Boolean {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            try {
                projectDao.update(dslContext, userId, projectId, projectUpdateInfo)
                projectPermissionService.modifyResource(
                    projectCode = projectUpdateInfo.englishName,
                    projectName = projectUpdateInfo.projectName
                )
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectUpdateInfo", e)
                throw OperationException("项目名或英文名重复")
            }
            success = true
        } finally {
            projectJmxApi.execute(ProjectJmxApi.PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
        return success
    }

    /**
     * 获取所有项目信息
     */
    override fun list(userId: String): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {

            val projects = projectPermissionService.getUserProjects(userId)

            val list = ArrayList<ProjectVO>()
            projectDao.listByEnglishName(dslContext, projects).map {
                list.add(packagingBean(it, grayProjectSet()))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun list(projectCodes: Set<String>): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = ArrayList<ProjectVO>()

            val grayProjectSet = grayProjectSet()

            projectDao.listByCodes(dslContext, projectCodes).filter { it.enabled == null || it.enabled }.map {
                list.add(packagingBean(it, grayProjectSet))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    /**
     * 获取用户已的可访问项目列表
     */
    override fun getProjectByUser(userName: String): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val projectList = projectPermissionService.getUserProjectsAvailable(userName)

            val list = ArrayList<ProjectVO>()
            val projectCodes = projectList.map { it.key }

            val grayProjectSet = grayProjectSet()

            projectDao.listByCodes(dslContext, projectCodes.toSet()).filter { it.enabled == null || it.enabled }.map {
                list.add(packagingBean(it, grayProjectSet))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun getNameByCode(projectCodes: String): HashMap<String, String> {
        val map = HashMap<String, String>()
        projectDao.listByCodes(dslContext, projectCodes.split(",").toSet()).map {
            map.put(it.englishName, it.projectName)
        }
        return map
    }

    override fun grayProjectSet() =
        (redisOperation.getSetMembers(gray.getGrayRedisKey()) ?: emptySet()).filter { !it.isBlank() }.toSet()

    private fun packagingBean(tProjectRecord: TProjectRecord, grayProjectSet: Set<String>): ProjectVO {
        return ProjectVO(
            id = tProjectRecord.id,
            projectId = tProjectRecord.projectId ?: "",
            projectName = tProjectRecord.projectName,
            projectCode = tProjectRecord.englishName ?: "",
            projectType = tProjectRecord.projectType ?: 0,
            approvalStatus = tProjectRecord.approvalStatus ?: 0,
            approvalTime = if (tProjectRecord.approvalTime == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.approvalTime, "yyyy-MM-dd'T'HH:mm:ssZ")
            },
            approver = tProjectRecord.approver ?: "",
            bgId = tProjectRecord.bgId,
            bgName = tProjectRecord.bgName ?: "",
            ccAppId = tProjectRecord.ccAppId ?: 0,
            ccAppName = tProjectRecord.ccAppName ?: "",
            centerId = tProjectRecord.centerId ?: 0,
            centerName = tProjectRecord.centerName ?: "",
            createdAt = DateTimeUtil.toDateTime(tProjectRecord.createdAt, "yyyy-MM-dd"),
            creator = tProjectRecord.creator ?: "",
            dataId = tProjectRecord.dataId ?: 0,
            deployType = tProjectRecord.deployType ?: "",
            deptId = tProjectRecord.deptId ?: 0,
            deptName = tProjectRecord.deptName ?: "",
            description = tProjectRecord.description ?: "",
            englishName = tProjectRecord.englishName ?: "",
            extra = tProjectRecord.extra ?: "",
            isOfflined = tProjectRecord.isOfflined,
            isSecrecy = tProjectRecord.isSecrecy,
            isHelmChartEnabled = tProjectRecord.isHelmChartEnabled,
            kind = tProjectRecord.kind,
            logoAddr = tProjectRecord.logoAddr ?: "",
            remark = tProjectRecord.remark ?: "",
            updatedAt = if (tProjectRecord.updatedAt == null) {
                ""
            } else {
                DateTimeUtil.toDateTime(tProjectRecord.updatedAt, "yyyy-MM-dd")
            },
            useBk = tProjectRecord.useBk,
            enabled = tProjectRecord.enabled,
            gray = grayProjectSet.contains(tProjectRecord.englishName),
            enableExternal = tProjectRecord.enableExternal
        )
    }

    override fun updateLogo(
        userId: String,
        projectId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<Boolean> {
        logger.info("Update the logo of project $projectId")
        val project = projectDao.get(dslContext, projectId)
        if (project != null) {
            var logoFile: File? = null
            try {
                logoFile = FileUtil.convertTempFile(inputStream)
                val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
                val result =
                    CommonUtils.serviceUploadFile(userId, serviceUrlPrefix, logoFile, FileChannelTypeEnum.WEB_SHOW.name)
                if (result.isNotOk()) {
                    return Result(result.status, result.message, false)
                }
                projectDao.updateLogoAddress(dslContext, userId, projectId, result.data!!)
            } catch (e: Exception) {
                logger.warn("fail update projectLogo", e)
                throw OperationException("更新项目logo失败")
            } finally {
                logoFile?.delete()
            }
        } else {
            logger.warn("$project is null or $project is empty")
            throw OperationException("查询不到有效的项目")
        }
        return Result(true)
    }

    override fun updateEnabled(
        userId: String,
        accessToken: String,
        projectId: String,
        enabled: Boolean
    ): Result<Boolean> {
        logger.info("Update the enabled of project $projectId")
        val project = projectDao.get(dslContext, projectId)
        if (project != null) {
            projectDao.updateEnabled(dslContext, userId, projectId, enabled)
        } else {
            logger.warn("$project is null or $project is empty")
            throw OperationException("查询不到有效的项目")
        }
        return Result(true)
    }

    companion object {
        private const val Width = 128
        private const val Height = 128
        private val logger = LoggerFactory.getLogger(ProjectServiceImpl::class.java)!!
        private const val ENGLISH_NAME_PATTERN = "[a-z][a-zA-Z0-9]+"
    }
}
