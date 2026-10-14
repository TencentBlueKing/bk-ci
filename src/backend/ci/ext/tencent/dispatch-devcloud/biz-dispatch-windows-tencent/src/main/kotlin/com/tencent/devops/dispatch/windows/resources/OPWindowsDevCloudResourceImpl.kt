package com.tencent.devops.dispatch.windows.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.windows.api.op.OPWindowsDevCloudResource
import com.tencent.devops.dispatch.windows.client.WindowsDevCloudRequestFactory

@RestResource
class OPWindowsDevCloudResourceImpl(
    private val requestFactory: WindowsDevCloudRequestFactory
) : OPWindowsDevCloudResource {

    override fun getSgwEnabled(userId: String): Result<Boolean> {
        return Result(requestFactory.isSgwEnabled())
    }

    override fun setSgwEnabled(userId: String, enabled: Boolean): Result<Boolean> {
        return Result(requestFactory.setSgwEnabled(enabled = enabled, operator = userId))
    }
}
