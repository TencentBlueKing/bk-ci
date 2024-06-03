package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.view.PipelineViewGroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineViewResourceImpl @Autowired constructor(
    private val pipelineViewGroupService: PipelineViewGroupService
) : OpPipelineViewResource {
    override fun initAllView(userId: String): Result<Boolean> {
        Thread { pipelineViewGroupService.initAllView() }.start()
        return Result(true)
    }
}
