package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.InstancesDTO
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.V2ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.bk.sdk.iam.dto.response.GroupPermissionDetailResponseDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.ApplyJoinProjectInfo
import com.tencent.devops.auth.pojo.RelatedResourceInfo
import com.tencent.devops.auth.pojo.SearchGroupInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.auth.pojo.vo.AuthRedirectGroupInfoVo
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.process.api.user.UserPipelineViewResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class RbacPermissionApplyService @Autowired constructor(
    val dslContext: DSLContext,
    val v2ManagerService: V2ManagerService,
    val strategyService: StrategyService,
    val authResourceService: AuthResourceService,
    val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    val authResourceGroupDao: AuthResourceGroupDao,
    val rbacCacheService: RbacCacheService,
    val config: CommonConfig,
    val client: Client,
    val authResourceCodeConverter: AuthResourceCodeConverter
) : PermissionApplyService {
    @Value("\${auth.iamSystem:}")
    private val systemId = ""

    private val authApplyRedirectUrl = "${config.devopsHostGateway}/console/permission/%s/applyPermission?" +
        "projectId=%s&groupId=%s&resourceType=%s&resourceName=%s&action=%s&iamResourceCode=%s"

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
    ): V2ManagerRoleGroupVO {
        logger.info("RbacPermissionApplyService|listGroups: searchGroupInfo=$searchGroupInfo")
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId
        )
        val iamResourceCode = searchGroupInfo.iamResourceCode
        val resourceType = searchGroupInfo.resourceType
        try {
            val bkIamPath = buildBkIamPath(
                userId = userId,
                resourceType = resourceType,
                iamResourceCode = iamResourceCode,
                projectId = projectId
            )
            logger.info("RbacPermissionApplyService|listGroups: bkIamPath=$bkIamPath")
            val managerRoleGroupVO = getGradeManagerRoleGroup(
                searchGroupInfo = searchGroupInfo,
                bkIamPath = bkIamPath,
                relationId = projectInfo.relationId
            )
            logger.info("RbacPermissionApplyService|listGroups: managerRoleGroupVO=$managerRoleGroupVO")
            // 校验用户是否属于组
            verifyGroupValidMember(
                userId = userId,
                groupInfoList = managerRoleGroupVO.results
            )
            return managerRoleGroupVO
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_IAM_GROUP_FAIL,
                defaultMessage = "权限系统：获取用户组失败！"
            )
        }
    }

    private fun buildBkIamPath(
        userId: String,
        resourceType: String?,
        iamResourceCode: String?,
        projectId: String
    ): String {
        var bkIamPath: StringBuilder? = null
        if (iamResourceCode != null) {
            if (resourceType == null) {
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.RESOURCE_TYPE_NOT_EMPTY,
                    params = arrayOf(iamResourceCode),
                    defaultMessage = "权限系统：资源实例筛选时，资源类型不能为空！"
                )
            }
            bkIamPath = StringBuilder("/${systemId},${AuthResourceType.PROJECT.value},${projectId}/")
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
                        bkIamPath.append("${systemId},${AuthResourceType.PIPELINE_GROUP.value},${it}/")
                    }
                }
            }
        }
        return bkIamPath?.toString() ?: return ""
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

    private fun verifyGroupValidMember(
        userId: String,
        groupInfoList: List<V2ManagerRoleGroupInfo>
    ) {
        if (groupInfoList.isNotEmpty()) {
            val groupIds = groupInfoList.map { it.id }.joinToString(",")
            val verifyGroupValidMember = v2ManagerService.verifyGroupValidMember(userId, groupIds)
            groupInfoList.forEach {
                it.joined = verifyGroupValidMember[it.id.toInt()]?.belong ?: false
            }
        }
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
                defaultMessage = "权限系统：申请加入用户组[${applyJoinGroupInfo.groupIds}]失败！"
            )
        }
        return true
    }

    override fun applyToJoinProject(
        userId: String,
        projectId: String,
        applyJoinProjectInfo: ApplyJoinProjectInfo
    ): Boolean {
        logger.info("user $userId apply join project $projectId)|${applyJoinProjectInfo.expireTime}")
        val resourceGroup = authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectId,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectId,
            groupCode = DefaultGroupType.VIEWER.value
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_AUTH_GROUP_NOT_EXIST,
            params = arrayOf(DefaultGroupType.VIEWER.displayName),
            defaultMessage = "group ${DefaultGroupType.VIEWER.displayName} not exist"
        )
        return applyToJoinGroup(
            userId = userId,
            applyJoinGroupInfo = ApplyJoinGroupInfo(
                groupIds = listOf(resourceGroup.relationId.toInt()),
                expiredAt = applyJoinProjectInfo.expireTime,
                applicant = userId,
                reason = applyJoinProjectInfo.reason
            )
        )
    }

    override fun getGroupPermissionDetail(userId: String, groupId: Int): List<GroupPermissionDetailVo> {
        val iamGroupPermissionDetailList: List<GroupPermissionDetailResponseDTO>
        try {
            iamGroupPermissionDetailList = v2ManagerService.getGroupPermissionDetail(groupId)
            logger.info("RbacPermissionApplyService|getGroupPermissionDetail: iamGroupPermissionDetailList=$iamGroupPermissionDetailList")
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_GROUP_PERMISSION_DETAIL_FAIL,
                params = arrayOf(groupId.toString()),
                defaultMessage = "权限系统：获取用户组[$groupId]权限信息失败！"
            )
        }
        val groupPermissionDetailVoList: MutableList<GroupPermissionDetailVo> = ArrayList()
        iamGroupPermissionDetailList.forEach {
            val relatedResourceTypesDTO = it.resourceGroups[0].relatedResourceTypesDTO[0]
            buildRelatedResourceTypesDTO(instancesDTO = relatedResourceTypesDTO.condition[0].instances[0])
            val relatedResourceInfo = RelatedResourceInfo(
                type = relatedResourceTypesDTO.type,
                name = rbacCacheService.getResourceTypeInfo(relatedResourceTypesDTO.type).name,
                instances = relatedResourceTypesDTO.condition[0].instances[0]
            )
            groupPermissionDetailVoList.add(
                GroupPermissionDetailVo(
                    actionId = it.id,
                    name = rbacCacheService.getActionInfo(action = it.id).actionName,
                    relatedResourceInfo = relatedResourceInfo
                )
            )
        }
        return groupPermissionDetailVoList
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
        action: String
    ): AuthApplyRedirectInfoVo {
        logger.info("RbacPermissionApplyService|getRedirectInformation: $userId|$projectId|$resourceType|$resourceCode|$action|")
        val groupInfoList: ArrayList<AuthRedirectGroupInfoVo> = ArrayList()
        val actionInfo = rbacCacheService.getActionInfo(action)
        val iamRelatedResourceType = actionInfo.relatedResourceType
        val resourceTypeName = rbacCacheService.getResourceTypeInfo(resourceType).name
        val resourceInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = iamRelatedResourceType,
            resourceCode = resourceCode
        )
        val resourceName = resourceInfo.resourceName
        val iamResourceCode = resourceInfo.iamResourceCode
        logger.info("RbacPermissionApplyService|getRedirectInformation: $iamRelatedResourceType|$resourceTypeName|$resourceInfo|")
        val isEnablePermission: Boolean =
            if (iamRelatedResourceType == AuthResourceType.PROJECT.value) false
            else resourceInfo.enable
        buildRedirectGroupInfoResult(
            iamRelatedResourceType = iamRelatedResourceType,
            isEnablePermission = isEnablePermission,
            groupInfoList = groupInfoList,
            userId = userId,
            projectId = projectId,
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
                defaultMessage = "权限系统: 获取权限申请跳转信息失败！"
            )
        }
        return AuthApplyRedirectInfoVo(
            auth = isEnablePermission,
            resourceTypeName = resourceTypeName,
            resourceName = resourceName,
            actionName = actionInfo.actionName,
            groupInfoList = groupInfoList
        )
    }

    private fun buildRedirectGroupInfoResult(
        iamRelatedResourceType: String,
        isEnablePermission: Boolean,
        groupInfoList: ArrayList<AuthRedirectGroupInfoVo>,
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String,
        action: String,
        resourceName: String,
        iamResourceCode: String
    ) {
        if (iamRelatedResourceType == AuthResourceType.PROJECT.value) {
            groupInfoList.add(
                AuthRedirectGroupInfoVo(
                    url = String.format(
                        authApplyRedirectUrl, userId, projectId,
                        "", resourceType, resourceName, action, iamResourceCode
                    )
                )
            )
        } else {
            if (isEnablePermission) {
                // 若开启权限,则得根据资源类型去查询默认组，然后查询组的策略，看是否包含对应 资源+动作
                val actionId = action.substring(action.lastIndexOf("_") + 1)
                authResourceGroupConfigDao.get(dslContext, resourceType).forEach {
                    val strategy = strategyService.getStrategyByName(it.resourceType + "_" + it.groupCode)?.strategy
                    if (strategy != null) {
                        val isStrategyContainsAction = strategy[resourceType]?.contains(actionId)
                        if (isStrategyContainsAction != null && isStrategyContainsAction) {
                            buildRedirectGroupInfo(
                                groupInfoList = groupInfoList,
                                projectId = projectId,
                                userId = userId,
                                resourceName = resourceName,
                                action = action,
                                resourceType = resourceType,
                                resourceCode = resourceCode,
                                groupCode = it.groupCode,
                                iamResourceCode = iamResourceCode
                            )
                        }
                    }
                }
            } else {
                buildRedirectGroupInfo(
                    groupInfoList = groupInfoList,
                    projectId = projectId,
                    userId = userId,
                    resourceName = resourceName,
                    action = action,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    groupCode = "manager",
                    iamResourceCode = iamResourceCode
                )
            }
        }
    }

    private fun buildRedirectGroupInfo(
        projectId: String,
        groupInfoList: ArrayList<AuthRedirectGroupInfoVo>,
        userId: String,
        resourceName: String,
        action: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
        iamResourceCode: String
    ) {
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
                        authApplyRedirectUrl, userId, projectId,
                        resourceGroup.relationId, resourceType, resourceName, action, iamResourceCode
                    ),
                    groupName = resourceGroup.groupName
                )
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GroupUserService::class.java)
    }
}
