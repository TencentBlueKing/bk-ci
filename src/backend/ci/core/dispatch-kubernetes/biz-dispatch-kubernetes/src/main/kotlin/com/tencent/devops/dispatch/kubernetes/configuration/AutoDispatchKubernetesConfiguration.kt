package com.tencent.devops.dispatch.kubernetes.configuration

import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.BatchV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.Config
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean

@org.springframework.context.annotation.Configuration
class AutoDispatchKubernetesConfiguration : ApplicationContextAware {
    @Bean
    fun apiClient(): ApiClient {
        // TODO 这个要支持可配置化
        return Config.defaultClient()
    }

    @Bean
    fun coreApiV1(apiClient: ApiClient): CoreV1Api {
        return CoreV1Api(apiClient)
    }

    @Bean
    fun batchApiV1(apiClient: ApiClient): BatchV1Api {
        return BatchV1Api(apiClient)
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        Configuration.setDefaultApiClient(applicationContext.getBean(ApiClient::class.java))
    }
}
