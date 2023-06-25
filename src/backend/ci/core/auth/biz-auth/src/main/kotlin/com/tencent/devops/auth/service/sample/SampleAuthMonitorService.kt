package com.tencent.devops.auth.service.sample

import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.devops.auth.pojo.MonitorSpaceCreateInfo
import com.tencent.devops.auth.pojo.MonitorSpaceDetailVO
import com.tencent.devops.auth.service.AuthMonitorService

class SampleAuthMonitorService : AuthMonitorService {
    override fun generateMonitorAuthorizationScopes(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): List<AuthorizationScopes> = emptyList()

    override fun createMonitorSpace(
        monitorSpaceCreateInfo: MonitorSpaceCreateInfo
    ): MonitorSpaceDetailVO = MonitorSpaceDetailVO(0L, "", "", "", "", "", "")

    override fun getMonitorSpaceDetail(spaceUid: String)
        : MonitorSpaceDetailVO? = MonitorSpaceDetailVO(0L, "", "", "", "", "", "")
}
