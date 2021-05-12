package com.tencent.devops.auth.service.iam.impl

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.utils.AuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

open class AbsPermissionProjectService @Autowired constructor(
    open val permissionRoleService: PermissionRoleService,
    open val permissionRoleMemberService: PermissionRoleMemberService,
    open val authHelper: AuthHelper,
    open val policyService: PolicyService,
    open val client: Client,
    open val iamConfiguration: IamConfiguration,
    open val deptService: DeptService
) : PermissionProjectService {

    private val projectIdCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, String>()

    override fun getProjectUsers(serviceCode: String, projectCode: String, group: BkAuthGroup?): List<String> {
        val allGroupAndUser = getProjectGroupAndUserList(serviceCode, projectCode)
        return if (group == null) {
            val allMembers = mutableListOf<String>()
            allGroupAndUser.map { allMembers.addAll(it.userIdList) }
            allMembers
        } else {
            // TODO: 待iam完成group lable后补齐
            // 通过分组类型匹配分组用户
            emptyList()
        }
    }

    override fun getProjectGroupAndUserList(serviceCode: String, projectCode: String): List<BkAuthGroupAndUserList> {
        // 1. 转换projectCode为iam侧分级管理员Id
        val iamProjectId = getProjectId(projectCode)
        // 2. 获取项目下的所有用户组
        val roleInfos = permissionRoleService.getPermissionRole(iamProjectId).result
        logger.info("[IAM] $projectCode $iamProjectId roleInfos: $roleInfos")
        val result = mutableListOf<BkAuthGroupAndUserList>()
        // 3. 获取用户组下的所有用户
        roleInfos.forEach {
            val groupMemberInfos = permissionRoleMemberService.getRoleMember(iamProjectId, it.id, 0, 1000).results
            logger.info("[IAM] $projectCode $iamProjectId ,role ${it.id}| users $groupMemberInfos")
            val members = mutableListOf<String>()
            groupMemberInfos.forEach { memberInfo ->
                // 如果为组织需要获取组织对应的用户
                if (memberInfo.type == ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)) {
                    logger.info("[IAM] $projectCode $iamProjectId ,role ${it.id}| dept ${memberInfo.id}")
                    val deptUsers = deptService.getDeptUser(memberInfo.id.toInt()) ?: null
                    if (deptUsers != null) {
                        members.addAll(deptUsers)
                    }
                } else {
                    members.add(memberInfo.id)
                }
            }
            val groupAndUser = BkAuthGroupAndUserList(
                displayName = it.name,
                roleId = it.id,
                roleName = it.name,
                userIdList = members,
                // TODO: 待iam完成group lable后补齐
                type = ""
            )
            result.add(groupAndUser)
        }
        return result
    }

    override fun getUserProjects(userId: String): List<String> {
        val viewAction = "project_view"
        val managerAction = "all_action"
        val actionDTOs = mutableListOf<ActionDTO>()
        val viewActionDto = ActionDTO()
        viewActionDto.id = viewAction
        val managerActionDto = ActionDTO()
        managerActionDto.id = managerAction
        actionDTOs.add(viewActionDto)
        actionDTOs.add(managerActionDto)
        val actionPolicyDTOs = policyService.batchGetPolicyByActionList(userId, actionDTOs, null) ?: return emptyList()
        logger.info("[IAM] getUserProjects actionPolicyDTOs $actionPolicyDTOs")
        val actionPolicy = actionPolicyDTOs[0]
        return AuthUtils.getProjects(actionPolicy.condition)
    }

    /**
     * 判断是否为项目下用户, 若提供角色，则需要判断是否为改角色下用户
     * 优先判断all_action(项目管理员)
     * 若未提供需判断是否有project_veiws权限
     */
    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {

        val managerActionType = "all_action"
        val instance = buildInstance(projectCode, AuthResourceType.PROJECT.value)
        logger.info("[IAM] v3 isProjectUser actionType[$managerActionType] instance[$instance]")
        val managerPermission = authHelper.isAllowed(userId, managerActionType, instance)
        // 若为校验管理员权限,直接返回是否有all_action接口
        if (group != null && group == BkAuthGroup.MANAGER) {
            return managerPermission
        }
        // 有管理员权限直接返回
        if (managerPermission) {
            return managerPermission
        }
        // 无管理员权限判断是否有项目查看权限来判断是否为项目人员
        val viewActionType = "project_view"
        return authHelper.isAllowed(userId, viewActionType, instance)
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        val projectId = getProjectId(projectCode)
        val projectRoles = permissionRoleService.getPermissionRole(projectId).result
        var roleId = 0
        projectRoles.forEach {
            // TODO: 通过roleType匹配对应的roleId

        }
        val managerRole = role == BkAuthGroup.MANAGER.value
        val members = mutableListOf<RoleMemberDTO>()
        members.add(RoleMemberDTO(type = ManagerScopesEnum.USER, id = userId))
        permissionRoleMemberService.createRoleMember(userId, projectId, roleId, members, managerRole)
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        val roleInfos = permissionRoleService.getPermissionRole(projectId.toInt()).result
        logger.info("[IAM] getProjectRole $roleInfos")
        val roleList = mutableListOf<BKAuthProjectRolesResources>()
        roleInfos.forEach {
            val role = BKAuthProjectRolesResources(
                displayName = it.name,
                roleName = it.name,
                roleId = it.id,
                type = ""
            )
            roleList.add(role)
        }
        return roleList
    }

    private fun getProjectId(projectCode: String): Int {
        val iamProjectId = if (projectIdCache.getIfPresent(projectCode) != null) {
            projectIdCache.getIfPresent(projectCode)!!
        } else {
            val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data

            if (projectInfo != null && !projectInfo.relationId.isNullOrEmpty()) {
                projectIdCache.put(projectCode, projectInfo!!.relationId!!)
            }
            projectInfo?.relationId
        }
        if (iamProjectId.isNullOrEmpty()) {
            logger.warn("[IAM] $projectCode iamProject is empty")
            throw RuntimeException()
        }
        return iamProjectId.toInt()
    }

    private fun buildInstance(id: String, type: String): InstanceDTO {
        val instance = InstanceDTO()
        instance.id = id
        instance.system = iamConfiguration.systemId
        instance.type = type
        return instance
    }

    companion object {
        val logger = LoggerFactory.getLogger(AbsPermissionProjectService::class.java)
    }
}
