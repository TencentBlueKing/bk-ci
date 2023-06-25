package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.devops.auth.pojo.MonitorSpaceCreateInfo
import com.tencent.devops.auth.pojo.MonitorSpaceDetailVO

/**
 * 蓝盾权限对接监控平台配置类
 */
interface AuthMonitorService {
    /**
     * 生成监控平台组授权范围
     */
    fun generateMonitorAuthorizationScopes(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String? = null
    ): List<AuthorizationScopes>

    /**
     * 创建监控平台空间
     */
    fun createMonitorSpace(monitorSpaceCreateInfo: MonitorSpaceCreateInfo): MonitorSpaceDetailVO

    /**
     * 查询监控平台空间详情
     */
    fun getMonitorSpaceDetail(spaceUid: String): MonitorSpaceDetailVO?
}
