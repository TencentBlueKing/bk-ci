package com.tencent.devops.process.pojo.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "yaml文件类型")
enum class YamlFileType {
    @Schema(description = "流水线")
    PIPELINE,

    @Schema(description = "模板")
    TEMPLATE;

    /**
     * 是否可以执行
     */
    fun canExecute(): Boolean {
        return this == PIPELINE
    }

    companion object {
        fun getFileType(filePath: String): YamlFileType {
            return when {
                filePath.startsWith(".ci/templates") -> TEMPLATE
                else -> PIPELINE
            }
        }
    }
}
