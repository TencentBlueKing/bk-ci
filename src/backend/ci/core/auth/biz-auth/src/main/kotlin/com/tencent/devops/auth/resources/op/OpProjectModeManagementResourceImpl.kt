package com.tencent.devops.auth.resources.op

import com.tencent.devops.auth.api.op.OpProjectModeManagementResource
import com.tencent.devops.auth.pojo.enum.RoutingMode
import com.tencent.devops.auth.pojo.request.BatchSetProjectModesRequest
import com.tencent.devops.auth.provider.rbac.service.ProjectModeManager
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class OpProjectModeManagementResourceImpl(
    private val projectModeManager: ProjectModeManager
) : OpProjectModeManagementResource {

    override fun setProjectMode(
        projectCode: String,
        mode: String
    ): Result<Boolean> {
        return Result(
            data = projectModeManager.setProjectMode(
                projectCode = projectCode,
                mode = RoutingMode.valueOf(mode.trim().uppercase())
            )
        )
    }

    override fun removeProjectMode(
        projectCode: String
    ): Result<Boolean> {
        return Result(
            data = projectModeManager.removeProjectMode(projectCode)
        )
    }

    override fun batchSetProjectModes(
        batchRequest: BatchSetProjectModesRequest
    ): Result<Boolean> {
        projectModeManager.batchSetProjectModes(batchRequest.projectCodes, batchRequest.mode)
        return Result(true)
    }

    override fun setDefaultMode(
        mode: String
    ): Result<Boolean> {
        return Result(
            data = projectModeManager.setDefaultMode(
                mode = RoutingMode.valueOf(mode.trim().uppercase())
            )
        )
    }

    override fun clearAllProjectModes(
        confirm: Boolean
    ): Result<Boolean> {
        return Result(
            data = if (confirm) {
                projectModeManager.clearAllProjectModes()
            } else {
                false
            }
        )
    }

    override fun getProjectMode(
        projectCode: String
    ): Result<RoutingMode?> {
        return Result(
            data = projectModeManager.getProjectMode(projectCode)
        )
    }

    override fun getDefaultMode(): Result<RoutingMode> {
        return Result(
            data = projectModeManager.getDefaultMode()
        )
    }
}
