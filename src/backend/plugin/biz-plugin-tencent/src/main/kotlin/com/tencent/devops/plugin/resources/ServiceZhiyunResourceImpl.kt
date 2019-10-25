package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceZhiyunResource
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunUploadParam
import com.tencent.devops.plugin.service.ZhiyunService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceZhiyunResourceImpl @Autowired constructor(
    private val zhiyunService: ZhiyunService
) : ServiceZhiyunResource {
    override fun pushFile(uploadParam: ZhiyunUploadParam): Result<List<String>> {
        return Result(zhiyunService.pushFile(uploadParam))
    }
}