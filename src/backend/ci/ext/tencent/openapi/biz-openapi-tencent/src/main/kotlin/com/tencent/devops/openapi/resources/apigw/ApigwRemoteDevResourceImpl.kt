package com.tencent.devops.openapi.resources.apigw

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.ApigwRemoteDevResource
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.expert.SupRecordDataResp
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.windows.QuotaInApiRes
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwRemoteDevResourceImpl @Autowired constructor(private val client: Client) :
    ApigwRemoteDevResource {

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
            ip = ip,
            businessLineName = null,
            ownerName = null
        )
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
        projectId: String,
        ip: String?
    ): Result<List<WeSecProjectWorkspace>> {
        logger.info("listWorkspacesWithProjectId|appcode=$appCode|projectId=$projectId|ip=$ip")
        return client.get(ServiceRemoteDevResource::class).getProjectWorkspace(
            projectId = projectId,
            ip = ip,
            businessLineName = null,
            ownerName = null
        )
    }

    override fun notifyWorkspaceInfo(
        appCode: String?,
        apigwType: String?,
        operator: String,
        notifyData: WorkspaceNotifyData
    ): Result<Boolean> {
        logger.info("notify workspace|notifyData|$notifyData")
        if (notifyData.projectId.isNullOrEmpty() && notifyData.ip.isNullOrEmpty() && notifyData.owner.isNullOrEmpty()) {
            return Result(false)
        }
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

    override fun querySGProjectWorkspace(
        appCode: String?,
        apigwType: String?,
        userId: String,
        taiUser: String
    ): Result<List<WeSecProjectWorkspace>> {
        logger.info("Get projects workspace from user $userId ,taiUser:$taiUser")
        val userInfo = client.get(ServiceUserResource::class).getDetailFromCache(userId).data
        if (userInfo?.businessLineName.isNullOrBlank()) {
            logger.info("Get projects workspace from user $userId ,businessLineName is null")
            return Result(emptyList())
        }
        return client.get(ServiceRemoteDevResource::class).getProjectWorkspace(
            businessLineName = userInfo?.businessLineName,
            ownerName = taiUser,
            ip = null,
            projectId = null
        )
    }

    override fun createPersonalWorkspace(userId: String, data: WindowsWorkspaceCreate): Result<Boolean> {
        logger.info("createPersonalWorkspace $userId|$data")
        return client.get(ServiceRemoteDevResource::class).createPersonalWorkspace(userId, data)
    }

    override fun deletePersonalWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        logger.info("deletePersonalWorkspace $userId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).deletePersonalWorkspace(userId, workspaceName)
    }

    override fun getPersonalWorkspace(userId: String, workspaceName: String): Result<WeSecProjectWorkspace?> {
        logger.info("getPersonalWorkspace $userId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).getPersonalWorkspace(userId, workspaceName)
    }

    override fun createProjectWorkspace(
        userId: String,
        projectId: String,
        data: WindowsWorkspaceCreate
    ): Result<Boolean> {
        logger.info("createProjectWorkspace $userId|$projectId|$data")
        return client.get(ServiceRemoteDevResource::class).createProjectWorkspace(userId, projectId, data)
    }

    override fun deleteProjectWorkspace(userId: String, projectId: String, workspaceName: String): Result<Boolean> {
        logger.info("deleteProjectWorkspace $userId|$projectId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).deleteProjectWorkspace(userId, projectId, workspaceName)
    }

    override fun getProjectWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String
    ): Result<WeSecProjectWorkspace?> {
        logger.info("getProjectWorkspace $userId|$projectId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).getProjectWorkspace(userId, projectId, workspaceName)
    }

    override fun getExpertSupRecords(userId: String, workspaceName: String): Result<SupRecordDataResp> {
        logger.info("getExpertSupRecords $userId|$workspaceName")
        val records = client.get(ServiceRemoteDevResource::class).fetchExpertSupRecord(
            userId = userId,
            workspaceName = workspaceName,
            createLaterTimestamp = LocalDateTime.now().minusDays(30).timestamp()
        ).data ?: emptyList()
        return Result(
            SupRecordDataResp(
                count = records.size,
                records = records
            )
        )
    }

    override fun getWindowsQuota(userId: String, type: QuotaType): Result<Map<String, Map<String, Int>>> {
        logger.info("getWindowsQuota $userId|$type")
        return client.get(ServiceRemoteDevResource::class).getWindowsQuota(userId, type)
    }

    override fun updateUsageLimit(
        userId: String,
        projectId: String?,
        machineType: String?,
        count: Int,
        available: Boolean?
    ): Result<QuotaInApiRes> {
        logger.info("updateUsageLimit $userId|$projectId|$count|$available")
        return client.get(ServiceRemoteDevResource::class).updateUsageLimit(
            userId = userId,
            projectId = projectId,
            machineType = machineType,
            count = count,
            available = available
        )
    }

    override fun assignWorkspaceUsers(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        workspaceName: String,
        assigns: List<ProjectWorkspaceAssign>
    ): Result<Boolean> {
        logger.info("assignWorkspaceUsers $userId|$projectId|$workspaceName|$assigns")
        return client.get(ServiceRemoteDevResource::class).assignUser(
            userId = userId,
            projectId = projectId,
            workspaceName = workspaceName,
            assigns = assigns
        )
    }
}
