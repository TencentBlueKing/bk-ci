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

    /**
     * 是否需要通知调度器,唤醒其他等到的文件
     */
    fun needNotifyScheduler(): Boolean {
        return this == TEMPLATE
    }

    /**
     * 依赖的类型列表
     */
    fun dependencyType(): List<YamlFileType> {
        return when (this) {
            PIPELINE -> return listOf(TEMPLATE)
            else -> emptyList()
        }
    }

    fun notifyType(): List<YamlFileType> {
        return when (this) {
            TEMPLATE -> return listOf(PIPELINE)
            else -> emptyList()
        }
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
