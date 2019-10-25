package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildOnsResource
import com.tencent.devops.plugin.pojo.ons.OnsNameInfo
import com.tencent.devops.plugin.service.OnsService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildOnsResourceImpl @Autowired constructor(
    private val onsService: OnsService
) : BuildOnsResource {

    override fun getHostByDomainName(domainName: String): Result<OnsNameInfo?> {
        return onsService.getOnsNameInfo(domainName)
    }
}
