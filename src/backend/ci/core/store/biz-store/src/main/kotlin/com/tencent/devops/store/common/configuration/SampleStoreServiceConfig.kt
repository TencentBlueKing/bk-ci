/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.common.configuration

import com.tencent.devops.store.atom.service.AtomNotifyService
import com.tencent.devops.store.atom.service.impl.AtomCooperationServiceImpl
import com.tencent.devops.store.atom.service.impl.AtomMemberServiceImpl
import com.tencent.devops.store.atom.service.impl.SampleAtomCooperationServiceImpl
import com.tencent.devops.store.atom.service.impl.SampleAtomMemberServiceImpl
import com.tencent.devops.store.atom.service.impl.SampleAtomNotifyServiceImpl
import com.tencent.devops.store.common.service.StoreNotifyService
import com.tencent.devops.store.common.service.StoreUserService
import com.tencent.devops.store.common.service.impl.ContainerServiceImpl
import com.tencent.devops.store.common.service.impl.SampleContainerServiceImpl
import com.tencent.devops.store.common.service.impl.SampleStoreCommonServiceImpl
import com.tencent.devops.store.common.service.impl.SampleStoreLogoServiceImpl
import com.tencent.devops.store.common.service.impl.SampleStoreNotifyServiceImpl
import com.tencent.devops.store.common.service.impl.SampleStoreUserServiceImpl
import com.tencent.devops.store.common.service.impl.StoreCommonServiceImpl
import com.tencent.devops.store.common.service.impl.StoreLogoServiceImpl
import com.tencent.devops.store.template.service.TemplateNotifyService
import com.tencent.devops.store.template.service.impl.MarketTemplateServiceImpl
import com.tencent.devops.store.template.service.impl.SampleMarketTemplateServiceImpl
import com.tencent.devops.store.template.service.impl.SampleTemplateNotifyServiceImpl
import com.tencent.devops.store.template.service.impl.SampleTemplateReleaseServiceImpl
import com.tencent.devops.store.template.service.impl.TemplateReleaseServiceImpl
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
    @ConditionalOnMissingBean(AtomMemberServiceImpl::class)
    fun atomMemberService() = SampleAtomMemberServiceImpl()

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
