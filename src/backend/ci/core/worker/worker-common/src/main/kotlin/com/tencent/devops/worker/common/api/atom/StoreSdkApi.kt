package com.tencent.devops.worker.common.api.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.worker.common.api.WorkerRestApiSDK

interface StoreSdkApi : WorkerRestApiSDK {

    fun isPlatformCodeRegistered(platformCode: String): Result<Boolean>
}
