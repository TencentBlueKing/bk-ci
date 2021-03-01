package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.PipelineTaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineTaskResourceImpl @Autowired constructor(
    private val pipelineTaskService: PipelineTaskService
) : OpPipelineTaskResource {

    override fun asyncUpdateTaskAtomVersion(): Result<Boolean> {
        return Result(pipelineTaskService.asyncUpdateTaskAtomVersion())
    }
}
