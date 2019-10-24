package com.tencent.devops.plugin.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ExternalJinGangResourece
import com.tencent.devops.plugin.pojo.JinGangAppCallback
import com.tencent.devops.plugin.service.JinGangService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalJinGangResoureceImpl @Autowired constructor(
    private val jinGangService: JinGangService
) : ExternalJinGangResourece {
    override fun callback(data: JinGangAppCallback) {
        jinGangService.callback(data)
    }
}