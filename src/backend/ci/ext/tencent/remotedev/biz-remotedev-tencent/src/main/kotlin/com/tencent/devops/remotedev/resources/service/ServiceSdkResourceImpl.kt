package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceSDKResource
import com.tencent.devops.remotedev.pojo.DesktopTokenSign
import com.tencent.devops.remotedev.pojo.sdk.SdkReportData
import com.tencent.devops.remotedev.service.RemotedevSdkService
import org.slf4j.LoggerFactory

@RestResource
@Suppress("ALL")
class ServiceSdkResourceImpl(
    private val remotedevSdkService: RemotedevSdkService
) : ServiceSDKResource {
    companion object {
        private val logger = LoggerFactory.getLogger(ServiceSdkResourceImpl::class.java)
    }

    override fun getToken(desktopIP: String, sign: DesktopTokenSign): Result<String> {
        return Result(remotedevSdkService.getAppToken(desktopIP, sign))
    }

    override fun sdkGetAccessToken(desktopIP: String, sign: DesktopTokenSign): Result<Oauth2AccessTokenVo> {
        return Result(remotedevSdkService.getAccessToken(desktopIP, sign))
    }

    override fun getAppIdOauthClientDetail(desktopIP: String, appId: String): Result<ClientDetailsDTO?> {
        return Result(remotedevSdkService.getAppIdOauthClientDetail(desktopIP, appId))
    }

    override fun sdkReportData(data: SdkReportData): Result<Boolean> {
        remotedevSdkService.reportData(data.reportType, data.reportData)
        return Result(true)
    }
}
