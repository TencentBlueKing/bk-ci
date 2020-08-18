package com.tencent.devops.monitoring.configuration

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.xpack.client.PreBuiltXPackTransportClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetAddress

@Configuration
class MonitorEsConfiguration {
    @Value("\${elasticsearch.ip}")
    private val ip: String? = null

    @Value("\${elasticsearch.port}")
    private val port: Int? = 0

    @Value("\${elasticsearch.user}")
    private val user: String? = null

    @Value("\${elasticsearch.password}")
    private val password: String? = null

    @Bean
    fun transportClient(): TransportClient {
        if (ip.isNullOrBlank()) {
            throw IllegalArgumentException("ES集群地址尚未配置: elasticsearch.ip")
        }
        if (port == null) {
            throw IllegalArgumentException("ES集群端口尚未配置: elasticsearch.port")
        }
        if (user.isNullOrBlank()) {
            throw IllegalArgumentException("ES集群名称尚未配置: elasticsearch.user")
        }
        if (password.isNullOrBlank()) {
            throw IllegalArgumentException("ES唯一名称尚未配置: elasticsearch.password")
        }
        return PreBuiltXPackTransportClient(
            Settings.builder()
                .put("xpack.security.user", "$user:$password")
                .build()
        ).addTransportAddress(InetSocketTransportAddress(InetAddress.getByName(ip), port))
    }
}