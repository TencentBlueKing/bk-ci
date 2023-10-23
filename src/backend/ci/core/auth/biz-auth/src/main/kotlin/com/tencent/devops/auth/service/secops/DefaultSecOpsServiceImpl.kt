package com.tencent.devops.auth.service.secops

import com.tencent.devops.auth.pojo.vo.SecOpsWaterMarkInfoVo

class DefaultSecOpsServiceImpl : SecOpsService {
    override fun getUserWaterMark(userId: String): SecOpsWaterMarkInfoVo {
        return SecOpsWaterMarkInfoVo(
            type = "",
            data = ""
        )
    }
}
