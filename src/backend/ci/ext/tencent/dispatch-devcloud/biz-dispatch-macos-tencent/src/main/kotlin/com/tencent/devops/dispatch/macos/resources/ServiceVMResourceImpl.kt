package com.tencent.devops.dispatch.macos.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.ServiceVMResource
import com.tencent.devops.dispatch.macos.pojo.VirtualMachineInfo
import com.tencent.devops.dispatch.macos.service.DevCloudMacosService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceVMResourceImpl @Autowired constructor(
    private val devCloudMacosService: DevCloudMacosService
) : ServiceVMResource {
    override fun list(): Result<List<VirtualMachineInfo>> {
        val macosVMList = mutableListOf<VirtualMachineInfo>()
        val devcloudMacosVmList = devCloudMacosService.getVmList()
        devcloudMacosVmList.forEach {
            val vmInfo = VirtualMachineInfo(it.id)
            vmInfo.name = it.name
            vmInfo.ip = it.ip
            macosVMList.add(vmInfo)
        }
        return Result(macosVMList)
    }
}
