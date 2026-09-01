package com.tencent.devops.process.yaml.transfer

data class PipelineTransferValidateDetail(
    val messageCode: String,
    val params: List<String>? = null
)
