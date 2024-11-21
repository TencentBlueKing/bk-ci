package com.tencent.devops.common.archive.pojo

data class ProjectMetadata(
    /**
     * 元数据键
     */
    val key: String,
    /**
     * 元数据值
     */
    var value: Any,
)
