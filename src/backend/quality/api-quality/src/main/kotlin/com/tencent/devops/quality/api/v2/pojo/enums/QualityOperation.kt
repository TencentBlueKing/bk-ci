package com.tencent.devops.quality.api.v2.pojo.enums

enum class QualityOperation {
    GT,
    GE,
    LT,
    LE,
    EQ;

    companion object {
        fun convertToSymbol(operation: QualityOperation): String {
            return when (operation) {
                GT -> ">"
                GE -> ">="
                LT -> "<"
                LE -> "<="
                EQ -> "="
            }
        }
    }
}