package com.tencent.devops.auth.provider.sample.service

import com.tencent.devops.auth.pojo.MonitorSpaceCreateInfo
import com.tencent.devops.auth.pojo.MonitorSpaceDetailVO
import com.tencent.devops.auth.service.AuthMonitorSpaceService

class SampleAuthMonitorSpaceService : AuthMonitorSpaceService {
    override fun createMonitorSpace(monitorSpaceCreateInfo: MonitorSpaceCreateInfo): String = ""

    override fun getOrCreateMonitorSpace(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): String = ""

    override fun getMonitorSpaceBizId(projectCode: String): String = ""

    override fun listMonitorSpaceBizIds(projectCode: List<String>): Map<String, String> {
        return emptyMap()
    }

    override fun getMonitorSpaceDetail(spaceUid: String): MonitorSpaceDetailVO? = null

    override fun getMonitorGroupConfig(groupCode: String): String? = null

    override fun getMonitorActionName(action: String): String? = null
}
