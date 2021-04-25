package com.tencent.devops.process.engine.service

import com.tencent.devops.process.pojo.pipeline.enums.CallBackNetWorkRegionType
import org.springframework.stereotype.Service

@Service
class ProjectPipelineCallBackUrlGeneratorImpl : ProjectPipelineCallBackUrlGenerator {

    override fun generateCallBackUrl(region: CallBackNetWorkRegionType?, url: String): String {
        return url
    }
}
