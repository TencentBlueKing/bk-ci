package com.tencent.devops.remotedev.pojo.op

data class RemotedevCvmFetchData(
    val projectId: String?,
    val zone: String?,
    val ips: List<String>?,
    val page: Int?,
    val pageSize: Int?
)
