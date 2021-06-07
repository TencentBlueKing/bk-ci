package com.tencent.bk.codecc.task.pojo

data class CodeCCPipelineReq(
    // 流水线运行时参数
    val runtimeParamMap: List<CodeCCRuntimeParam>?,
    // codecc过滤插件
    val codeccPreTreatment: CodeCCPreTreatment?,
    // 代码库信息
    val repoInfo: CodeCCAuthInfo,
    // codecc代码扫描信息
    val codeScanInfo: CodeCCCodeScan,
    // devnet回调插件
    val customPostDevnetAtom: List<CodeCCMarketAtom>?,
    // IDC回调插件
    val customPostIDCAtom: List<CodeCCMarketAtom>?
)