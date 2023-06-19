package com.tencent.devops.auth.service

/**
 * 蓝盾权限对接监控平台配置类
 */
interface AuthMonitorConfigService {
    /**
     * 获取监控平台组配置
     */
    fun getMonitorGroupConfig(): String
}
