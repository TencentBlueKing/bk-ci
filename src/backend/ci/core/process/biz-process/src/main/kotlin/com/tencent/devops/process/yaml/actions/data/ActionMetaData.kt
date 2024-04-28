package com.tencent.devops.process.yaml.actions.data

import com.tencent.devops.process.yaml.v2.enums.StreamObjectKind

/**
 * 保存action级别的一些meta数据
 * @param streamObjectKind action所属的stream事件
 */
data class ActionMetaData(
    val streamObjectKind: StreamObjectKind
)
