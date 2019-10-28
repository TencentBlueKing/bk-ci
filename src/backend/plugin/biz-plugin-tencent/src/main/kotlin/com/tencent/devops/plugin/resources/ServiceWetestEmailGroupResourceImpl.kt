package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceWetestEmailGroupResource
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroup
import com.tencent.devops.plugin.service.WetestEmailGroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceWetestEmailGroupResourceImpl @Autowired constructor(private val wetestEmailGroupService: WetestEmailGroupService)
    : ServiceWetestEmailGroupResource {
    override fun get(projectId: String, id: Int): Result<WetestEmailGroup?> {
        return Result(wetestEmailGroupService.getWetestEmailGroup(projectId, id))
    }
}