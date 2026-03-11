package com.tencent.devops.remotedev.pojo.remotedevsup

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DevcloudCVMData(
    val itemId: String?,
    val itemName: String?,
    val ip: String?,
    val name: String?,
    val resourceType: String?,
    val assetId: String?,
    val instanceId: String?,
    val tcinstanceType: String?,
    val cpu: String?,
    val memory: String?,
    val dataDiskType: String?,
    val dataDiskSize: String?,
    val systemDiskType: String?,
    val systemDiskSize: String?,
    val arch: String?,
    val regionId: String?,
    val zoneId: String?,
    val subnet: String?,
    val vpc: String?
)
