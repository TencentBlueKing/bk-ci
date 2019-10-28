package com.tencent.devops.store

import com.tencent.devops.store.service.common.impl.BkStoreNotifyServiceImpl
import com.tencent.devops.store.service.common.impl.BkStoreUserServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BkServiceConfig {

    @Bean
    fun storeUserService() = BkStoreUserServiceImpl()

    @Bean
    fun storeNotifyService() = BkStoreNotifyServiceImpl()
}