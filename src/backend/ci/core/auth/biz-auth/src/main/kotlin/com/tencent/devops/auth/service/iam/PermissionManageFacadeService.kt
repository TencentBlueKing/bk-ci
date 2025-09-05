package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.DepartmentUserCount
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.InvalidAuthorizationsDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.enum.OperateChannel
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.BatchRemoveMemberFromProjectResponse
import com.tencent.devops.auth.pojo.request.GroupMemberCommonConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberHandoverConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRemoveConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberRenewalConditionReq
import com.tencent.devops.auth.pojo.request.GroupMemberSingleRenewalReq
import com.tencent.devops.auth.pojo.request.HandoverDetailsQueryReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewBatchUpdateReq
import com.tencent.devops.auth.pojo.request.HandoverOverviewUpdateReq
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.pojo.request.RemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ResourceType2CountOfHandoverQuery
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.HandoverAuthorizationDetailVo
import com.tencent.devops.auth.pojo.vo.HandoverGroupDetailVo
import com.tencent.devops.auth.pojo.vo.MemberExitsProjectCheckVo
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.common.api.model.SQLPage

/**
 * 权限管理门面类
 */
interface PermissionManageFacadeService {
    /**
     * 查询成员所在资源用户组详情
     * 管理员视角返回用户直接加入/模板加入的用户组
     * 个人视角返回用户直接加入/模板加入/组织加入的用户组
     * */
    fun getMemberGroupsDetails(
        projectId: String,
        memberId: String,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        groupName: String? = null,
        minExpiredAt: Long? = null,
        maxExpiredAt: Long? = null,
        relatedResourceType: String? = null,
        relatedResourceCode: String? = null,
        action: String? = null,
        operateChannel: OperateChannel? = OperateChannel.MANAGER,
        uniqueManagerGroupsQueryFlag: Boolean? = null,
        start: Int? = null,
        limit: Int? = null
    ): SQLPage<GroupDetailsInfoVo>

    /**
     * 获取用户有权限的用户组数量
     * 管理员视角返回用户直接加入/模板加入的用户组
     * 个人视角返回用户直接加入/模板加入/组织加入的用户组
     * */
    fun getMemberGroupsCount(
        projectCode: String,
        memberId: String,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        operateChannel: OperateChannel? = OperateChannel.MANAGER,
        uniqueManagerGroupsQueryFlag: Boolean?
    ): List<ResourceType2CountVo>

    /**
     * 根据条件查询组ID
     * */
    fun listIamGroupIdsByConditions(
        condition: IamGroupIdsQueryConditionDTO
    ): List<Int>

    /**
     * 查询成员所在资源用户组列表
     * 管理员视角返回用户直接加入/模板加入的用户组
     * 个人视角返回用户直接加入/模板加入/组织加入的用户组
     * */
    fun listResourceGroupMembers(
        projectCode: String,
        memberId: String,
        resourceType: String? = null,
        iamGroupIds: List<Int>? = null,
        minExpiredAt: Long? = null,
        maxExpiredAt: Long? = null,
        operateChannel: OperateChannel? = OperateChannel.MANAGER,
        filterMemberType: MemberType? = null,
        excludeIamGroupIds: List<Int>? = null,
        /*与excludeIamGroupIds参数搭配使用，用于排除用户直接加入的组*/
        onlyExcludeUserDirectlyJoined: Boolean? = false,
        start: Int? = null,
        limit: Int? = null
    ): Pair<Long, List<AuthResourceGroupMember>>

    /**
     * 根据复杂条件进行搜索，用于用户管理界面
     * */
    fun listProjectMembersByComplexConditions(
        conditionReq: ProjectMembersQueryConditionReq
    ): SQLPage<ResourceMemberInfo>

    /**
     * 为了避免流授权失效，需要对用户退出/交接用户组进行检查。
     * 入参：
     * 1、项目ID
     * 2、用户交接/移除的用户组（直接加入）
     * 3、用户ID
     * 返回结果：
     * 1、引起代持人权限失效的用户组。
     * 2、引起代持人权限失效的流水线。
     * 3、引起代码库oauth失效的代码库（当用户操作完组后，不再拥有项目访问权限时，会代码库oauth引起失效）
     * 4、引起失效的环境节点授权
     **/
    fun listInvalidAuthorizationsAfterOperatedGroups(
        projectCode: String,
        iamGroupIdsOfDirectlyJoined: List<Int>,
        memberId: String
    ): InvalidAuthorizationsDTO

    /**
     * 续期用户权限-无需审批版本
     * */
    fun renewalGroupMember(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberSingleRenewalReq
    ): Boolean

    /**
     * 批量续期用户权限-管理员视角
     * */
    fun batchRenewalGroupMembersFromManager(
        userId: String,
        projectCode: String,
        renewalConditionReq: GroupMemberRenewalConditionReq
    ): Boolean

    /**
     * 批量交接-管理员视角
     * */
    fun batchHandoverGroupMembersFromManager(
        userId: String,
        projectCode: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): Boolean

    /**
     * 批量交接申请-个人视角
     * */
    fun batchHandoverApplicationFromPersonal(
        userId: String,
        projectCode: String,
        handoverMemberDTO: GroupMemberHandoverConditionReq
    ): String

    /**
     * 批量移除-管理员视角
     * */
    fun batchDeleteResourceGroupMembersFromManager(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): Boolean

    /**
     * 批量退出-个人视角
     * */
    fun batchDeleteResourceGroupMembersFromPersonal(
        userId: String,
        projectCode: String,
        removeMemberDTO: GroupMemberRemoveConditionReq
    ): String

    /**
     * 退出单个组
     * */
    fun deleteResourceGroupMembers(
        userId: String,
        projectCode: String,
        groupId: Int,
        targetMember: ResourceMemberInfo
    ): Boolean

    /**
     * 批量操作检查
     * */
    fun batchOperateGroupMembersCheck(
        userId: String,
        projectCode: String,
        batchOperateType: BatchOperateType,
        conditionReq: GroupMemberCommonConditionReq
    ): BatchOperateGroupMemberCheckVo

    /**
     * 将用户移出项目-管理员视角
     * */
    fun removeMemberFromProject(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): List<ResourceMemberInfo>

    /**
     * 批量将用户移出项目-管理员视角
     * */
    fun batchRemoveMemberFromProject(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: BatchRemoveMemberFromProjectReq
    ): BatchRemoveMemberFromProjectResponse

    /**
     * 将用户移出项目检查-管理员视角
     * */
    fun removeMemberFromProjectCheck(
        userId: String,
        projectCode: String,
        removeMemberFromProjectReq: RemoveMemberFromProjectReq
    ): Boolean

    /**
     * 批量将用户移出项目检查-管理员视角
     * */
    fun batchRemoveMemberFromProjectCheck(
        userId: String,
        projectCode: String,
        targetMembers: List<ResourceMemberInfo>
    ): Boolean

    /**
     * 处理交接审批单
     * */
    fun handleHanoverApplication(request: HandoverOverviewUpdateReq): Boolean

    /**
     * 批量处理交接审批单
     * */
    fun batchHandleHanoverApplications(request: HandoverOverviewBatchUpdateReq): Boolean

    /**
     * 根据资源类型进行分类-交接
     * */
    fun getResourceType2CountOfHandover(queryReq: ResourceType2CountOfHandoverQuery): List<ResourceType2CountVo>

    /**
     * 获取交接中授权相关-分为预览/交接单审批两个场景
     * */
    fun listAuthorizationsOfHandover(queryReq: HandoverDetailsQueryReq): SQLPage<HandoverAuthorizationDetailVo>

    /**
     * 获取交接中用户组相关-分为预览/交接单审批两个场景
     * */
    fun listGroupsOfHandover(queryReq: HandoverDetailsQueryReq): SQLPage<HandoverGroupDetailVo>

    /**
     * 校验是否为项目成员
     * */
    fun isProjectMember(
        projectCode: String,
        userId: String
    ): Boolean

    /**
     * 用户主动退出项目检查
     * */
    fun checkMemberExitsProject(
        projectCode: String,
        userId: String
    ): MemberExitsProjectCheckVo

    /**
     * 用户主动退出项目
     * */
    fun memberExitsProject(
        projectCode: String,
        request: RemoveMemberFromProjectReq
    ): String

    /**
     * 获取项目用户部门分布情况
     * */
    fun getProjectUserDepartmentDistribution(
        projectCode: String,
        parentDepartmentId: Int
    ): List<DepartmentUserCount>
}
