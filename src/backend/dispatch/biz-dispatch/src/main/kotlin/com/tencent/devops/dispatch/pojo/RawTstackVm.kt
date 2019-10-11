package com.tencent.devops.dispatch.pojo

data class RawTstackVm(
    var id: String,
    var floatingIp: String,
    var vmName: String,
    var vmOs: String,
    var vmOsVersion: String,
    var vmCpu: String,
    var vmMemory: String
)
