package com.tencent.devops.repository.service.tapd

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.sdk.tapd.TapdResult
import com.tencent.devops.common.sdk.tapd.request.StatusMapRequest
import com.tencent.devops.repository.tapd.service.ITapdWorkflowService
import com.tencent.devops.scm.api.ServiceTapdResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TencentTapdWorkflowService @Autowired constructor(
    private val client: Client
) : ITapdWorkflowService {

    override fun getWorkflowStatusMap(request: StatusMapRequest): TapdResult<Map<String, String>> {
        return client.getScm(ServiceTapdResource::class).getWorkflowStatusMap(request)
    }
}
