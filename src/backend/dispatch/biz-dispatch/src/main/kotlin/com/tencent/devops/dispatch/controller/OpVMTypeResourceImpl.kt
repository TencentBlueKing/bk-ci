package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpVMTypeResource
import com.tencent.devops.dispatch.pojo.VMType
import com.tencent.devops.dispatch.pojo.VMTypeCreate
import com.tencent.devops.dispatch.service.VMTypeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpVMTypeResourceImpl @Autowired constructor(
    private val vmTypeService: VMTypeService
) : OpVMTypeResource {

    override fun list() = Result(vmTypeService.queryAllVMType())

    override fun create(typeName: VMTypeCreate) = vmTypeService.createType(typeName.typeName)

    override fun update(vmType: VMType) = vmTypeService.updateType(vmType)

    override fun delete(typeId: Int): Result<Boolean> {
        vmTypeService.deleteType(typeId)
        return Result(true)
    }
}