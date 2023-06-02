package com.tencent.devops.dispatch.macos.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.macos.api.OpVMTypeResource
import com.tencent.devops.dispatch.macos.pojo.VMType
import com.tencent.devops.dispatch.macos.pojo.VMTypeCreate
import com.tencent.devops.dispatch.macos.pojo.VMTypeUpdate
import com.tencent.devops.dispatch.macos.service.MacVmTypeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpVMTypeResourceImpl @Autowired constructor(
    private val macVmTypeService: MacVmTypeService
) : OpVMTypeResource {
    override fun delete(vmTypeId: Int): Result<Boolean> {
        return Result(macVmTypeService.deleteVMType(vmTypeId))
    }

    override fun get(vmTypeId: Int): Result<VMType?> {
        return Result(macVmTypeService.getVMType(vmTypeId))
    }

    override fun list(): Result<List<VMType>?> {
        return Result(macVmTypeService.listVMType())
    }

    override fun create(vmType: VMTypeCreate): Result<Boolean> {
        return Result(macVmTypeService.createVMType(vmType))
    }

    override fun update(vmType: VMTypeUpdate): Result<Boolean> {
        return Result(macVmTypeService.updateVMType(vmType))
    }
}
