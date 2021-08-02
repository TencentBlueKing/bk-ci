package com.tencent.devops.common.web

import com.tencent.devops.common.web.proxy.CustomProxySelector
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector

@Order(Ordered.HIGHEST_PRECEDENCE)
class GlobalProxyConfiguration(
    /**
     * 是否开启，设置为 true 才生效
     */
    private val enableProxy: String,
    /**
     *  需要代理的hosts，多个使用","隔开，支持正则表达式
     */
    private val proxyHosts: String,
    /**
     *  代理服务器类型，可 HTTP, SOCKS
     */
    private val proxyServerType: String,
    /**
     *  代理服务器主机，host 或者 ip
     */
    private val proxyServerHost: String,
    /**
     *  代理服务器端口
     */
    private val proxyServerPort: String
) {

    companion object {
        private val spliterator = Regex(",")
        private val LOG = LoggerFactory.getLogger(GlobalProxyConfiguration::class.java.name)
    }

    init {
        proxyConfiguration()
    }

    private fun proxyConfiguration() {
        LOG.info("proxy configuration enable: $enableProxy")
        LOG.info("proxy configuration proxyHosts: $proxyHosts")
        LOG.info("proxy configuration proxyServerType: $proxyServerType")
        LOG.info("proxy configuration proxyServerHost: $proxyServerHost")
        LOG.info("proxy configuration proxyServerPort: $proxyServerPort")
        val port = proxyServerPort.toIntOrNull()
        val enableFlag = enableProxy.toBoolean() && proxyHosts.isNotBlank() &&
            proxyServerType.isNotBlank() && proxyServerHost.isNotBlank()
        if (enableFlag && port != null) {
            Proxy.Type.values().firstOrNull { it.name == proxyServerType }?.also { type ->
                val proxyHostsRegex = spliterator.split(proxyHosts).toSet().map { Regex(it.trim()) }
                val socketAddress = InetSocketAddress(proxyServerHost, port)
                val proxy = Proxy(type, socketAddress)
                val proxySelector = CustomProxySelector(proxy, proxyHostsRegex)
                LOG.info("$proxySelector will use $proxy to proxy request")
                ProxySelector.setDefault(proxySelector)
            }
        }
        LOG.info("current default proxy selector: ${ProxySelector.getDefault()}")
    }
}
