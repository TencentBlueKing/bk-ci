package com.tencent.devops.common.api.enums.log

import io.swagger.v3.oas.annotations.media.Schema

enum class LogStorageMode {
    @Schema(title = "上报服务")
    UPLOAD,
    @Schema(title = "本地保存")
    LOCAL,
    @Schema(title = "仓库已归档")
    ARCHIVED;

    companion object {
        fun parse(modeName: String?): LogStorageMode {
            return when (modeName) {
                LOCAL.name -> LOCAL
                ARCHIVED.name -> ARCHIVED
                else -> UPLOAD
            }
        }
    }
}
