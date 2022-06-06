package com.tencent.devops.dispatch.kubernetes.configuration

import com.tencent.devops.dispatch.kubernetes.kubernetes.client.V1ApiSet
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.Configuration
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.BatchV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.Config
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean

@org.springframework.context.annotation.Configuration
class AutoDispatchKubernetesConfiguration : ApplicationContextAware {
    @Bean
    fun v1ApiSet(): V1ApiSet {
        val client = Config.defaultClient() // TODO("需要可配置化")
        return V1ApiSet(
            CoreV1Api(client),
            BatchV1Api(client),
            AppsV1Api(client)
        )
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        Configuration.setDefaultApiClient(applicationContext.getBean(ApiClient::class.java))
    }
}
