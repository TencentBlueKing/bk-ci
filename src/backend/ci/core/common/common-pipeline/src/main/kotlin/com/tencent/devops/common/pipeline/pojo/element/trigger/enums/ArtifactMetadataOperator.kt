package com.tencent.devops.common.pipeline.pojo.element.trigger.enums

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 制品元数据过滤运算符。
 *
 * 序列化/存储/运行时上下文统一使用大写枚举名（EQ/NE/CONTAINS/EXISTS/NOT_EXISTS）；
 * [value] 为小写形态，仅用于 YAML 序列化（接入规范要求小写），不参与模型序列化。
 */
@Schema(title = "制品元数据运算符")
enum class ArtifactMetadataOperator(val value: String) {
    @Schema(title = "等于")
    EQ("eq"),

    @Schema(title = "不等于")
    NE("ne"),

    @Schema(title = "包含")
    CONTAINS("contains"),

    @Schema(title = "键存在")
    EXISTS("exists"),

    @Schema(title = "键不存在")
    NOT_EXISTS("not_exists");

    companion object {
        fun parse(raw: String?): ArtifactMetadataOperator {
            if (raw.isNullOrBlank()) return EQ
            return runCatching { valueOf(raw.trim().uppercase()) }.getOrDefault(EQ)
        }
    }
}
