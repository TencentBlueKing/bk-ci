package com.tencent.devops.remotedev.filter.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class IpFilterTest {

    @Test
    fun isIpInWhitelist() {
        val whitelist = mutableSetOf("9.112.211.0/24")
        val ip1 = "192.168.1.100"
        val ip2 = "9.112.211.22"
        val ip3 = "9.112.211.177"
        val ip4 = "9.112.212.12"

        Assertions.assertFalse(IpFilter.isIpInWhitelist(ip1, whitelist)) // false
        Assertions.assertTrue(IpFilter.isIpInWhitelist(ip2, whitelist)) // true
        Assertions.assertTrue(IpFilter.isIpInWhitelist(ip3, whitelist)) // true
        Assertions.assertFalse(IpFilter.isIpInWhitelist(ip4, whitelist)) // false

        whitelist.clear()
        whitelist.add("0.0.0.0/0")

        Assertions.assertTrue(IpFilter.isIpInWhitelist(ip1, whitelist)) // true
        Assertions.assertTrue(IpFilter.isIpInWhitelist(ip2, whitelist)) // true
        Assertions.assertTrue(IpFilter.isIpInWhitelist(ip3, whitelist)) // true
        Assertions.assertTrue(IpFilter.isIpInWhitelist(ip4, whitelist)) // true
    }
}
