package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpPipelineResource
import com.tencent.devops.dispatch.service.PipelineVMService
import org.springframework.beans.factory.annotation.Autowired

/**
 * Created by rdeng on 2017/9/4.
 */
@RestResource
class OpPipelineResourceImpl @Autowired constructor(private val pipelineVMService: PipelineVMService) : OpPipelineResource {

    override fun setVMs(
        pipelineId: String,
        vmNames: String,
        vmSeqId: Int?
    ): Result<Boolean> {
        if (pipelineId.isBlank()) {
            throw OperationException("pipelineId为空")
        }

        if (vmNames.isBlank()) {
            throw OperationException("虚拟机为空")
        }
        pipelineVMService.setVMs(pipelineId, vmNames, vmSeqId)
        return Result(true)
    }

    override fun getVMs(
        pipelineId: String,
        vmSeqId: Int?
    ): Result<String> {
        if (pipelineId.isBlank()) {
            throw OperationException("pipelineId为空")
        }
        return Result(pipelineVMService.getVMs(pipelineId, vmSeqId) ?: "")
    }
}