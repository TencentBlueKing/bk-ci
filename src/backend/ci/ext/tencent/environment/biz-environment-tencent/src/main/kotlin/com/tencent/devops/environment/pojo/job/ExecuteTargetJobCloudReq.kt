package com.tencent.devops.environment.pojo.job

@Suppress("ALL")
data class ExecuteTargetJobCloudReq(
    val dynamic_group_list: List<String>?,
    val topo_node_list: List<String>?,
    val ip_list: List<HostJobCloudReq>?
)