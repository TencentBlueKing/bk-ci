package com.tencent.devops.remotedev.service.job

interface JobReceiptInfo

data class PipelineJobReceiptInfo(
    val buildId: String,
    val buildNum: Int?
) : JobReceiptInfo
