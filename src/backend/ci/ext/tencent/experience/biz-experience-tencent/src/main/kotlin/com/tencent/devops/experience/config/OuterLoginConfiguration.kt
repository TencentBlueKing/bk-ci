package com.tencent.devops.experience.config

import com.tencent.bkuser.ApiClient
import com.tencent.bkuser.api.ProfilesApi
import com.tencent.bkuser.api.V1Api
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OuterLoginConfiguration {
    @Value("\${outer.api.host:#{null}}")
    private var outApiHost: String? = null

    @Value("\${outer.api.token:#{null}}")
    private var outApiToken: String? = null

    @Bean
    fun bkApiClient(): ApiClient {
        return ApiClient().setBasePath(outApiHost).addDefaultHeader("Authorization", "iBearer $outApiToken")
    }

    @Bean
    fun v1Api(apiClient: ApiClient): V1Api {
        return V1Api(apiClient)
    }

    @Bean
    fun profileApi(apiClient: ApiClient): ProfilesApi {
        return ProfilesApi(apiClient)
    }
}
