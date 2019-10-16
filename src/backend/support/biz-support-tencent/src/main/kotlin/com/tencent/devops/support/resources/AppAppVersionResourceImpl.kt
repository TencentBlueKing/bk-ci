package com.tencent.devops.support.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.AppAppVersionResource
import com.tencent.devops.support.model.app.pojo.AppVersion
import com.tencent.devops.support.services.AppVersionService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppAppVersionResourceImpl @Autowired constructor(private val appVersionService: AppVersionService) : AppAppVersionResource {
    override fun getAllAppVersion(channelType: Byte): Result<List<AppVersion>> {
        return Result(data = appVersionService.getAllAppVersionByChannelType(channelType))
    }

    override fun getLastAppVersion(channelType: Byte): Result<AppVersion?> {
        return Result(data = appVersionService.getLastAppVersion(channelType))
    }
}