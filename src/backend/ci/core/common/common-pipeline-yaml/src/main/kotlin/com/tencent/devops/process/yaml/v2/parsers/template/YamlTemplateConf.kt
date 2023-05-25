package com.tencent.devops.process.yaml.v2.parsers.template

/**
 * 模板替换的配置选项
 * @param useOldParametersExpression 是否使用旧版的表达式逻辑替换parameters
 */
data class YamlTemplateConf(
    val useOldParametersExpression: Boolean = false
)
