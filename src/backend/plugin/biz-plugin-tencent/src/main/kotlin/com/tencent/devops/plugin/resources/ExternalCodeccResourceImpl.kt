package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ExternalCodeccResource
import com.tencent.devops.plugin.pojo.codecc.CodeccCallback
import com.tencent.devops.plugin.service.CodeccService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalCodeccResourceImpl @Autowired constructor(
    private val codeccService: CodeccService
) : ExternalCodeccResource {
    override fun callback(callback: CodeccCallback): Result<String> {
        return Result(codeccService.callback(callback))
    }
}
