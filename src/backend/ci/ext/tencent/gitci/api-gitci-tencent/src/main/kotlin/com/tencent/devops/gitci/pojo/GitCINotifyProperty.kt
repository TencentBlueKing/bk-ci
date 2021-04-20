package com.tencent.devops.gitci.pojo

import com.tencent.devops.gitci.pojo.enums.GitCINotifyType

abstract class GitCINotifyProperty(open val enabled: Boolean, open val notifyType: GitCINotifyType)

class RtxCustomProperty(
    override val enabled: Boolean,
    val receivers: Set<String>
) : GitCINotifyProperty(enabled, GitCINotifyType.RTX_CUSTOM)

class EmailProperty(
    override val enabled: Boolean,
    val receivers: Set<String>
) : GitCINotifyProperty(enabled, GitCINotifyType.EMAIL)

class RtxGroupProperty(
    override val enabled: Boolean,
    val groupIds: Set<String>
) : GitCINotifyProperty(enabled, GitCINotifyType.RTX_GROUP)
