package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.auth.pojo.dto.ClientDetailsDTO
import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceSDKResource
import com.tencent.devops.remotedev.pojo.CdsToken
import com.tencent.devops.remotedev.pojo.DesktopTokenSign
import com.tencent.devops.remotedev.pojo.sdk.SdkReportData
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.RemotedevSdkService
import com.tencent.devops.remotedev.service.WorkspaceService
import org.slf4j.LoggerFactory

@RestResource
@Suppress("ALL")
class ServiceSdkResourceImpl(
    private val remotedevSdkService: RemotedevSdkService,
    private val permissionService: PermissionService,
    private val workspaceService: WorkspaceService
) : ServiceSDKResource {
    companion object {
        private val logger = LoggerFactory.getLogger(ServiceSdkResourceImpl::class.java)
    }

    override fun getToken(desktopIP: String, sign: DesktopTokenSign): Result<String> {
        return Result(remotedevSdkService.getAppToken(desktopIP, sign))
    }

    override fun sdkGetAccessToken(
        desktopIP: String,
        new: Boolean?,
        sign: DesktopTokenSign
    ): Result<Oauth2AccessTokenVo> {
        return Result(remotedevSdkService.getAccessToken(desktopIP, new, sign))
    }

    override fun getAppIdOauthClientDetail(desktopIP: String, appId: String): Result<ClientDetailsDTO?> {
        return Result(remotedevSdkService.getAppIdOauthClientDetail(desktopIP, appId))
    }

    override fun checkCdsToken(cdsToken: String): Result<CdsToken?> {
        return Result(permissionService.checkCdsToken(cdsToken))
    }

    override fun sdkReportData(data: SdkReportData): Result<Boolean> {
        remotedevSdkService.reportData(data.reportType, data.reportData)
        return Result(true)
    }

    override fun checkCDIOauth(cdiToken: String): Result<Pair<String, String>?> {
        return Result(permissionService.checkCDIOauth(cdiToken))
    }

    override fun getLoginUserId(workspaceName: String): Result<String?> {
        return Result(kotlin.runCatching {
            workspaceService.getWorkspaceDetail(
                userId = "cdi",
                workspaceName = workspaceName,
                checkPermission = false
            )?.currentLoginUser?.first()
        }.fold(
            { it },
            {
                logger.warn("getLoginUserId failed|$workspaceName", it)
                null
            }
        )
        )
    }
}
