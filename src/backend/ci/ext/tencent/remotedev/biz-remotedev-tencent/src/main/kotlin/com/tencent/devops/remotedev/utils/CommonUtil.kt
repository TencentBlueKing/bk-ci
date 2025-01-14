package com.tencent.devops.remotedev.utils

import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.common.QuotaType
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
            if (zoneIdCheck(
                    quotaType = null,
                    zoneType = zoneConfig.type,
                    zoneShortName = lazy { listOf(zoneConfig.zoneShortName) },
                    zoneId = resp.zoneId,
                    spec = spec
                )
            ) {
                return@forEach
            }
            res[resp.zoneId] = resp.machineResources?.firstOrNull { ma -> ma.machineType == size }?.free ?: 0
        }
        return res
    }

    fun zoneIdCheck(
        quotaType: QuotaType?,
        zoneType: WindowsResourceZoneConfigType,
        zoneId: String,
        zoneShortName: Lazy<List<String>>,
        spec: Lazy<List<String>>
    ): Boolean {
        // todo 待废弃
        if (quotaType != null) {
            return quotaType == QuotaType.OFFSHORE && zoneId in spec.value
        }
        /*特殊区域需要完全匹配*/
        if (zoneType != WindowsResourceZoneConfigType.DEFAULT && zoneId !in zoneShortName.value) {
            return true
        }
        /*DEFAULT需要不在特殊区域*/
        if (zoneType == WindowsResourceZoneConfigType.DEFAULT && zoneId in spec.value) {
            return true
        }
        /*非特殊区域需要去掉数字后完全匹配*/
        if (zoneType == WindowsResourceZoneConfigType.DEFAULT &&
            zoneId.replace(Regex("\\d+"), "") in zoneShortName.value
        ) {
            return true
        }

        return false
    }
}
