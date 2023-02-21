package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.InstancesDTO
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
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
import com.tencent.devops.common.service.config.CommonConfig
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
    val config: CommonConfig
) : PermissionApplyService {
    @Value("\${auth.iamSystem:}")
    private val systemId = ""

    private val authApplyRedirectUrl = "${config.devopsHostGateway}/console/permission/%s/applyPermission?" +
        "projectId=%s&groupId=%s&resourceType=%s&resourceName=%s&action=%s"

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
        val projectInfo = authResourceService.get(
            projectCode = projectId,
            resourceType = "project",
            resourceCode = projectId
        )
        // 如果选择了资源实例，首先校验一下资源类型是否为空
        val resourceCode = searchGroupInfo.resourceCode
        if (resourceCode != null) {
            searchGroupInfo.resourceType ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.RESOURCE_TYPE_NOT_EMPTY,
                params = arrayOf(resourceCode),
                defaultMessage = "权限系统：资源实例筛选时，资源类型不能为空！"
            )
        }

        // 如果资源实例不为空，则bkIamPath 得拼成 /bk_ci_rbac,searchGroupInfo.resourceType,projectId/
        // 然后进行搜索一次。
        // 接着如果该资源类型是流水线，这得搜索出所有包含该流水线的流水线组的id，然后拼成/bk_ci_rbac,searchGroupInfo.resourceType,projectId/
        // 查找组，然后
        val searchGroupDTO = SearchGroupDTO
            .builder()
            .inherit(searchGroupInfo.inherit)
            .id(searchGroupInfo.groupId)
            .actionId(searchGroupInfo.actionId)
            .resourceTypeSystemId(systemId)
            .resourceTypeId(searchGroupInfo.resourceType)
            .resourceId(searchGroupInfo.resourceCode)
            .bkIamPath(searchGroupInfo.bkIamPath)
            .name(searchGroupInfo.name)
            .description(searchGroupInfo.description)
            .build()
        val v2PageInfoDTO = V2PageInfoDTO()
        v2PageInfoDTO.pageSize = searchGroupInfo.pageSize
        v2PageInfoDTO.page = searchGroupInfo.page




        try {
            // 校验用户是否属于组
            val managerRoleGroupVO = v2ManagerService.getGradeManagerRoleGroupV2(projectInfo.relationId, searchGroupDTO, v2PageInfoDTO)
            val groupInfoList = managerRoleGroupVO.results
            val groupIds = groupInfoList.map { it.id }.joinToString(",")
            val verifyGroupValidMember = v2ManagerService.verifyGroupValidMember(userId, groupIds)
            groupInfoList.forEach {
                it.joined = verifyGroupValidMember[it.id.toInt()]?.belong ?: false
            }
            return managerRoleGroupVO
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_IAM_GROUP_FAIL,
                defaultMessage = "权限系统：获取用户组失败！"
            )
        }
    }

    override fun applyToJoinGroup(userId: String, applyJoinGroupInfo: ApplyJoinGroupInfo): Boolean {
        try {
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
            handleRelatedResourceTypesDTO(instancesDTO = relatedResourceTypesDTO.condition[0].instances[0])
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

    private fun handleRelatedResourceTypesDTO(instancesDTO: InstancesDTO) {
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
        val isEnablePermission: Boolean
        if (iamRelatedResourceType == AuthResourceType.PROJECT.value) {
            isEnablePermission = false
            groupInfoList.add(
                AuthRedirectGroupInfoVo(
                    url = String.format(
                        authApplyRedirectUrl, userId, projectId,
                        "", resourceType, resourceName, action
                    )
                )
            )
        } else {
            isEnablePermission = resourceInfo.enable
            if (isEnablePermission) {
                // 若开启权限,则得根据资源类型去查询默认组，然后查询组的策略，看是否包含对应 资源+动作
                val actionId = action.substring(action.lastIndexOf("_") + 1)
                authResourceGroupConfigDao.get(dslContext, resourceType).forEach {
                    val strategy = strategyService.getStrategyByName(it.resourceType + "_" + it.groupCode)?.strategy
                    if (strategy != null) {
                        val isStrategyContainsAction = strategy[resourceType]?.contains(actionId)
                        if (isStrategyContainsAction != null && isStrategyContainsAction) {
                            buildGroupInfoList(
                                groupInfoList = groupInfoList,
                                projectId = projectId,
                                userId = userId,
                                resourceName = resourceName,
                                action = action,
                                resourceType = resourceType,
                                resourceCode = resourceCode,
                                groupCode = it.groupCode
                            )
                        }
                    }
                }
            } else {
                buildGroupInfoList(
                    groupInfoList = groupInfoList,
                    projectId = projectId,
                    userId = userId,
                    resourceName = resourceName,
                    action = action,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    groupCode = "manager"
                )
            }

        }
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

    private fun buildGroupInfoList(
        projectId: String,
        groupInfoList: ArrayList<AuthRedirectGroupInfoVo>,
        userId: String,
        resourceName: String,
        action: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
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
                        resourceGroup.relationId, resourceType, resourceName, action
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
