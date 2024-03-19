package com.tencent.devops.openapi.resources.apigw

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.ApigwRemoteDevResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@RestResource
class ApigwRemoteDevResourceImpl @Autowired constructor(private val client: Client) :
    ApigwRemoteDevResource {

    @Value("\${devx.gwToken:}")
    val devxGwToken = ""
    @Value("\${devx.originalHost:}")
    val devxOriginalHost = ""

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwProjectResourceImpl::class.java)
    }

    override fun validateUserTicket(
        appCode: String?,
        apigwType: String?,
        userId: String,
        isOffshore: Boolean,
        ticket: String
    ): Result<Boolean> {
        logger.info("Get  projects info by group ,userId:$userId,isOffshore:$isOffshore,ticket:$ticket")
        return client.get(ServiceRemoteDevResource::class).validateUserTicket(
            userId = userId,
            isOffshore = isOffshore,
            ticket = ticket
        )
    }

    override fun queryProjectWorkspace(
        appCode: String?,
        apigwType: String?,
        projectId: String?,
        ip: String?
    ): Result<List<WeSecProjectWorkspace>> {
        logger.info("Get  projects workspace ,projectId:$projectId")
        return client.get(ServiceRemoteDevResource::class).getProjectWorkspace(
            projectId = projectId,
            ip = ip
        )
    }

    override fun getProjectWorkspace(
        appCode: String?,
        apigwType: String?,
        ip: String?,
        originalHost: String?,
        devxToken: String?
    ): Result<WeSecProjectWorkspace?> {
        return when {
            ip.isNullOrBlank() -> Result(
                message = "获取到的云桌面IP为空",
                data = null
            )
            !originalHost.isNullOrBlank() && originalHost != devxOriginalHost -> Result(
                message = "来源请求域名不符",
                data = null
            )
            !devxToken.isNullOrBlank() && devxToken != devxGwToken -> Result(
                message = "来源请求token不符",
                data = null
            )
            else -> {
                logger.info("Get projects workspace ip $ip")
                client.get(ServiceRemoteDevResource::class).getProjectWorkspaceIp(ip = ip)
            }
        }
    }

    override fun queryWorkspaceProjects(
        appCode: String?,
        apigwType: String?,
        projectId: String?
    ): Result<List<RemotedevProject>> {
        logger.info("Get  workspace projects")
        return client.get(ServiceRemoteDevResource::class).getRemotedevProjects(projectId)
    }

    override fun queryProjectRemoteDevCvm(
        appCode: String?,
        apigwType: String?,
        projectId: String?
    ): Result<List<RemotedevCvmData>> {
        logger.info("Get  project cvm")
        return client.get(ServiceRemoteDevResource::class).queryProjectRemoteDevCvm(projectId)
    }

    override fun checkUserCgsPermission(
        appCode: String?,
        apigwType: String?,
        userId: String,
        ip: String
    ): Result<Boolean> {
        logger.info("check user cgs permission")
        return client.get(ServiceRemoteDevResource::class).checkUserIpPermission(userId, ip)
    }

    override fun assignWorkspace(
        appCode: String?,
        apigwType: String?,
        operator: String,
        owner: String?,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        logger.info("assign workspace|operator|$operator|owner|$owner|data|$data")
        return client.get(ServiceRemoteDevResource::class).assignWorkspace(
            operator = operator,
            owner = owner,
            data = data
        )
    }

    override fun listWorkspacesWithProjectId(
        appCode: String?,
        apigwType: String?,
        projectId: String
    ): Result<List<WeSecProjectWorkspace>> {
        logger.info("List  projects workspace ,projectId:$projectId")
        return client.get(ServiceRemoteDevResource::class).getProjectWorkspace(
            projectId = projectId,
            ip = null
        )
    }

    override fun notifyWorkspaceInfo(
        appCode: String?,
        apigwType: String?,
        operator: String,
        notifyData: WorkspaceNotifyData
    ): Result<Boolean> {
        logger.info("notify workspace|notifyData|$notifyData")
        return client.get(ServiceRemoteDevResource::class).notifyWorkspaceInfo(
            operator = operator,
            notifyData = notifyData
        )
    }

    override fun checkWorkspaceProject(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        ip: String
    ): Result<Boolean> {
        logger.info("check  project workspace ,projectId:$projectId,ip:$ip")
        return client.get(ServiceRemoteDevResource::class).checkWorkspaceProject(
            projectId = projectId,
            ip = ip
        )
    }

    override fun getWindowsResourceList(appCode: String?, apigwType: String?): Result<List<WindowsResourceTypeConfig>> {
        return client.get(ServiceRemoteDevResource::class).getWindowsResourceList()
    }
}
