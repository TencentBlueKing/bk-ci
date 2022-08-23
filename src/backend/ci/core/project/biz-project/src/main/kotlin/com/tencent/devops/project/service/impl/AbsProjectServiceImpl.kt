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

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.SECRECY_PROJECT_REDIS_KEY
import com.tencent.devops.project.constant.ProjectConstant.NAME_MAX_LENGTH
import com.tencent.devops.project.constant.ProjectConstant.NAME_MIN_LENGTH
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.jmx.api.ProjectJmxApi.Companion.PROJECT_LIST
import com.tencent.devops.project.pojo.ProjectBaseInfo
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectLogo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.service.ShardingRoutingRuleAssignService
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
import java.util.regex.Pattern
import javax.ws.rs.NotFoundException

@Suppress("ALL")
abstract class AbsProjectServiceImpl @Autowired constructor(
    val projectPermissionService: ProjectPermissionService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val projectJmxApi: ProjectJmxApi,
    val redisOperation: RedisOperation,
    val client: Client,
    private val projectDispatcher: ProjectDispatcher,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val shardingRoutingRuleAssignService: ShardingRoutingRuleAssignService
) : ProjectService {

    override fun validate(validateType: ProjectValidateType, name: String, projectId: String?) {
        if (name.isBlank()) {
            throw ErrorCodeException(
                defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NAME_EMPTY),
                errorCode = ProjectMessageCode.NAME_EMPTY
            )
        }
        when (validateType) {
            ProjectValidateType.project_name -> {
                if (name.isEmpty() || name.length > NAME_MAX_LENGTH) {
                    throw ErrorCodeException(
                        defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NAME_TOO_LONG),
                        errorCode = ProjectMessageCode.NAME_TOO_LONG
                    )
                }
                if (projectDao.existByProjectName(dslContext, name, projectId)) {
                    throw ErrorCodeException(
                        defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST),
                        errorCode = ProjectMessageCode.PROJECT_NAME_EXIST
                    )
                }
            }
            ProjectValidateType.english_name -> {
                // 2 ~ 64 个字符+数字，以小写字母开头
                if (name.length < NAME_MIN_LENGTH || name.length > NAME_MAX_LENGTH) {
                    throw ErrorCodeException(
                        defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.EN_NAME_INTERVAL_ERROR),
                        errorCode = ProjectMessageCode.EN_NAME_INTERVAL_ERROR
                    )
                }
                if (!Pattern.matches(ENGLISH_NAME_PATTERN, name)) {
                    logger.warn("Project English Name($name) is not match")
                    throw ErrorCodeException(
                        defaultMessage = MessageCodeUtil.getCodeLanMessage(
                            ProjectMessageCode.EN_NAME_COMBINATION_ERROR),
                        errorCode = ProjectMessageCode.EN_NAME_COMBINATION_ERROR
                    )
                }
                if (projectDao.existByEnglishName(dslContext, name, projectId)) {
                    throw ErrorCodeException(
                        defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.EN_NAME_EXIST),
                        errorCode = ProjectMessageCode.EN_NAME_EXIST
                    )
                }
            }
        }
    }

    /**
     * 创建项目信息
     */
    override fun create(
        userId: String,
        projectCreateInfo: ProjectCreateInfo,
        accessToken: String?,
        createExtInfo: ProjectCreateExtInfo,
        defaultProjectId: String?,
        projectChannel: ProjectChannelCode
    ): String {
        logger.info("create project| $userId | $accessToken| $createExtInfo | $projectCreateInfo")
        if (createExtInfo.needValidate!!) {
            validate(ProjectValidateType.project_name, projectCreateInfo.projectName)
            validate(ProjectValidateType.english_name, projectCreateInfo.englishName)
        }

        val userDeptDetail = getDeptInfo(userId)
        var projectId = defaultProjectId
        try {
            if (createExtInfo.needAuth!!) {
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
                val projectInfo = organizationMarkUp(projectCreateInfo, userDeptDetail)
                val context = DSL.using(configuration)
                projectDao.create(
                    dslContext = context,
                    userId = userId,
                    logoAddress = "",
                    projectCreateInfo = projectInfo,
                    userDeptDetail = userDeptDetail,
                    projectId = projectId,
                    channelCode = projectChannel
                )

                try {
                    createExtProjectInfo(
                        userId = userId,
                        projectId = projectId,
                        accessToken = accessToken,
                        projectCreateInfo = projectInfo,
                        createExtInfo = createExtInfo
                    )
                } catch (e: Exception) {
                    logger.warn("fail to create the project[$projectId] ext info $projectCreateInfo", e)
                    projectDao.delete(dslContext, projectId)
                    throw e
                }
                // 为项目分配数据源
                shardingRoutingRuleAssignService.assignShardingRoutingRule(
                    channelCode = projectChannel,
                    routingName = projectCreateInfo.englishName,
                    moduleCodes = listOf(SystemModuleEnum.PROCESS, SystemModuleEnum.METRICS)
                )
                if (projectInfo.secrecy) {
                    redisOperation.addSetValue(SECRECY_PROJECT_REDIS_KEY, projectInfo.englishName)
                }
            }
        } catch (e: DuplicateKeyException) {
            logger.warn("Duplicate project $projectCreateInfo", e)
            if (createExtInfo.needAuth) {
                deleteAuth(projectId, accessToken)
            }
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
        } catch (ignored: Throwable) {
            logger.warn(
                "Fail to create the project ($projectCreateInfo)",
                ignored
            )
            if (createExtInfo.needAuth) {
                deleteAuth(projectId, accessToken)
            }
            throw ignored
        }
        return projectId
    }

    override fun createExtProject(
        userId: String,
        projectCode: String,
        projectCreateInfo: ProjectCreateInfo,
        needAuth: Boolean,
        needValidate: Boolean,
        channel: ProjectChannelCode
    ): ProjectVO? {
        if (getByEnglishName(projectCode) == null) {
            logger.warn("createExtProject $projectCode exist")
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NAME_EXIST,
                defaultMessage = MessageCodeUtil.getCodeLanMessage(
                    ProjectMessageCode.PROJECT_NAME_EXIST
                )
            )
        }
        val projectCreateExtInfo = ProjectCreateExtInfo(
            needValidate = needValidate,
            needAuth = needAuth
        )
        create(
            userId = userId,
            projectChannel = channel,
            projectCreateInfo = projectCreateInfo,
            accessToken = null,
            defaultProjectId = projectCode,
            createExtInfo = projectCreateExtInfo
        )
        return getByEnglishName(projectCode)
    }

    // 内部版独立实现
    override fun getByEnglishName(userId: String, englishName: String, accessToken: String?): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return ProjectUtils.packagingBean(record)
    }

    override fun getByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return ProjectUtils.packagingBean(record)
    }

    override fun update(
        userId: String,
        englishName: String,
        projectUpdateInfo: ProjectUpdateInfo,
        accessToken: String?
    ): Boolean {
        validate(
            validateType = ProjectValidateType.project_name,
            name = projectUpdateInfo.projectName,
            projectId = projectUpdateInfo.englishName
        )
        val startEpoch = System.currentTimeMillis()
        var success = false
        validatePermission(projectUpdateInfo.englishName, userId, AuthPermission.EDIT)
        try {
            updateInfoReplace(projectUpdateInfo)
            try {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    val projectId = projectDao.getByEnglishName(
                        dslContext = dslContext,
                        englishName = englishName
                    )?.projectId ?: throw NotFoundException("项目 -$englishName 不存在")
                    projectDao.update(
                        dslContext = context,
                        userId = userId,
                        projectId = projectId,
                        projectUpdateInfo = projectUpdateInfo
                    )
                    modifyProjectAuthResource(
                        projectUpdateInfo.englishName,
                        projectUpdateInfo.projectName
                    )
                    if (!projectUpdateInfo.secrecy) {
                        redisOperation.removeSetMember(SECRECY_PROJECT_REDIS_KEY, projectUpdateInfo.englishName)
                    } else {
                        redisOperation.addSetValue(SECRECY_PROJECT_REDIS_KEY, projectUpdateInfo.englishName)
                    }
                    projectDispatcher.dispatch(ProjectUpdateBroadCastEvent(
                        userId = userId,
                        projectId = projectId,
                        projectInfo = projectUpdateInfo
                    ))
                }
                success = true
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectUpdateInfo", e)
                throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
            }
        } finally {
            projectJmxApi.execute(ProjectJmxApi.PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
        return success
    }

    /**
     * 获取所有项目信息
     */
    override fun list(userId: String, accessToken: String?, enabled: Boolean?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {

            val projects = getProjectFromAuth(userId, accessToken)
            if (projects.isEmpty()) {
                return emptyList()
            }
            val list = ArrayList<ProjectVO>()
            projectDao.listByEnglishName(
                dslContext = dslContext,
                englishNameList = projects,
                offset = null,
                limit = null,
                searchName = null,
                enabled = enabled
            ).map {
                list.add(ProjectUtils.packagingBean(it))
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

            projectDao.listByCodes(dslContext, projectCodes, enabled = true).map {
                list.add(ProjectUtils.packagingBean(it))
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

            projectDao.listByCodes(dslContext, projectCodes, enabled = null).map {
                list.add(ProjectUtils.packagingBean(it))
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

            val projects = getProjectFromAuth(userId, null)
            logger.info("项目列表：$projects")
            val list = ArrayList<ProjectVO>()
            projectDao.listByEnglishName(dslContext, projects, null, null, null).map {
                list.add(ProjectUtils.packagingBean(it))
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

            projectCodes.forEach {
                // 多次查询保证有序
                val projectRecord =
                    projectDao.getByEnglishName(dslContext, it) ?: throw ProjectNotExistException("projectCode=$it")
                list.add(ProjectUtils.packagingBean(projectRecord))
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
                list.add(ProjectUtils.packagingBean(it))
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
        var success = false
        try {
            val list = ArrayList<ProjectVO>()
            projectDao.list(dslContext, limit, offset).map {
                list.add(ProjectUtils.packagingBean(it))
            }
            val count = projectDao.getCount(dslContext)
            success = true
            logger.info("list count : $count")
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

    override fun listByChannel(limit: Int, offset: Int, projectChannelCode: ProjectChannelCode): Page<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        try {
            val list = ArrayList<ProjectVO>()
            projectDao.listByChannel(dslContext, limit, offset, projectChannelCode).map {
                list.add(ProjectUtils.packagingBean(it))
            }
            val count = projectDao.getCount(dslContext)
            logger.info("list count : $count")
            return Page(
                count = count,
                page = limit,
                pageSize = offset,
                records = list
            )
        } finally {
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

            projectDao.listByCodes(dslContext, projectCodes.toSet(), enabled = true).map {
                list.add(ProjectUtils.packagingBean(it))
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
        projectDao.listByCodes(dslContext, projectCodes.split(",").toSet(), enabled = null).map {
            map.put(it.englishName, it.projectName)
        }
        return map
    }

    override fun updateLogo(
        userId: String,
        englishName: String /* englishName is projectId */,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        accessToken: String?
    ): Result<ProjectLogo> {
        logger.info("Update the logo of project : englishName = $englishName")
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
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.QUERY_PROJECT_FAIL))
        }
    }

    override fun updateProjectName(userId: String, projectId: String, projectName: String): Boolean {
        if (projectName.isEmpty() || projectName.length > MAX_PROJECT_NAME_LENGTH) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.NAME_TOO_LONG,
                defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NAME_TOO_LONG)
            )
        }
        if (projectDao.existByProjectName(dslContext, projectName, projectId)) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NAME_EXIST,
                defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST)
            )
        }
        return projectDao.updateProjectName(dslContext, projectId, projectName) > 0
    }

    override fun updateUsableStatus(userId: String, englishName: String, enabled: Boolean) {
        logger.info("updateUsableStatus userId[$userId], englishName[$englishName] , enabled[$enabled]")

        val projectInfo = projectDao.getByEnglishName(dslContext, englishName)
            ?: throw ErrorCodeException(errorCode = ProjectMessageCode.PROJECT_NOT_EXIST)
        val verify = validatePermission(
            userId = userId,
            projectCode = englishName,
            permission = AuthPermission.MANAGE
        )
        if (!verify) {
            logger.info("$englishName| $userId| ${AuthPermission.DELETE} validatePermission fail")
            throw PermissionForbiddenException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL))
        }
        projectDao.updateUsableStatus(
            dslContext = dslContext,
            userId = userId,
            projectId = projectInfo.projectId,
            enabled = enabled
        )
    }

    override fun searchProjectByProjectName(projectName: String, limit: Int, offset: Int): Page<ProjectVO> {
        val startTime = System.currentTimeMillis()
        val list = mutableListOf<ProjectVO>()
        projectDao.searchByProjectName(
            dslContext = dslContext,
            projectName = projectName,
            limit = limit,
            offset = offset
        ).map {
            list.add(ProjectUtils.packagingBean(it))
        }
        val count = projectDao.countByProjectName(dslContext, projectName).toLong()
        LogUtils.costTime("search project by projectName", startTime)
        return Page(
            count = count,
            page = offset,
            pageSize = limit,
            records = list
        )
    }

    override fun hasCreatePermission(userId: String): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = projectAuthServiceCode,
            resourceType = AuthResourceType.PROJECT,
            projectCode = "",
            permission = AuthPermission.CREATE
        )
    }

    override fun getMinId(): Long {
        return projectDao.getMinId(dslContext)
    }

    override fun getMaxId(): Long {
        return projectDao.getMaxId(dslContext)
    }

    override fun getProjectListById(
        minId: Long,
        maxId: Long
    ): List<ProjectBaseInfo> {
        val list = ArrayList<ProjectBaseInfo>()
        projectDao.getProjectListById(dslContext, minId, maxId)?.map {
            list.add(
                ProjectBaseInfo(
                    id = it["ID"] as Long,
                    englishName = it["ENGLISH_NAME"] as String
                )
            )
        }
        return list
    }

    override fun verifyUserProjectPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        accessToken: String?
    ): Boolean {
        return validatePermission(projectId, userId, permission)
    }

    override fun listSecrecyProject(): Set<String>? {
        var projectIds = redisOperation.getSetMembers(SECRECY_PROJECT_REDIS_KEY)
        if (projectIds.isNullOrEmpty()) {
            projectIds = projectDao.listSecrecyProject(dslContext)?.map { it.value1() }?.toSet()
            if (projectIds != null) {
                redisOperation.sadd(SECRECY_PROJECT_REDIS_KEY, *projectIds.toTypedArray())
            }
        }
        return projectIds
    }

    override fun relationIamProject(projectCode: String, relationId: String): Boolean {
        val projectInfo = projectDao.getByEnglishName(dslContext, projectCode) ?: throw InvalidParamException("项目不存在")
        val currentRelationId = projectInfo.relationId
        if (!currentRelationId.isNullOrEmpty()) {
            throw InvalidParamException("$projectCode 已绑定IAM分级管理员")
        }
        val updateCount = projectDao.updateRelationByCode(dslContext, projectCode, relationId)
        return updateCount > 0
    }

    override fun getProjectByName(projectName: String): ProjectVO? {
        return projectDao.getProjectByName(dslContext, projectName)
    }

    abstract fun validatePermission(projectCode: String, userId: String, permission: AuthPermission): Boolean

    abstract fun getDeptInfo(userId: String): UserDeptDetail

    abstract fun createExtProjectInfo(
        userId: String,
        projectId: String,
        accessToken: String?,
        projectCreateInfo: ProjectCreateInfo,
        createExtInfo: ProjectCreateExtInfo
    )

    abstract fun saveLogoAddress(userId: String, projectCode: String, logoFile: File): String

    abstract fun deleteAuth(projectId: String, accessToken: String?)

    abstract fun getProjectFromAuth(userId: String?, accessToken: String?): List<String>

    abstract fun updateInfoReplace(projectUpdateInfo: ProjectUpdateInfo)

    abstract fun organizationMarkUp(
        projectCreateInfo: ProjectCreateInfo,
        userDeptDetail: UserDeptDetail
    ): ProjectCreateInfo

    abstract fun modifyProjectAuthResource(projectCode: String, projectName: String)

    companion object {
        const val MAX_PROJECT_NAME_LENGTH = 64
        private val logger = LoggerFactory.getLogger(AbsProjectServiceImpl::class.java)!!
        private const val ENGLISH_NAME_PATTERN = "[a-z][a-zA-Z0-9-]+"
    }
}
