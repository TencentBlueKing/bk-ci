package com.tencent.devops.store.pojo.common.version

data class StoreVersionLogInfo(
    val version: String,
    val tag: String,
    val lastUpdateTime: String,
    val updateLog: String

)
