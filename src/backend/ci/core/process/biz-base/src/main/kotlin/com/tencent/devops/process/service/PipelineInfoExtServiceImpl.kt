package com.tencent.devops.process.service

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.process.engine.service.PipelineInfoExtService

class PipelineInfoExtServiceImpl : PipelineInfoExtService {
    override fun failNotifyChannel(): String {
        return "${NotifyType.WEWORK.name}"
    }
}
