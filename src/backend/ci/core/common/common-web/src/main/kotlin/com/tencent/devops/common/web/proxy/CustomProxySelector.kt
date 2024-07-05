package com.tencent.devops.common.web.proxy

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

class CustomProxySelector(
    private val proxy: Proxy,
    private val proxyHostsRegex: List<Regex>
) : ProxySelector() {

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
        return getDefault().select(uri)
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        LOG.warn("[CustomProxySelector|connect to proxy failed: uri: $uri|sa: $sa|", ioe)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CustomProxySelector::class.java.name)
    }
}
