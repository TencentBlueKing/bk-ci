package com.tencent.devops.stream.v1.pojo

class V1RtxCustomProperty(
    val enabled: Boolean,
    val receivers: Set<String>
)

class V1EmailProperty(
    val enabled: Boolean,
    val receivers: Set<String>
)

class V1RtxGroupProperty(
    val enabled: Boolean,
    val groupIds: Set<String>
)
