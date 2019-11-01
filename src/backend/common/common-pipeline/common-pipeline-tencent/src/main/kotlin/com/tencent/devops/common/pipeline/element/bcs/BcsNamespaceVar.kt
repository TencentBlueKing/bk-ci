package com.tencent.devops.common.pipeline.element.bcs

data class BcsNamespaceVar(
    val namespace: String = "",
    val varKey: String = "",
    val varValue: String = ""
)
