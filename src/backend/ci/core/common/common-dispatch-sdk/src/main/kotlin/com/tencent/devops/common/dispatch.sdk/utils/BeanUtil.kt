package com.tencent.devops.common.dispatch.sdk.utils

import com.tencent.devops.common.dispatch.sdk.service.DispatchMessageTracking
import com.tencent.devops.common.service.utils.SpringContextUtil

object BeanUtil {
    fun getDispatchMessageTracking(): DispatchMessageTracking {
        return SpringContextUtil.getBean(DispatchMessageTracking::class.java)
    }
}