package com.tencent.devops.store.common.config

import com.tencent.devops.common.security.crypto.CryptoKeyRefreshExecutor
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshProperties
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshStartup
import com.tencent.devops.common.security.crypto.CryptoKeyRefreshWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class StoreCryptoKeyRefreshConfiguration {
    @Bean
    fun storeCryptoKeyRefreshStartup(
        @Value("\${spring.application.name:store}")
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
