package com.tencent.devops.process.permission.`var`

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.process.pojo.`var`.PublicVarGroupPermissions

interface PublicVarGroupPermissionService {
    /**
     * 校验公共变量组权限
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param groupName 变量组ID
     * @param permission 权限类型
     * @return 有权限返回true
     */
    fun checkPublicVarGroupPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        groupName: String
    ): Boolean

    /**
     * 校验公共变量组权限（带异常抛出）
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param groupName 变量组ID
     * @param permission 权限类型
     * @return 有权限返回true，无权限抛出异常
     */
    fun checkPublicVarGroupPermissionWithMessage(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        groupName: String
    ): Boolean

    /**
     * 校验公共变量组权限（项目级别）
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param permission 权限类型
     * @return 有权限返回true，无权限抛出异常
     */
    fun checkPublicVarGroupPermissions(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean

    /**
     * 获取用户对公共变量组的所有权限
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param groupName 变量组ID（可选）
     * @return PublicVarGroupPermissions对象
     */
    fun getPublicVarGroupPermissions(
        userId: String,
        projectId: String,
        groupName: String
    ): PublicVarGroupPermissions

    /**
     * 获取拥有指定权限的公共变量组资源
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param permissions 权限集合
     */
    fun getResourcesByPermission(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>>

    /**
     * 批量过滤公共变量组权限
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param authPermissions 权限集合
     * @param groupNames 变量组名称列表
     * @return 权限与对应的变量组名称列表的映射
     */
    fun filterPublicVarGroups(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>,
        groupNames: List<String>
    ): Map<AuthPermission, List<String>>

    /**
     * 注册公共变量组到权限中心
     * @param userId 用户ID
     * @param projectId 项目ID
     * @param groupCode 变量组ID
     * @param groupName 变量组名称
     */
    fun createResource(
        userId: String,
        projectId: String,
        groupCode: String,
        name: String
    )

    /**
     * 从权限中心删除公共变量组资源
     * @param projectId 项目ID
     * @param groupName 变量组ID
     */
    fun deleteResource(
        projectId: String,
        groupName: String
    )
}
