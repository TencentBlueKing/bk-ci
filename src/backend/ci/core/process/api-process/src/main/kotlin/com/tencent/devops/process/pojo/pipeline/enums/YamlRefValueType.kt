package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "yaml引用值类型")
enum class YamlRefValueType {
    @Schema(description = "分支")
    BRANCH,
    @Schema(description = "blobId")
    BLOB_ID
}
