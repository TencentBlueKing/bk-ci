package com.tencent.devops.auth.service.secops

import com.tencent.devops.auth.pojo.vo.SecOpsWaterMarkInfoVo

/**
 * 安全相关接口
 */
interface SecOpsService {
    /**
     * 获取用户水印信息
     */
    fun getUserWaterMark(userId: String): SecOpsWaterMarkInfoVo
}
