package com.tencent.devops.websocket.v2

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.websocket.v2.WsTopic
import com.tencent.devops.process.api.service.ServiceBuildPermissionResource
import com.tencent.devops.process.api.service.ServicePipelinePermissionResource
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "websocket.v2", name = ["enabled"], havingValue = "true")
class ProcessWsTopicAuthorizer {
    @Bean
    @ConditionalOnMissingBean(WsTopicAuthorizer::class)
    fun websocketTopicAuthorizer(client: Client): WsTopicAuthorizer = WsTopicAuthorizer { userId, topics ->
        val accepted = mutableSetOf<String>()
        topics.groupBy { it.projectId to it.pipelineId }.forEach { (pipeline, interests) ->
            if (client.get(ServicePipelinePermissionResource::class).checkPipelinePermission(
                    userId, pipeline.first, pipeline.second, AuthPermission.VIEW
                ).data == true
            ) {
                interests.filter { it.buildId == null }.forEach { accepted.add(it.value) }
                interests.filter { it.buildId != null }.groupBy { it.buildId!! }.forEach { (buildId, attempts) ->
                    if (client.get(ServiceBuildPermissionResource::class).checkViewPermission(
                            userId, pipeline.first, pipeline.second, buildId
                        ).data == true
                    ) attempts.forEach { accepted.add(it.value) }
                }
            }
        }
        accepted
    }
}
