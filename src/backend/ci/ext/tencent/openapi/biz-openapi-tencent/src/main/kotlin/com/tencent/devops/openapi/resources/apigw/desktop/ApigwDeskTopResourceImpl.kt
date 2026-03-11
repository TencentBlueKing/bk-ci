package com.tencent.devops.openapi.resources.apigw.desktop

import com.tencent.devops.auth.pojo.vo.Oauth2AccessTokenVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.SensitiveApiPermission
import com.tencent.devops.openapi.api.apigw.desktop.ApigwDeskTopResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.api.service.ServiceSDKResource
import com.tencent.devops.remotedev.pojo.DesktopTokenSign
import com.tencent.devops.remotedev.pojo.DesktopTokenSignBody
import com.tencent.devops.remotedev.pojo.op.WorkspaceDesktopNotifyData
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwDeskTopResourceImpl @Autowired constructor(private val client: Client) :
    ApigwDeskTopResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwDeskTopResourceImpl::class.java)
    }

    @SensitiveApiPermission("desktop_get_token")
    override fun getToken(
        appCode: String?,
        apigwType: String?,
        desktopIP: String,
        devxGwToken: String,
        sha1: String,
        osName: String,
        osArch: String,
        storeCode: String,
        storeType: String,
        storeVersion: String,
        sign: DesktopTokenSignBody
    ): Result<String> {
        logger.info(
            "$apigwType|$appCode|getToken|sign=$sign|sha1=$sha1|" +
                "storeCode=$storeCode|storeVersion=$storeVersion"
        )
        return client.get(ServiceSDKResource::class).getToken(desktopIP = desktopIP, sign = with(sign) {
            DesktopTokenSign(
                fingerprint = fingerprint,
                appId = storeCode,
                fileName = fileName,
                fileVersion = fileVersion,
                fileUpdateTime = fileUpdateTime,
                productName = productName,
                productVersion = storeVersion,
                sha1 = sha1,
                timestamp = timestamp,
                publicKey = publicKey,
                sign = this.sign
            )
        })
    }

    override fun getProjectWorkspace(
        appCode: String?,
        apigwType: String?,
        desktopIP: String,
        devxGwToken: String
    ): Result<WeSecProjectWorkspace?> {
        logger.info("$apigwType|$appCode|desktop_getProjectWorkspace|desktopIP=$desktopIP")
        return client.get(ServiceRemoteDevResource::class).getProjectWorkspaceIp(ip = desktopIP)
    }

    @SensitiveApiPermission("desktop_message_register")
    override fun messageRegister(
        appCode: String?,
        apigwType: String?,
        desktopIP: String,
        devxGwToken: String,
        data: WorkspaceDesktopNotifyData
    ): Result<Boolean> {
        logger.info("$apigwType|$appCode|desktop_messageRegister|desktopIP=$desktopIP|$data")
        return client.get(ServiceRemoteDevResource::class).notifyDesktopCheckIp(desktopIP, data)
    }

    @SensitiveApiPermission("desktop_get_oauth")
    override fun sdkGetAccessToken(
        appCode: String?,
        apigwType: String?,
        desktopIP: String,
        devxGwToken: String,
        sha1: String,
        osName: String,
        osArch: String,
        storeCode: String,
        storeType: String,
        storeVersion: String,
        new: Boolean?,
        sign: DesktopTokenSignBody
    ): Result<Oauth2AccessTokenVo> {
        logger.info(
            "$apigwType|$appCode|sdkGetAccessToken|sign=$sign|sha1=$sha1|" +
                "storeCode=$storeCode|storeVersion=$storeVersion"
        )
        return client.get(ServiceSDKResource::class)
            .sdkGetAccessToken(desktopIP = desktopIP, new = new, sign = with(sign) {
                DesktopTokenSign(
                    fingerprint = fingerprint,
                    appId = storeCode,
                    fileName = fileName,
                    fileVersion = fileVersion,
                    fileUpdateTime = fileUpdateTime,
                    productName = productName,
                    productVersion = storeVersion,
                    sha1 = sha1,
                    timestamp = timestamp,
                    publicKey = publicKey,
                    sign = this.sign
                )
            })
    }
}
