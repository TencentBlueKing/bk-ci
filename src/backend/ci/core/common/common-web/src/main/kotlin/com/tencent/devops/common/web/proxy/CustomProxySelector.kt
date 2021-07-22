package com.tencent.devops.common.web.proxy

import org.slf4j.LoggerFactory
import sun.net.spi.DefaultProxySelector
import java.net.Proxy
import java.net.SocketAddress
import java.net.URI

class CustomProxySelector(
    private val type: Proxy.Type,
    private val sa: SocketAddress,
    private val proxyHostsRegex: List<Regex>
) : DefaultProxySelector() {

    private val proxy: Proxy by lazy { Proxy(type, sa) }

    override fun select(uri: URI?): MutableList<Proxy> {
        if (uri == null) return mutableListOf(Proxy.NO_PROXY)
        LOG.debug("[CustomProxySelector|try to match $uri]")
        val inHosts = proxyHostsRegex.any {
            val matches = it.containsMatchIn(uri.host)
            if (matches) {
                LOG.debug("[CustomProxySelector|$uri matched $it]")
            }
            matches
        }
        if (inHosts) {
            return mutableListOf(proxy)
        }
        // 匹配不上的保留 Java 原有逻辑
        return super.select(uri)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CustomProxySelector::class.java.name)
    }
}
