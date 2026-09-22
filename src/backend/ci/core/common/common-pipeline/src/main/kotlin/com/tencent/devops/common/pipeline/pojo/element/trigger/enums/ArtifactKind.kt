package com.tencent.devops.common.pipeline.pojo.element.trigger.enums

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品触发形态。
 *
 * - [FILE]：单个文件；
 * - [FOLDER]：整个目录（上游将该目录一次性归档完成后触发）；
 * - [IMAGE]：容器镜像。
 *
 * 插件表单 / YAML 的 `kind` 在流水线、自定义仓库下取值 file/folder；
 * 镜像仓库不填该字段，运行时 kind 固定为 image。
 * 序列化/存储/运行时上下文统一使用大写枚举名（FILE/FOLDER/IMAGE）；
 * [value] 为小写形态，用于 YAML 序列化与 `ci.artifact_kind` 输出。
 */
@Schema(title = "触发形态")
enum class ArtifactKind(val value: String) {
    @Schema(title = "单个文件")
    FILE("file"),

    @Schema(title = "整个目录")
    FOLDER("folder"),

    @Schema(title = "容器镜像")
    IMAGE("image");

    companion object {
        fun parse(raw: String?): ArtifactKind {
            if (raw.isNullOrBlank()) return FILE
            return runCatching { valueOf(raw.trim().uppercase()) }.getOrDefault(FILE)
        }
    }
}
