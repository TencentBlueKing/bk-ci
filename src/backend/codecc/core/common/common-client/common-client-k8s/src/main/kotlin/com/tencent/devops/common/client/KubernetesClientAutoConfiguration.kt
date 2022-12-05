/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.devops.common.client

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.client.discovery.DiscoveryUtils
import com.tencent.devops.common.client.discovery.KubernetesDiscoveryUtils
import com.tencent.devops.common.client.ms.KubernetesClient
import com.tencent.devops.common.client.pojo.AllProperties
import com.tencent.devops.common.client.proxy.DevopsProxy
import com.tencent.devops.common.service.ServiceAutoConfiguration
import com.tencent.devops.common.util.JsonUtil
import feign.RequestInterceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration
import org.springframework.cloud.kubernetes.client.discovery.KubernetesInformerDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.tencent.devops.common.service.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.core.Ordered
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
@PropertySource("classpath:/common-client.properties")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureAfter(ServiceAutoConfiguration::class, LoadBalancerAutoConfiguration::class)
class KubernetesClientAutoConfiguration {

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesClientAutoConfiguration::class.java)
    }

    private val languageHeaderName = "Accept-Language"

    @Bean
    fun allProperties() = AllProperties()

    @Bean
    fun clientErrorDecoder() = ClientErrorDecoder(JsonUtil.getObjectMapper())


    @Bean
    @ConditionalOnMissingBean(Client::class)
    fun client(
            clientErrorDecoder: ClientErrorDecoder,
            @Autowired allProperties: AllProperties,
            @Autowired(required = false) discoveryClient: KubernetesInformerDiscoveryClient
    ) = KubernetesClient(discoveryClient, clientErrorDecoder, allProperties)


    @Bean(name = ["normalRequestInterceptor"])
    fun requestInterceptor(): RequestInterceptor {
        return RequestInterceptor { requestTemplate ->
            logger.info("url:${requestTemplate.path()}")
            val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
                ?: return@RequestInterceptor
            val request = attributes.request
            val languageHeaderValue = request.getHeader(languageHeaderName)
            if (!languageHeaderValue.isNullOrBlank())
                requestTemplate.header(languageHeaderName, languageHeaderValue) // 设置Accept-Language请求头
        }
    }


    @Bean(name = ["devopsRequestInterceptor"])
    fun bsRequestInterceptor(): RequestInterceptor {
        return RequestInterceptor { requestTemplate ->
            logger.info("url:${requestTemplate.path()}")
            val projectId = DevopsProxy.projectIdThreadLocal.get() as String?
            if(!projectId.isNullOrBlank()){
                logger.info("project id of header: $projectId")
                requestTemplate.header(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            }

            val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
                ?: return@RequestInterceptor
            val request = attributes.request
            val bkTicket = request.getHeader(AUTH_HEADER_DEVOPS_BK_TICKET)
            val userName = request.getHeader(AUTH_HEADER_DEVOPS_USER_ID)

            if(!bkTicket.isNullOrBlank()){
                requestTemplate.header(AUTH_HEADER_DEVOPS_BK_TICKET, bkTicket)
            }
            if(!userName.isNullOrBlank()){
                requestTemplate.header(AUTH_HEADER_DEVOPS_USER_ID, userName)
            }
        }
    }

    @Bean
    @ConditionalOnMissingBean(DiscoveryUtils::class)
    fun discoveryUtils(@Autowired discoveryClient: DiscoveryClient,
                       @Autowired profile: Profile
    ) = KubernetesDiscoveryUtils(discoveryClient,profile)

}
