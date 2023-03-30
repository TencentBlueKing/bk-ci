package com.tencent.devops.environment.pojo.enums

import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode

enum class GatewayType(val typeName: String) {
    SZ("深圳"),
    SH("上海"),
    CD("成都"),
    TJ("天津"),
    XJP("新加坡");


    companion object {
        
        fun i18n(typeName: String): String {
            return when (typeName) {
                SZ.typeName -> MessageCodeUtil.getCodeLanMessage(EnvironmentMessageCode.BK_SZ)
                SH.typeName -> MessageCodeUtil.getCodeLanMessage(EnvironmentMessageCode.BK_SH)
                CD.typeName -> MessageCodeUtil.getCodeLanMessage(EnvironmentMessageCode.BK_CD)
                TJ.typeName -> MessageCodeUtil.getCodeLanMessage(EnvironmentMessageCode.BK_TJ)
                else -> {
                    MessageCodeUtil.getCodeLanMessage(EnvironmentMessageCode.BK_XJP)}
            }
        }

    }
}