package com.tencent.devops.common.pulsar.properties

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties(prefix = "pulsar")
class PulsarProperties {
    var serviceUrl = "pulsar://localhost:6650"
    var ioThreads = 10
    var listenerThreads = 10
    var isEnableTcpNoDelay = false
    var keepAliveIntervalSec = 20
    var connectionTimeoutSec = 10
    var operationTimeoutSec = 15
    var startingBackoffIntervalMs = 100L
    var maxBackoffIntervalSec = 10L
    var consumerNameDelimiter = ""
    var namespace = "default"
    var tenant = "public"
    var tlsTrustCertsFilePath: String? = null
    var tlsCiphers: Set<String> = HashSet()
    var tlsProtocols: Set<String> = HashSet()
    var tlsTrustStorePassword: String? = null
    var tlsTrustStorePath: String? = null
    var tlsTrustStoreType: String? = null
    var useKeyStoreTls = false
    var allowTlsInsecureConnection = false
    var enableTlsHostnameVerification = false
    var tlsAuthCertFilePath: String? = null
    var tlsAuthKeyFilePath: String? = null
    var tokenAuthValue: String? = null
    var oauth2IssuerUrl: String? = null
    var oauth2CredentialsUrl: String? = null
    var oauth2Audience: String? = null
    var isAutoStart = true
}
