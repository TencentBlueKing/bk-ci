package com.tencent.devops.auth.service

import com.tencent.devops.auth.pojo.MonitorSpaceCreateInfo
import com.tencent.devops.auth.pojo.MonitorSpaceDetailVO

/**
 * 监控平台接口类
 */
interface AuthMonitorSpaceService {
    /**
     * 创建监控空间
     */
    fun createMonitorSpace(monitorSpaceCreateInfo: MonitorSpaceCreateInfo): String

    /**
     * 获取监控空间业务id
     */
    fun getMonitorSpaceBizId(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): String

    /**
     * 获取监控空间详情
     */
    fun getMonitorSpaceDetail(spaceUid: String): MonitorSpaceDetailVO?
}
