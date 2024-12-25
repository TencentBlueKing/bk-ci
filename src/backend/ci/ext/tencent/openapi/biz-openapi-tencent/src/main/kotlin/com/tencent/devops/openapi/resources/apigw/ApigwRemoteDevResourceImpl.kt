package com.tencent.devops.openapi.resources.apigw

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.ApigwRemoteDevResource
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import com.tencent.devops.remotedev.pojo.OperateCvmData
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.UserOnePassword
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WindowsWorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceCloneReq
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceRebuildReq
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceUpgradeReq
import com.tencent.devops.remotedev.pojo.common.QuotaType
import com.tencent.devops.remotedev.pojo.expert.CreateDiskResp
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskValidateResp
import com.tencent.devops.remotedev.pojo.expert.SupRecordData
import com.tencent.devops.remotedev.pojo.expert.SupRecordDataResp
import com.tencent.devops.remotedev.pojo.expert.WorkspaceTaskStatus
import com.tencent.devops.remotedev.pojo.image.MakeWorkspaceImageReq
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.op.WorkspaceNotifyData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.RemotedevProjectNew
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import com.tencent.devops.remotedev.pojo.record.CheckWorkspaceRecordData
import com.tencent.devops.remotedev.pojo.record.FetchMetaDataParam
import com.tencent.devops.remotedev.pojo.record.UserWorkspaceRecordPermissionInfo
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordMetadata
import com.tencent.devops.remotedev.pojo.remotedev.VmDiskInfo
import com.tencent.devops.remotedev.pojo.remotedevsup.DevcloudCVMData
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

    override fun desktopTokenCheck(
        appCode: String?,
        apigwType: String?,
        token: String,
        dToken: String
    ): Result<UserOnePassword> {
        logger.info("Get  projects info by group ,userId:$dToken")
        return client.get(ServiceRemoteDevResource::class).desktopTokenCheck(
            token = token,
            dToken = dToken
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

    override fun queryWorkspaceProjectsNew(
        appCode: String?,
        apigwType: String?,
        projectId: String?,
        page: Int,
        pageSize: Int
    ): Result<List<RemotedevProjectNew>> {
        logger.info("Get  workspace projects new $projectId|$page|$pageSize")
        return client.get(ServiceRemoteDevResource::class).getRemotedevProjectsNew(projectId, page, pageSize)
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
        zoneType: WindowsResourceZoneConfigType?,
        data: OpProjectWorkspaceAssignData
    ): Result<Boolean> {
        logger.info("assign workspace|operator|$operator|owner|$owner|data|$data")
        return client.get(ServiceRemoteDevResource::class).assignWorkspace(
            operator = operator,
            owner = owner,
            zoneType = zoneType,
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

    @Suppress("ComplexCondition")
    override fun notifyWorkspaceInfo(
        appCode: String?,
        apigwType: String?,
        operator: String,
        notifyData: WorkspaceNotifyData
    ): Result<Boolean> {
        logger.info("notify workspace|notifyData|$notifyData")
        if ((notifyData.projectId.isNullOrEmpty() || notifyData.projectId?.all { it.isBlank() } == true) &&
            (notifyData.ip.isNullOrEmpty() || notifyData.ip?.all { it.isBlank() } == true) &&
            (notifyData.owner.isNullOrEmpty() || notifyData.owner?.all { it.isBlank() } == true)
        ) {
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

    override fun createPersonalWorkspace(
        userId: String,
        zoneType: WindowsResourceZoneConfigType?,
        data: WindowsWorkspaceCreate
    ): Result<Boolean> {
        logger.info("createPersonalWorkspace $userId|$data")
        return client.get(ServiceRemoteDevResource::class).createPersonalWorkspace(userId, zoneType, data)
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
        zoneType: WindowsResourceZoneConfigType?,
        data: WindowsWorkspaceCreate
    ): Result<Boolean> {
        logger.info("createProjectWorkspace $userId|$projectId|$data")
        return client.get(ServiceRemoteDevResource::class).createProjectWorkspace(
            userId = userId,
            projectId = projectId,
            zoneType = zoneType,
            data = data
        )
    }

    override fun workspaceClone(
        userId: String,
        projectId: String,
        workspaceName: String,
        req: WorkspaceCloneReq
    ): Result<Boolean> {
        logger.info("workspaceClone $userId|$projectId|$workspaceName|$req")
        return client.get(ServiceRemoteDevResource::class).workspaceClone(
            userId = userId,
            projectId = projectId,
            workspaceName = workspaceName,
            req = req
        )
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

    override fun getExpertSupRecord(appCode: String?, apigwType: String?, id: Long): Result<SupRecordData?> {
        logger.info("getExpertSupRecord $id")
        return client.get(ServiceRemoteDevResource::class).fetchExpertSupRecordAny(id)
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

    override fun fetchCvmList(
        userId: String,
        projectId: String,
        page: Int,
        pageSize: Int
    ): Result<Page<DevcloudCVMData>?> {
        logger.info("fetchCvmList $userId|$projectId|$page|$pageSize")
        return client.get(ServiceRemoteDevResource::class).fetchCvmList(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize
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

    override fun queryWorkspaceImageList(
        appCode: String?,
        apigwType: String?,
        projectId: String?,
        imageId: String?
    ): Result<Map<String, Any>> {
        logger.info("queryWorkspaceImageList |$projectId|")
        return client.get(ServiceRemoteDevResource::class).getWorkspaceImageList(
            projectId = projectId,
            imageId = imageId
        )
    }

    override fun modifyWorkspaceDisplayName(userId: String, ip: String, displayName: String): Result<Boolean> {
        logger.info("modifyWorkspaceDisplayName |$userId|$ip|$displayName")
        return client.get(ServiceRemoteDevResource::class).modifyWorkspaceDisplayName(
            userId = userId,
            ip = ip,
            displayName = displayName
        )
    }

    override fun reBuildWorkspace(
        userId: String,
        workspaceName: String,
        rebuildReq: WorkspaceRebuildReq
    ): Result<Boolean> {
        logger.info("reBuildWorkspace $userId|$userId|$workspaceName|$rebuildReq")
        return client.get(ServiceRemoteDevResource::class).reBuildWorkspace(userId, workspaceName, rebuildReq)
    }

    override fun startWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        logger.info("startWorkspace $userId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).startWorkspace(userId, workspaceName)
    }

    override fun stopWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        logger.info("stopWorkspace $userId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).stopWorkspace(userId, workspaceName)
    }

    override fun restartWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        logger.info("restartWorkspace $userId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).restartWorkspace(userId, workspaceName)
    }

    override fun makeImageByVm(
        userId: String,
        workspaceName: String,
        makeImageReq: MakeWorkspaceImageReq
    ): Result<Boolean> {
        logger.info("makeImageByVm $userId|$workspaceName|$makeImageReq")
        return client.get(ServiceRemoteDevResource::class).makeImageByVm(userId, workspaceName, makeImageReq)
    }

    override fun modifyWorkspaceProperty(
        userId: String,
        workspaceName: String?,
        ip: String?,
        workspaceProperty: WorkspaceProperty
    ): Result<Boolean> {
        logger.info("modifyWorkspaceProperty $userId|$workspaceName|$ip|$workspaceProperty")
        return client.get(ServiceRemoteDevResource::class).modifyWorkspaceProperty(
            userId = userId,
            workspaceName = workspaceName,
            ip = ip,
            workspaceProperty = workspaceProperty
        )
    }

    override fun deleteProjectImage(userId: String, projectId: String, imageId: String): Result<Boolean> {
        logger.info("deleteProjectImage $userId|$projectId|$imageId")
        return client.get(ServiceRemoteDevResource::class).deleteProjectImage(
            userId = userId,
            projectId = projectId,
            imageId = imageId
        )
    }

    override fun operateCvmCallback(appCode: String?, apigwType: String?, data: OperateCvmData): Result<Boolean> {
        logger.info("operateCvmCallback $data")
        return client.get(ServiceRemoteDevResource::class).opCvm(data)
    }

    override fun enableWorkspaceRecord(
        userId: String,
        projectId: String,
        workspaceName: String,
        enable: Boolean
    ): Result<Boolean> {
        logger.info("enableWorkspaceRecord |$userId|$projectId|$workspaceName|$enable")
        return client.get(ServiceRemoteDevResource::class).enableWorkspaceRecord(
            userId = userId,
            projectId = projectId,
            workspaceName = workspaceName,
            enable = enable
        )
    }

    override fun checkWorkspaceEnableAddress(
        userId: String,
        appId: Long,
        ip: String
    ): Result<CheckWorkspaceRecordData> {
        logger.info("checkWorkspaceEnableAddress |$userId|$appId|$ip")
        return client.get(ServiceRemoteDevResource::class).checkWorkspaceEnableAddress(
            userId = userId,
            appId = appId,
            ip = ip
        )
    }

    override fun checkUserViewWorkspacePermission(
        userId: String,
        workspaceName: String
    ): Result<Boolean> {
        logger.info("checkUserViewWorkspacePermission |$userId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).checkUserViewWorkspacePermission(userId, workspaceName)
    }

    override fun expandWorkspaceDisk(
        userId: String,
        workspaceName: String,
        size: String,
        pvcId: String?
    ): Result<ExpandDiskValidateResp?> {
        logger.info("expandWorkspaceDisk |$userId|$workspaceName|$size|$pvcId")
        return client.get(ServiceRemoteDevResource::class).expandDisk(userId, workspaceName, size, pvcId)
    }

    override fun createWorkspaceDisk(
        userId: String,
        workspaceName: String,
        size: String
    ): Result<CreateDiskResp> {
        logger.info("createWorkspaceDisk |$userId|$workspaceName|$size")
        return client.get(ServiceRemoteDevResource::class).createDisk(userId, workspaceName, size)
    }

    override fun fetchWorkspaceDiskList(userId: String, workspaceName: String): Result<List<VmDiskInfo>?> {
        logger.info("fetchWorkspaceDiskList |$userId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).fetchDiskList(userId, workspaceName)
    }

    override fun upgradeWorkspace(
        userId: String,
        projectId: String,
        workspaceName: String,
        upgradeReq: WorkspaceUpgradeReq
    ): Result<Boolean> {
        logger.info("expandWorkspaceDisk |$userId|$workspaceName|$projectId|$upgradeReq")
        return client.get(ServiceRemoteDevResource::class).upgradeWorkspace(
            userId = userId,
            projectId = projectId,
            workspaceName = workspaceName,
            upgradeReq = upgradeReq
        )
    }

    override fun removeUserPermission(
        appCode: String?,
        apigwType: String?,
        userId: String,
        removeUser: String
    ): Result<Boolean> {
        logger.info("removeUserPermission $appCode|$userId|$removeUser")
        return client.get(ServiceRemoteDevResource::class).removeUserPermission(
            userId = userId,
            removeUser = removeUser
        )
    }

    override fun getWorkspaceListNew(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        search: WorkspaceSearch
    ): Result<Page<ProjectWorkspace>> {
        logger.info("getWorkspaceListNew $appCode|$userId|$projectId|$page|$pageSize|$search")
        return client.get(ServiceRemoteDevResource::class).getWorkspaceListNew(
            userId = userId,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            search = search
        )
    }

    override fun getUserWorkspaceRecordPermission(
        appCode: String?,
        apigwType: String?,
        userId: String,
        workspaceName: String
    ): Result<UserWorkspaceRecordPermissionInfo> {
        logger.info("getUserWorkspaceRecordPermission $appCode|$userId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).getUserWorkspaceRecordPermission(
            userId = userId,
            workspaceName = workspaceName
        )
    }

    override fun updateUserWorkspaceRecordPermission(
        appCode: String?,
        apigwType: String?,
        userId: String,
        workspaceName: String
    ): Result<Boolean> {
        logger.info("updateUserWorkspaceRecordPermission $appCode|$userId|$workspaceName")
        return client.get(ServiceRemoteDevResource::class).updateUserWorkspaceRecordPermission(
            userId = userId,
            workspaceName = workspaceName
        )
    }

    override fun getViewRecordMetadata(
        appCode: String?,
        apigwType: String?,
        data: FetchMetaDataParam
    ): Result<Page<WorkspaceRecordMetadata>> {
        logger.info("getViewRecordMetadata $appCode|$data")
        return client.get(ServiceRemoteDevResource::class).getViewRecordMetadata(data)
    }

    override fun getWorkspaceTimeline(
        userId: String,
        workspaceName: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<WorkspaceOpHistory>> {
        logger.info("getWorkspaceTimeline $workspaceName|$workspaceName|$page|$pageSize")
        return client.get(ServiceRemoteDevResource::class).getWorkspaceTimeline(
            userId = userId,
            workspaceName = workspaceName,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getWorkspaceRecordTicket(userId: String, workspaceName: String, token: String): Result<String> {
        logger.info("getWorkspaceRecordTicket |$userId|$workspaceName|$token")
        return client.get(ServiceRemoteDevResource::class).getWorkspaceRecordTicket(userId, workspaceName, token)
    }

    override fun getTaskStatus(userId: String, taskId: String): Result<WorkspaceTaskStatus?> {
        logger.info("getTaskStatus |$userId|$taskId")
        return client.get(ServiceRemoteDevResource::class).getTaskStatus(userId, taskId)
    }
}
