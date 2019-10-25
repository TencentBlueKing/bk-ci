package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ExternalWetestResource
import com.tencent.devops.plugin.service.WetestService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalWetestResourceImpl @Autowired constructor(
    private val wetestService: WetestService
) : ExternalWetestResource {
    override fun taskCallback(testId: String, callback: Map<String, Any>): Result<String> {
        return Result(wetestService.taskCallback(testId, callback))
    }
}