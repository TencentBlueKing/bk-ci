package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.InstancesDTO
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
import com.tencent.devops.auth.constant.AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.ManagerRoleGroupInfo
import com.tencent.devops.auth.pojo.RelatedResourceInfo
import com.tencent.devops.auth.pojo.SearchGroupInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.auth.pojo.vo.AuthRedirectGroupInfoVo
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.ManagerRoleGroupVO
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.user.UserPipelineViewResource
import com.tencent.devops.project.api.service.ServiceProjectTagResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Suppress("ALL")
class RbacPermissionApplyService @Autowired constructor(
    val dslContext: DSLContext,
    val v2ManagerService: V2ManagerService,
    val authResourceService: AuthResourceService,
    val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    val authResourceGroupDao: AuthResourceGroupDao,
    val rbacCacheService: RbacCacheService,
    val config: CommonConfig,
    val client: Client,
    val authResourceCodeConverter: AuthResourceCodeConverter,
    val permissionService: PermissionService
) : PermissionApplyService {
    @Value("\${auth.iamSystem:}")
    private val systemId = ""

    private val authApplyRedirectUrl = "${config.devopsHostGateway}/console/permission/apply?" +
        "project_code=%s&projectName=%s&resourceType=%s&resourceName=%s" +
        "&iamResourceCode=%s&action=%s&groupName=%s&groupId=%s"

    override fun listResourceTypes(userId: String): List<ResourceTypeInfoVo> {
        return rbacCacheService.listResourceTypes()
    }

    override fun listActions(userId: String, resourceType: String): List<ActionInfoVo> {
        return rbacCacheService.listResourceType2Action(resourceType)
    }

    override fun listGroups(
        userId: String,
        projectId: String,
        searchGroupInfo: SearchGroupInfo
    ): ManagerRoleGroupVO {
        logger.info("RbacPermissionApplyService|listGroups:searchGroupInfo=$searchGroupInfo")
        verifyProjectRouterTag(projectId)

        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val visitProjectPermission =
            permissionService.validateUserResourcePermission(
                userId = userId,
                action = RbacAuthUtils.buildAction(AuthPermission.VISIT, AuthResourceType.PROJECT),
                projectCode = projectId,
                resourceType = AuthResourceType.PROJECT.value
            )

        val iamResourceCode = searchGroupInfo.iamResourceCode
        val resourceType = searchGroupInfo.resourceType
        // 如果没有访问权限，并且资源类型是项目或者不选择，则inherit为false,即只展示项目下用户组
        if (!visitProjectPermission && (resourceType == null || resourceType == AuthResourceType.PROJECT.value)) {
            searchGroupInfo.inherit = false
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
        try {
            managerRoleGroupVO = getGradeManagerRoleGroup(
                searchGroupInfo = searchGroupInfo,
                bkIamPath = bkIamPath,
                relationId = projectInfo.relationId
            )
            logger.info("RbacPermissionApplyService|listGroups: managerRoleGroupVO=$managerRoleGroupVO")
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_IAM_GROUP_FAIL
            )
        }
        val groupInfoList = buildGroupInfoList(
            userId = userId,
            projectId = projectId,
            projectName = projectInfo.resourceName,
            managerRoleGroupInfoList = managerRoleGroupVO.results
        )
        return ManagerRoleGroupVO(
            count = managerRoleGroupVO.count,
            results = groupInfoList
        )
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
            .inherit(searchGroupInfo.inherit)
            .id(searchGroupInfo.groupId)
            .actionId(searchGroupInfo.actionId)
            .resourceTypeSystemId(systemId)
            .resourceTypeId(searchGroupInfo.resourceType)
            .resourceId(searchGroupInfo.iamResourceCode)
            .bkIamPath(bkIamPath)
            .name(searchGroupInfo.name)
            .description(searchGroupInfo.description)
            .build()
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
            val resourceTypeName = rbacCacheService.getResourceTypeInfo(resourceType).name
            val resourceName = dbGroupRecord?.resourceName ?: projectName
            val resourceCode = dbGroupRecord?.resourceCode ?: projectId
            ManagerRoleGroupInfo(
                id = gInfo.id,
                name = gInfo.name,
                description = gInfo.description,
                readonly = gInfo.readonly,
                userCount = gInfo.userCount,
                departmentCount = gInfo.departmentCount,
                joined = verifyMemberJoinedResult[gInfo.id.toInt()]?.belong ?: false,
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
            logger.info("RbacPermissionApplyService|applyToJoinGroup: applyJoinGroupInfo=$applyJoinGroupInfo")
            val iamApplicationDTO = ApplicationDTO
                .builder()
                .groupId(applyJoinGroupInfo.groupIds)
                .applicant(userId)
                .expiredAt(applyJoinGroupInfo.expiredAt.toLong())
                .reason(applyJoinGroupInfo.reason).build()
            v2ManagerService.createRoleGroupApplicationV2(iamApplicationDTO)
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.APPLY_TO_JOIN_GROUP_FAIL,
                params = arrayOf(applyJoinGroupInfo.groupIds.toString()),
                defaultMessage = "Failed to apply to join group(${applyJoinGroupInfo.groupIds})"
            )
        }
        return true
    }

    override fun getGroupPermissionDetail(userId: String, groupId: Int): List<GroupPermissionDetailVo> {
        val iamGroupPermissionDetailList = try {
            v2ManagerService.getGroupPermissionDetail(groupId)
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_GROUP_PERMISSION_DETAIL_FAIL,
                params = arrayOf(groupId.toString()),
                defaultMessage = "Failed to get group($groupId) permission info"
            )
        }
        return iamGroupPermissionDetailList.map { detail ->
            val relatedResourceTypesDTO = detail.resourceGroups[0].relatedResourceTypesDTO[0]
            buildRelatedResourceTypesDTO(instancesDTO = relatedResourceTypesDTO.condition[0].instances[0])
            val relatedResourceInfo = RelatedResourceInfo(
                type = relatedResourceTypesDTO.type,
                name = I18nUtil.getCodeLanMessage(
                    relatedResourceTypesDTO.type + RESOURCE_TYPE_NAME_SUFFIX
                ),
                instances = relatedResourceTypesDTO.condition[0].instances[0]
            )
            GroupPermissionDetailVo(
                actionId = detail.id,
                name = rbacCacheService.getActionInfo(action = detail.id).actionName,
                relatedResourceInfo = relatedResourceInfo
            )
        }.sortedBy { it.relatedResourceInfo.type }
    }

    private fun buildRelatedResourceTypesDTO(instancesDTO: InstancesDTO) {
        instancesDTO.let {
            it.name = rbacCacheService.getResourceTypeInfo(it.type).name
            it.path.forEach { element1 ->
                element1.forEach { element2 ->
                    element2.typeName = rbacCacheService.getResourceTypeInfo(element2.type).name
                }
            }
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
            "RbacPermissionApplyService|getRedirectInformation: $userId|$projectId" +
                "|$resourceType|$resourceCode|$action|"
        )
        val groupInfoList: MutableList<AuthRedirectGroupInfoVo> = mutableListOf()
        // 判断action是否为空
        val actionInfo = if (action != null) rbacCacheService.getActionInfo(action) else null
        val iamRelatedResourceType = actionInfo?.relatedResourceType ?: resourceType
        val resourceTypeName = I18nUtil.getCodeLanMessage(
            messageCode = resourceType + AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX,
            defaultMessage = rbacCacheService.getResourceTypeInfo(resourceType).name
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
            groupInfoList = groupInfoList
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
                        resourceName, iamResourceCode, action ?: "", "", ""
                    )
                )
            )
        } else {
            if (isEnablePermission) {
                rbacCacheService.getGroupConfigAction(finalResourceType).forEach {
                    if (it.actions.contains(action)) {
                        buildRedirectGroupInfo(
                            groupInfoList = groupInfoList,
                            projectInfo = projectInfo,
                            resourceName = resourceName,
                            action = action,
                            resourceType = finalResourceType,
                            resourceCode = resourceCode,
                            groupCode = it.groupCode,
                            iamResourceCode = iamResourceCode
                        )
                    }
                }
            } else {
                buildRedirectGroupInfo(
                    groupInfoList = groupInfoList,
                    projectInfo = projectInfo,
                    resourceName = resourceName,
                    action = action,
                    resourceType = finalResourceType,
                    resourceCode = resourceCode,
                    groupCode = DefaultGroupType.MANAGER.value,
                    iamResourceCode = iamResourceCode
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
        iamResourceCode: String
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
                        resourceName, iamResourceCode, action, resourceGroup.groupName, resourceGroup.relationId
                    ),
                    groupName = I18nUtil.getCodeLanMessage(
                        messageCode = "${resourceGroup.resourceType}.${resourceGroup.groupCode}" +
                            AUTH_RESOURCE_GROUP_CONFIG_GROUP_NAME_SUFFIX,
                        defaultMessage = resourceGroup.groupName
                    )
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GroupUserService::class.java)
        private val executor = Executors.newFixedThreadPool(10)
    }
}
