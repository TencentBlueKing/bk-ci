package com.tencent.devops.lambda.pojo

data class BuildResult(
    val buildData: BuildData,
    val elementData: List<ElementData>
)