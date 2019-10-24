package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceMigCDNResource
import com.tencent.devops.plugin.pojo.migcdn.MigCDNUploadParam
import com.tencent.devops.plugin.service.MigCDNService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMigCDNResourceImpl @Autowired constructor(
    private val migCDNService: MigCDNService
) : ServiceMigCDNResource {
    override fun pushFile(uploadParam: MigCDNUploadParam): Result<String> {
        return Result(migCDNService.pushFile(uploadParam))
    }
}