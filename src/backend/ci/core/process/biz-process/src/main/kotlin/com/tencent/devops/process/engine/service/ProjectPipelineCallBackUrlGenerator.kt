package com.tencent.devops.process.engine.service

import com.tencent.devops.process.pojo.pipeline.enums.CallBackNetWorkRegionType

interface ProjectPipelineCallBackUrlGenerator {

    fun generateCallBackUrl(region: CallBackNetWorkRegionType?, url: String): String
}
