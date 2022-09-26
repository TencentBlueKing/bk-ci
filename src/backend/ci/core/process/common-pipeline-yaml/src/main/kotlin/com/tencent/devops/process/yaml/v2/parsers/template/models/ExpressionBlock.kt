package com.tencent.devops.process.yaml.v2.parsers.template.models

/**
 * 表达式括号项 ${{ }}
 * @param startIndex 括号开始位置即 $ 位置
 * @param endIndex 括号结束位置即最后一个 } 位置
 */
data class ExpressionBlock(
    var startIndex: Int,
    var endIndex: Int
)
