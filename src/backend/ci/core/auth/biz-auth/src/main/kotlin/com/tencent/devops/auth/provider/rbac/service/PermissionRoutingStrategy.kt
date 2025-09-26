package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.pojo.enum.RoutingMode

/**
 * 权限路由策略接口，定义了如何根据项目代码获取其应采用的权限校验路由模式。
 */
interface PermissionRoutingStrategy {

    /**
     * 根据项目ID获取其对应的路由模式
     *
     * @param projectCode 项目ID
     * @return 对应的路由模式 (RoutingMode)
     */
    fun getModeForProject(projectCode: String): RoutingMode

    /**
     * 获取默认模式
     */
    fun getDefaultMode(): RoutingMode
}
