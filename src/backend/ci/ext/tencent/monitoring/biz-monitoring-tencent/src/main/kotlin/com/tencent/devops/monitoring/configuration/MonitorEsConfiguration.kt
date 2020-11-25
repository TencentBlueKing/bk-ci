package com.tencent.devops.monitoring.configuration

import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MonitorEsConfiguration {
    @Value("\${elasticsearch.ip:#{null}}")
    private var ip: String? = null

    @Value("\${elasticsearch.port:#{null}}")
    private var port: Int? = 0

    @Value("\${elasticsearch.user:#{null}}")
    private var user: String? = null

    @Value("\${elasticsearch.password:#{null}}")
    private var password: String? = null

    @Bean
    fun restHighLevelClient(): RestHighLevelClient {
        val credentialsProvider: CredentialsProvider = BasicCredentialsProvider()
        credentialsProvider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(user, password))
        val builder: RestClientBuilder = RestClient.builder(HttpHost(ip, port!!))
            .setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(
                    credentialsProvider
                )
            }

        return RestHighLevelClient(builder.build())
    }
}