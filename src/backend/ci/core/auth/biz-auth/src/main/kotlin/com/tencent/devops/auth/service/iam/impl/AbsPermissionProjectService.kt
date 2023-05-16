package com.tencent.devops.auth.service.iam.impl

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.common.Constants.ALL_ACTION
import com.tencent.devops.auth.common.Constants.PROJECT_VIEW
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.utils.AuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("LongParameterList", "MagicNumber", "ReturnCount", "NestedBlockDepth", "ForbiddenComment")
abstract class AbsPermissionProjectService @Autowired constructor(
    open val permissionRoleService: PermissionRoleService,
    open val permissionRoleMemberService: PermissionRoleMemberService,
    open val authHelper: AuthHelper,
    open val policyService: PolicyService,
    open val client: Client,
    open val iamConfiguration: IamConfiguration,
    open val deptService: DeptService,
    open val groupService: AuthGroupService,
    open val iamCacheService: IamCacheService
) : PermissionProjectService {

    private val projectIdCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, String>()

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        val allGroupAndUser = getProjectGroupAndUserList(projectCode)
        return if (group == null) {
            val allMembers = mutableSetOf<String>()
            allGroupAndUser.map { allMembers.addAll(it.userIdList) }
            allMembers.toList()
        } else {
            // 获取扩展系统内项目下的用户
            getUserByExt(group, projectCode)
        }
    }

    override fun getUserProjectsByPermission(
        userId: String,
        action: String
    ): List<String> {
        return emptyList()
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        // 1. 转换projectCode为iam侧分级管理员Id
        val iamProjectId = getProjectId(projectCode)
        // 2. 获取项目下的所有用户组
        val roleInfos = permissionRoleService.getPermissionRole(iamProjectId)
        logger.info(
            "[IAM] getProjectGroupAndUserList: projectCode = $projectCode |" +
                " iamProjectId = $iamProjectId | roleInfos: $roleInfos"
        )
        val result = mutableListOf<BkAuthGroupAndUserList>()
        // 3. 获取用户组下的所有用户
        roleInfos.forEach {
            val groupMemberInfos = permissionRoleMemberService.getRoleMember(iamProjectId, it.id, 0, 1000).results
            logger.info("[IAM] $projectCode $iamProjectId ,role ${it.id} | users $groupMemberInfos")
            val members = mutableListOf<String>()
            groupMemberInfos.forEach { memberInfo ->
                // 如果为组织需要获取组织对应的用户
                if (memberInfo.type == ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)) {
                    logger.info("[IAM] $projectCode $iamProjectId ,role ${it.id} | dept ${memberInfo.id}")
                    val deptUsers = deptService.getDeptUser(memberInfo.id.toInt(), null)
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
        // v3 会拉取用户有 PROJECT_VIEW+ALL_ACTION的项目，rbac会拉取出用户有PROJECT_VIEW的项目
        // 所以要拉出v3(PROJECT_VIEW+ALL_ACTION)就会包含rbac(PROJECT_VIEW)的项目
        val viewAction = PROJECT_VIEW
        val managerAction = ALL_ACTION
        val actionDTOs = mutableListOf<ActionDTO>()
        val viewActionDto = ActionDTO()
        viewActionDto.id = viewAction
        val managerActionDto = ActionDTO()
        managerActionDto.id = managerAction
        actionDTOs.add(viewActionDto)
        actionDTOs.add(managerActionDto)
        val actionPolicyDTOs = policyService.batchGetPolicyByActionList(userId, actionDTOs, null) ?: return emptyList()
        logger.info("[IAM] getUserProjects: actionPolicyDTOs = $actionPolicyDTOs")
        val projectCodes = mutableSetOf<String>()
        actionPolicyDTOs.forEach {
            projectCodes.addAll(AuthUtils.getProjects(it.condition))
        }
        return projectCodes.toList()
    }

    /**
     * 判断是否为项目下用户, 若提供角色，则需要判断是否为改角色下用户
     * 优先判断all_action(项目管理员)
     * 若未提供需判断是否有project_veiws权限
     */
    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {

        val managerPermission = checkProjectManager(userId, projectCode)
        // 若为校验管理员权限,直接返回是否有all_action接口
        if (group != null && group == BkAuthGroup.MANAGER) {
            return managerPermission
        }
        // 有管理员权限直接返回
        if (managerPermission) {
            return managerPermission
        }

        // 优先匹配缓存
        if (iamCacheService.checkProjectUser(userId, projectCode)) {
            return true
        }
        // 获取用户所在的所有组,包括以组织架构形式加入的用户组
        val joinGroupIds = policyService.getUserGroup(userId, true)?.map { it.id } ?: emptyList()
        val projectGroupIds = iamCacheService.getProjectGroup(projectCode)
        // 加入的用户组与项目下的用户组取交集。若有交集说明加入的用户组内存在待校验项目下的用户组
        return joinGroupIds.intersect(projectGroupIds).isNotEmpty()
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        return iamCacheService.checkProjectManager(userId, projectCode)
    }

    override fun createProjectUser(userId: String, projectCode: String, roleCode: String): Boolean {
        val extProjectId = getProjectId(projectCode)
        val projectRoles = groupService.getGroupByCode(projectCode, roleCode)
        if (projectRoles == null) {
            logger.warn("$projectCode | $roleCode group not exists")
            throw ParamBlankException("user group $roleCode not exists")
        }
        val managerRole = roleCode == BkAuthGroup.MANAGER.value
        val members = mutableListOf<RoleMemberDTO>()
        members.add(RoleMemberDTO(type = ManagerScopesEnum.USER, id = userId))
        permissionRoleMemberService.createRoleMember(
            userId = userId,
            projectId = extProjectId,
            roleId = projectRoles!!.relationId.toInt(),
            members = members,
            managerGroup = managerRole
        )
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        val roleInfos = permissionRoleService.getPermissionRole(projectId.toInt())
        logger.info("[IAM] getProjectRoles : roleInfos = $roleInfos")
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

    fun getProjectId(projectCode: String): Int {
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
            throw ErrorCodeException(errorCode = AuthMessageCode.RELATED_RESOURCE_EMPTY)
        }
        return iamProjectId.toInt()
    }

    abstract fun getUserByExt(group: BkAuthGroup, projectCode: String): List<String>

    companion object {
        private val logger = LoggerFactory.getLogger(AbsPermissionProjectService::class.java)
    }
}
