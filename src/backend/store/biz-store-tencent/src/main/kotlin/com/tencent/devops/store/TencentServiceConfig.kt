package com.tencent.devops.store

import com.tencent.devops.store.service.common.impl.TxStoreNotifyServiceImpl
import com.tencent.devops.store.service.common.impl.TxStoreUserServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TencentServiceConfig {

    @Bean
    fun storeUserService() = TxStoreUserServiceImpl()

    @Bean
    fun storeNotifyService() = TxStoreNotifyServiceImpl()
}