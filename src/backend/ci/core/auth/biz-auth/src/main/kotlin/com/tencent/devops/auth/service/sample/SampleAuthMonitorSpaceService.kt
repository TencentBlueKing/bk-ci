package com.tencent.devops.auth.service.sample

import com.tencent.devops.auth.pojo.MonitorSpaceCreateInfo
import com.tencent.devops.auth.pojo.MonitorSpaceDetailVO
import com.tencent.devops.auth.service.AuthMonitorSpaceService

class SampleAuthMonitorSpaceService : AuthMonitorSpaceService {
    override fun createMonitorSpace(monitorSpaceCreateInfo: MonitorSpaceCreateInfo): String = ""

    override fun getMonitorSpaceBizId(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): String = ""

    override fun getMonitorSpaceDetail(spaceUid: String): MonitorSpaceDetailVO? = null
}
