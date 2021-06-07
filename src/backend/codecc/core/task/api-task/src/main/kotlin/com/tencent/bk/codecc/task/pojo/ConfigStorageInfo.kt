package com.tencent.bk.codecc.task.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class ConfigStorageInfo(
    @JsonProperty("limit_lfs_file_size")
    val limitLfsFileSize: Int,
    @JsonProperty("limit_size")
    val limitSize: Int,
    @JsonProperty("limit_file_size")
    val limitFileSize: Int,
    @JsonProperty("limit_lfs_size")
    val limitLfsSize: Int
)