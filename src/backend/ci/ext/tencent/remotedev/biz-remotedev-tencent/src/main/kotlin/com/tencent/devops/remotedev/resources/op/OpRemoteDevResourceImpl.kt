package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpRemoteDevResource
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpRemoteDevResourceImpl @Autowired constructor(
    private val workspaceTemplateService: WorkspaceTemplateService,
    private val workspaceService: WorkspaceService,
    private val remoteDevSettingService: RemoteDevSettingService
) : OpRemoteDevResource {

    override fun addWorkspaceTemplate(userId: String, workspaceTemplate: WorkspaceTemplate): Result<Boolean> {
        return Result(workspaceTemplateService.addWorkspaceTemplate(userId, workspaceTemplate))
    }

    override fun getWorkspaceTemplateList(userId: String): Result<List<WorkspaceTemplate>> {
        return Result(workspaceTemplateService.getWorkspaceTemplateList(userId) ?: emptyList())
    }

    override fun updateWorkspaceTemplate(
        userId: String,
        workspaceTemplateId: Long,
        workspaceTemplate: WorkspaceTemplate
    ): Result<Boolean> {
        return Result(workspaceTemplateService.updateWorkspaceTemplate(userId, workspaceTemplateId, workspaceTemplate))
    }

    override fun deleteWorkspaceTemplate(userId: String, wsTemplateId: Long): Result<Boolean> {
        return Result(workspaceTemplateService.deleteWorkspaceTemplate(userId, wsTemplateId))
    }

    override fun initBilling(userId: String, freeTime: Int): Result<Boolean> {
        workspaceService.initBilling(freeTime)
        return Result(true)
    }

    override fun updateUserSetting(userId: String, data: List<OPUserSetting>): Result<Boolean> {
        data.forEach {
            remoteDevSettingService.updateSetting4Op(it)
        }
        return Result(true)
    }
}
