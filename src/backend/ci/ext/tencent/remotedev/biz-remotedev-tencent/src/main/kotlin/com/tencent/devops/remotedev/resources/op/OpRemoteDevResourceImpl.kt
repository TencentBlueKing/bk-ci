package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpRemoteDevResource
import com.tencent.devops.remotedev.pojo.ImageSpec
import com.tencent.devops.remotedev.pojo.OPUserSetting
import com.tencent.devops.remotedev.pojo.RemoteDevUserSettings
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import com.tencent.devops.remotedev.service.UserRefreshService
import com.tencent.devops.remotedev.service.WhiteListService
import com.tencent.devops.remotedev.service.WorkspaceImageService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.WorkspaceTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpRemoteDevResourceImpl @Autowired constructor(
    private val workspaceTemplateService: WorkspaceTemplateService,
    private val workspaceService: WorkspaceService,
    private val userRefreshService: UserRefreshService,
    private val remoteDevSettingService: RemoteDevSettingService,
    private val whiteListService: WhiteListService,
    private val workspaceImageService: WorkspaceImageService
) : OpRemoteDevResource {

    override fun addWorkspaceTemplate(userId: String, workspaceTemplate: WorkspaceTemplate): Result<Boolean> {
        return Result(workspaceTemplateService.addWorkspaceTemplate(userId, workspaceTemplate))
    }

    override fun getWorkspaceTemplateList(userId: String): Result<List<WorkspaceTemplate>> {
        return Result(workspaceTemplateService.getWorkspaceTemplateList())
    }

    override fun updateWorkspaceTemplate(
        userId: String,
        workspaceTemplateId: Long,
        workspaceTemplate: WorkspaceTemplate
    ): Result<Boolean> {
        return Result(workspaceTemplateService.updateWorkspaceTemplate(workspaceTemplateId, workspaceTemplate))
    }

    override fun deleteWorkspaceTemplate(userId: String, wsTemplateId: Long): Result<Boolean> {
        return Result(workspaceTemplateService.deleteWorkspaceTemplate(wsTemplateId))
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
    override fun getUserSetting(userId: String): Result<RemoteDevUserSettings> {
        return Result(remoteDevSettingService.getUserSetting4Op(userId))
    }
    override fun refreshUserInfo(userId: String): Result<Boolean> {
        return Result(userRefreshService.refreshAllUser())
    }

    override fun addWhiteListUser(userId: String, whiteListUser: String): Result<Boolean> {
        return Result(whiteListService.addWhiteListUser(userId, whiteListUser))
    }

    override fun addGPUWhiteListUser(userId: String, whiteListUser: String): Result<Boolean> {
        return Result(whiteListService.addGPUWhiteListUser(userId, whiteListUser))
    }

    override fun addImageSpec(spec: ImageSpec): Result<Boolean> {
        return Result(workspaceImageService.addImageSpecConfig(spec))
    }

    override fun deleteImageSpec(id: Int): Result<Boolean> {
        return Result(workspaceImageService.deleteImageSpecConfig(id))
    }

    override fun updateImageSpec(id: Int, spec: ImageSpec): Result<Boolean> {
        return Result(workspaceImageService.updateImageSpecConfig(id, spec))
    }

    override fun listImageSpec(): Result<List<ImageSpec>?> {
        return Result(workspaceImageService.listImageSpecConfig())
    }
}
