package com.tencent.devops.process.dao

/**
 * 流水线回调来源。MODEL 随编排保存，API 由回调接口注册。
 */
enum class PipelineCallbackSource {
    MODEL,
    API
}
