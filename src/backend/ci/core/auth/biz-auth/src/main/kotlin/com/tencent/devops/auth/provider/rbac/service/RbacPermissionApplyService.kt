package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.GroupMemberVerifyInfo
import com.tencent.bk.sdk.iam.dto.manager.V2ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthI18nConstants
import com.tencent.devops.auth.constant.AuthI18nConstants.ACTION_NAME_SUFFIX
import com.tencent.devops.auth.constant.AuthI18nConstants.AUTH_RESOURCE_GROUP_CONFIG_GROUP_NAME_SUFFIX
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceGroupApplyDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.ApplyJoinGroupFormDataInfo
import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.ManagerRoleGroupInfo
import com.tencent.devops.auth.pojo.ResourceGroupInfo
import com.tencent.devops.auth.pojo.SearchGroupInfo
import com.tencent.devops.auth.pojo.enum.GroupLevel
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.auth.pojo.vo.AuthRedirectGroupInfoVo
import com.tencent.devops.auth.pojo.vo.ManagerRoleGroupVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.user.UserPipelineViewResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import com.tencent.devops.project.constant.ProjectMessageCode
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.net.URLEncoder
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Suppress("ALL")
class RbacPermissionApplyService @Autowired constructor(
    val dslContext: DSLContext,
    val v2ManagerService: V2ManagerService,
    val authResourceService: AuthResourceService,
    val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    val authResourceGroupDao: AuthResourceGroupDao,
    val rbacCommonService: RbacCommonService,
    val config: CommonConfig,
    val client: Client,
    val authResourceCodeConverter: AuthResourceCodeConverter,
    val permissionService: PermissionService,
    val itsmService: ItsmService,
    val deptService: DeptService,
    val authResourceGroupApplyDao: AuthResourceGroupApplyDao,
    val permissionResourceMemberService: PermissionResourceMemberService
) : PermissionApplyService {
    @Value("\${auth.iamSystem:}")
    private val systemId = ""

    private val authApplyRedirectUrl = "${config.devopsHostGateway}/console/permission/apply?" +
        "project_code=%s&projectName=%s&resourceType=%s&resourceName=%s" +
        "&iamResourceCode=%s&action=%s&groupName=%s&groupId=%s&iamRelatedResourceType=%s"
    private val pipelineDetailRedirectUri = "${config.devopsHostGateway}/console/pipeline/%s/%s/history"
    private val environmentDetailRedirectUri = "${config.devopsHostGateway}/console/environment/%s/envDetail/%s"
    private val codeccTaskDetailRedirectUri = "${config.devopsHostGateway}/console/codecc/%s/task/%s/detail?buildNum=latest"
    private val groupPermissionDetailRedirectUri = "${config.devopsHostGateway}/permission/group/detail?group_id=%s&x-devops-project-id=%s"
    override fun listResourceTypes(userId: String): List<ResourceTypeInfoVo> {
        return rbacCommonService.listResourceTypes()
    }

    override fun listActions(userId: String, resourceType: String): List<ActionInfoVo> {
        return rbacCommonService.listResourceType2Action(resourceType)
    }

    override fun listGroupsForApply(
        userId: String,
        projectId: String,
        searchGroupInfo: SearchGroupInfo
    ): ManagerRoleGroupVO {
        logger.info("RbacPermissionApplyService|listGroups:searchGroupInfo=$searchGroupInfo")
        verifyProjectRouterTag(projectId)
        // 校验新用户信息是否同步完成
        isUserExists(userId)
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val visitProjectPermission = permissionService.validateUserProjectPermission(
            userId = userId,
            projectCode = projectId,
            permission = AuthPermission.VISIT
        )

        val iamResourceCode = searchGroupInfo.iamResourceCode
        val resourceType = searchGroupInfo.resourceType
        // 如果没有访问权限，不允许访问资源级别的组，只允许访问项目级别的组
        if (!visitProjectPermission && searchGroupInfo.groupLevel == GroupLevel.OTHER) {
            return ManagerRoleGroupVO(
                count = 0,
                results = emptyList()
            )
        }

        val bkIamPath = buildBkIamPath(
            userId = userId,
            resourceType = resourceType,
            iamResourceCode = iamResourceCode,
            projectId = projectId,
            visitProjectPermission = visitProjectPermission
        )
        logger.info("RbacPermissionApplyService|listGroups: bkIamPath=$bkIamPath")
        val managerRoleGroupVO: V2ManagerRoleGroupVO
        val groupInfoList: List<ManagerRoleGroupInfo>
        try {
            managerRoleGroupVO = getGradeManagerRoleGroup(
                searchGroupInfo = searchGroupInfo,
                bkIamPath = bkIamPath,
                relationId = projectInfo.relationId
            )
            logger.info("RbacPermissionApplyService|listGroups: managerRoleGroupVO=$managerRoleGroupVO")
            groupInfoList = buildGroupInfoList(
                userId = userId,
                projectId = projectId,
                projectName = projectInfo.resourceName,
                managerRoleGroupInfoList = managerRoleGroupVO.results
            )
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_IAM_GROUP_FAIL
            )
        }
        return ManagerRoleGroupVO(
            count = managerRoleGroupVO.count,
            results = groupInfoList
        )
    }

    private fun isUserExists(userId: String) {
        // 校验新用户信息是否同步完成
        val userExists = deptService.getUserInfo(userId) != null
        if (!userExists) {
            logger.warn("user($userId) does not exist")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_USER_INFORMATION_NOT_SYNCED
            )
        }
    }

    private fun buildBkIamPath(
        userId: String,
        resourceType: String?,
        iamResourceCode: String?,
        projectId: String,
        visitProjectPermission: Boolean
    ): String {
        var bkIamPath: StringBuilder? = null
        if (iamResourceCode != null) {
            if (resourceType == null) {
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.RESOURCE_TYPE_NOT_EMPTY,
                    defaultMessage = "the resource type cannot be empty"
                )
            }
            // 若无项目访问权限，则只搜索出对应资源下的用户组
            if (!visitProjectPermission)
                return ""
            bkIamPath = StringBuilder("/$systemId,${AuthResourceType.PROJECT.value},$projectId/")
            if (resourceType == AuthResourceType.PIPELINE_DEFAULT.value) {
                val pipelineId = authResourceCodeConverter.iamCode2Code(
                    projectCode = projectId,
                    resourceType = resourceType,
                    iamResourceCode = iamResourceCode
                )
                // 获取包含该流水线的所有流水线组
                val viewIds = client.get(UserPipelineViewResource::class).listViewIdsByPipelineId(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId
                ).data
                if (viewIds != null && viewIds.isNotEmpty()) {
                    viewIds.forEach {
                        bkIamPath.append("$systemId,${AuthResourceType.PIPELINE_GROUP.value},$it/")
                    }
                }
            }
        }
        return bkIamPath?.toString() ?: return ""
    }

    private fun verifyProjectRouterTag(projectId: String) {
        val isRbacPermission = client.get(ServiceProjectTagResource::class)
            .isRbacPermission(projectId).data
        // 校验项目是否为RBAC,若不是，则抛出异常
        if (isRbacPermission != true) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_PROJECT_NOT_UPGRADE,
                params = arrayOf(projectId),
                defaultMessage = "The project has not been upgraded to the new permission system," +
                    " please return to the old permission center to apply!"
            )
        }
    }

    private fun getGradeManagerRoleGroup(
        searchGroupInfo: SearchGroupInfo,
        bkIamPath: String?,
        relationId: String
    ): V2ManagerRoleGroupVO {
        val searchGroupDTO = SearchGroupDTO
            .builder()
            .id(searchGroupInfo.groupId)
            .actionId(searchGroupInfo.actionId)
            .resourceTypeSystemId(systemId)
            .resourceTypeId(searchGroupInfo.resourceType)
            .resourceId(searchGroupInfo.iamResourceCode)
            .bkIamPath(bkIamPath)
            .name(searchGroupInfo.name)
            .description(searchGroupInfo.description)
            .let {
                if (searchGroupInfo.groupLevel == GroupLevel.PROJECT) {
                    it.inherit(false)
                } else {
                    it.onlyInherit(true)
                }
            }.build()

        val v2PageInfoDTO = V2PageInfoDTO()
        v2PageInfoDTO.pageSize = searchGroupInfo.pageSize
        v2PageInfoDTO.page = searchGroupInfo.page
        return v2ManagerService.getGradeManagerRoleGroupV2(relationId, searchGroupDTO, v2PageInfoDTO)
    }

    private fun buildGroupInfoList(
        userId: String,
        projectId: String,
        projectName: String,
        managerRoleGroupInfoList: List<V2ManagerRoleGroupInfo>
    ): List<ManagerRoleGroupInfo> {
        if (managerRoleGroupInfoList.isEmpty()) return emptyList()

        val groupIds = managerRoleGroupInfoList.map { it.id.toString() }
        val verifyMemberJoinedResult = verifyMemberJoined(userId, groupIds)
        val dbGroupRecords = authResourceGroupDao.listByRelationId(dslContext, projectId, groupIds)

        return managerRoleGroupInfoList.map { gInfo ->
            val dbGroupRecord = dbGroupRecords.find { record -> record.relationId == gInfo.id.toString() }
            val resourceType = dbGroupRecord?.resourceType ?: AuthResourceType.PROJECT.value
            val resourceTypeName = rbacCommonService.getResourceTypeInfo(resourceType).name
            val resourceName = dbGroupRecord?.resourceName ?: projectName
            val resourceCode = dbGroupRecord?.resourceCode ?: projectId
            val memberJoinedResult = verifyMemberJoinedResult[gInfo.id.toInt()]
            val isMemberJoinedGroup = when {
                memberJoinedResult?.belong == true &&
                    memberJoinedResult.expiredAt > LocalDateTime.now().timestamp() -> true

                else -> false
            }
            ManagerRoleGroupInfo(
                id = gInfo.id,
                name = gInfo.name,
                description = gInfo.description,
                readonly = gInfo.readonly,
                userCount = gInfo.userCount,
                departmentCount = gInfo.departmentCount,
                joined = isMemberJoinedGroup,
                resourceType = resourceType,
                resourceTypeName = resourceTypeName,
                resourceName = resourceName,
                resourceCode = resourceCode
            )
        }.sortedBy { it.resourceType }
    }

    private fun verifyMemberJoined(
        userId: String,
        groupIds: List<String>
    ): Map<Int, GroupMemberVerifyInfo> {
        val verifyGroupValidMemberResult = mutableMapOf<Int, GroupMemberVerifyInfo>()
        val futures = mutableListOf<Future<*>>()
        groupIds.chunked(20).forEach { batchGroupIds ->
            futures.add(executor.submit {
                val batchVerifyGroupValidMember = v2ManagerService.verifyGroupValidMember(userId, batchGroupIds.joinToString(","))
                verifyGroupValidMemberResult.putAll(batchVerifyGroupValidMember)
            })
        }
        futures.forEach { it.get() } // 等待所有异步任务完成
        return verifyGroupValidMemberResult
    }

    override fun applyToJoinGroup(userId: String, applyJoinGroupInfo: ApplyJoinGroupInfo): Boolean {
        try {
            logger.info("apply to join group: applyJoinGroupInfo=$applyJoinGroupInfo")
            val projectCode = applyJoinGroupInfo.projectCode
            val projectInfo = client.get(ServiceProjectResource::class).get(englishName = projectCode).data
                ?: throw OperationException(
                    I18nUtil.getCodeLanMessage(
                        messageCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                        defaultMessage = "The project does not exist! | englishName = $projectCode"
                    )
                )
            // 构造itsm表格中对应组的详细内容
            val groupContent = applyJoinGroupInfo.groupIds.map { it.toString() }.associateWith {
                val resourceGroupInfo = getResourceGroupInfoForApply(
                    projectCode = projectCode,
                    projectName = projectInfo.projectName,
                    groupId = it
                )
                itsmService.buildGroupApplyItsmValue(
                    ApplyJoinGroupFormDataInfo(
                        projectName = projectInfo.projectName,
                        resourceTypeName = rbacCommonService.getResourceTypeInfo(resourceGroupInfo.resourceType).name,
                        resourceName = resourceGroupInfo.resourceName,
                        groupName = resourceGroupInfo.groupName,
                        validityPeriod = generateValidityPeriod(applyJoinGroupInfo.expiredAt.toLong()),
                        resourceRedirectUri = generateResourceRedirectUri(
                            projectCode = resourceGroupInfo.projectCode,
                            resourceType = resourceGroupInfo.resourceType,
                            resourceCode = resourceGroupInfo.resourceCode
                        ),
                        groupPermissionDetailRedirectUri = String.format(
                            groupPermissionDetailRedirectUri,
                            it,
                            projectCode
                        )
                    )
                )
            }
            logger.info("apply to join group: groupContent=$groupContent")
            val iamApplicationDTO = ApplicationDTO
                .builder()
                .groupId(applyJoinGroupInfo.groupIds)
                .applicant(userId)
                .contentTemplate(itsmService.buildGroupApplyItsmContentDTO())
                .groupContent(groupContent)
                .expiredAt(applyJoinGroupInfo.expiredAt.toLong())
                .titlePrefix(
                    I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_APPLY_TO_JOIN_PROJECT) +
                        "[${projectInfo.projectName}]"
                )
                .reason(applyJoinGroupInfo.reason).build()
            logger.info("apply to join group: iamApplicationDTO=$iamApplicationDTO")
            v2ManagerService.createRoleGroupApplicationV2(iamApplicationDTO)
            // 记录单据，用于同步用户组
            authResourceGroupApplyDao.batchCreate(
                dslContext = dslContext,
                applyJoinGroupInfo = applyJoinGroupInfo
            )
        } catch (e: Exception) {
            when {
                e.message?.contains("审批人不允许为空") == true -> {
                    val resourceCodes = authResourceGroupDao.listByRelationId(
                        dslContext = dslContext,
                        projectCode = applyJoinGroupInfo.projectCode,
                        iamGroupIds = applyJoinGroupInfo.groupIds.map { it.toString() }
                    ).distinctBy { it.resourceCode }.map { it.resourceCode }
                    val listResourcesCreator = authResourceService.listResourcesCreator(
                        projectCode = applyJoinGroupInfo.projectCode,
                        resourceCodes = resourceCodes
                    )
                    val departedUsers = listResourcesCreator.filter {
                        deptService.isUserDeparted(it)
                    }.joinToString(",")
                    throw ErrorCodeException(
                        errorCode = AuthMessageCode.APPLY_TO_JOIN_GROUP_FAIL,
                        params = arrayOf(
                            "该资源的管理员${departedUsers}已离职，请麻烦联系项目管理员或者蓝盾小助手进行交接该用户的权限!"
                        )
                    )
                }

                else -> {
                    throw ErrorCodeException(
                        errorCode = AuthMessageCode.APPLY_TO_JOIN_GROUP_FAIL,
                        params = arrayOf("${e.message}")
                    )
                }
            }
        }
        return true
    }

    private fun getResourceGroupInfoForApply(
        projectCode: String,
        projectName: String,
        groupId: String
    ): ResourceGroupInfo {
        logger.info("get resource group for apply :$projectCode|$projectName|$groupId")
        val dbResourceGroupInfo = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            relationId = groupId
        )
        return if (dbResourceGroupInfo != null) {
            ResourceGroupInfo(
                groupId = groupId,
                groupName = dbResourceGroupInfo.groupName,
                projectCode = projectCode,
                resourceType = dbResourceGroupInfo.resourceType,
                resourceName = dbResourceGroupInfo.resourceName,
                resourceCode = dbResourceGroupInfo.resourceCode
            )
        } else {
            // 若是在权限中心界面创建的组，不会同步到蓝盾库，需要再次调iam查询
            val gradeManagerId = authResourceService.get(
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode
            ).relationId
            val iamGroupInfo = getGradeManagerRoleGroup(
                searchGroupInfo = SearchGroupInfo(
                    groupId = groupId.toInt(),
                    page = 1,
                    pageSize = 10
                ),
                bkIamPath = null,
                relationId = gradeManagerId
            ).results.first()
            logger.info("get resource group info from iam:$projectCode|$projectName|$groupId|$iamGroupInfo")
            ResourceGroupInfo(
                groupId = groupId,
                groupName = iamGroupInfo.name,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceName = projectName,
                resourceCode = projectCode
            )
        }
    }

    private fun generateValidityPeriod(expiredAt: Long): String {
        val between = expiredAt * 1000 - System.currentTimeMillis()
        return DateTimeUtil.formatDay(between).plus(
            I18nUtil.getCodeLanMessage(AuthI18nConstants.BK_DAY)
        )
    }

    private fun generateResourceRedirectUri(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): String? {
        return when (resourceType) {
            AuthResourceType.PIPELINE_DEFAULT.value -> {
                String.format(pipelineDetailRedirectUri, projectCode, resourceCode)
            }

            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> {
                String.format(environmentDetailRedirectUri, projectCode, resourceCode)
            }

            AuthResourceType.CODECC_TASK.value -> {
                String.format(codeccTaskDetailRedirectUri, projectCode, resourceCode)
            }

            else -> null
        }
    }

    override fun getRedirectInformation(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        action: String?
    ): AuthApplyRedirectInfoVo {
        logger.info(
            "PermissionApplyService|getRedirectInformation: $userId|$projectId" +
                "|$resourceType|$resourceCode|$action|"
        )
        val groupInfoList: MutableList<AuthRedirectGroupInfoVo> = mutableListOf()
        // 判断action是否为空
        val actionInfo = if (action != null) rbacCommonService.getActionInfo(action) else null
        val iamRelatedResourceType = actionInfo?.relatedResourceType ?: resourceType
        val resourceTypeName = I18nUtil.getCodeLanMessage(
            messageCode = resourceType + AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX,
            defaultMessage = rbacCommonService.getResourceTypeInfo(resourceType).name
        )

        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val resourceInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = iamRelatedResourceType,
            resourceCode = resourceCode
        )
        val resourceName = resourceInfo.resourceName
        val iamResourceCode = resourceInfo.iamResourceCode
        logger.info(
            "RbacPermissionApplyService|getRedirectInformation: $iamRelatedResourceType|" +
                "$resourceTypeName|$resourceInfo|"
        )
        val isEnablePermission: Boolean =
            if (action == null || iamRelatedResourceType == AuthResourceType.PROJECT.value) false
            else resourceInfo.enable
        buildRedirectGroupInfoResult(
            iamRelatedResourceType = iamRelatedResourceType,
            isEnablePermission = isEnablePermission,
            groupInfoList = groupInfoList,
            projectInfo = projectInfo,
            resourceType = resourceType,
            resourceCode = resourceCode,
            action = action,
            resourceName = resourceName,
            iamResourceCode = iamResourceCode
        )
        logger.info("RbacPermissionApplyService|getRedirectInformation: groupInfoList=$groupInfoList")
        if (groupInfoList.isEmpty()) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_REDIRECT_INFORMATION_FAIL,
                defaultMessage = "Failed to get redirect url"
            )
        }
        val managers = permissionResourceMemberService.getResourceGroupMembers(
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode,
            group = BkAuthGroup.MANAGER
        )

        return AuthApplyRedirectInfoVo(
            auth = isEnablePermission,
            resourceTypeName = resourceTypeName,
            resourceName = resourceName,
            actionName = actionInfo?.let {
                I18nUtil.getCodeLanMessage(
                    messageCode = "${it.action}$ACTION_NAME_SUFFIX",
                    defaultMessage = it.actionName
                )
            },
            groupInfoList = groupInfoList,
            managers = managers
        )
    }

    private fun buildRedirectGroupInfoResult(
        iamRelatedResourceType: String,
        isEnablePermission: Boolean,
        groupInfoList: MutableList<AuthRedirectGroupInfoVo>,
        projectInfo: AuthResourceInfo,
        resourceType: String,
        resourceCode: String,
        action: String?,
        resourceName: String,
        iamResourceCode: String
    ) {
        val projectId = projectInfo.resourceCode
        val projectName = projectInfo.resourceName
        val encodedResourceName = URLEncoder.encode(resourceName, "UTF-8")
        // 若动作是挂在项目下，返回的资源类型必须是project
        val finalResourceType =
            if (action?.substringBeforeLast("_") == AuthResourceType.PROJECT.value) {
                AuthResourceType.PROJECT.value
            } else {
                resourceType
            }
        logger.info("buildRedirectGroupInfoResult|finalResourceType:$finalResourceType")
        if (action == null || iamRelatedResourceType == AuthResourceType.PROJECT.value) {
            groupInfoList.add(
                AuthRedirectGroupInfoVo(
                    url = String.format(
                        authApplyRedirectUrl, projectId, projectName, finalResourceType,
                        encodedResourceName, iamResourceCode, action ?: "", "", "", iamRelatedResourceType
                    )
                )
            )
        } else {
            if (isEnablePermission) {
                rbacCommonService.getGroupConfigAction(finalResourceType).forEach {
                    if (it.actions.contains(action)) {
                        buildRedirectGroupInfo(
                            groupInfoList = groupInfoList,
                            projectInfo = projectInfo,
                            resourceName = encodedResourceName,
                            action = action,
                            resourceType = finalResourceType,
                            resourceCode = resourceCode,
                            groupCode = it.groupCode,
                            iamResourceCode = iamResourceCode,
                            iamRelatedResourceType = iamRelatedResourceType,
                            groupDesc = it.desc
                        )
                    }
                }
            } else {
                buildRedirectGroupInfo(
                    groupInfoList = groupInfoList,
                    projectInfo = projectInfo,
                    resourceName = encodedResourceName,
                    action = action,
                    resourceType = finalResourceType,
                    resourceCode = resourceCode,
                    groupCode = DefaultGroupType.MANAGER.value,
                    iamResourceCode = iamResourceCode,
                    iamRelatedResourceType = iamRelatedResourceType,
                    groupDesc = rbacCommonService.getGroupConfigAction(finalResourceType).firstOrNull {
                        it.groupCode == DefaultGroupType.MANAGER.value
                    }?.desc
                )
            }
        }
    }

    private fun buildRedirectGroupInfo(
        projectInfo: AuthResourceInfo,
        groupInfoList: MutableList<AuthRedirectGroupInfoVo>,
        resourceName: String,
        action: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
        iamResourceCode: String,
        iamRelatedResourceType: String,
        groupDesc: String? = null
    ) {
        val projectId = projectInfo.resourceCode
        val projectName = projectInfo.resourceName
        val resourceGroup = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode,
            groupCode = groupCode
        )
        if (resourceGroup != null) {
            groupInfoList.add(
                AuthRedirectGroupInfoVo(
                    url = String.format(
                        authApplyRedirectUrl, projectId, projectName, resourceType,
                        resourceName, iamResourceCode, action, resourceGroup.groupName,
                        resourceGroup.relationId, iamRelatedResourceType
                    ),
                    groupName = I18nUtil.getCodeLanMessage(
                        messageCode = "${resourceGroup.resourceType}.${resourceGroup.groupCode}" +
                            AUTH_RESOURCE_GROUP_CONFIG_GROUP_NAME_SUFFIX,
                        defaultMessage = resourceGroup.groupName
                    ),
                    groupId = resourceGroup.relationId,
                    groupDesc = groupDesc
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionApplyService::class.java)
        private val executor = Executors.newFixedThreadPool(10)
    }
}
