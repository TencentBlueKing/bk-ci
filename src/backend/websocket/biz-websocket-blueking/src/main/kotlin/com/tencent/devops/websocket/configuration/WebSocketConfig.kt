package com.tencent.devops.websocket.configuration

import com.tencent.devops.common.client.Client
import com.tencent.devops.websocket.handler.BKHandshakeInterceptor
import com.tencent.devops.websocket.handler.ConnectChannelInterceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry

@Configuration
@EnableWebSocketMessageBroker
// @EnableMBeanExport
class WebSocketConfig @Autowired constructor(
    private val client: Client,
    private val bkHandshake: BKHandshakeInterceptor,
    private val connectChannelInterceptor: ConnectChannelInterceptor
) : AbstractWebSocketMessageBrokerConfigurer() {

    @Value("\${thread.min}")
    private val min: Int = 8

    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws/user/pipelines/pipelineStatus").addInterceptors(
//            object : HandshakeInterceptor {
//                @Throws(Exception::class)
//                override fun beforeHandshake(
//                    request: ServerHttpRequest,
//                    response: ServerHttpResponse,
//                    wsHandler: WebSocketHandler,
//                    attributes: MutableMap<String, Any>
//                ): Boolean {
//                    val req = request as ServletServerHttpRequest
//                    val projectId = req.servletRequest.getParameter("projectId")
//                    val accessToken = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
//                    val userId = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_USER_ID)
//                    logger.info("before hand shake, userId is $userId, projectId is $projectId")
//                    val projectList = client.get(ServiceProjectResource::class).list(accessToken).data
//                    val privilegeProjectCodeList = mutableListOf<String>()
//                    projectList?.map {
//                        privilegeProjectCodeList.add(it.project_code)
//                    }
//                    if (privilegeProjectCodeList.contains(projectId)) {
//                        logger.info("hand shake success.")
//                        return true
//                    }
//
//                    logger.error("userId or projectId is empty.")
//                    return false
//                }
//
//                override fun afterHandshake(
//                    request: ServerHttpRequest,
//                    response: ServerHttpResponse,
//                    wsHandler: WebSocketHandler,
//                    exception: Exception
//                ) {
//                    logger.info("after hand shake, do nothing!")
//                }
//            }
        ).addInterceptors(bkHandshake).setAllowedOrigins("*").withSockJS()
        registry.addEndpoint("/ws/user").addInterceptors(bkHandshake).setAllowedOrigins("*").withSockJS()
    }

//    private fun checkProject(projectId: String,req: ServletServerHttpRequest): Boolean
//    {
//        val accessToken = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
//        val userId = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_USER_ID)
//        logger.info("before hand shake, userId is $userId, projectId is $projectId")
//        val projectList = client.get(ServiceProjectResource::class).list(accessToken).data
//        val privilegeProjectCodeList = mutableListOf<String>()
//        projectList?.map {
//            privilegeProjectCodeList.add(it.project_code)
//        }
//        if (privilegeProjectCodeList.contains(projectId)) {
//            logger.info("hand shake success.")
//            return true
//        } else {
//            logger.warn("hand shake fail, check project fail.userId:$userId,project:$projectId")
//            throw RuntimeException("hand shake fail, check project fail")
//        }
//        return false
//    }

    @Override
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        var defaultCorePoolSize = min
        if (defaultCorePoolSize < Runtime.getRuntime().availableProcessors() * 2) {
            defaultCorePoolSize = Runtime.getRuntime().availableProcessors() * 2
        }
        registration.taskExecutor().corePoolSize(defaultCorePoolSize)
            .maxPoolSize(defaultCorePoolSize * 2)
            .keepAliveSeconds(60)
    }

    @Override
    override fun configureClientOutboundChannel(registration: ChannelRegistration) {
        var defaultCorePoolSize = min
        if (defaultCorePoolSize < Runtime.getRuntime().availableProcessors() * 2) {
            defaultCorePoolSize = Runtime.getRuntime().availableProcessors() * 2
        }
        registration.taskExecutor().corePoolSize(defaultCorePoolSize).maxPoolSize(defaultCorePoolSize * 2)
//        registration.interceptors(connectChannelInterceptor)
    }
}