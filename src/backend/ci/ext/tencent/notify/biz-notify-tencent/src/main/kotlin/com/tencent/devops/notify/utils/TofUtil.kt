package com.tencent.devops.notify.utils

import com.tencent.devops.common.notify.utils.TOFConfiguration
import com.tencent.devops.notify.pojo.BaseMessage
import com.tencent.devops.notify.pojo.ExtTOF4SecurityInfo

object TofUtil {

    /**
     * 获取TOF配置
     */
    fun getTofConfig(
        rtxNotifyMessage: BaseMessage,
        tofConfiguration: TOFConfiguration
    ): Map<String, String>? {
        val version4Systems = tofConfiguration.getVersion4Systems()
        if (version4Systems != null) {
            // 优先获取第三方的配置
            val tof4EncryptKey = version4Systems["ext-encrypt-key"]
            val extTof4SecurityInfo = ExtTOF4SecurityInfo.get(rtxNotifyMessage, tof4EncryptKey)
            if (rtxNotifyMessage.v2ExtInfo.isNotBlank() && !extTof4SecurityInfo.enable) {
                return null
            }
            if (extTof4SecurityInfo.enable) {
                return mapOf(
                    "tof4Enabled" to "true",
                    "paasId" to extTof4SecurityInfo.passId,
                    "token" to extTof4SecurityInfo.token,
                    "host" to version4Systems["host"]!!,
                    "sys-id" to extTof4SecurityInfo.passId // 兼容数据库
                )
            }
            // 其次处理蓝盾tof4
            return mapOf(
                "tof4Enabled" to "true",
                "paasId" to version4Systems["paas-id"]!!,
                "token" to version4Systems["token"]!!,
                "host" to version4Systems["host"]!!,
                "sys-id" to version4Systems["paas-id"]!! // 兼容数据库
            )
        }
        // 最后处理蓝盾tof3
        return tofConfiguration.getConfigurations(rtxNotifyMessage.tofSysId)
    }
}
