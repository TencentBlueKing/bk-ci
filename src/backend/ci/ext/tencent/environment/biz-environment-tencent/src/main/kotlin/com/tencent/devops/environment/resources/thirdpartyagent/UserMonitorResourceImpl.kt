package com.tencent.devops.environment.resources.thirdpartyagent

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.thirdpartyagent.UserMonitorResource
import com.tencent.devops.environment.pojo.EnableDashboardResp
import com.tencent.devops.environment.service.BkBizProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserMonitorResourceImpl @Autowired constructor(
    private val bkBizProjectService: BkBizProjectService
) : UserMonitorResource {
    override fun checkEnableDashboard(userId: String, projectId: String): Result<EnableDashboardResp> {
        return Result(bkBizProjectService.checkEnableDashboard(projectId))
    }
}
