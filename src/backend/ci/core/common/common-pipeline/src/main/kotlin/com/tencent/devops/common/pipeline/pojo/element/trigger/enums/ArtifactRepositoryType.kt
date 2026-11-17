package com.tencent.devops.common.pipeline.pojo.element.trigger.enums

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 监听的目标仓库类型（制品到达触发器 repository 字段）
 *
 * 序列化/存储使用枚举名（大写 PIPELINE/CUSTOM/IMAGE），与系统其它触发器枚举保持一致；
 * [value] 为小写形态，仅用于写入 `ci.artifact_*` 运行时上下文（接入规范要求小写），不参与序列化。
 *
 * 仓库类型已隐含制品形态：
 * - PIPELINE / CUSTOM：文件或目录；
 * - IMAGE：容器镜像。
 */
@Schema(title = "监听仓库类型")
enum class ArtifactRepositoryType(val value: String) {
    @Schema(title = "流水线仓库")
    PIPELINE("pipeline"),

    @Schema(title = "自定义仓库")
    CUSTOM("custom"),

    @Schema(title = "镜像仓库")
    IMAGE("image");
}
