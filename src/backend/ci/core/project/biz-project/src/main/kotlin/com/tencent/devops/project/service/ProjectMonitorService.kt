package com.tencent.devops.project.service

import com.tencent.devops.project.pojo.MonitorSpaceCreateInfo
import com.tencent.devops.project.pojo.MonitorSpaceDetailVO

/**
 * 蓝盾项目对接监控平台类
 */
interface ProjectMonitorService {
    /**
     * 创建监控平台空间
     */
    fun createMonitorSpace(monitorSpaceCreateInfo: MonitorSpaceCreateInfo): MonitorSpaceDetailVO

    /**
     * 查询监控平台空间详情
     */
    fun getMonitorSpaceDetail(spaceUid: String): MonitorSpaceDetailVO
}
