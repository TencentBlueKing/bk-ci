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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Pagination
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.audit.ActionAuditContent.PROJECT_CREATE_CONTENT
import com.tencent.devops.common.audit.ActionAuditContent.PROJECT_EDIT_CONTENT
import com.tencent.devops.common.audit.ActionAuditContent.PROJECT_ENABLE_CONTENT
import com.tencent.devops.common.audit.ActionAuditContent.PROJECT_ENABLE_OR_DISABLE_TEMPLATE
import com.tencent.devops.common.auth.api.ActionId.PROJECT_CREATE
import com.tencent.devops.common.auth.api.ActionId.PROJECT_EDIT
import com.tencent.devops.common.auth.api.ActionId.PROJECT_ENABLE
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId.PROJECT
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.auth.code.ProjectAuthServiceCode
import com.tencent.devops.common.auth.enums.SubjectScopeType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.dialect.PipelineDialectType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.SECRECY_PROJECT_REDIS_KEY
import com.tencent.devops.project.constant.ProjectConstant.NAME_MIN_LENGTH
import com.tencent.devops.project.constant.ProjectConstant.PIPELINE_NAME_FORMAT_MAX_LENGTH
import com.tencent.devops.project.constant.ProjectConstant.PROJECT_ID_MAX_LENGTH
import com.tencent.devops.project.constant.ProjectConstant.PROJECT_NAME_MAX_LENGTH
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.constant.ProjectMessageCode.BOUND_IAM_GRADIENT_ADMIN
import com.tencent.devops.project.constant.ProjectMessageCode.PROJECT_NOT_EXIST
import com.tencent.devops.project.constant.ProjectMessageCode.UNDER_APPROVAL_PROJECT
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectUpdateHistoryDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.jmx.api.ProjectJmxApi.Companion.PROJECT_LIST
import com.tencent.devops.project.pojo.AuthProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectBaseInfo
import com.tencent.devops.project.pojo.ProjectByConditionDTO
import com.tencent.devops.project.pojo.ProjectCollation
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectDiffVO
import com.tencent.devops.project.pojo.ProjectLogo
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.ProjectProductValidateDTO
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectSortType
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import com.tencent.devops.project.pojo.ProjectUpdateHistoryInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.ResourceUpdateInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.PluginDetailsDisplayOrder
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.pojo.enums.ProjectOperation
import com.tencent.devops.project.pojo.enums.ProjectTipsStatus
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.pojo.mq.ProjectEnableStatusBroadCastEvent
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
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import java.io.File
import java.io.InputStream
import java.util.regex.Pattern
import jakarta.ws.rs.NotFoundException

@Suppress("ALL")
abstract class AbsProjectServiceImpl @Autowired constructor(
    val projectPermissionService: ProjectPermissionService,
    val dslContext: DSLContext,
    val projectDao: ProjectDao,
    private val projectJmxApi: ProjectJmxApi,
    val redisOperation: RedisOperation,
    val client: Client,
    private val projectDispatcher: SampleEventDispatcher,
    private val authPermissionApi: AuthPermissionApi,
    private val projectAuthServiceCode: ProjectAuthServiceCode,
    private val shardingRoutingRuleAssignService: ShardingRoutingRuleAssignService,
    private val objectMapper: ObjectMapper,
    private val projectExtService: ProjectExtService,
    private val projectApprovalService: ProjectApprovalService,
    private val clientTokenService: ClientTokenService,
    private val profile: Profile,
    private val projectUpdateHistoryDao: ProjectUpdateHistoryDao
) : ProjectService {

    override fun validate(validateType: ProjectValidateType, name: String, projectId: String?) {
        if (name.isBlank()) {
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.NAME_EMPTY,
                defaultMessage = "Project name cannot be blank!"
            )
        }
        when (validateType) {
            ProjectValidateType.project_name -> {
                if (name.isEmpty() || name.length > PROJECT_NAME_MAX_LENGTH) {
                    throw ErrorCodeException(
                        errorCode = ProjectMessageCode.NAME_TOO_LONG,
                        defaultMessage = "The length of the project name cannot exceed 64 characters!"
                    )
                }
                if (projectDao.existByProjectName(dslContext, name, projectId)) {
                    throw ErrorCodeException(
                        errorCode = ProjectMessageCode.PROJECT_NAME_EXIST,
                        defaultMessage = "The name of the project already exists!"
                    )
                }
            }

            ProjectValidateType.english_name -> {
                // 2 ~ 32 个字符+数字，以小写字母开头
                if (name.length < NAME_MIN_LENGTH) {
                    throw ErrorCodeException(
                        errorCode = ProjectMessageCode.EN_NAME_INTERVAL_ERROR,
                        defaultMessage = "Project id length cannot be less than 2 characters!"
                    )
                }
                if (name.length > PROJECT_ID_MAX_LENGTH) {
                    throw ErrorCodeException(
                        errorCode = ProjectMessageCode.EN_NAME_INTERVAL_ERROR,
                        defaultMessage = "The length of the project id cannot exceed 32 characters!"
                    )
                }

                if (!Pattern.matches(ENGLISH_NAME_PATTERN, name)) {
                    logger.warn("Project English Name($name) is not match")
                    throw ErrorCodeException(
                        errorCode = ProjectMessageCode.EN_NAME_COMBINATION_ERROR,
                        defaultMessage = "The project id is illegal!"
                    )
                }
                if (projectDao.existByEnglishName(dslContext, name, projectId)) {
                    throw ErrorCodeException(
                        errorCode = ProjectMessageCode.EN_NAME_EXIST,
                        defaultMessage = "The project id already exists!"
                    )
                }
            }
        }
    }

    /**
     * 创建项目信息
     */
    @ActionAuditRecord(
        actionId = PROJECT_CREATE,
        instance = AuditInstanceRecord(
            resourceType = PROJECT,
            instanceIds = "#projectCreateInfo?.englishName",
            instanceNames = "#projectCreateInfo?.projectName"
        ),
        scopeId = "#projectCreateInfo?.englishName",
        content = PROJECT_CREATE_CONTENT
    )
    override fun create(
        userId: String,
        projectCreateInfo: ProjectCreateInfo,
        accessToken: String?,
        createExtInfo: ProjectCreateExtInfo,
        defaultProjectId: String?,
        projectChannel: ProjectChannelCode
    ): String {
        logger.info("create project| $userId | $accessToken| $createExtInfo | $projectCreateInfo")
        validateWhenCreateProject(
            userId = userId,
            projectChannel = projectChannel,
            needValidate = createExtInfo.needValidate!!,
            projectCreateInfo = projectCreateInfo
        )
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
        ActionAuditContext.current().setInstance(projectCreateInfo)
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
                message = "${e.message}"
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
                    subjectScopesStr = subjectScopesStr,
                    properties = buildProjectProperties(projectInfo.properties)
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
                    moduleCodes = listOf(SystemModuleEnum.PROCESS, SystemModuleEnum.METRICS),
                    dataTag = projectInfo.properties?.dataTag
                )
                if (projectInfo.secrecy) {
                    redisOperation.addSetValue(SECRECY_PROJECT_REDIS_KEY, projectInfo.englishName)
                }
            }
            updateProjectRouterTag(projectCreateInfo.englishName)
        } catch (e: DuplicateKeyException) {
            logger.warn("Duplicate project $projectCreateInfo", e)
            if (createExtInfo.needAuth) {
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

    private fun validateWhenCreateProject(
        userId: String,
        projectChannel: ProjectChannelCode,
        needValidate: Boolean,
        projectCreateInfo: ProjectCreateInfo
    ) {
        with(projectCreateInfo) {
            if (needValidate) {
                validate(ProjectValidateType.project_name, projectName)
                validate(ProjectValidateType.english_name, englishName)
            }
            validateProjectRelateProduct(
                ProjectProductValidateDTO(
                    englishName = englishName,
                    userId = userId,
                    projectOperation = ProjectOperation.CREATE,
                    channelCode = projectChannel,
                    productId = productId
                )
            )
            validateProjectOrganization(
                projectChannel = projectChannel,
                bgId = bgId,
                bgName = bgName,
                deptId = deptId,
                deptName = deptName
            )
            validateProperties(properties)
        }
    }

    private fun buildProjectProperties(properties: ProjectProperties?): ProjectProperties? {
        var finalProperties = properties
        if (profile.isRbac()) {
            // rbac新建项目默认开启流水线模板管理权限
            finalProperties = properties ?: ProjectProperties()
            finalProperties.enableTemplatePermissionManage = true
        }
        return finalProperties
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
        val rightProjectOrganization = fixProjectOrganization(tProjectRecord = record)
        val projectInfo = ProjectUtils.packagingBean(
            tProjectRecord = record,
            projectOrganizationInfo = rightProjectOrganization
        )
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
        return projectInfo.copy(
            tipsStatus = tipsStatus,
            productName = projectInfo.productId?.let { getProductByProductId(it)?.productName }
        )
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
        val rightProjectOrganization = fixProjectOrganization(tProjectRecord = record)
        val beforeProductName = if (record.productId != null) {
            getProductByProductId(record.productId)
        } else {
            null
        }
        return ProjectUtils.packagingBean(
            tProjectRecord = record,
            projectApprovalInfo = projectApprovalInfo,
            projectOrganizationInfo = rightProjectOrganization,
            beforeProductName = beforeProductName?.productName
        )
    }

    override fun getByEnglishName(englishName: String): ProjectVO? {
        val record = projectDao.getByEnglishName(dslContext, englishName) ?: return null
        return ProjectUtils.packagingBean(record)
    }

    @ActionAuditRecord(
        actionId = PROJECT_EDIT,
        instance = AuditInstanceRecord(
            resourceType = PROJECT,
            instanceIds = "#englishName",
            instanceNames = "#projectUpdateInfo?.projectName"
        ),
        scopeId = "#englishName",
        content = PROJECT_EDIT_CONTENT
    )
    override fun update(
        userId: String,
        englishName: String,
        projectUpdateInfo: ProjectUpdateInfo,
        accessToken: String?,
        needApproval: Boolean?
    ): Boolean {
        val startEpoch = System.currentTimeMillis()
        var success = false
        validateWhenUpdateProject(
            englishName = englishName,
            userId = userId,
            projectUpdateInfo = projectUpdateInfo
        )
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
                // 审计
                ActionAuditContext.current()
                    .setOriginInstance(ProjectUtils.packagingBean(projectInfo))
                    .setInstance(projectUpdateInfo)

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
                // 属性只能变更前端展示的,其他的字段由op变更
                val properties = projectInfo.properties?.let { JsonUtil.to(it, ProjectProperties::class.java) }
                    ?: ProjectProperties()
                projectUpdateInfo.properties = projectUpdateInfo.properties?.let { properties.userCopy(it) }
                // 判断是否需要审批,当修改最大授权范围/权限敏感/关联运营产品时需要审批
                val (finalNeedApproval, newApprovalStatus) = getUpdateApprovalStatus(
                    needApproval = needApproval,
                    projectInfo = projectInfo,
                    afterSubjectScopes = subjectScopes,
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
                if (needModifyAuthResource(
                        originalProjectName = projectInfo.projectName,
                        modifiedProjectName = projectUpdateInfo.projectName,
                        finalNeedApproval = finalNeedApproval,
                        beforeSubjectScopes = JsonUtil.to(
                            projectInfo.subjectScopes, object : TypeReference<List<SubjectScopeInfo>>() {}
                        ),
                        afterSubjectScopes = subjectScopes
                    )) {
                    modifyProjectAuthResource(resourceUpdateInfo)
                }
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
                // 记录项目更新记录
                val projectUpdateHistoryInfo = ProjectUpdateHistoryInfo(
                    englishName = englishName,
                    beforeProjectName = projectInfo.projectName,
                    afterProjectName = projectUpdateInfo.projectName,
                    beforeProductId = projectInfo.productId,
                    afterProductId = projectUpdateInfo.productId,
                    beforeOrganization = with(projectInfo) {
                        getOrganizationStr(bgName, businessLineName, deptName, centerName)
                    },
                    afterOrganization = with(projectUpdateInfo) {
                        getOrganizationStr(bgName, businessLineName, deptName, centerName)
                    },
                    beforeSubjectScopes = projectInfo.subjectScopes,
                    afterSubjectScopes = subjectScopesStr,
                    operator = userId,
                    approvalStatus = newApprovalStatus
                )
                projectUpdateHistoryDao.create(
                    dslContext = dslContext,
                    projectUpdateHistoryInfo = projectUpdateHistoryInfo
                )
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
                    ) + ": ${e.message}"
                )
            }
        } finally {
            projectJmxApi.execute(ProjectJmxApi.PROJECT_UPDATE, System.currentTimeMillis() - startEpoch, success)
        }
        return success
    }

    private fun validateWhenUpdateProject(
        englishName: String,
        userId: String,
        projectUpdateInfo: ProjectUpdateInfo
    ) {
        with(projectUpdateInfo) {
            validate(
                validateType = ProjectValidateType.project_name,
                name = projectUpdateInfo.projectName,
                projectId = projectUpdateInfo.englishName
            )
            validateProjectRelateProduct(
                ProjectProductValidateDTO(
                    englishName = englishName,
                    userId = userId,
                    projectOperation = ProjectOperation.UPDATE,
                    productId = productId
                )
            )
            validateProjectOrganization(
                bgId = bgId,
                bgName = bgName,
                deptId = deptId,
                deptName = deptName
            )
            validateProperties(properties)
        }
    }

    private fun getOrganizationStr(
        bgName: String?,
        businessLineName: String?,
        deptName: String?,
        centerName: String?
    ): String {
        return listOf(
            bgName, businessLineName, deptName, centerName
        ).filter { !it.isNullOrBlank() }.joinToString("-")
    }

    /**
     * 修改蓝盾项目需要修改权限中心的场景。
     * 1.若需要审批则必然需要修改到权限中心资源
     * 2.修改到名称需要同步修改权限中心资源
     * 3.通过service接口时，不需要审批，但是修改到可授权范围也需要修改权限中心资源
     * */
    private fun needModifyAuthResource(
        originalProjectName: String,
        modifiedProjectName: String,
        finalNeedApproval: Boolean,
        beforeSubjectScopes: List<SubjectScopeInfo>,
        afterSubjectScopes: List<SubjectScopeInfo>
    ): Boolean {
        val isSubjectScopesChange = isSubjectScopesChange(
            beforeSubjectScopes = beforeSubjectScopes,
            afterSubjectScopes = afterSubjectScopes
        )
        return originalProjectName != modifiedProjectName || finalNeedApproval ||
            isSubjectScopesChange
    }

    private fun getUpdateApprovalStatus(
        needApproval: Boolean?,
        projectInfo: TProjectRecord,
        afterSubjectScopes: List<SubjectScopeInfo>,
        projectUpdateInfo: ProjectUpdateInfo
    ): Pair<Boolean, Int> {
        val authNeedApproval = projectPermissionService.needApproval(needApproval)
        val approveStatus = ProjectApproveStatus.parse(projectInfo.approvalStatus)
        // 判断是否需要审批
        return if (approveStatus.isSuccess()) {
            val isSubjectScopesChange = isSubjectScopesChange(
                beforeSubjectScopes = JsonUtil.to(
                    projectInfo.subjectScopes,
                    object : TypeReference<List<SubjectScopeInfo>>() {}
                ),
                afterSubjectScopes = afterSubjectScopes
            )
            // 当项目创建成功,则只有最大授权范围和项目性质修改才审批
            val finalNeedApproval = authNeedApproval &&
                (isSubjectScopesChange || projectInfo.authSecrecy != projectUpdateInfo.authSecrecy ||
                    projectInfo.productId != projectUpdateInfo.productId)
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

    private fun isSubjectScopesChange(
        beforeSubjectScopes: List<SubjectScopeInfo>,
        afterSubjectScopes: List<SubjectScopeInfo>
    ): Boolean {
        val beforeUsernames = beforeSubjectScopes
            .filter { it.type == SubjectScopeType.USER.value }
            .map { it.username }
            .toSet()

        val afterUsernames = afterSubjectScopes
            .filter { it.type == SubjectScopeType.USER.value }
            .map { it.username }
            .toSet()

        val beforeDeptIds = beforeSubjectScopes
            .filter { it.type != SubjectScopeType.USER.value }
            .map { it.id }
            .toSet()

        val afterDeptIds = afterSubjectScopes
            .filter { it.type != SubjectScopeType.USER.value }
            .map { it.id }
            .toSet()

        return beforeUsernames != afterUsernames || beforeDeptIds != afterDeptIds
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
        unApproved: Boolean,
        sortType: ProjectSortType?,
        collation: ProjectCollation?
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
                val projectsWithPipelineTemplateCreatePerm = try {
                    getProjectFromAuth(
                        userId = userId,
                        accessToken = accessToken,
                        permission = AuthPermission.CREATE,
                        resourceType = AuthResourceType.PIPELINE_TEMPLATE.value
                    )
                } catch (ex: Exception) {
                    emptyList()
                }
                val projectsWithViewPermission = getProjectFromAuth(
                    userId = userId,
                    accessToken = accessToken,
                    permission = AuthPermission.VIEW
                )
                projectDao.listByEnglishName(
                    dslContext = dslContext,
                    englishNameList = projectsWithVisitPermission.toList(),
                    enabled = enabled,
                    sortType = sortType,
                    collation = collation
                ).forEach {
                    val pipelineTemplateInstallPerm = pipelineTemplateInstallPerm(
                        projectsWithPipelineTemplateCreatePerm = projectsWithPipelineTemplateCreatePerm,
                        tProjectRecord = it
                    )
                    projectsResp.add(
                        ProjectUtils.packagingBean(
                            tProjectRecord = it,
                            managePermission = projectsWithManagePermission?.contains(it.englishName),
                            showUserManageIcon = isShowUserManageIcon(it.routerTag),
                            viewPermission = projectsWithViewPermission?.contains(it.englishName),
                            pipelineTemplateInstallPerm = pipelineTemplateInstallPerm
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

    private fun pipelineTemplateInstallPerm(
        projectsWithPipelineTemplateCreatePerm: List<String>?,
        tProjectRecord: TProjectRecord
    ): Boolean {
        val properties = tProjectRecord.properties?.let { self ->
            JsonUtil.to(self, ProjectProperties::class.java)
        }
        return if (properties != null && properties.enableTemplatePermissionManage == true) {
            // 开启了模板权限，在给项目安装研发商店模板时，需要校验是否有当前项目的模板创建权限。
            projectsWithPipelineTemplateCreatePerm?.contains(tProjectRecord.englishName) ?: false
        } else {
            // 未开启模板权限的默认有安装模板权限
            true
        }
    }

    override fun listProjectsForApply(
        userId: String,
        accessToken: String?,
        projectName: String?,
        projectId: String?,
        page: Int,
        pageSize: Int
    ): Pagination<ProjectByConditionDTO> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val projectsResp = mutableListOf<ProjectByConditionDTO>()
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
                ProjectByConditionDTO(
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

    override fun list(
        projectCodes: Set<String>,
        enabled: Boolean?
    ): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            success = true
            return projectDao.listByCodes(
                dslContext = dslContext,
                projectCodeList = projectCodes,
                enabled = enabled
            ).map {
                ProjectUtils.packagingBean(it)
            }
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
    override fun list(userId: String, productIds: String?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {

            val projects = getProjectFromAuth(userId, null)
            logger.info("projects：$projects")
            val list = ArrayList<ProjectVO>()
            projectDao.listByEnglishName(
                dslContext = dslContext,
                englishNameList = projects,
                offset = null,
                limit = null,
                searchName = null,
                productIds = productIds?.split(",")?.map { it.toInt() }?.toSet() ?: setOf()
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

    override fun listProjectsByCondition(
        projectConditionDTO: ProjectConditionDTO,
        limit: Int,
        offset: Int
    ): List<ProjectByConditionDTO> {
        logger.info("list projects by condition:$projectConditionDTO|$limit|$offset")
        return projectDao.listProjectsByCondition(
            dslContext = dslContext,
            projectConditionDTO = projectConditionDTO,
            limit = limit,
            offset = offset
        ).map {
            ProjectByConditionDTO(
                projectName = it.projectName,
                englishName = it.englishName,
                permission = true,
                routerTag = buildRouterTag(it.routerTag),
                bgId = it.bgId,
                remotedevManager = it.properties?.let { properties ->
                    JsonUtil.to(
                        properties, ProjectProperties::class.java
                    )
                }?.remotedevManager
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

    override fun listByChannel(limit: Int, offset: Int, projectChannelCode: List<String>): Page<ProjectVO> {
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

    @ActionAuditRecord(
        actionId = PROJECT_ENABLE,
        instance = AuditInstanceRecord(
            resourceType = PROJECT,
            instanceIds = "#englishName"
        ),
        scopeId = "#englishName",
        content = PROJECT_ENABLE_CONTENT
    )
    override fun updateUsableStatus(
        userId: String?,
        englishName: String,
        enabled: Boolean,
        checkPermission: Boolean
    ) {
        logger.info("updateUsableStatus userId[$userId], englishName[$englishName] , enabled[$enabled]")
        val projectInfo = projectDao.getByEnglishName(dslContext, englishName)
            ?: throw ErrorCodeException(
                errorCode = PROJECT_NOT_EXIST,
                defaultMessage = "project($englishName) not exist!"
            )
        if (checkPermission) {
            val verify = validatePermission(
                userId = userId!!,
                projectCode = englishName,
                permission = AuthPermission.ENABLE
            )
            if (!verify) {
                logger.info("$englishName| $userId| ${AuthPermission.DELETE} validatePermission fail")
                throw PermissionForbiddenException(
                    I18nUtil.getCodeLanMessage(ProjectMessageCode.PEM_CHECK_FAIL)
                )
            }
            if (enabled) {
                validateProjectRelateProduct(
                    ProjectProductValidateDTO(
                        englishName = englishName,
                        userId = userId,
                        projectOperation = ProjectOperation.ENABLE,
                        productId = projectInfo.productId
                    )
                )
            }
        }
        ActionAuditContext.current()
            .setInstanceName(projectInfo.projectName)
        if (enabled) {
            ActionAuditContext.current()
                .addAttribute(PROJECT_ENABLE_OR_DISABLE_TEMPLATE, "enable")
        } else {
            ActionAuditContext.current()
                .addAttribute(PROJECT_ENABLE_OR_DISABLE_TEMPLATE, "disable")
        }
        projectDao.updateUsableStatus(
            dslContext = dslContext,
            userId = userId,
            projectId = projectInfo.projectId,
            enabled = enabled
        )
        try {
            projectExtService.enableProject(
                userId = userId ?: "",
                projectId = englishName,
                enabled = enabled
            )
        } catch (ex: Exception) {
            logger.warn("enable bkrepo project failed $englishName|$enabled|$ex")
        }
        projectDispatcher.dispatch(
            ProjectEnableStatusBroadCastEvent(
                userId = userId ?: "",
                projectId = englishName,
                enabled = enabled
            )
        )
    }

    override fun searchProjectByProjectName(projectName: String, limit: Int, offset: Int): Page<ProjectVO> {
        val startTime = System.currentTimeMillis()
        val projectList = projectDao.searchByProjectName(
            dslContext = dslContext,
            projectName = projectName,
            channelCodes = listOf(ProjectChannelCode.BS.name, ProjectChannelCode.PREBUILD.name),
            limit = limit,
            offset = offset
        ).map {
            ProjectUtils.packagingBean(it)
        }
        val count = projectDao.countByProjectName(
            dslContext = dslContext,
            projectName = projectName,
            channelCodes = listOf(ProjectChannelCode.BS.name, ProjectChannelCode.PREBUILD.name)
        ).toLong()
        LogUtils.costTime("search project by projectName", startTime)
        return Page(
            count = count,
            page = offset,
            pageSize = limit,
            records = projectList
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
            errorCode = PROJECT_NOT_EXIST,
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

    override fun setDisableWhenInactiveFlag(projectCodes: List<String>): Boolean {
        projectCodes.forEach {
            val projectInfo = getByEnglishName(
                englishName = it
            ) ?: return@forEach
            val properties = projectInfo.properties ?: ProjectProperties()
            properties.disableWhenInactive = false
            projectDao.updatePropertiesByCode(
                dslContext = dslContext,
                projectCode = it,
                properties = properties
            )
        }
        return true
    }

    override fun updateProjectProperties(
        userId: String?,
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

    override fun updateProjectProductId(
        englishName: String,
        productName: String?,
        productId: Int?
    ) {
        logger.info("update project productId|$englishName|$productName")
        if (productId == null && productName == null) {
            throw NotFoundException("productName or productId must not be null")
        }
        projectDao.getByEnglishName(
            dslContext = dslContext,
            englishName = englishName
        ) ?: throw NotFoundException("project - $englishName is not exist!")
        val products = getOperationalProducts()
        val product = if (productId != null) {
            products.firstOrNull { it.productId == productId }
        } else {
            products.firstOrNull { it.productName == productName }
        } ?: throw NotFoundException("product is not exist!")
        projectDao.updateProductId(
            dslContext = dslContext,
            englishName = englishName,
            productId = product.productId!!
        )
    }

    override fun updateOrganizationByEnglishName(
        englishName: String,
        projectOrganizationInfo: ProjectOrganizationInfo
    ) {
        projectDao.updateOrganizationByEnglishName(
            dslContext = dslContext,
            englishName = englishName,
            projectOrganizationInfo = projectOrganizationInfo
        )
    }

    override fun getProjectListByProductId(productId: Int): List<ProjectBaseInfo> {
        return projectDao.getProjectListByProductId(
            dslContext = dslContext,
            productId = productId
        ).map {
            ProjectBaseInfo(
                id = it.value1(),
                englishName = it.value2(),
                projectName = it.value3(),
                enabled = it.value4()
            )
        }
    }

    override fun getExistedEnglishName(englishNameList: List<String>): List<String>? {
        return projectDao.getExistedEnglishName(dslContext, englishNameList)
    }

    abstract fun validatePermission(projectCode: String, userId: String, permission: AuthPermission): Boolean

    abstract fun getDeptInfo(userId: String): UserDeptDetail

    abstract fun saveLogoAddress(userId: String, projectCode: String, logoFile: File): String

    abstract fun deleteAuth(projectId: String, accessToken: String?)

    abstract fun getProjectFromAuth(userId: String?, accessToken: String?): List<String>

    abstract fun getProjectFromAuth(
        userId: String,
        accessToken: String?,
        permission: AuthPermission,
        resourceType: String? = null
    ): List<String>?

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

    abstract fun validateProjectRelateProduct(
        projectProductValidateDTO: ProjectProductValidateDTO
    )

    abstract fun validateProjectOrganization(
        projectChannel: ProjectChannelCode? = null,
        bgId: Long,
        bgName: String,
        deptId: Long?,
        deptName: String?
    )

    override fun updatePluginDetailsDisplay(
        englishName: String,
        pluginDetailsDisplayOrder: List<PluginDetailsDisplayOrder>
    ): Boolean {
        logger.info("update plugin details display|$englishName|$pluginDetailsDisplayOrder")
        val projectInfo = getByEnglishName(englishName)
            ?: throw NotFoundException("project - $englishName is not exist!")

        val validDisplayOrder = setOf(
            PluginDetailsDisplayOrder.LOG,
            PluginDetailsDisplayOrder.ARTIFACT,
            PluginDetailsDisplayOrder.CONFIG
        )

        val isParamsLegal = pluginDetailsDisplayOrder.size == 3 && pluginDetailsDisplayOrder.toSet() == validDisplayOrder

        if (isParamsLegal) {
            val properties = projectInfo.properties ?: ProjectProperties()
            properties.pluginDetailsDisplayOrder = pluginDetailsDisplayOrder
            updateProjectProperties(null, englishName, properties)
        } else {
            throw IllegalArgumentException("The parameter is invalid. It must contain LOG, ARTIFACT, CONFIG in any order.")
        }

        return true
    }

    override fun getPipelineDialect(projectId: String): String {
        return getByEnglishName(englishName = projectId)?.properties?.pipelineDialect
            ?: PipelineDialectType.CLASSIC.name
    }

    private fun validateProperties(properties: ProjectProperties?) {
        properties?.pipelineNameFormat?.let {
            if (it.length > PIPELINE_NAME_FORMAT_MAX_LENGTH) {
                throw ErrorCodeException(
                    errorCode = ProjectMessageCode.ERROR_PIPELINE_NAME_FORMAT_TOO_LONG,
                    defaultMessage = "The naming convention for pipelines should not exceed 200 characters."
                )
            }
        }
    }

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
