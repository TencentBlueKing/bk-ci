package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceGcloudConfResource
import com.tencent.devops.plugin.pojo.GcloudConf
import com.tencent.devops.plugin.service.gcloud.GcloudConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceGcloudConfResourceImpl @Autowired constructor(
    private val gcloudConfService: GcloudConfService
) : ServiceGcloudConfResource {
    override fun getByConfigId(configId: Int): Result<GcloudConf?> {
        return Result(gcloudConfService.getGcloudConf(configId))
    }
}