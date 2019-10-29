package com.tencent.devops.store

import com.tencent.devops.store.service.atom.impl.BkAtomMemberServiceImpl
import com.tencent.devops.store.service.atom.impl.BkAtomNotifyServiceImpl
import com.tencent.devops.store.service.atom.impl.BkAtomReleaseServiceImpl
import com.tencent.devops.store.service.atom.impl.BkAtomServiceImpl
import com.tencent.devops.store.service.atom.impl.BkMarketAtomServiceImpl
import com.tencent.devops.store.service.common.impl.BkStoreNotifyServiceImpl
import com.tencent.devops.store.service.common.impl.BkStoreUserServiceImpl
import com.tencent.devops.store.service.template.impl.BkMarketTemplateServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class BkServiceConfig {

    @Bean
    fun storeUserService() = BkStoreUserServiceImpl()

    @Bean
    fun storeNotifyService() = BkStoreNotifyServiceImpl()

    @Bean
    fun atomService() = BkAtomServiceImpl()

    @Bean
    fun marketAtomService() = BkMarketAtomServiceImpl()

    @Bean
    fun atomMemberService() = BkAtomMemberServiceImpl()

    @Bean
    fun atomReleaseService() = BkAtomReleaseServiceImpl()

    @Bean
    fun atomNotifyService() = BkAtomNotifyServiceImpl()

    @Bean
    fun marketTemplateService() = BkMarketTemplateServiceImpl()
}