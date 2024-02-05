/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
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

package com.tencent.devops.store.configuration

import com.tencent.devops.store.service.atom.AtomNotifyService
import com.tencent.devops.store.service.atom.impl.AtomCooperationServiceImpl
import com.tencent.devops.store.service.atom.impl.AtomMemberServiceImpl
import com.tencent.devops.store.service.atom.impl.AtomReleaseServiceImpl
import com.tencent.devops.store.service.atom.impl.AtomServiceImpl
import com.tencent.devops.store.service.atom.impl.MarketAtomServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleAtomCooperationServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleAtomMemberServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleAtomNotifyServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleAtomReleaseServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleAtomServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleMarketAtomServiceImpl
import com.tencent.devops.store.service.common.StoreNotifyService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.common.impl.StoreCommonServiceImpl
import com.tencent.devops.store.service.common.impl.StoreLogoServiceImpl
import com.tencent.devops.store.service.common.sample.impl.SampleStoreCommonServiceImpl
import com.tencent.devops.store.service.common.sample.impl.SampleStoreLogoServiceImpl
import com.tencent.devops.store.service.common.sample.impl.SampleStoreNotifyServiceImpl
import com.tencent.devops.store.service.common.sample.impl.SampleStoreUserServiceImpl
import com.tencent.devops.store.service.container.impl.ContainerServiceImpl
import com.tencent.devops.store.service.container.impl.SampleContainerServiceImpl
import com.tencent.devops.store.service.template.TemplateNotifyService
import com.tencent.devops.store.service.template.impl.MarketTemplateServiceImpl
import com.tencent.devops.store.service.template.impl.SampleMarketTemplateServiceImpl
import com.tencent.devops.store.service.template.impl.SampleTemplateNotifyServiceImpl
import com.tencent.devops.store.service.template.impl.SampleTemplateReleaseServiceImpl
import com.tencent.devops.store.service.template.impl.TemplateReleaseServiceImpl
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@Suppress("ALL")
class SampleStoreServiceConfig {

    @Bean
    @ConditionalOnMissingBean(ContainerServiceImpl::class)
    fun containerService() = SampleContainerServiceImpl()

    @Bean
    @ConditionalOnMissingBean(StoreUserService::class)
    fun storeUserService() = SampleStoreUserServiceImpl()

    @Bean
    @ConditionalOnMissingBean(StoreNotifyService::class)
    fun storeNotifyService() = SampleStoreNotifyServiceImpl()

    @Bean
    @ConditionalOnMissingBean(AtomServiceImpl::class)
    fun atomService() = SampleAtomServiceImpl()

    @Bean
    @ConditionalOnMissingBean(MarketAtomServiceImpl::class)
    fun marketAtomService() = SampleMarketAtomServiceImpl()

    @Bean
    @ConditionalOnMissingBean(AtomMemberServiceImpl::class)
    fun atomMemberService() = SampleAtomMemberServiceImpl()

    @Bean
    @ConditionalOnMissingBean(AtomReleaseServiceImpl::class)
    fun atomReleaseService() = SampleAtomReleaseServiceImpl()

    @Bean
    @ConditionalOnMissingBean(AtomNotifyService::class)
    fun atomNotifyService() = SampleAtomNotifyServiceImpl()

    @Bean
    @ConditionalOnMissingBean(AtomCooperationServiceImpl::class)
    fun atomCooperationService() = SampleAtomCooperationServiceImpl()

    @Bean
    @ConditionalOnMissingBean(TemplateNotifyService::class)
    fun templateNotifyService() = SampleTemplateNotifyServiceImpl()

    @Bean
    @ConditionalOnMissingBean(MarketTemplateServiceImpl::class)
    fun marketTemplateService() = SampleMarketTemplateServiceImpl()

    @Bean
    @ConditionalOnMissingBean(TemplateReleaseServiceImpl::class)
    fun templateReleaseService() = SampleTemplateReleaseServiceImpl()

    @Bean
    @ConditionalOnMissingBean(StoreLogoServiceImpl::class)
    fun storeLogoService() = SampleStoreLogoServiceImpl()

    @Bean
    @ConditionalOnMissingBean(StoreCommonServiceImpl::class)
    fun storeCommonService() = SampleStoreCommonServiceImpl()
}
