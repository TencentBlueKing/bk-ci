package com.tencent.bk.codecc.task.pojo

data class CodeCCMarketAtom(
    val atomCode: String,
    val version: String?,
    val inputs: Map<String, Any>?,
    val outputs: Map<String, Any>?
)