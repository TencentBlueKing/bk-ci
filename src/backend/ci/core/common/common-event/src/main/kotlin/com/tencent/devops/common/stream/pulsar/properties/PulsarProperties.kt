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

package com.tencent.devops.common.stream.pulsar.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.pulsar")
data class PulsarProperties(
    var serviceUrl: String = "pulsar://localhost:6650",
    var ioThreads: Int = 1,
    var listenerThreads: Int = 1,
    var enableTcpNoDelay: Boolean = false,
    var keepAliveIntervalSec: Int = 20,
    var connectionTimeoutSec: Int = 10,
    var operationTimeoutSec: Int = 15,
    var startingBackoffIntervalMs: Int = 100,
    var maxBackoffIntervalSec: Int = 10,
    var consumerNameDelimiter: String = "",
    var namespace: String = "default",
    var tenant: String = "public",
    var tlsTrustCertsFilePath: String? = null,
    var tlsCiphers: Set<String> = emptySet<String>(),
    var tlsProtocols: Set<String> = emptySet<String>(),
    var tlsTrustStorePassword: String? = null,
    var tlsTrustStorePath: String? = null,
    var tlsTrustStoreType: String? = null,
    var useKeyStoreTls: Boolean = false,
    var allowTlsInsecureConnection: Boolean = false,
    var enableTlsHostnameVerification: Boolean = false,
    var tlsAuthCertFilePath: String? = null,
    var tlsAuthKeyFilePath: String? = null,
    var tokenAuthValue: String? = null,
    var oauth2IssuerUrl: String? = null,
    var oauth2CredentialsUrl: String? = null,
    var oauth2Audience: String? = null,
    var autoStart: Boolean = true,
    var enableTransaction: Boolean = false
)
