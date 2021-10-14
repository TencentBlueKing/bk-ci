package com.tencent.devops.process.service

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.process.engine.service.PipelineInfoExtService

class PipelineInfoExtTencentService : PipelineInfoExtService {
    override fun failNotifyChannel(): String {
        return "${NotifyType.EMAIL.name},${NotifyType.RTX.name}"
    }
}
