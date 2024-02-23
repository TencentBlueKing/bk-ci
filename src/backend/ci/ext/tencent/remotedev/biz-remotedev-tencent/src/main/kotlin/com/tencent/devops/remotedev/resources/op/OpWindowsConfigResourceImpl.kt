package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpWindowsConfigResource
import com.tencent.devops.remotedev.pojo.WindowsResourceTypeConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.op.WindowsSpecResInfo
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpWindowsConfigResourceImpl @Autowired constructor(
    private val windowsResourceConfigService: WindowsResourceConfigService
) : OpWindowsConfigResource {

    override fun getWindowsResourceList(userId: String): Result<List<WindowsResourceTypeConfig>> {
        return Result(windowsResourceConfigService.getAllType(true, null))
    }

    override fun addWindowsResource(userId: String, windowsResourceConfig: WindowsResourceTypeConfig): Result<Boolean> {
        return Result(windowsResourceConfigService.addWindowsResource(windowsResourceConfig))
    }

    override fun updateWindowsResource(
        userId: String,
        id: Long,
        windowsResourceConfig: WindowsResourceTypeConfig
    ): Result<Boolean> {
        return Result(windowsResourceConfigService.updateWindowsResource(id, windowsResourceConfig))
    }

    override fun deleteWindowsResource(userId: String, id: Long): Result<Boolean> {
        return Result(windowsResourceConfigService.deleteWindowsResource(id))
    }

    override fun addWindowsZone(userId: String, windowsResourceConfig: WindowsResourceZoneConfig): Result<Boolean> {
        return Result(windowsResourceConfigService.addWindowsResourceZone(windowsResourceConfig))
    }

    override fun getWindowsResourceZoneList(userId: String): Result<List<WindowsResourceZoneConfig>> {
        return Result(windowsResourceConfigService.getAllZone())
    }

    override fun updateWindowsZone(
        userId: String,
        id: Long,
        windowsResourceConfig: WindowsResourceZoneConfig
    ): Result<Boolean> {
        return Result(windowsResourceConfigService.updateWindowsResourceZone(id, windowsResourceConfig))
    }

    override fun deleteWindowsZone(userId: String, id: Long): Result<Boolean> {
        return Result(windowsResourceConfigService.deleteWindowsResourceZone(id))
    }

    override fun createOrUpdateSpec(userId: String, data: WindowsSpecResInfo): Result<Boolean> {
        return Result(windowsResourceConfigService.createOrUpdateSpec(data))
    }

    override fun deleteSpec(userId: String, projectId: String, size: String): Result<Boolean> {
        return Result(windowsResourceConfigService.deleteSpec(projectId, size))
    }

    override fun fetchSpec(
        userId: String,
        projectId: String?,
        machineType: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<WindowsSpecResInfo>> {
        return Result(windowsResourceConfigService.fetchSpec(projectId, machineType, page, pageSize))
    }

    override fun addProjectTotalQuota(userId: String, projectId: String, quota: Int): Result<Boolean> {
        return Result(windowsResourceConfigService.addProjectTotalQuota(userId, projectId, quota))
    }
}
