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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store

import com.tencent.devops.store.service.atom.impl.SampleAtomCooperationServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleAtomMemberServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleAtomNotifyServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleAtomReleaseServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleAtomServiceImpl
import com.tencent.devops.store.service.atom.impl.SampleMarketAtomServiceImpl
import com.tencent.devops.store.service.common.impl.SampleStoreLogoServiceImpl
import com.tencent.devops.store.service.common.impl.SampleStoreNotifyServiceImpl
import com.tencent.devops.store.service.common.impl.SampleStoreUserServiceImpl
import com.tencent.devops.store.service.container.impl.SampleContainerServiceImpl
import com.tencent.devops.store.service.template.impl.SampleMarketTemplateServiceImpl
import com.tencent.devops.store.service.template.impl.SampleTemplateNotifyServiceImpl
import com.tencent.devops.store.service.template.impl.SampleTemplateReleaseServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SampleStoreServiceConfig {

    @Bean
    fun containerService() = SampleContainerServiceImpl()

    @Bean
    fun storeUserService() = SampleStoreUserServiceImpl()

    @Bean
    fun storeNotifyService() = SampleStoreNotifyServiceImpl()

    @Bean
    fun atomService() = SampleAtomServiceImpl()

    @Bean
    fun marketAtomService() = SampleMarketAtomServiceImpl()

    @Bean
    fun atomMemberService() = SampleAtomMemberServiceImpl()

    @Bean
    fun atomReleaseService() = SampleAtomReleaseServiceImpl()

    @Bean
    fun atomNotifyService() = SampleAtomNotifyServiceImpl()

    @Bean
    fun atomCooperationService() = SampleAtomCooperationServiceImpl()

    @Bean
    fun templateNotifyService() = SampleTemplateNotifyServiceImpl()

    @Bean
    fun marketTemplateService() = SampleMarketTemplateServiceImpl()

    @Bean
    fun templateReleaseService() = SampleTemplateReleaseServiceImpl()

    @Bean
    fun storeLogoService() = SampleStoreLogoServiceImpl()
}