package com.tencent.devops.project.service

import com.tencent.devops.project.pojo.MonitorSpaceCreateInfo
import com.tencent.devops.project.pojo.MonitorSpaceDetailVO

class SimpleProjectMonitorServiceImpl : ProjectMonitorService {
    override fun createMonitorSpace(
        monitorSpaceCreateInfo: MonitorSpaceCreateInfo
    ): MonitorSpaceDetailVO = MonitorSpaceDetailVO(0L, "", "", "", "", "", "")

    override fun getMonitorSpaceDetail(spaceUid: String):
        MonitorSpaceDetailVO = MonitorSpaceDetailVO(0L, "", "", "", "", "", "")
}
