package com.tencent.devops.remotedev.utils

import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.remotedev.ResourceVmRespData

object CommonUtil {

    fun parseResourceVmRespData(
        data: List<ResourceVmRespData>?,
        zoneConfig: WindowsResourceZoneConfig,
        spec: Lazy<List<String>>,
        size: String
    ): Map<String, Int> {
        val res = mutableMapOf<String, Int>()
        data?.forEach { resp ->
            /*特殊区域需要完全匹配*/
            if (zoneConfig.type != WindowsResourceZoneConfigType.DEFAULT && resp.zoneId != zoneConfig.zoneShortName) {
                return@forEach
            }
            /*DEFAULT需要不在特殊区域*/
            if (zoneConfig.type == WindowsResourceZoneConfigType.DEFAULT && resp.zoneId in spec.value) {
                return@forEach
            }
            /*非特殊区域需要去掉数字后完全匹配*/
            if (zoneConfig.type == WindowsResourceZoneConfigType.DEFAULT &&
                resp.zoneId.replace(Regex("\\d+"), "") != zoneConfig.zoneShortName
            ) {
                return@forEach
            }
            res[resp.zoneId] = resp.machineResources?.firstOrNull { ma -> ma.machineType == size }?.free ?: 0
        }
        return res
    }
}
