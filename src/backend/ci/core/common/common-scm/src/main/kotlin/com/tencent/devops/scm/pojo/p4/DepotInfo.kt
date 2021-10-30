package com.tencent.devops.scm.pojo.p4

import com.perforce.p4java.core.IDepot
import com.perforce.p4java.core.IMapEntry
import com.perforce.p4java.core.ViewMap
import java.time.LocalDateTime

data class DepotInfo(
    val name: String,
    val ownerName: String,
    val modDate: LocalDateTime,
    val description: String,
    val depotType: IDepot.DepotType,
    val address: String? = null,
    val suffix: String? = null,
    val streamDepth: String? = null,
    val depotMap: String = "$name/...",
    val specMap: ViewMap<IMapEntry>? = null
)
