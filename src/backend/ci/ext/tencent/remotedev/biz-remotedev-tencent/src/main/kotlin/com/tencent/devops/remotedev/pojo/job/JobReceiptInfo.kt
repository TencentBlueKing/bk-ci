package com.tencent.devops.remotedev.pojo.job

interface JobReceiptInfo

data class PipelineJobReceiptInfo(
    val buildId: String,
    val buildNum: Int?
) : JobReceiptInfo
