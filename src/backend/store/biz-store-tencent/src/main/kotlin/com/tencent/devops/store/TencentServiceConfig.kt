package com.tencent.devops.store

import com.tencent.devops.store.service.atom.impl.TxAtomMemberServiceImpl
import com.tencent.devops.store.service.atom.impl.TxAtomNotifyServiceImpl
import com.tencent.devops.store.service.atom.impl.TxAtomReleaseServiceImpl
import com.tencent.devops.store.service.atom.impl.TxAtomServiceImpl
import com.tencent.devops.store.service.atom.impl.TxMarketAtomServiceImpl
import com.tencent.devops.store.service.common.impl.TxStoreNotifyServiceImpl
import com.tencent.devops.store.service.common.impl.TxStoreUserServiceImpl
import com.tencent.devops.store.service.template.impl.TxMarketTemplateServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TencentServiceConfig {

    @Bean
    fun storeUserService() = TxStoreUserServiceImpl()

    @Bean
    fun storeNotifyService() = TxStoreNotifyServiceImpl()

    @Bean
    fun atomService() = TxAtomServiceImpl()

    @Bean
    fun marketAtomService() = TxMarketAtomServiceImpl()

    @Bean
    fun atomMemberService() = TxAtomMemberServiceImpl()

    @Bean
    fun atomReleaseService() = TxAtomReleaseServiceImpl()

    @Bean
    fun atomNotifyService() = TxAtomNotifyServiceImpl()

    @Bean
    fun marketTemplateService() = TxMarketTemplateServiceImpl()
}