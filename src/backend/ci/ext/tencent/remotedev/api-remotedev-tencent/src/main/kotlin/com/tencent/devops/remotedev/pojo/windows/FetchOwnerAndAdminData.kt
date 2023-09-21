package com.tencent.devops.remotedev.pojo.windows

data class FetchOwnerAndAdminData(
    val projectIds: List<String>
)

data class FetchOwnerAndAdminItem(
    val admin: Set<String>?,
    val owner: MutableSet<String>?
)