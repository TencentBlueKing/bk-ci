package com.tencent.devops.remotedev.resources.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevRecordResource
import com.tencent.devops.remotedev.pojo.FeatureSwitch
import com.tencent.devops.remotedev.pojo.FeatureSwitchType
import com.tencent.devops.remotedev.pojo.record.CheckWorkspaceRecordData
import com.tencent.devops.remotedev.pojo.record.FetchMetaDataParam
import com.tencent.devops.remotedev.pojo.record.UserWorkspaceRecordPermissionInfo
import com.tencent.devops.remotedev.pojo.record.WorkspaceLiveResolution
import com.tencent.devops.remotedev.pojo.record.WorkspaceLiveResp
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordMetadata
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordTicketType
import com.tencent.devops.remotedev.service.FeatureSwitchService
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.WorkspaceRecordService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceRemoteDevRecordResourceImpl @Autowired constructor(
    private val workspaceRecordService: WorkspaceRecordService,
    private val featureSwitchService: FeatureSwitchService,
    private val permissionService: PermissionService
) : ServiceRemoteDevRecordResource {

    override fun enableWorkspaceRecord(
        userId: String,
        projectId: String,
        workspaceName: String,
        enable: Boolean
    ): Result<Boolean> {
        permissionService.checkUserProjectManager(userId, projectId)
        workspaceRecordService.enableRecord(
            workspaceName = workspaceName,
            enableUser = if (enable) {
                userId
            } else {
                null
            }
        )
        return Result(true)
    }

    override fun checkWorkspaceEnableAddress(
        userId: String,
        appId: Long,
        ip: String?,
        mediaGary: Boolean?,
        envUid: String?
    ): Result<CheckWorkspaceRecordData> {
        val (enable, address) = workspaceRecordService.checkRecordAndAddress(
            userId = userId,
            appId = appId,
            ip = ip,
            mediaGary = mediaGary,
            envUid = envUid
        )
        return Result(CheckWorkspaceRecordData(enable, address))
    }

    override fun getUserWorkspaceRecordPermission(
        userId: String,
        workspaceName: String
    ): Result<UserWorkspaceRecordPermissionInfo> {
        return Result(workspaceRecordService.getUserWorkspaceRecordPermission(userId, workspaceName))
    }

    override fun updateUserWorkspaceRecordPermission(userId: String, workspaceName: String): Result<Boolean> {
        workspaceRecordService.updateApprovalRecordViewPermission(userId, workspaceName)
        return Result(true)
    }

    override fun getViewRecordMetadata(data: FetchMetaDataParam): Result<Page<WorkspaceRecordMetadata>> {
        return Result(
            workspaceRecordService.getWorkspaceRecordMetadata(
                projectId = data.projectId,
                userId = data.userId,
                workspaceName = data.workspaceName,
                page = data.page,
                pageSize = data.pageSize,
                startTime = data.startTime,
                stopTime = data.stopTime
            )
        )
    }

    override fun getWorkspaceRecordTicket(
        userId: String,
        workspaceName: String,
        token: String
    ): Result<String> {
        return Result(
            workspaceRecordService.getWorkspaceRecordTicket(
                workspaceName = workspaceName,
                token = token,
                type = WorkspaceRecordTicketType.RECORD
            )
        )
    }

    override fun getWorkspaceLiveInfo(
        userId: String,
        projectId: String,
        workspaceName: String,
        resolution: WorkspaceLiveResolution?
    ): Result<WorkspaceLiveResp> {
        return Result(workspaceRecordService.getWorkspaceLiveInfo(userId, projectId, workspaceName, resolution))
    }

    override fun enableLive(
        userId: String,
        projectId: String,
        enable: Boolean,
        switchType: FeatureSwitchType
    ): Result<Boolean> {
        permissionService.checkUserManager(userId, projectId)
        val records = featureSwitchService.list(
            projectId = projectId,
            userId = null,
            workspaceName = null,
            featureType = switchType
        )
        if (!enable) {
            records.forEach {
                featureSwitchService.delete(userId, id = it.id ?: return@forEach)
            }
            return Result(true)
        }
        if (records.isNotEmpty()) {
            return Result(true)
        }
        featureSwitchService.create(
            userId,
            FeatureSwitch(projectId = projectId, featureType = switchType)
        )
        return Result(true)
    }

    override fun checkViewLive(
        userId: String,
        projectId: String,
        workspaceName: String
    ): Result<Boolean> {
        return Result(workspaceRecordService.checkViewLive(userId, projectId, workspaceName))
    }
}