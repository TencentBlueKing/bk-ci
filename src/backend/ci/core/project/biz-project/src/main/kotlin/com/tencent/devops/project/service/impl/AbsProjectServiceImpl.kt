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

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.SECRECY_PROJECT_REDIS_KEY
import com.tencent.devops.project.constant.ProjectConstant.NAME_MAX_LENGTH
import com.tencent.devops.project.constant.ProjectConstant.NAME_MIN_LENGTH
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.constant.ProjectMessageCode.BOUND_IAM_GRADIENT_ADMIN
import com.tencent.devops.project.constant.ProjectMessageCode.PROJECT_NOT_EXIST
import com.tencent.devops.project.constant.ProjectMessageCode.UNDER_APPROVAL_PROJECT
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.jmx.api.ProjectJmxApi.Companion.PROJECT_LIST
import com.tencent.devops.project.pojo.AuthProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectBaseInfo
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectDiffVO
import com.tencent.devops.project.pojo.ProjectLogo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.ProjectWithPermission
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectTipsStatus
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectApprovalService
import com.tencent.devops.project.service.ProjectExtService
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.service.ShardingRoutingRuleAssignService
import com.tencent.devops.project.util.ProjectUtils
import com.tencent.devops.project.util.exception.ProjectNotExistException
import java.io.File
import java.io.InputStream
import java.util.regex.Pattern
import javax.ws.rs.NotFoundException
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException

@Suppress("ALL")
abstract class AbsProjectServiceImpl @Autowired constructor(
    val projectPermissionService: ProjectPermissionService,
    val dslContext: DSLContext,
    val projectDao: ProjectDao,
    private val projectJmxApi: ProjectJmxApi,
    val redisOperation: RedisOperation,
    val client: Client,
    private val projectDispatcher: ProjectDispatcher,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val shardingRoutingRuleAssignService: ShardingRoutingRuleAssignService,
    private val objectMapper: ObjectMapper,
    private val projectExtService: ProjectExtService,
    private val projectApprovalService: ProjectApprovalService
) : ProjectService {

    override fun validate(validateType: ProjectValidateType, name: String, projectId: String?) {
        if (name.isBlank()) {
            throw ErrorCodeException(errorCode = ProjectMessageCode.NAME_EMPTY)
        }
        when (validateType) {
            ProjectValidateType.project_name -> {
                if (name.isEmpty() || name.length > NAME_MAX_LENGTH) {
                    throw ErrorCodeException(
                        errorCode = ProjectMessageCode.NAME_TOO_LONG
                    )
                }
                if (projectDao.existByProjectName(dslContext, name, projectId)) {
                    throw ErrorCodeException(errorCode = ProjectMessageCode.PROJECT_NAME_EXIST)
                }
            }
            ProjectValidateType.english_name -> {
                // 2 ~ 64 个字符+数字，以小写字母开头
                if (name.length < NAME_MIN_LENGTH || name.length > NAME_MAX_LENGTH) {
                    throw ErrorCodeException(
                        errorCode = ProjectMessageCode.EN_NAME_INTERVAL_ERROR
                    )
                }
                if (!Pattern.matches(ENGLISH_NAME_PATTERN, name)) {
                    logger.warn("Project English Name($name) is not match")
                    throw ErrorCodeException(
                        errorCode = ProjectMessageCode.EN_NAME_COMBINATION_ERROR
                    )
                }
                if (projectDao.existByEnglishName(dslContext, name, projectId)) {
                    throw ErrorCodeException(
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
        val subjectScopes = projectCreateInfo.subjectScopes!!.ifEmpty {
            listOf(SubjectScopeInfo(id = ALL_MEMBERS, type = ALL_MEMBERS, name = getAllMembersName()))
        }
        val needApproval = projectPermissionService.needApproval(createExtInfo.needApproval)
        val approvalStatus = if (needApproval) {
            ProjectApproveStatus.CREATE_PENDING.status
        } else {
            ProjectApproveStatus.APPROVED.status
        }
        val projectInfo = organizationMarkUp(projectCreateInfo, userDeptDetail)
        try {
            if (createExtInfo.needAuth!!) {
                val authProjectCreateInfo = AuthProjectCreateInfo(
                    userId = userId,
                    accessToken = accessToken,
                    userDeptDetail = userDeptDetail,
                    subjectScopes = subjectScopes,
                    projectCreateInfo = projectCreateInfo,
                    approvalStatus = approvalStatus
                )
                // 注册项目到权限中心
                projectId = projectPermissionService.createResources(
                    resourceRegisterInfo = ResourceRegisterInfo(
                        resourceCode = projectCreateInfo.englishName,
                        resourceName = projectCreateInfo.projectName
                    ),
                    authProjectCreateInfo = authProjectCreateInfo
                )
            }
        } catch (e: PermissionForbiddenException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to create project in permission center： $projectCreateInfo | ${e.message}")
            throw OperationException(
                I18nUtil.getCodeLanMessage(ProjectMessageCode.PEM_CREATE_FAIL)
            )
        }
        if (projectId.isNullOrEmpty()) {
            projectId = UUIDUtil.generate()
        }
        try {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val subjectScopesStr = objectMapper.writeValueAsString(subjectScopes)
                val logoAddress = projectCreateInfo.logoAddress
                projectDao.create(
                    dslContext = context,
                    userId = userId,
                    logoAddress = logoAddress,
                    projectCreateInfo = projectInfo,
                    userDeptDetail = userDeptDetail,
                    projectId = projectId,
                    channelCode = projectChannel,
                    approvalStatus = approvalStatus,
                    subjectScopesStr = subjectScopesStr
                )
                if (!needApproval) {
                    projectExtService.createExtProjectInfo(
                        userId = userId,
                        authProjectId = projectId,
                        accessToken = accessToken,
                        projectCreateInfo = projectInfo,
                        createExtInfo = createExtInfo,
                        logoAddress = logoAddress
                    )
                }
                // 为项目分配数据源
                shardingRoutingRuleAssignService.assignShardingRoutingRule(
                    channelCode = projectChannel,
                    routingName = projectInfo.englishName,
                    moduleCodes = listOf(SystemModuleEnum.PROCESS, SystemModuleEnum.METRICS)
                )
                if (projectInfo.secrecy) {
                    redisOperation.addSetValue(SECRECY_PROJECT_REDIS_KEY, projectInfo.englishName)
                }
            }
            updateProjectRouterTag(projectCreateInfo.englishName)
        } catch (e: DuplicateKeyException) {
            logger.warn("Duplicate project $projectCreateInfo", e)
            if (createExtInfo.needAuth) {
                // todo 待确定，切换v3-RBAC后，是否需要做其他操作
                deleteAuth(projectId, accessToken)
            }
            throw OperationException(I18nUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
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
        if (getByEnglishName(projectCode) != null) {
            logger.warn("createExtProject $projectCode exist")
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NAME_EXIST
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
        updateProjectProperties(
            userId = userId,
            projectCode = projectCode,
            properties = projectCreateInfo.properties
                ?: ProjectProperties(PipelineAsCodeSettings(true))
        )
        return getByEnglishName(projectCode)
    }

    // 内部版独立实现
    override fun getByEnglishName(
        userId: String,
        englishName: String,
        accessToken: String?
    ): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        val projectVO = ProjectUtils.packagingBean(record)
        val englishNames = getProjectFromAuth(userId, accessToken)
        if (englishNames.isEmpty()) {
            return null
        }
        if (!englishNames.contains(projectVO.englishName)) {
            logger.warn("The user don't have the permission to get the project $englishName")
            return null
        }
        return projectVO
    }

    override fun show(userId: String, englishName: String, accessToken: String?): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        val projectInfo = ProjectUtils.packagingBean(record)
        val approvalStatus = ProjectApproveStatus.parse(projectInfo.approvalStatus)
        if (approvalStatus.isCreatePending() && record.creator != userId) {
            throw ErrorCodeException(
                errorCode = UNDER_APPROVAL_PROJECT,
                params = arrayOf(englishName),
                defaultMessage = "project {0} is being approved, please wait patiently, or contact the approver"
            )
        }
        if (approvalStatus.isSuccess()) {
            val verify = validatePermission(
                userId = userId,
                projectCode = englishName,
                permission = AuthPermission.VIEW
            )
            if (!verify) {
                logger.info("$englishName| $userId| ${AuthPermission.VIEW} validatePermission fail")
                throw PermissionForbiddenException(I18nUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL))
            }
        }
        val tipsStatus = getAndUpdateTipsStatus(userId = userId, projectId = englishName)
        return projectInfo.copy(tipsStatus = tipsStatus)
    }

    protected fun getAndUpdateTipsStatus(userId: String, projectId: String): Int {
        val projectApprovalInfo = projectApprovalService.get(projectId) ?: return ProjectTipsStatus.NOT_SHOW.status
        return with(projectApprovalInfo) {
            // 项目创建成功和编辑审批成功,只有第一次进入页面需要展示tips,后面都不需要展示
            val needUpdateTipsStatus = approvalStatus == ProjectApproveStatus.APPROVED.status &&
                updator == userId &&
                tipsStatus != ProjectTipsStatus.NOT_SHOW.status
            // 只有第一次进来需要展示,后面再进来不需要再展示
            if (needUpdateTipsStatus) {
                logger.info("update project tips status|$userId|$projectId")
                projectApprovalService.updateTipsStatus(
                    projectId = projectId,
                    tipsStatus = ProjectTipsStatus.NOT_SHOW.status
                )
            }
            tipsStatus
        }
    }

    override fun diff(userId: String, englishName: String, accessToken: String?): ProjectDiffVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        val projectApprovalInfo = projectApprovalService.get(englishName)
        return ProjectUtils.packagingBean(record, projectApprovalInfo)
    }

    override fun getByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return ProjectUtils.packagingBean(record)
    }

    override fun update(
        userId: String,
        englishName: String,
        projectUpdateInfo: ProjectUpdateInfo,
        accessToken: String?,
        needApproval: Boolean?
    ): Boolean {
        validate(
            validateType = ProjectValidateType.project_name,
            name = projectUpdateInfo.projectName,
            projectId = projectUpdateInfo.englishName
        )
        val startEpoch = System.currentTimeMillis()
        var success = false
        val subjectScopes = projectUpdateInfo.subjectScopes!!.ifEmpty {
            listOf(SubjectScopeInfo(id = ALL_MEMBERS, type = ALL_MEMBERS, name = getAllMembersName()))
        }
        val subjectScopesStr = objectMapper.writeValueAsString(subjectScopes)
        logger.info(
            "update project : $userId | $englishName | $projectUpdateInfo | " +
                "$needApproval | $subjectScopes"
        )
        try {
            try {
                val projectInfo = projectDao.getByEnglishName(
                    dslContext = dslContext,
                    englishName = englishName
                ) ?: throw NotFoundException("project - $englishName is not exist!")
                val approvalStatus = ProjectApproveStatus.parse(projectInfo.approvalStatus)
                if (approvalStatus.isSuccess() || projectInfo.creator != userId) {
                    val verify = validatePermission(projectUpdateInfo.englishName, userId, AuthPermission.EDIT)
                    if (!verify) {
                        logger.info("$englishName| $userId| ${AuthPermission.EDIT} validatePermission fail")
                        throw PermissionForbiddenException(
                            I18nUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL)
                        )
                    }
                }
                // 判断是否需要审批,只有修改最大授权范围和权限敏感才需要审批
                val (finalNeedApproval, newApprovalStatus) = getUpdateApprovalStatus(
                    needApproval = needApproval,
                    projectInfo = projectInfo,
                    subjectScopesStr = subjectScopesStr,
                    projectUpdateInfo = projectUpdateInfo
                )
                val projectId = projectInfo.projectId
                val logoAddress = projectUpdateInfo.logoAddress
                val resourceUpdateInfo = ResourceUpdateInfo(
                    userId = userId,
                    projectUpdateInfo = projectUpdateInfo,
                    needApproval = needApproval!!,
                    subjectScopes = subjectScopes,
                    approvalStatus = newApprovalStatus
                )
                modifyProjectAuthResource(resourceUpdateInfo)
                if (finalNeedApproval) {
                    updateApprovalInfo(
                        userId = userId,
                        projectId = projectId,
                        projectUpdateInfo = projectUpdateInfo,
                        subjectScopesStr = subjectScopesStr,
                        logoAddress = logoAddress,
                        approvalStatus = newApprovalStatus
                    )
                } else {
                    dslContext.transaction { configuration ->
                        val context = DSL.using(configuration)
                        projectDao.update(
                            dslContext = context,
                            userId = userId,
                            projectId = projectId,
                            projectUpdateInfo = projectUpdateInfo,
                            subjectScopesStr = subjectScopesStr,
                            logoAddress = logoAddress
                        )
                        projectDispatcher.dispatch(
                            ProjectUpdateBroadCastEvent(
                                userId = userId,
                                projectId = projectId,
                                projectInfo = projectUpdateInfo
                            )
                        )
                        if (logoAddress != null) {
                            projectDispatcher.dispatch(
                                ProjectUpdateLogoBroadCastEvent(
                                    userId = userId,
                                    projectId = projectId,
                                    logoAddr = logoAddress
                                )
                            )
                        }
                    }
                }
                if (!projectUpdateInfo.secrecy) {
                    redisOperation.removeSetMember(SECRECY_PROJECT_REDIS_KEY, projectUpdateInfo.englishName)
                } else {
                    redisOperation.addSetValue(SECRECY_PROJECT_REDIS_KEY, projectUpdateInfo.englishName)
                }
                success = true
            } catch (e: DuplicateKeyException) {
                logger.warn("Duplicate project $projectUpdateInfo", e)
                throw OperationException(I18nUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST))
            } catch (e: Exception) {
                logger.warn("update project failed :$projectUpdateInfo", e)
                throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProjectMessageCode.PROJECT_UPDATE_FAIL,
                        defaultMessage = "update project failed: $e "
                    )
                )
            }
        } finally {
            projectJmxApi.execute(ProjectJmxApi.PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
        return success
    }

    private fun getUpdateApprovalStatus(
        needApproval: Boolean?,
        projectInfo: TProjectRecord,
        subjectScopesStr: String,
        projectUpdateInfo: ProjectUpdateInfo
    ): Pair<Boolean, Int> {
        val authNeedApproval = projectPermissionService.needApproval(needApproval)
        val approveStatus = ProjectApproveStatus.parse(projectInfo.approvalStatus)
        // 判断是否需要审批
        return if (approveStatus.isSuccess()) {
            // 当项目创建成功,则只有最大授权范围和项目性质修改才审批
            val finalNeedApproval = authNeedApproval &&
                (projectInfo.subjectScopes != subjectScopesStr ||
                    projectInfo.authSecrecy != projectUpdateInfo.authSecrecy
                    )
            val approvalStatus = if (finalNeedApproval) {
                ProjectApproveStatus.UPDATE_PENDING.status
            } else {
                ProjectApproveStatus.APPROVED.status
            }
            Pair(finalNeedApproval, approvalStatus)
        } else {
            // 当创建驳回时，需要再审批,状态又为重新创建
            Pair(authNeedApproval, ProjectApproveStatus.CREATE_PENDING.status)
        }
    }

    private fun updateApprovalInfo(
        userId: String,
        projectId: String,
        projectUpdateInfo: ProjectUpdateInfo,
        subjectScopesStr: String,
        logoAddress: String?,
        approvalStatus: Int
    ) {
        // 如果是审批拒绝后修改再创建，需要修改项目信息
        if (approvalStatus == ProjectApproveStatus.CREATE_PENDING.status) {
            projectDao.update(
                dslContext = dslContext,
                userId = userId,
                projectId = projectId,
                projectUpdateInfo = projectUpdateInfo,
                subjectScopesStr = subjectScopesStr,
                logoAddress = logoAddress,
                approvalStatus = approvalStatus
            )
        } else {
            projectDao.updateProjectStatusByEnglishName(
                dslContext = dslContext,
                userId = userId,
                englishName = projectUpdateInfo.englishName,
                approvalStatus = approvalStatus
            )
        }
    }

    /**
     * 获取所有项目信息
     */
    override fun list(
        userId: String,
        accessToken: String?,
        enabled: Boolean?,
        unApproved: Boolean
    ): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val projectsWithVisitPermission = getProjectFromAuth(
                userId = userId,
                accessToken = accessToken
            ).toSet()
            if (projectsWithVisitPermission.isEmpty() && !unApproved) {
                return emptyList()
            }
            val projectsResp = mutableListOf<ProjectVO>()
            if (projectsWithVisitPermission.isNotEmpty()) {
                val projectsWithManagePermission = getProjectFromAuth(
                    userId = userId,
                    accessToken = accessToken,
                    permission = AuthPermission.MANAGE
                )
                projectDao.listByEnglishName(
                    dslContext = dslContext,
                    englishNameList = projectsWithVisitPermission.toList(),
                    enabled = enabled
                ).forEach {
                    projectsResp.add(
                        ProjectUtils.packagingBean(
                            tProjectRecord = it,
                            managePermission = projectsWithManagePermission?.contains(it.englishName),
                            showUserManageIcon = isShowUserManageIcon(it.routerTag)
                        )
                    )
                }
            }
            // 将用户创建的项目，但还未审核通过的，一并拉出来，用户项目管理界面
            if (unApproved) {
                projectDao.listUnApprovedByUserId(
                    dslContext = dslContext,
                    userId = userId
                ).forEach {
                    projectsResp.add(
                        ProjectUtils.packagingBean(
                            tProjectRecord = it,
                            managePermission = true,
                            showUserManageIcon = true
                        )
                    )
                }
            }
            success = true
            return projectsResp
        } finally {
            projectJmxApi.execute(PROJECT_LIST, System.currentTimeMillis() - startEpoch, success)
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun listProjectsForApply(
        userId: String,
        accessToken: String?,
        projectName: String?,
        projectId: String?,
        page: Int,
        pageSize: Int
    ): Pagination<ProjectWithPermission> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val projectsResp = mutableListOf<ProjectWithPermission>()
        // 拉取出该用户有访问权限的项目
        val hasVisitPermissionProjectIds = getProjectFromAuth(userId, accessToken)
        projectDao.listProjectsForApply(
            dslContext = dslContext,
            projectName = projectName,
            projectId = projectId,
            authEnglishNameList = hasVisitPermissionProjectIds,
            offset = sqlLimit.offset,
            limit = sqlLimit.limit
        ).forEach {
            projectsResp.add(
                ProjectWithPermission(
                    projectName = it.value1(),
                    englishName = it.value2(),
                    permission = hasVisitPermissionProjectIds.contains(it.value2()),
                    routerTag = buildRouterTag(it.value3())
                )
            )
        }
        return Pagination(
            hasNext = projectsResp.size == pageSize,
            records = projectsResp
        )
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
            logger.info("projects：$projects")
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

    override fun listMigrateProjects(
        migrateProjectConditionDTO: MigrateProjectConditionDTO,
        limit: Int,
        offset: Int
    ): List<ProjectWithPermission> {
        return projectDao.listMigrateProjects(
            dslContext = dslContext,
            migrateProjectConditionDTO = migrateProjectConditionDTO,
            limit = limit,
            offset = offset
        ).map {
            ProjectWithPermission(
                projectName = it.projectName,
                englishName = it.englishName,
                permission = true,
                routerTag = buildRouterTag(it.routerTag)
            )
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
        val verify = validatePermission(englishName, userId, AuthPermission.EDIT)
        if (!verify) {
            logger.info("$englishName| $userId| ${AuthPermission.EDIT} validatePermission fail")
            throw PermissionForbiddenException(I18nUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL))
        }
        val projectRecord = projectDao.getByEnglishName(dslContext, englishName)
        if (projectRecord != null) {
            var logoFile: File? = null
            try {
                logoFile = FileUtil.convertTempFile(inputStream)
                val logoAddress = saveLogoAddress(userId, projectRecord.englishName, logoFile)
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    projectDao.updateLogoAddress(context, userId, projectRecord.projectId, logoAddress)
                    projectDispatcher.dispatch(
                        ProjectUpdateLogoBroadCastEvent(
                            userId = userId,
                            projectId = projectRecord.projectId,
                            logoAddr = logoAddress
                        )
                    )
                }
                return Result(ProjectLogo(logoAddress))
            } catch (e: Exception) {
                logger.warn("fail update projectLogo", e)
                throw OperationException(
                    I18nUtil.getCodeLanMessage(ProjectMessageCode.UPDATE_LOGO_FAIL)
                )
            } finally {
                logoFile?.delete()
            }
        } else {
            throw OperationException(
                I18nUtil.getCodeLanMessage(ProjectMessageCode.QUERY_PROJECT_FAIL)
            )
        }
    }

    override fun uploadLogo(
        userId: String,
        inputStream: InputStream,
        accessToken: String?
    ): Result<String> {
        var logoFile: File? = null
        try {
            logoFile = FileUtil.convertTempFile(inputStream)
            val logoAddress = saveLogoAddress(userId, "", logoFile)
            return Result(logoAddress)
        } catch (e: Exception) {
            logger.warn("fail update projectLogo", e)
            throw OperationException(I18nUtil.getCodeLanMessage(ProjectMessageCode.UPDATE_LOGO_FAIL))
        }
    }

    override fun updateProjectName(userId: String, projectId: String, projectName: String): Boolean {
        if (projectName.isEmpty() || projectName.length > MAX_PROJECT_NAME_LENGTH) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.NAME_TOO_LONG
            )
        }
        if (projectDao.existByProjectName(dslContext, projectName, projectId)) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NAME_EXIST
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
            permission = AuthPermission.ENABLE
        )
        if (!verify) {
            logger.info("$englishName| $userId| ${AuthPermission.DELETE} validatePermission fail")
            throw PermissionForbiddenException(
                I18nUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL)
            )
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
        val projectInfo = projectDao.getByEnglishName(dslContext, projectCode) ?: throw InvalidParamException(
            I18nUtil.getCodeLanMessage(PROJECT_NOT_EXIST)
        )
        val currentRelationId = projectInfo.relationId
        if (!currentRelationId.isNullOrEmpty()) {
            throw InvalidParamException(
                projectCode + I18nUtil.getCodeLanMessage(BOUND_IAM_GRADIENT_ADMIN)
            )
        }
        val updateCount = projectDao.updateRelationByCode(dslContext, projectCode, relationId)
        return updateCount > 0
    }

    override fun cancelCreateProject(userId: String, projectId: String): Boolean {
        logger.info("$userId cancel create project($projectId)")
        val projectInfo = projectDao.get(dslContext, projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            params = arrayOf(projectId),
            defaultMessage = "project - $projectId is not exist!"
        )
        val status = projectInfo.approvalStatus
        if (status != ProjectApproveStatus.CREATE_PENDING.status &&
            status != ProjectApproveStatus.CREATE_REJECT.status
        ) {
            logger.warn("The project can't be cancel:${projectInfo.englishName}|$status")
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.CANCEL_CREATION_PROJECT_FAIL,
                params = arrayOf(projectId),
                defaultMessage = "The project can be canceled only it under approval or " +
                    "rejected during creation！| EnglishName=${projectInfo.englishName}"
            )
        }
        try {
            cancelCreateAuthProject(userId = userId, projectCode = projectInfo.englishName)
            projectDao.delete(dslContext = dslContext, projectId = projectId)
        } catch (e: Exception) {
            logger.warn("The project cancel creation failed: ${projectInfo.englishName}", e)
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.CANCEL_CREATION_PROJECT_FAIL,
                    defaultMessage = "The project cancel creation failed: ${projectInfo.englishName}"
                )
            )
        }
        return true
    }

    override fun cancelUpdateProject(userId: String, projectId: String): Boolean {
        logger.info("$userId cancel update project($projectId)")
        val projectInfo = projectDao.get(dslContext, projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            params = arrayOf(projectId),
            defaultMessage = "project - $projectId is not exist!"
        )
        val status = projectInfo.approvalStatus
        if (status != ProjectApproveStatus.UPDATE_PENDING.status) {
            logger.warn("The project can't be cancel:${projectInfo.englishName}|$status")
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.CANCEL_CREATION_PROJECT_FAIL,
                params = arrayOf(projectId),
                defaultMessage = "The project can be canceled only it under approval or " +
                    "rejected during creation！| EnglishName=${projectInfo.englishName}"
            )
        }
        try {
            cancelUpdateAuthProject(userId = userId, projectCode = projectInfo.englishName)
            projectDao.updateProjectStatusByEnglishName(
                dslContext = dslContext,
                userId = userId,
                englishName = projectInfo.englishName,
                approvalStatus = ProjectApproveStatus.APPROVED.status
            )
        } catch (e: Exception) {
            logger.warn("The project cancel update failed: ${projectInfo.englishName}", e)
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.CANCEL_CREATION_PROJECT_FAIL,
                    defaultMessage = "The project cancel update failed: ${projectInfo.englishName}"
                )
            )
        }
        return true
    }

    override fun getProjectByName(projectName: String): ProjectVO? {
        return projectDao.getProjectByName(dslContext, projectName)
    }

    override fun updateProjectProperties(
        userId: String,
        projectCode: String,
        properties: ProjectProperties
    ): Boolean {
        logger.info("[$projectCode]|updateProjectProperties|userId=$userId|properties=$properties")
        return projectDao.updatePropertiesByCode(dslContext, projectCode, properties) == 1
    }

    override fun updateProjectSubjectScopes(
        projectId: String,
        subjectScopes: List<SubjectScopeInfo>
    ): Boolean {
        projectDao.getByEnglishName(
            dslContext = dslContext,
            englishName = projectId
        ) ?: throw NotFoundException("project - $projectId is not exist!")
        val subjectScopesStr = objectMapper.writeValueAsString(subjectScopes)
        projectDao.updateSubjectScopes(
            dslContext = dslContext,
            englishName = projectId,
            subjectScopesStr = subjectScopesStr
        )
        return true
    }

    override fun updateProjectCreator(projectUpdateCreatorDtoList: List<ProjectUpdateCreatorDTO>): Boolean {
        logger.info("update project create start | $projectUpdateCreatorDtoList")
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

    abstract fun validatePermission(projectCode: String, userId: String, permission: AuthPermission): Boolean

    abstract fun getDeptInfo(userId: String): UserDeptDetail

    abstract fun saveLogoAddress(userId: String, projectCode: String, logoFile: File): String

    abstract fun deleteAuth(projectId: String, accessToken: String?)

    abstract fun getProjectFromAuth(userId: String?, accessToken: String?): List<String>

    abstract fun getProjectFromAuth(userId: String, accessToken: String?, permission: AuthPermission): List<String>?

    abstract fun isShowUserManageIcon(routerTag: String?): Boolean

    abstract fun updateInfoReplace(projectUpdateInfo: ProjectUpdateInfo)

    abstract fun organizationMarkUp(
        projectCreateInfo: ProjectCreateInfo,
        userDeptDetail: UserDeptDetail
    ): ProjectCreateInfo

    abstract fun modifyProjectAuthResource(
        resourceUpdateInfo: ResourceUpdateInfo
    )

    abstract fun cancelCreateAuthProject(
        userId: String,
        projectCode: String
    )

    abstract fun cancelUpdateAuthProject(
        userId: String,
        projectCode: String
    )

    abstract fun updateProjectRouterTag(englishName: String)

    private fun getAllMembersName() = I18nUtil.getCodeLanMessage(ALL_MEMBERS_NAME)

    abstract fun buildRouterTag(routerTag: String?): String?

    companion object {
        const val MAX_PROJECT_NAME_LENGTH = 64
        private val logger = LoggerFactory.getLogger(AbsProjectServiceImpl::class.java)!!
        private const val ENGLISH_NAME_PATTERN = "[a-z][a-zA-Z0-9-]+"
        private const val ALL_MEMBERS = "*"
        private const val ALL_MEMBERS_NAME = "allMembersName"
        private const val FIRST_PAGE = 1

        // 项目tips默认展示时间
        private const val DEFAULT_TIPS_SHOW_TIME = 7
    }
}
