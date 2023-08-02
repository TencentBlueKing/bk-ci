package com.tencent.devops.notify.wework.config

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.notify.dao.WeworkNotifyDao
import com.tencent.devops.notify.service.WeworkService
import com.tencent.devops.notify.wework.service.inner.WeworkServiceImpl
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Suppress("LongParameterList")
@Configuration
@ConditionalOnProperty(prefix = "notify", name = ["weworkChannel"], havingValue = "weworkAgent")
class WeworkBeanConfiguration {

    @Bean
    fun weworkConfiguration(
        @Value("\${wework.corpId:}")
        corpId: String,
        @Value("\${wework.corpSecret:}")
        corpSecret: String,
        @Value("\${wework.apiUrl:https://qyapi.weixin.qq.com}")
        apiUrl: String,
        @Value("\${wework.agentId:}")
        agentId: String,
        @Value("\${wework.tempDirectory:}")
        tempDirectory: String,
        /**
         *  表示是否是保密消息，0表示可对外分享，1表示不能分享且内容显示水印，默认为0
         */
        @Value("\${wework.safe:#{null}}")
        safe: String? = null,
        /**
         * 表示是否开启重复消息检查，0表示否，1表示是，默认0
         */
        @Value("\${wework.enableDuplicateCheck:#{null}}")
        enableDuplicateCheck: String? = null,
        /**
         *  表示是否重复消息检查的时间间隔，默认1800s，最大不超过4小时
         */
        @Value("\${wework.duplicateCheckInterval:#{null}}")
        duplicateCheckInterval: String? = null,
        /**
         *  表示是否开启id转译，0表示否，1表示是，默认0。仅第三方应用需要用到，企业自建应用可以忽略。
         */
        @Value("\${wework.enableIdTrans:#{null}}")
        enableIdTrans: String? = null
    ) = WeworkConfiguration(
        corpId = corpId,
        corpSecret = corpSecret,
        apiUrl = apiUrl,
        agentId = agentId,
        tempDirectory = tempDirectory,
        safe = safe,
        enableDuplicateCheck = enableDuplicateCheck,
        duplicateCheckInterval = duplicateCheckInterval,
        enableIdTrans = enableIdTrans
    )

    @Bean
    fun weworkService(
        @Autowired weworkConfiguration: WeworkConfiguration,
        @Autowired weworkNotifyDao: WeworkNotifyDao,
        @Autowired rabbitTemplate: RabbitTemplate,
        @Autowired redisOperation: RedisOperation
    ): WeworkService =
        WeworkServiceImpl(weworkConfiguration, weworkNotifyDao, rabbitTemplate, redisOperation)
}
