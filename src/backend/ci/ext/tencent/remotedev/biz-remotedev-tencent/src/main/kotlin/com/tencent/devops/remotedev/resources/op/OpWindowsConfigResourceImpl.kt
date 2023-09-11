package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpWindowsConfigResource
import com.tencent.devops.remotedev.pojo.windows.WindowsResourceConfig
import com.tencent.devops.remotedev.pojo.windows.ZoneConfig
import com.tencent.devops.remotedev.service.WindowsResourceConfigService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpWindowsConfigResourceImpl @Autowired constructor(
    private val windowsResourceConfigService: WindowsResourceConfigService
) : OpWindowsConfigResource {

    override fun getWindowsResourceList(userId: String): Result<List<WindowsResourceConfig>> {
        return Result(windowsResourceConfigService.getAllConfig())
    }

    override fun addWindowsResource(userId: String, windowsResourceConfig: WindowsResourceConfig): Result<Boolean> {
        return Result(windowsResourceConfigService.addWindowsResource(windowsResourceConfig))
    }

    override fun updateWindowsResource(
        userId: String,
        id: Long,
        windowsResourceConfig: WindowsResourceConfig
    ): Result<Boolean> {
        return Result(windowsResourceConfigService.updateWindowsResource(id, windowsResourceConfig))
    }

    override fun deleteWindowsResource(userId: String, id: Long): Result<Boolean> {
        return Result(windowsResourceConfigService.deleteWindowsResource(id))
    }

    override fun getWindowsZoneList(userId: String): Result<List<ZoneConfig>> {
        return Result(windowsResourceConfigService.getAllZone())
    }

    override fun addWindowsZone(userId: String, zoneConfig: ZoneConfig): Result<Boolean> {
        return Result(windowsResourceConfigService.addWindowsZone(zoneConfig))
    }

    override fun updateWindowsZone(userId: String, id: Long, zoneConfig: ZoneConfig): Result<Boolean> {
        return Result(windowsResourceConfigService.updateWindowsZone(id, zoneConfig))
    }

    override fun deleteWindowsZone(userId: String, id: Long): Result<Boolean> {
        return Result(windowsResourceConfigService.deleteWindowsZone(id))
    }
}
