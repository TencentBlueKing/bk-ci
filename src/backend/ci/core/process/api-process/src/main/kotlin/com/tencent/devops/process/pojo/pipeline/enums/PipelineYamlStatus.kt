package com.tencent.devops.process.pojo.pipeline.enums

/**
 * YAML状态
 */
enum class PipelineYamlStatus {
    // 状态OK,已合入到主干
    OK,

    // 主干已删除
    DELETED,

    // 没有合入到主干
    UN_MERGED;
}
