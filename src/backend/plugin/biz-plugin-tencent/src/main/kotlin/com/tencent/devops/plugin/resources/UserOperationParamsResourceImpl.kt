package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserOperationParamsResource
import com.tencent.devops.plugin.service.OperationService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserOperationParamsResourceImpl @Autowired constructor(
    private val service: OperationService
) : UserOperationParamsResource {
    override fun templateList(id: String, pipelineId: String): Result<Page<Any>> {
        return service.getList(id, pipelineId)
    }

    override fun operationParams(id: String, pipelineId: String): Result<List<Any>> {
        if (id.isEmpty())
            return Result(emptyList())
        return service.getParam(id, pipelineId)
    }
}