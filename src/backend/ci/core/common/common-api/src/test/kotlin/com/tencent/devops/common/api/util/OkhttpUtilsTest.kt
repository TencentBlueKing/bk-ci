/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.UnknownHostException

class OkhttpUtilsTest {

    // -----------------------------------------------------------------------------------------------------------------
    // L1（始终拒绝）：回环 / 链路本地 / 元数据 host —— 任何配置都不能放行
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    fun `isAlwaysBlockedHost rejects loopback`() {
        assertTrue(OkhttpUtils.isAlwaysBlockedHost("127.0.0.1"))
        assertTrue(OkhttpUtils.isAlwaysBlockedHost("localhost"))
        assertTrue(OkhttpUtils.isAlwaysBlockedHost("ip6-localhost"))
    }

    @Test
    fun `isAlwaysBlockedHost rejects link-local and metadata addresses`() {
        assertTrue(OkhttpUtils.isAlwaysBlockedHost("169.254.169.254"))
        assertTrue(OkhttpUtils.isAlwaysBlockedHost("169.254.0.1"))
    }

    @Test
    fun `isAlwaysBlockedHost rejects metadata and k8s host names`() {
        assertTrue(OkhttpUtils.isAlwaysBlockedHost("metadata.google.internal"))
        assertTrue(OkhttpUtils.isAlwaysBlockedHost("metadata.tencentyun.com"))
        assertTrue(OkhttpUtils.isAlwaysBlockedHost("kubernetes.default"))
        assertTrue(OkhttpUtils.isAlwaysBlockedHost("kubernetes.default.svc"))
    }

    @Test
    fun `isAlwaysBlockedHost allows rfc1918 and public hosts`() {
        // L1 不拦截 RFC1918，是否拒绝由 L2 决定
        assertFalse(OkhttpUtils.isAlwaysBlockedHost("10.0.0.1"))
        assertFalse(OkhttpUtils.isAlwaysBlockedHost("192.168.1.1"))
        assertFalse(OkhttpUtils.isAlwaysBlockedHost("172.16.0.1"))
        // 公网 IP / 域名也不会命中 L1
        assertFalse(OkhttpUtils.isAlwaysBlockedHost("8.8.8.8"))
    }

    @Test
    fun `isAlwaysBlockedAddress rejects loopback any-local link-local multicast`() {
        assertTrue(OkhttpUtils.isAlwaysBlockedAddress(InetAddress.getByName("127.0.0.1")))
        assertTrue(OkhttpUtils.isAlwaysBlockedAddress(InetAddress.getByName("0.0.0.0")))
        assertTrue(OkhttpUtils.isAlwaysBlockedAddress(InetAddress.getByName("169.254.169.254")))
        assertTrue(OkhttpUtils.isAlwaysBlockedAddress(InetAddress.getByName("224.0.0.1")))
    }

    @Test
    fun `isAlwaysBlockedAddress allows rfc1918 site-local`() {
        assertFalse(OkhttpUtils.isAlwaysBlockedAddress(InetAddress.getByName("10.0.0.1")))
        assertFalse(OkhttpUtils.isAlwaysBlockedAddress(InetAddress.getByName("192.168.1.1")))
        assertFalse(OkhttpUtils.isAlwaysBlockedAddress(InetAddress.getByName("172.16.0.1")))
    }

    // -----------------------------------------------------------------------------------------------------------------
    // L1+L2（严格模式）：isExternalUrl / isPublicAddress —— 同时拒绝 RFC1918
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    fun `isExternalUrl rejects loopback and link-local`() {
        assertFalse(OkhttpUtils.isExternalUrl("http://127.0.0.1:8000/management/env"))
        assertFalse(OkhttpUtils.isExternalUrl("http://[::1]:8000/"))
        assertFalse(OkhttpUtils.isExternalUrl("http://169.254.169.254/latest/meta-data/iam/"))
    }

    @Test
    fun `isExternalUrl rejects rfc1918 site-local addresses`() {
        assertFalse(OkhttpUtils.isExternalUrl("http://10.0.0.1/"))
        assertFalse(OkhttpUtils.isExternalUrl("http://192.168.1.1/"))
        assertFalse(OkhttpUtils.isExternalUrl("http://172.16.0.1/"))
        assertFalse(OkhttpUtils.isExternalUrl("http://172.31.255.255/"))
    }

    @Test
    fun `isExternalUrl rejects metadata host names`() {
        assertFalse(OkhttpUtils.isExternalUrl("http://metadata.google.internal/computeMetadata/v1/"))
        assertFalse(OkhttpUtils.isExternalUrl("http://kubernetes.default/api/v1/secrets"))
        assertFalse(OkhttpUtils.isExternalUrl("http://localhost:8080/"))
    }

    @Test
    fun `isExternalUrl rejects invalid url`() {
        assertFalse(OkhttpUtils.isExternalUrl(""))
        assertFalse(OkhttpUtils.isExternalUrl("not a url"))
        assertFalse(OkhttpUtils.isExternalUrl("ftp://example.com/"))
    }

    @Test
    fun `isExternalUrl accepts public ip`() {
        assertTrue(OkhttpUtils.isExternalUrl("http://8.8.8.8/"))
        assertTrue(OkhttpUtils.isExternalUrl("https://1.1.1.1/"))
    }

    @Test
    fun `isPublicAddress rejects all internal addresses`() {
        // L1
        assertFalse(OkhttpUtils.isPublicAddress(InetAddress.getByName("127.0.0.1")))
        assertFalse(OkhttpUtils.isPublicAddress(InetAddress.getByName("0.0.0.0")))
        assertFalse(OkhttpUtils.isPublicAddress(InetAddress.getByName("169.254.169.254")))
        assertFalse(OkhttpUtils.isPublicAddress(InetAddress.getByName("224.0.0.1")))
        // L2
        assertFalse(OkhttpUtils.isPublicAddress(InetAddress.getByName("10.1.2.3")))
        assertFalse(OkhttpUtils.isPublicAddress(InetAddress.getByName("192.168.0.1")))
    }

    @Test
    fun `isPublicAddress accepts public address`() {
        assertTrue(OkhttpUtils.isPublicAddress(InetAddress.getByName("8.8.8.8")))
        assertTrue(OkhttpUtils.isPublicAddress(InetAddress.getByName("1.1.1.1")))
    }

    // -----------------------------------------------------------------------------------------------------------------
    // metadataSafeDns —— 私有云模式：仅拦截 L1，允许 RFC1918
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    fun `metadataSafeDns blocks loopback host`() {
        val dns = OkhttpUtils.metadataSafeDns()
        assertThrows(UnknownHostException::class.java) { dns.lookup("127.0.0.1") }
        assertThrows(UnknownHostException::class.java) { dns.lookup("localhost") }
    }

    @Test
    fun `metadataSafeDns blocks metadata and link-local hosts`() {
        val dns = OkhttpUtils.metadataSafeDns()
        assertThrows(UnknownHostException::class.java) { dns.lookup("169.254.169.254") }
        assertThrows(UnknownHostException::class.java) { dns.lookup("metadata.google.internal") }
        assertThrows(UnknownHostException::class.java) { dns.lookup("kubernetes.default") }
    }

    @Test
    fun `metadataSafeDns allows rfc1918 host`() {
        val dns = OkhttpUtils.metadataSafeDns()
        assertDoesNotThrow { dns.lookup("10.0.0.1") }
        assertDoesNotThrow { dns.lookup("192.168.0.1") }
        assertDoesNotThrow { dns.lookup("172.16.0.1") }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // ssrfSafeDns —— 严格模式：L1 + L2 全拦截
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    fun `ssrfSafeDns blocks loopback host`() {
        val dns = OkhttpUtils.ssrfSafeDns()
        assertThrows(UnknownHostException::class.java) { dns.lookup("127.0.0.1") }
        assertThrows(UnknownHostException::class.java) { dns.lookup("localhost") }
    }

    @Test
    fun `ssrfSafeDns blocks metadata host`() {
        val dns = OkhttpUtils.ssrfSafeDns()
        assertThrows(UnknownHostException::class.java) { dns.lookup("169.254.169.254") }
        assertThrows(UnknownHostException::class.java) { dns.lookup("metadata.google.internal") }
        assertThrows(UnknownHostException::class.java) { dns.lookup("kubernetes.default") }
    }

    @Test
    fun `ssrfSafeDns blocks rfc1918 host`() {
        val dns = OkhttpUtils.ssrfSafeDns()
        assertThrows(UnknownHostException::class.java) { dns.lookup("10.0.0.1") }
        assertThrows(UnknownHostException::class.java) { dns.lookup("192.168.0.1") }
    }
}
