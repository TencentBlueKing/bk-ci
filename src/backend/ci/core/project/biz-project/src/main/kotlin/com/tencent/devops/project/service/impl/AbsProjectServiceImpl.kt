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

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.jmx.api.ProjectJmxApi.Companion.PROJECT_LIST
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectLogo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.util.ProjectUtils
import com.tencent.devops.project.util.exception.ProjectNotExistException
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import java.io.File
import java.io.InputStream
import java.util.ArrayList
import java.util.regex.Pattern

abstract class AbsProjectServiceImpl @Autowired constructor(
    val projectPermissionService: ProjectPermissionService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val projectJmxApi: ProjectJmxApi,
    val redisOperation: RedisOperation,
    private val gray: Gray,
    val client: Client,
    private val projectDispatcher: ProjectDispatcher
) : ProjectService {

    override fun validate(validateType: ProjectValidateType, name: String, projectId: String?) {
        if (name.isBlank()) {
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NAME_EMPTY))
        }
        when (validateType) {
            ProjectValidateType.project_name -> {
                if (name.isEmpty() || name.length > 12) {
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NAME_TOO_LONG))
                }
                if (projectDao.existByProjectName(dslContext, name, projectId)) {
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
                }
            }
            ProjectValidateType.english_name -> {
                // 2 ~ 32 个字符+数字，以小写字母开头
                if (name.length < 2 || name.length > 32) {
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.EN_NAME_INTERVAL_ERROR))
                }
                if (!Pattern.matches(ENGLISH_NAME_PATTERN, name)) {
                    logger.warn("Project English Name($name) is not match")
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.EN_NAME_COMBINATION_ERROR))
                }
                if (projectDao.existByEnglishName(dslContext, name, projectId)) {
                    throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.EN_NAME_EXIST))
                }
            }
        }
    }

    /**
     * 创建项目信息
     */
    override fun create(userId: String, projectCreateInfo: ProjectCreateInfo, accessToken: String?, isUserProject: Boolean?): String {
        logger.info("create project| $userId | $accessToken| $isUserProject | $projectCreateInfo")
        if (isUserProject!!) {
            validate(ProjectValidateType.project_name, projectCreateInfo.projectName)
            validate(ProjectValidateType.english_name, projectCreateInfo.englishName)
        }

        // 随机生成首字母图片
        val logoFile = drawFile(projectCreateInfo.englishName)
        try {
            // 保存Logo文件
            val logoAddress = saveLogoAddress(userId, projectCreateInfo.englishName, logoFile)
            val userDeptDetail = getDeptInfo(userId)
            var projectId = ""
            try {
                if(isUserProject!!) {
                    // 注册项目到权限中心
                    projectId = projectPermissionService.createResources(
                            userId = userId,
                            accessToken = accessToken,
                            resourceRegisterInfo = ResourceRegisterInfo(
                                    resourceCode = projectCreateInfo.englishName,
                                    resourceName = projectCreateInfo.projectName
                            ),
                            userDeptDetail = userDeptDetail
                    )
                }
            } catch (e: PermissionForbiddenException) {
                throw e
            } catch (e: Exception) {
                logger.warn("权限中心创建项目信息： $projectCreateInfo", e)
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_FAIL))
            }
            if (projectId.isNullOrEmpty()) {
                projectId = UUIDUtil.generate()
            }

            try {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    projectDao.create(context, userId, logoAddress, projectCreateInfo, userDeptDetail, projectId)

                    try{
                        createExtProjectInfo(
                                userId = userId,
                                projectId = projectId,
                                accessToken = accessToken,
                                projectCreateInfo = projectCreateInfo,
                                isUserProject = isUserProject
                        )
                    } catch (e: Exception) {
                        logger.warn("fail to create the project[$projectId] ext info $projectCreateInfo", e)
                        projectDao.delete(dslContext, projectId)
                        throw e
                    }
                }
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectCreateInfo", e)
                if(isUserProject) {
                    deleteAuth(projectId, accessToken)
                }
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
            } catch (ignored: Throwable) {
                logger.warn(
                    "Fail to create the project ($projectCreateInfo)",
                    ignored
                )
                if(isUserProject) {
                    deleteAuth(projectId, accessToken)
                }
                throw ignored
            }
            return projectId
        } finally {
            if (logoFile.exists()) {
                logoFile.delete()
            }
        }
    }

    // 内部版独立实现
    override fun getByEnglishName(englishName: String, accessToken: String?): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return ProjectUtils.packagingBean(record, grayProjectSet())
    }

    override fun getByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return ProjectUtils.packagingBean(record, grayProjectSet())
    }

    override fun update(userId: String, englishName: String, projectUpdateInfo: ProjectUpdateInfo, accessToken: String?): Boolean {
        validate(ProjectValidateType.project_name, projectUpdateInfo.projectName, projectUpdateInfo.englishName)
        val startEpoch = System.currentTimeMillis()
        var success = false
        validatePermission(projectUpdateInfo.englishName, userId, AuthPermission.EDIT)
        try {
            updateInfoReplace(projectUpdateInfo)
            try {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    val projectId = projectDao.getByEnglishName(dslContext, englishName)?.projectId ?: throw RuntimeException("项目 -$englishName 不存在")
                    projectDao.update(context, userId, projectId!!, projectUpdateInfo)
                    projectPermissionService.modifyResource(
                        projectCode = projectUpdateInfo.englishName,
                        projectName = projectUpdateInfo.projectName
                    )
                    projectDispatcher.dispatch(ProjectUpdateBroadCastEvent(
                        userId = userId,
                        projectId = englishName,
                        projectInfo = projectUpdateInfo
                    ))
                }
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectUpdateInfo", e)
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
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
    override fun list(userId: String, accessToken: String?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {

            val projects = getProjectFromAuth(userId, accessToken)
            logger.info("项目列表：$projects")
            val list = ArrayList<ProjectVO>()
            projectDao.list(dslContext, projects).map {
                list.add(ProjectUtils.packagingBean(it, grayProjectSet()))
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
                list.add(ProjectUtils.packagingBean(it, grayProjectSet))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun listOnlyByProjectCode(projectCodes: Set<String>): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = ArrayList<ProjectVO>()

            val grayProjectSet = grayProjectSet()

            projectDao.listByCodes(dslContext, projectCodes).map {
                list.add(ProjectUtils.packagingBean(it, grayProjectSet))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    /**
     * 获取所有项目信息
     */
    override fun list(userId: String): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {

            val projects = projectPermissionService.getUserProjects(userId)
            logger.info("项目列表：$projects")
            val list = ArrayList<ProjectVO>()
            projectDao.listByEnglishName(dslContext, projects).map {
                list.add(ProjectUtils.packagingBean(it, grayProjectSet()))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    /**
     * 根据有序projectCode列表获取有序项目信息列表
     * 不过滤已删除项目，调用业务端根据enable过滤
     */
    override fun list(projectCodes: List<String>): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = ArrayList<ProjectVO>()
            val grayProjectSet = grayProjectSet()

            projectCodes.forEach {
                // 多次查询保证有序
                val projectRecord =
                    projectDao.getByEnglishName(dslContext, it) ?: throw ProjectNotExistException("projectCode=$it")
                list.add(ProjectUtils.packagingBean(projectRecord, grayProjectSet))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun getAllProject(): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val list = ArrayList<ProjectVO>()
            projectDao.getAllProject(dslContext).filter { it.enabled == null || it.enabled }.map {
                list.add(ProjectUtils.packagingBean(it, emptySet()))
            }
            success = true
            return list
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun list(limit: Int, offset: Int): Page<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        val pageNotNull = limit ?: 1
        val pageSizeNotNull = offset ?: 20
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        var success = false
        try {
            val list = ArrayList<ProjectVO>()
            projectDao.list(dslContext, sqlLimit.limit, sqlLimit.offset).map {
                list.add(ProjectUtils.packagingBean(it, emptySet()))
            }
            val count = projectDao.getCount(dslContext)
            success = true
            logger.info("list count$count")
            return Page(
                count = count,
                page = limit,
                pageSize = offset,
                records = list
            )
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
                list.add(ProjectUtils.packagingBean(it, grayProjectSet))
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

    override fun grayProjectSet() = gray.grayProjectSet(redisOperation)

    override fun updateLogo(
        userId: String,
        englishName: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        accessToken: String?
    ): Result<ProjectLogo> {
        logger.info("Update the logo of project $englishName")
        val projectRecord = projectDao.getByEnglishName(dslContext, englishName)
        if (projectRecord != null) {
            var logoFile: File? = null
            try {
                logoFile = FileUtil.convertTempFile(inputStream)
                val logoAddress = saveLogoAddress(userId, projectRecord.englishName, logoFile)
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    projectDao.updateLogoAddress(context, userId, projectRecord.projectId, logoAddress)
                    projectDispatcher.dispatch(ProjectUpdateLogoBroadCastEvent(
                        userId = userId,
                        projectId = projectRecord.projectId,
                        logoAddr = logoAddress
                    ))
                }
                return Result(ProjectLogo(logoAddress))
            } catch (e: Exception) {
                logger.warn("fail update projectLogo", e)
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.UPDATE_LOGO_FAIL))
            } finally {
                logoFile?.delete()
            }
        } else {
            logger.warn("$projectRecord is null or $projectRecord is empty")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.QUERY_PROJECT_FAIL))
        }
    }

    override fun updateUsableStatus(userId: String, englishName: String, enabled: Boolean) {
        logger.info("updateUsableStatus userId[$userId], englishName[$englishName] , enabled[$enabled]")

        val projectInfo = projectDao.getByEnglishName(dslContext, englishName) ?: throw RuntimeException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST))
        val verify = projectPermissionService.verifyUserProjectPermission(
                userId = userId,
                projectCode = englishName,
                permission = AuthPermission.MANAGE
        )
        if (!verify) {
            logger.info("$englishName| $userId| ${AuthPermission.DELETE} validatePermission fail")
            throw PermissionForbiddenException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL))
        }
        logger.info("updateUsableStatus userId[$userId], projectInfo[${projectInfo.projectId}]")
        projectDao.updateUsableStatus(
                dslContext = dslContext,
                userId = userId,
                projectId = projectInfo.projectId,
                enabled = enabled
        )
    }

    private fun validatePermission(projectCode: String, userId: String, permission: AuthPermission): Boolean {
        val validate = projectPermissionService.verifyUserProjectPermission(
                projectCode = projectCode,
                userId = userId,
                permission = permission
        )
        if (!validate) {
            logger.warn("$projectCode| $userId| ${permission.value} validatePermission fail")
            throw PermissionForbiddenException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL))
        }
        return true
    }

    abstract fun getDeptInfo(userId: String) : UserDeptDetail

    abstract fun createExtProjectInfo(userId: String, projectId: String, accessToken: String?, projectCreateInfo: ProjectCreateInfo, isUserProject: Boolean?)

    abstract fun saveLogoAddress(userId: String, projectCode: String, file: File): String

    abstract fun deleteAuth(projectId: String, accessToken: String?)

    abstract fun getProjectFromAuth(userId: String?, accessToken: String?): Set<String>

    abstract fun updateInfoReplace(projectUpdateInfo: ProjectUpdateInfo)

    abstract fun drawFile(projectCode: String): File

    companion object {
        const val Width = 128
        const val Height = 128
        private val logger = LoggerFactory.getLogger(AbsProjectServiceImpl::class.java)!!
        private const val ENGLISH_NAME_PATTERN = "[a-z][a-zA-Z0-9]+"
    }
}
