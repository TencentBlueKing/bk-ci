package com.tencent.devops.worker.common.api.quality

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.WorkerRestApiSDK

interface QualityGatewaySDKApi : WorkerRestApiSDK {

    fun saveScriptHisMetadata(elementType: String, data: Map<String, String>): Result<String>
}