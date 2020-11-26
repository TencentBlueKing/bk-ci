package com.tencent.devops.common.pipeline.pojo.element.atom

enum class ManualReviewParamType(val value: String) {
    STRING("string"),
    TEXTAREA("textarea"),
    BOOLEAN("boolean"),
    ENUM("enum"),
    MULTIPLE("multiple");

    override fun toString() = value
}