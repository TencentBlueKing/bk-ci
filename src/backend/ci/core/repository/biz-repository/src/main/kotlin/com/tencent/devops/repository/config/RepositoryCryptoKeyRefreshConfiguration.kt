package com.tencent.devops.repository.config

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshExecutor
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshProperties
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshStartup
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RepositoryCryptoKeyRefreshConfiguration {
    @Bean
    fun repositoryCryptoKeyRefreshStartup(
        @Value("\${spring.application.name:repository}")
        applicationName: String,
        properties: CryptoKeyRefreshProperties,
        executor: CryptoKeyRefreshExecutor,
        writers: List<CryptoKeyRefreshWriter>
    ) = CryptoKeyRefreshStartup(
        applicationName = applicationName,
        properties = properties,
        executor = executor,
        writers = writers
    )
}
