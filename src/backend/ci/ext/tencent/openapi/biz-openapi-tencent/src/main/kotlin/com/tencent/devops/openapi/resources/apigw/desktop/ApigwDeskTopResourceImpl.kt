package com.tencent.devops.openapi.resources.apigw.desktop

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.desktop.ApigwDeskTopResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwDeskTopResourceImpl @Autowired constructor(private val client: Client) :
    ApigwDeskTopResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwDeskTopResourceImpl::class.java)
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
}
