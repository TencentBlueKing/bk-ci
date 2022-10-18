package com.tencent.bk.codecc.apiquery.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class ApiGatewayAuthProperties {

    @Value("\${api.gateway.auth.enabled:false}")
    val enabled: String? = null

    companion object{
        var properties: ApiGatewayAuthProperties? = null
    }

    @PostConstruct
    private fun init(){
        ApiGatewayAuthProperties.properties = this;
    }

}