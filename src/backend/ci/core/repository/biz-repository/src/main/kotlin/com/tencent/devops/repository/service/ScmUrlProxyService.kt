package com.tencent.devops.repository.service

/**
 * 源代码管理url代理服务
 *
 * 当repository服务无法直接访问源代码库管理平台时，可以通过代理实现
 */
interface ScmUrlProxyService {

    fun getProxyUrl(url: String): String
}
