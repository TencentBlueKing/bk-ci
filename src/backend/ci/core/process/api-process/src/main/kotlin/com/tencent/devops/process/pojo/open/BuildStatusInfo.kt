package com.tencent.devops.process.pojo.open

import com.tencent.devops.common.pipeline.enums.BuildStatus

/**
 * open接口返回的构建状态封装
 */
data class BuildStatusInfo (
    val startUser: String,
    val debug: Boolean,
    val status: BuildStatus
)
