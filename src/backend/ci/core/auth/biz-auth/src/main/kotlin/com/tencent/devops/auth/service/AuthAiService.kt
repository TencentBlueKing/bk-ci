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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.enum.BatchOperateType
import com.tencent.devops.auth.pojo.request.ai.AiApplyJoinGroupReq
import com.tencent.devops.auth.pojo.request.ai.AiRemoveMemberFromProjectReq
import com.tencent.devops.auth.pojo.request.ai.BatchHandoverMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchOperateCheckReq
import com.tencent.devops.auth.pojo.request.ai.BatchRemoveMembersReq
import com.tencent.devops.auth.pojo.request.ai.BatchRenewalMembersReq
import com.tencent.devops.auth.pojo.request.ai.GroupRecommendReq
import com.tencent.devops.auth.pojo.vo.AuthorizationHealthVO
import com.tencent.devops.auth.pojo.vo.BatchOperateGroupMemberCheckVo
import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.auth.pojo.vo.GroupDetailsInfoVo
import com.tencent.devops.auth.pojo.vo.GroupRecommendationVO
import com.tencent.devops.auth.pojo.vo.MemberExitCheckVO
import com.tencent.devops.auth.pojo.vo.PermissionCloneResultVO
import com.tencent.devops.auth.pojo.vo.PermissionCompareVO
import com.tencent.devops.auth.pojo.vo.PermissionDiagnoseVO
import com.tencent.devops.auth.pojo.vo.ResolvedUserByNameVO
import com.tencent.devops.auth.pojo.vo.ResourcePermissionsMatrixVO
import com.tencent.devops.auth.pojo.vo.ResourceType2CountVo
import com.tencent.devops.auth.pojo.vo.UserPermissionAnalysisVO
import com.tencent.devops.auth.pojo.vo.UserSearchResultVO
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.common.api.model.SQLPage

/**
 * AI 场景权限服务接口
 * 提供权限诊断、克隆、对比、健康检查等 AI 辅助功能
 */
interface AuthAiService {

    /**
     * 权限诊断 - 分析用户为什么没有某个权限
     *
     * 权限要求：管理员可诊断任意成员，普通成员只能诊断自己
     *
     * @param userId 操作人用户ID
     * @param projectId 项目ID
     * @param memberId 目标成员ID
     * @param resourceType 资源类型
     * @param resourceCode 资源Code
     * @param action 操作类型
     * @return 诊断结果，包含缺失原因和可申请的用户组
     */
    fun diagnosePermission(
        userId: String,
        projectId: String,
        memberId: String,
        resourceType: String,
        resourceCode: String,
        action: String
    ): PermissionDiagnoseVO

    /**
     * 权限克隆 - 将一个用户的权限复制给另一个用户
     *
     * 权限要求：仅管理员可操作
     *
     * @param userId 操作人用户ID
     * @param projectId 项目ID
     * @param sourceUserId 来源用户ID
     * @param targetUserId 目标用户ID
     * @param resourceTypes 限定的资源类型列表，为空则克隆所有类型
     * @param dryRun 是否预检查模式，true=只返回将要克隆的内容，false=执行克隆
     * @return 克隆结果
     */
    fun clonePermissions(
        userId: String,
        projectId: String,
        sourceUserId: String,
        targetUserId: String,
        resourceTypes: List<String>?,
        dryRun: Boolean
    ): PermissionCloneResultVO

    /**
     * 权限对比 - 比较两个用户的权限差异
     *
     * 权限要求：仅管理员可操作
     *
     * @param userId 操作人用户ID
     * @param projectId 项目ID
     * @param userIdA 用户A的ID
     * @param userIdB 用户B的ID
     * @param resourceType 限定的资源类型，为空则对比所有类型
     * @return 对比结果
     */
    fun comparePermissions(
        userId: String,
        projectId: String,
        userIdA: String,
        userIdB: String,
        resourceType: String?
    ): PermissionCompareVO

    /**
     * 授权健康检查 - 扫描项目的授权风险
     *
     * 权限要求：仅管理员可操作
     *
     * 检测项：
     * - 流水线代持人即将离职/已离职
     * - 代码库授权人不在代码库成员中
     * - 环境节点授权人不是 CMDB 负责人
     * - 授权人权限即将过期
     * - 单点授权风险（只有一个授权人）
     *
     * @param userId 操作人用户ID
     * @param projectId 项目ID
     * @return 健康检查结果
     */
    fun checkAuthorizationHealth(
        userId: String,
        projectId: String
    ): AuthorizationHealthVO

    /**
     * 用户搜索 - 根据关键词搜索用户
     *
     * 权限要求：项目成员可操作
     *
     * @param userId 操作人用户ID
     * @param keyword 搜索关键词
     * @param projectId 项目ID，可选，限定在项目成员中搜索
     * @param limit 返回数量限制
     * @return 搜索结果
     */
    fun searchUsers(
        userId: String,
        keyword: String,
        projectId: String?,
        limit: Int
    ): UserSearchResultVO

    /**
     * 按中文显示名解析用户列表（精确匹配 USER_NAME，可能重名多条）
     *
     * @param userName 用户中文名称
     * @return 匹配到的用户列表，无匹配时为空列表
     */
    fun resolveUsersByName(
        userName: String
    ): List<ResolvedUserByNameVO>

    /**
     * 检查成员退出/交接权限 - 综合检查并返回推荐交接人
     *
     * 权限要求：
     * - 自己退出/交接：项目成员可操作
     * - 移出他人/帮他人交接：仅管理员可操作
     *
     * 该方法会：
     * 1. 检查目标成员是否可以直接退出（无授权需要交接）
     * 2. 推荐合适的交接人列表
     * 3. 如果指定了交接人，验证交接人能否接收全部授权
     * 4. 如果交接人无法接收全部授权，返回替代推荐
     *
     * 支持两种场景：
     * - 退出整个项目：groupIds 为空
     * - 交接特定用户组：groupIds 指定要交接的用户组
     *
     * @param userId 操作人用户ID
     * @param projectId 项目ID
     * @param targetMemberId 目标成员ID
     * @param handoverTo 指定的交接人ID（可选，不传则只返回推荐列表）
     * @param groupIds 用户组ID列表，逗号分隔（可选，不传则检查退出整个项目）
     * @param recommendLimit 推荐候选人数量限制
     * @return 检查结果，包含推荐交接人
     */
    fun checkMemberExitWithRecommendation(
        userId: String,
        projectId: String,
        targetMemberId: String,
        handoverTo: String? = null,
        groupIds: String? = null,
        recommendLimit: Int = 5
    ): MemberExitCheckVO

    fun searchResource(
        userId: String,
        projectId: String,
        resourceType: String,
        keyword: String
    ): List<AuthResourceInfo>

    fun listAuthResourceGroups(
        userId: String,
        projectId: String,
        condition: IamGroupIdsQueryConditionDTO
    ): SQLPage<AuthResourceGroup>

    fun getGroupPermissionDetail(
        userId: String,
        projectId: String,
        groupId: Int
    ): List<ResourceGroupPermissionDTO>

    /**
     * 查询用户组成员详情列表，支持丰富的筛选条件。
     */
    fun listGroupMembers(
        userId: String,
        projectId: String,
        resourceType: String?,
        resourceCode: String?,
        iamGroupId: Int?,
        groupCode: String?,
        memberId: String?,
        memberType: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        page: Int,
        pageSize: Int
    ): SQLPage<AuthResourceGroupMember>

    fun getMemberGroupCount(
        userId: String,
        projectId: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?
    ): List<ResourceType2CountVo>

    fun getMemberGroupsDetails(
        userId: String,
        projectId: String,
        resourceType: String,
        memberId: String,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        page: Int,
        pageSize: Int
    ): SQLPage<GroupDetailsInfoVo>

    fun getAllMemberGroupsDetails(
        userId: String,
        projectId: String,
        memberId: String,
        resourceType: String?,
        iamGroupIds: String?,
        groupName: String?,
        minExpiredAt: Long?,
        maxExpiredAt: Long?,
        relatedResourceType: String?,
        relatedResourceCode: String?,
        action: String?,
        page: Int,
        pageSize: Int
    ): SQLPage<GroupDetailsInfoVo>

    fun batchRenewalMembers(
        userId: String,
        projectId: String,
        request: BatchRenewalMembersReq
    ): Boolean

    /**
     * 普通用户申请续期权限（需要审批）
     */
    fun applyRenewalGroupMember(
        userId: String,
        projectId: String,
        groupIds: String,
        renewalDays: Int,
        reason: String
    ): Boolean

    fun batchRemoveMembers(
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): Boolean

    fun batchHandoverMembers(
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): Boolean

    /**
     * 成员自助退出用户组（个人视角）
     */
    fun exitGroupsFromPersonal(
        userId: String,
        projectId: String,
        request: BatchRemoveMembersReq
    ): String

    /**
     * 成员自助申请交接用户组（个人视角，需审批）
     */
    fun applyHandoverFromPersonal(
        userId: String,
        projectId: String,
        request: BatchHandoverMembersReq
    ): String

    fun batchOperateCheck(
        userId: String,
        projectId: String,
        batchOperateType: BatchOperateType,
        request: BatchOperateCheckReq
    ): BatchOperateGroupMemberCheckVo

    fun removeMemberFromProject(
        userId: String,
        projectId: String,
        request: AiRemoveMemberFromProjectReq
    ): List<ResourceMemberInfo>

    fun analyzeUserPermissions(
        userId: String,
        projectId: String,
        memberId: String
    ): UserPermissionAnalysisVO

    fun getResourcePermissionsMatrix(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): ResourcePermissionsMatrixVO

    fun recommendGroupsForGrant(
        userId: String,
        projectId: String,
        request: GroupRecommendReq
    ): GroupRecommendationVO

    fun applyToJoinGroup(
        userId: String,
        projectId: String,
        request: AiApplyJoinGroupReq
    ): Boolean
}
