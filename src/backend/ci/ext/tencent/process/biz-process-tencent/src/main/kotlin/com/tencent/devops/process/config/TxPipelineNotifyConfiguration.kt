package com.tencent.devops.process.config

import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.wechatwork.WechatWorkRobotService
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.process.service.notify.TxNotifySendGroupMsgCmdImpl
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered

@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class TxPipelineNotifyConfiguration {
    @Bean
    @Primary
    fun notifySendCmd(
        client: Client,
        bsAuthProjectApi: AuthProjectApi,
        bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
        wechatWorkService: WechatWorkService,
        wechatWorkRobotService: WechatWorkRobotService
    ) = TxNotifySendGroupMsgCmdImpl(
        client = client,
        bsAuthProjectApi = bsAuthProjectApi,
        bsPipelineAuthServiceCode = bsPipelineAuthServiceCode,
        wechatWorkService = wechatWorkService,
        wechatWorkRobotService = wechatWorkRobotService
    )
}
