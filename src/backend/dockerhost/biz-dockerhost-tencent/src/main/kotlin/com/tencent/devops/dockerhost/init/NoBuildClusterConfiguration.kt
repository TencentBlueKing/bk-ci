package com.tencent.devops.dockerhost.init

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQEventDispatcher
import com.tencent.devops.dockerhost.config.TXDockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import com.tencent.devops.dockerhost.listener.BuildLessStartListener
import com.tencent.devops.dockerhost.listener.BuildLessStopListener
import com.tencent.devops.dockerhost.service.DockerHostBuildLessService
import com.tencent.devops.dockerhost.service.DockerHostWorkSpaceService
import com.tencent.devops.dockerhost.utils.CommonUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.IntervalTask
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import kotlin.system.exitProcess

/**
 * 无构建环境的docker集群下才会生效
 * @version 1.0
 */

@Configuration
@ConditionalOnProperty(prefix = "run", name = ["mode"], havingValue = "docker_no_build")
@EnableScheduling
class NoBuildClusterConfiguration : SchedulingConfigurer {

    @Autowired
    private lateinit var dockerHostBuildLessService: DockerHostBuildLessService

    override fun configureTasks(scheduledTaskRegistrar: ScheduledTaskRegistrar) {
        // 5分钟清理一次已经退出的容器
        scheduledTaskRegistrar.addFixedRateTask(
            IntervalTask(
                Runnable { dockerHostBuildLessService.clearContainers() }, 300 * 1000, 180 * 1000
            )
        )
        scheduledTaskRegistrar.addFixedRateTask(
            IntervalTask(
                Runnable { dockerHostBuildLessService.endBuild() }, 20 * 1000, 120 * 1000
            )
        )
    }

    private val dockerHostBuildApi: DockerHostBuildResourceApi = DockerHostBuildResourceApi()

    @Bean
    fun pipelineEventDispatcher(rabbitTemplate: RabbitTemplate) = MQEventDispatcher(rabbitTemplate)

    @Bean
    fun dockerHostBuildLessService(
        dockerHostConfig: TXDockerHostConfig,
        pipelineEventDispatcher: PipelineEventDispatcher,
        dockerHostWorkSpaceService: DockerHostWorkSpaceService
    ): DockerHostBuildLessService {
        return DockerHostBuildLessService(dockerHostConfig, pipelineEventDispatcher, dockerHostWorkSpaceService)
    }

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun exchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_BUILD_LESS_AGENT_LISTENER_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun buildStartQueue(): Queue {
        val hostTag = CommonUtils.getInnerIP()
        logger.info("[Init]| hostTag=$hostTag")
        val result = dockerHostBuildApi.getHost(hostTag)
        if (result == null) {
            logger.error("[Init]| hostTag=$hostTag fail exit!")
            exitProcess(199)
        }
        val hostInfo = result.data
        if (hostInfo == null) {
            logger.error("[Init]| hostTag=$hostTag  hostInfo is null, exit!")
            exitProcess(198)
        }
        return Queue(MQ.QUEUE_BUILD_LESS_AGENT_STARTUP_PREFFIX + hostInfo.routeKey)
    }

    @Bean
    fun buildStartBind(@Autowired buildStartQueue: Queue, @Autowired exchange: DirectExchange): Binding {
        return BindingBuilder.bind(buildStartQueue).to(exchange).with(buildStartQueue.name)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun startListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildStartQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildLessStartListener: BuildLessStartListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(buildStartQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(50)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter =
            MessageListenerAdapter(buildLessStartListener, buildLessStartListener::handleMessage.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        return container
    }

    @Bean
    fun buildLessStartListener(dockerHostBuildLessService: DockerHostBuildLessService) =
        BuildLessStartListener(dockerHostBuildLessService)

    @Bean
    fun buildStopQueue(): Queue {
        val hostTag = CommonUtils.getInnerIP()
        logger.info("[Init]| hostTag=$hostTag")
//        val result = dockerHostBuildApi.getHost(hostTag)
//        if (result == null) {
//            logger.error("[Init]| hostTag=$hostTag fail exit!")
//            System.exit(199)
//        }
//        val hostInfo = result!!.data
//        if (hostInfo == null) {
//            logger.error("[Init]| hostTag=$hostTag  hostInfo is null, exit!")
//            System.exit(198)
//        }
        return Queue(MQ.QUEUE_BUILD_LESS_AGENT_SHUTDOWN_PREFFIX + hostTag)
    }

    @Bean
    fun buildShutdownBind(@Autowired buildStopQueue: Queue, @Autowired exchange: DirectExchange): Binding {
        return BindingBuilder.bind(buildStopQueue).to(exchange).with(buildStopQueue.name)
    }

    @Bean
    fun stopListener(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired buildStopQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired buildLessStopListener: BuildLessStopListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer(connectionFactory)
        container.setQueueNames(buildStopQueue.name)
        container.setConcurrentConsumers(10)
        container.setMaxConcurrentConsumers(20)
        container.setStartConsumerMinInterval(1)
        container.setConsecutiveActiveTrigger(1)
        container.setRabbitAdmin(rabbitAdmin)
        container.setMismatchedQueuesFatal(true)
        val messageListenerAdapter =
            MessageListenerAdapter(buildLessStopListener, buildLessStopListener::handleMessage.name)
        messageListenerAdapter.setMessageConverter(messageConverter)
        container.messageListener = messageListenerAdapter
        return container
    }

    @Bean
    fun buildLessStopListener(dockerHostBuildLessService: DockerHostBuildLessService) =
        BuildLessStopListener(dockerHostBuildLessService)

    companion object {
        private val logger = LoggerFactory.getLogger(NoBuildClusterConfiguration::class.java)
    }
}
