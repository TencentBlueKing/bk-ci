package com.tencent.devops.process.engine.service

interface ProjectPipelineCallBackUrlGenerator {

    fun generateCallBackUrl(url: String): String
}