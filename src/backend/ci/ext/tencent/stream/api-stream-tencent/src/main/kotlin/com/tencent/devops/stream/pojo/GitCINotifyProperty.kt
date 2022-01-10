package com.tencent.devops.stream.pojo

class RtxCustomProperty(
    val enabled: Boolean,
    val receivers: Set<String>
)

class EmailProperty(
    val enabled: Boolean,
    val receivers: Set<String>
)

class RtxGroupProperty(
    val enabled: Boolean,
    val groupIds: Set<String>
)
